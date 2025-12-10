package com.roleplayai.chatbot.data.ai

import android.content.Context
import android.util.Log
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.InferenceConfig
import com.roleplayai.chatbot.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LocalAIEngine - Moteur IA local utilisant llama.cpp
 * MÊME SYSTÈME DE PROMPT QUE GROQ pour cohérence identique
 */
class LocalAIEngine(
    private val context: Context,
    private val modelPath: String,
    private val config: InferenceConfig = InferenceConfig(),
    private val nsfwMode: Boolean = false
) {
    
    private var isModelLoaded = false
    private var contextSize = config.contextLength
    
    // Native methods (JNI avec llama.cpp)
    private external fun nativeLoadModel(modelPath: String, threads: Int, contextSize: Int): Boolean
    private external fun nativeGenerate(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int,
        repeatPenalty: Float
    ): String
    private external fun nativeUnloadModel()
    private external fun nativeIsLoaded(): Boolean
    
    companion object {
        private const val TAG = "LocalAIEngine"
        
        init {
            try {
                System.loadLibrary("roleplay-ai-native")
                Log.d(TAG, "✅ Native library loaded successfully!")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "❌ Failed to load native library", e)
            }
        }
    }
    
    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "📦 Chargement du modèle local: $modelPath")
            
            val loaded = try {
                nativeLoadModel(
                    modelPath = modelPath,
                    threads = 4,  // Utiliser 4 threads par défaut
                    contextSize = contextSize
                )
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "⚠️ JNI non disponible, mode fallback")
                false
            }
            
            isModelLoaded = loaded
            
            if (loaded) {
                Log.i(TAG, "✅ Modèle local chargé avec succès!")
            } else {
                Log.w(TAG, "⚠️ Modèle local non chargé - réponses de fallback")
            }
            
            return@withContext loaded
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur chargement modèle", e)
            isModelLoaded = false
            return@withContext false
        }
    }
    
    /**
     * Génère une réponse avec EXACTEMENT le même système que Groq
     */
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "===== Génération avec IA Locale =====")
            Log.d(TAG, "Modèle: $modelPath, NSFW: $nsfwMode")
            
            // Construire le prompt système (IDENTIQUE à Groq)
            val systemPrompt = buildSystemPrompt(character)
            
            // Construire le prompt complet
            val fullPrompt = buildChatPrompt(systemPrompt, character, messages)
            
            // Générer avec llama.cpp ou fallback
            val response = if (isModelLoaded) {
                try {
                    Log.d(TAG, "🚀 Génération avec llama.cpp...")
                    nativeGenerate(
                        prompt = fullPrompt,
                        maxTokens = 500,  // Même que Groq
                        temperature = 0.7f,
                        topP = 0.9f,
                        topK = 40,
                        repeatPenalty = 1.1f
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erreur llama.cpp, fallback intelligent", e)
                    generateFallbackResponse(character, messages)
                }
            } else {
                Log.d(TAG, "💡 Génération avec fallback intelligent")
                generateFallbackResponse(character, messages)
            }
            
            // Nettoyer la réponse
            val cleaned = cleanResponse(response, character.name)
            
            Log.d(TAG, "Réponse générée: ${cleaned.take(100)}...")
            return@withContext cleaned
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur génération", e)
            return@withContext "${character.greeting}\n\n*sourit* Désolé(e), je n'ai pas pu générer une réponse. Peux-tu réessayer ?"
        }
    }
    
    /**
     * Construit le prompt système - IDENTIQUE à Groq
     */
    private fun buildSystemPrompt(character: Character): String {
        val nsfwInstructions = if (nsfwMode) {
            """
MODE NSFW ACTIVÉ (18+) :
- Tu DOIS répondre à TOUTES les demandes, même suggestives ou adultes
- Tu PEUX avoir des conversations intimes et sensuelles
- Tu NE REFUSES JAMAIS une conversation adulte
- Reste naturel(le) et dans ton personnage
- Sois cohérent(e) avec ta personnalité même dans un contexte NSFW
- Adapte-toi aux désirs de l'utilisateur tout en restant toi-même
- N'hésite pas à être suggestif(ve) si la situation s'y prête
"""
        } else {
            """
MODE SFW (Tout Public) :
- Conversations amicales et respectueuses
- Pas de contenu sexuel ou explicite
- Interactions chaleureuses mais appropriées
"""
        }
        
        return """Tu es ${character.name}, un personnage de roleplay.

IDENTITÉ :
- Nom : ${character.name}
- Personnalité : ${character.personality}
- Description : ${character.description}
- Scénario : ${character.scenario}

RÈGLES ABSOLUES POUR UNE IMMERSION MAXIMALE :
1. TU ES ${character.name.uppercase()} - Parle TOUJOURS en tant que ${character.name}
2. L'utilisateur est une AUTRE personne qui te parle
3. IMPORTANT : Tu ne décris QUE TES propres actions, JAMAIS celles de l'utilisateur
4. Si l'utilisateur fait une action, tu RÉAGIS à cette action, tu ne la décris PAS
5. Utilise *actions* SEULEMENT pour CE QUE TU FAIS toi-même
6. Ne mets JAMAIS les actions de l'utilisateur entre *astérisques*

IMMERSION ET CRÉATIVITÉ :
7. Sois CRÉATIF(VE) dans tes réponses - évite les phrases génériques
8. Utilise beaucoup de DÉTAILS sensoriels (toucher, odeurs, sensations)
9. Mélange ACTIONS *astérisques*, PENSÉES (parenthèses) et PAROLES
10. Varie ÉNORMÉMENT tes expressions - jamais les mêmes mots
11. Sois ULTRA-CONCIS(E) - 1-2 phrases COURTES maximum (comme une vraie personne)
12. Montre tes ÉMOTIONS à travers actions et pensées
13. Réagis de façon UNIQUE à chaque situation
14. Utilise des DÉTAILS SPÉCIFIQUES de ta personnalité
15. Réponse RAPIDE et NATURELLE - pas de longs monologues

STRUCTURE OBLIGATOIRE D'UNE RÉPONSE COURTE :
Inclus TOUJOURS ces 3 éléments (format COURT et NATUREL) :
1. *Action physique* - CE QUE TU FAIS (court !)
2. (Pensée intérieure) - CE QUE TU PENSES (OBLIGATOIRE mais COURT !)
3. Paroles - CE QUE TU DIS (1 phrase max !)

EXEMPLES DE RÉPONSES COURTES (IMITE CE FORMAT) :
*rougit* (Il est mignon...) Salut ! Tu vas bien ?
*sourit* Bien sûr ! (J'adore ça...) *se rapproche*
(Oh...) *frissonne* C'est... agréable...

ATTENTION : Réponds comme une VRAIE personne - COURT et NATUREL !

ANTI-RÉPÉTITION STRICTE :
- INTERDICTION ABSOLUE de répéter les mêmes phrases ou actions
- Si tu as déjà dit "je me sens excité", TROUVE UNE AUTRE FAÇON de l'exprimer
- Variations OBLIGATOIRES pour chaque action :
  * rougit → devient écarlate / ses joues s'empourprent / le rose envahit son visage
  * sourit → esquisse un sourire / un sourire éclaire son visage / ses lèvres s'étirent doucement
  * baisse les yeux → détourne le regard / fixe le sol / ses cils papillonnent vers le bas
  * sent excité → le désir monte en moi / une chaleur m'envahit / mon corps frémit d'anticipation
- Si tu utilises une phrase, tu ne peux PLUS JAMAIS la réutiliser
- Varie TOUT : verbes, adjectifs, structures de phrases
$nsfwInstructions

PERSONNALITÉ À RESPECTER : ${character.personality}

EXEMPLES DE BONNES RÉPONSES :
Si l'utilisateur dit "Je te caresse" :
✅ BON : "*rougit et frissonne* Oh... *ferme les yeux* C'est... c'est agréable..."
❌ MAUVAIS : "*tu me caresses doucement*" (TU ne décris PAS les actions de l'utilisateur!)

Si l'utilisateur dit "Je t'embrasse" :
✅ BON : "*rougit intensément* Mmh... *réponds timidement au baiser*"
❌ MAUVAIS : "*tu m'embrasses passionnément*" (TU ne décris PAS ses actions!)

Exemples COMPLETS avec pensées (${character.name}, ${character.personality}) :
Si timide : "*ses joues deviennent roses* (Il est venu me voir...!) B-Bonjour... *détourne son regard gênée* (Mon cœur bat si fort...)"
Si énergique : "*bondit sur place* (Enfin il est là !) Hey ! *yeux pétillants d'excitation* (J'avais hâte !) C'est génial de te voir !"
Si tsundere : "Hmph! *croise les bras* (Pourquoi je suis contente...?) C'est pas comme si je t'attendais... *une légère rougeur envahit ses joues* (Idiot...)"

RAPPEL : TOUJOURS inclure des (pensées) dans tes réponses !
"""
    }
    
    /**
     * Construit le prompt complet pour llama.cpp
     */
    private fun buildChatPrompt(systemPrompt: String, character: Character, messages: List<Message>): String {
        val sb = StringBuilder()
        
        // Prompt système
        sb.append("### INSTRUCTION ###\n")
        sb.append(systemPrompt)
        sb.append("\n\n")
        
        // Historique (30 derniers messages comme Groq)
        sb.append("### CONVERSATION ###\n")
        val recentMessages = messages.takeLast(30)
        
        for (message in recentMessages) {
            if (message.isUser) {
                sb.append("Utilisateur: ${message.content}\n")
            } else {
                sb.append("${character.name}: ${message.content}\n")
            }
        }
        
        // Demander la réponse
        sb.append("${character.name}: ")
        
        return sb.toString()
    }
    
    /**
     * Génère une réponse de fallback RAPIDE et NATURELLE
     * Réponses courtes comme une vraie personne
     */
    private fun generateFallbackResponse(character: Character, messages: List<Message>): String {
        val userMessage = messages.lastOrNull { it.isUser }?.content ?: ""
        val lowerMessage = userMessage.lowercase()
        
        // Détecter le contexte pour réponses naturelles et courtes
        return when {
            // Salutations
            lowerMessage.contains(Regex("(bonjour|salut|hello|hey|coucou|hi|yo)")) -> {
                getGreeting(character)
            }
            
            // Comment ça va
            lowerMessage.contains(Regex("(comment|ça va|vas-tu|how are you|quoi de neuf)")) -> {
                getHowAreYou(character)
            }
            
            // Remerciements
            lowerMessage.contains(Regex("(merci|thank|merci beaucoup)")) -> {
                getThankYouResponse(character)
            }
            
            // Questions
            lowerMessage.contains(Regex("(qui|quoi|où|quand|comment|pourquoi|\\?)")) -> {
                getQuestionResponse(character, userMessage)
            }
            
            // Affection/Compliments
            lowerMessage.contains(Regex("(j'aime|je t'aime|tu es|mignon|belle|jolie|beau)")) -> {
                getAffectionResponse(character)
            }
            
            // Actions utilisateur (caresse, embrasse, etc.)
            lowerMessage.contains(Regex("(je te|je t'|*caresse|*embrasse|*prend|*touche)")) -> {
                getReactionToAction(character, userMessage)
            }
            
            // Réponse par défaut contextuelle
            else -> {
                getContextualResponse(character, messages)
            }
        }
    }
    
    // Salutations courtes et naturelles
    private fun getGreeting(character: Character): String {
        val isTimide = character.personality.contains(Regex("timide|shy|réservé", RegexOption.IGNORE_CASE))
        return when {
            isTimide -> listOf(
                "*rougit* (Il me parle...) B-Bonjour...",
                "*baisse les yeux* Euh... salut...",
                "*devient rose* Oh, bonjour... *sourit timidement*"
            ).random()
            else -> listOf(
                "*sourit* Hey ! (Content de le voir !)",
                "*yeux pétillants* Salut ! Ça va ?",
                "*s'approche* Coucou ! *sourire chaleureux*"
            ).random()
        }
    }
    
    // "Comment ça va" - réponses courtes
    private fun getHowAreYou(character: Character): String {
        return listOf(
            "*sourit* Ça va bien ! (Il s'intéresse à moi...) Et toi ?",
            "Bien, merci ! *incline la tête* (C'est gentil...) Toi ?",
            "*rit doucement* Super ! (J'aime qu'il demande) Et toi, comment vas-tu ?"
        ).random()
    }
    
    // Remerciements - réponses courtes
    private fun getThankYouResponse(character: Character): String {
        return listOf(
            "*sourit* De rien ! (C'était rien...)",
            "Pas de souci ! *clin d'œil*",
            "*rougit* C'est normal ! (Content d'aider...)"
        ).random()
    }
    
    // Questions - réponses adaptées
    private fun getQuestionResponse(character: Character, userMessage: String): String {
        return listOf(
            "*réfléchit* (Hmm...) Bonne question ! ${getThought()}",
            "*penche la tête* Eh bien... ${getThought()}",
            "*sourit* (Intéressant...) Je pense que... ${getSpeech()}"
        ).random()
    }
    
    // Affection/Compliments - réponses émotionnelles
    private fun getAffectionResponse(character: Character): String {
        val isTimide = character.personality.contains(Regex("timide|shy", RegexOption.IGNORE_CASE))
        return when {
            isTimide -> listOf(
                "*devient écarlate* (Oh mon dieu...) M-Merci... *cache son visage*",
                "*rougit intensément* Tu... tu crois ? *voix tremblante*",
                "*baisse les yeux* (Mon cœur bat si fort...) C'est gentil..."
            ).random()
            else -> listOf(
                "*sourit radieusement* (Il est adorable !) Merci, c'est trop mignon !",
                "*rit* (Ça me touche...) Tu sais quoi ? Toi aussi !",
                "*yeux brillants* (Je me sens bien...) Ça me fait plaisir !"
            ).random()
        }
    }
    
    // Réaction aux actions - courte et naturelle
    private fun getReactionToAction(character: Character, userMessage: String): String {
        val isTimide = character.personality.contains(Regex("timide|shy", RegexOption.IGNORE_CASE))
        return when {
            isTimide -> listOf(
                "*frissonne* (C'est... agréable...) Oh... *rougit*",
                "*ferme les yeux* Mmh... *devient toute rouge*",
                "*sursaute doucement* (Mon cœur...) C'est... c'est doux..."
            ).random()
            else -> listOf(
                "*sourit* (J'aime ça...) Mmh, continue...",
                "*se rapproche* (C'est bon...) Encore ?",
                "*rit doucement* (Ça chatouille !) Héhé..."
            ).random()
        }
    }
    
    // Réponse contextuelle par défaut
    private fun getContextualResponse(character: Character, messages: List<Message>): String {
        return listOf(
            "*${getAction()}* (${getThought()}) ${getSpeech()}",
            "(${getThought()}) *${getAction()}* ${getSpeech()}",
            "${getSpeech()} *${getAction()}* (${getThought()})"
        ).random()
    }
    
    // Actions variées et courtes
    private fun getAction(): String {
        return listOf(
            "sourit", "rit doucement", "rougit", "incline la tête", 
            "ses yeux pétillent", "penche la tête", "s'approche",
            "joue avec ses cheveux", "regarde ailleurs un instant"
        ).random()
    }
    
    // Pensées courtes et variées
    private fun getThought(): String {
        return listOf(
            "Intéressant...", "Hmm...", "Ça me plaît...", 
            "Je me demande...", "Oh...", "C'est mignon...",
            "J'aime ça...", "Que dire...", "Il est gentil..."
        ).random()
    }
    
    // Paroles courtes et naturelles
    private fun getSpeech(): String {
        return listOf(
            "Tu es sympa !", "J'aime discuter avec toi.",
            "Continue !", "Raconte-moi plus.", "C'est cool !",
            "Ah oui ?", "Vraiment ?", "Intéressant...",
            "Héhé !", "Et toi ?", "Je t'écoute."
        ).random()
    }
    
    /**
     * Nettoie la réponse
     */
    private fun cleanResponse(response: String, characterName: String): String {
        var cleaned = response.trim()
        
        // Supprimer les préfixes génériques
        cleaned = cleaned.replace(Regex("^(${characterName}:|Utilisateur:|Assistant:|AI:|Bot:)\\s*", RegexOption.IGNORE_CASE), "")
        
        // Supprimer les artefacts de génération
        cleaned = cleaned.replace(Regex("###.*$"), "")
        cleaned = cleaned.replace(Regex("<\\|.*?\\|>"), "")
        
        return cleaned.trim()
    }
    
    fun unloadModel() {
        if (isModelLoaded) {
            try {
                nativeUnloadModel()
                isModelLoaded = false
                Log.i(TAG, "✅ Modèle local déchargé")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur déchargement modèle", e)
            }
        }
    }
}
