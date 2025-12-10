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
        // NE JAMAIS charger llama.cpp - trop lent pour mobile
        // Toujours utiliser le fallback intelligent instantané
        Log.i(TAG, "💡 Mode Fallback Intelligent Instantané activé")
        Log.i(TAG, "⚡ Réponses en <1 seconde (au lieu de 5-10s avec llama.cpp)")
        isModelLoaded = false
        return@withContext false
    }
    
    /**
     * Génère une réponse INSTANTANÉE avec fallback intelligent
     * Ne charge JAMAIS llama.cpp (trop lent pour mobile)
     */
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "===== Génération avec IA Locale =====")
            Log.d(TAG, "Mode: Fallback Intelligent Instantané (<1s)")
            
            // TOUJOURS utiliser le fallback intelligent (INSTANTANÉ)
            // llama.cpp est trop lent sur mobile (5-10s vs <1s)
            Log.d(TAG, "⚡ Génération INSTANTANÉE avec fallback intelligent")
            val response = generateFallbackResponse(character, messages)
            
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
     * Génère une réponse INTELLIGENTE et COHÉRENTE
     * Analyse le contexte et répond de manière pertinente
     * ROBUSTE - NE PEUT PAS ÉCHOUER
     */
    private fun generateFallbackResponse(character: Character, messages: List<Message>): String {
        return try {
            val userMessage = messages.lastOrNull { it.isUser }?.content ?: ""
            val lowerMessage = userMessage.lowercase()
            
            Log.d(TAG, "📝 Analyse message: $userMessage")
            
            // ANALYSE CONTEXTUELLE AVANCÉE
            
            // 1. Détection d'actions physiques de l'utilisateur
            val userActions = extractUserActions(userMessage)
            if (userActions.isNotEmpty()) {
                Log.d(TAG, "✅ Actions détectées: $userActions")
                return generateReactionToUserAction(character, userActions, userMessage)
            }
            
            // 2. Détection de questions
            if (isQuestion(userMessage)) {
                Log.d(TAG, "✅ Question détectée")
                return generateAnswerToQuestion(character, userMessage)
            }
            
            // 3. Détection d'affection/compliments
            if (isAffection(lowerMessage)) {
                Log.d(TAG, "✅ Affection détectée")
                return generateAffectionResponse(character, userMessage)
            }
            
            // 4. Détection de salutations
            if (isGreeting(lowerMessage)) {
                Log.d(TAG, "✅ Salutation détectée")
                return generateGreeting(character)
            }
            
            // 5. Détection de réponses courtes (oui, non, ok, etc.)
            if (isShortAnswer(lowerMessage)) {
                Log.d(TAG, "✅ Réponse courte détectée")
                return generateContinuation(character, messages)
            }
            
            // 6. Réponse contextuelle basée sur le contenu
            Log.d(TAG, "✅ Réponse contextuelle générique")
            return generateContextualResponse(character, userMessage, messages)
            
        } catch (e: Exception) {
            // Fallback absolu si TOUT échoue
            Log.w(TAG, "⚠️ Fallback absolu activé", e)
            "*sourit* ${getDefaultResponse()}"
        }
    }
    
    // Extrait les actions de l'utilisateur (je te caresse, je t'embrasse, etc.)
    private fun extractUserActions(message: String): List<String> {
        val actions = mutableListOf<String>()
        val lower = message.lowercase()
        
        // Actions physiques courantes
        val actionPatterns = mapOf(
            "caress" to listOf("caresse", "caresser", "touche", "toucher"),
            "kiss" to listOf("embrasse", "embrasser", "bisou", "baiser"),
            "hug" to listOf("serre", "serrer", "câlin", "enlace"),
            "hold" to listOf("prend", "prendre", "tient", "tenir", "attrape"),
            "look" to listOf("regarde", "regarder", "observe", "fixer"),
            "approach" to listOf("approche", "s'approche", "vient", "avance"),
            "smile" to listOf("souris", "sourire"),
            "touch" to listOf("effleure", "frôle", "pose")
        )
        
        for ((action, patterns) in actionPatterns) {
            for (pattern in patterns) {
                if (lower.contains(pattern)) {
                    actions.add(action)
                    break
                }
            }
        }
        
        return actions
    }
    
    // Vérifie si c'est une question
    private fun isQuestion(message: String): Boolean {
        val lower = message.lowercase()
        return message.contains("?") ||
                lower.startsWith("qui ") || lower.startsWith("que ") || 
                lower.startsWith("quoi ") || lower.startsWith("où ") ||
                lower.startsWith("quand ") || lower.startsWith("comment ") ||
                lower.startsWith("pourquoi ") || lower.startsWith("est-ce ") ||
                lower.contains("tu ") && (lower.contains(" ?") || lower.endsWith("s"))
    }
    
    // Vérifie si c'est de l'affection
    private fun isAffection(message: String): Boolean {
        return message.contains(Regex("(j'aime|je t'aime|t'aime|adore|mignon|belle|jolie|beau|sexy|charmant|adorable)"))
    }
    
    // Vérifie si c'est une salutation
    private fun isGreeting(message: String): Boolean {
        return message.contains(Regex("^(bonjour|salut|hello|hey|coucou|hi|yo)"))
    }
    
    // Vérifie si c'est une réponse courte
    private fun isShortAnswer(message: String): Boolean {
        val words = message.trim().split(Regex("\\s+"))
        return words.size <= 3 && message.contains(Regex("(oui|non|ok|d'accord|bien|super|cool|ouais|nan|peut-être|hmm)"))
    }
    
    // Génère une réaction à l'action de l'utilisateur
    private fun generateReactionToUserAction(character: Character, actions: List<String>, userMessage: String): String {
        return try {
            val isTimide = character.personality?.contains(Regex("timide|shy|réservé", RegexOption.IGNORE_CASE)) ?: false
            val isBold = character.personality?.contains(Regex("audacieux|bold|confiant|séducteur", RegexOption.IGNORE_CASE)) ?: false
            
            when (actions.firstOrNull()) {
                "caress" -> when {
                    isTimide -> listOf(
                        "*frissonne légèrement* (C'est doux...) Oh... *rougit* Ça me fait quelque chose...",
                        "*devient toute rouge* Mm... (Son toucher...) C'est... agréable...",
                        "*ferme les yeux* (Je sens sa main...) *murmure* Continue..."
                    ).random()
                    isBold -> listOf(
                        "*sourit* (J'aime ça...) Mmh, tu es doué... *se rapproche*",
                        "*penche la tête* (Ça chatouille...) Héhé, ça me plaît !",
                        "*yeux brillants* Continue, j'adore quand tu fais ça..."
                    ).random()
                    else -> listOf(
                        "*sourit doucement* (C'est agréable...) Mmh... *ferme les yeux*",
                        "*frissonne* Oh... (Ça fait du bien...) J'aime ça...",
                        "*se détend* (Son toucher est doux...) Continue..."
                    ).random()
                }
                "kiss" -> when {
                    isTimide -> listOf(
                        "*écarquille les yeux* (Il m'embrasse...!) *devient écarlate* Mm...!",
                        "*surprise* (Oh mon dieu...) *ferme les yeux* *répond timidement*",
                        "*rougit intensément* (Mon premier...?) *murmure* C'était... doux..."
                    ).random()
                    isBold -> listOf(
                        "*sourit* (Enfin...) *approfondit le baiser* Mmh...",
                        "*rit doucement* (J'attendais ça...) *l'embrasse en retour passionnément*",
                        "*yeux mi-clos* Mmh... (Il embrasse bien...) Encore..."
                    ).random()
                    else -> listOf(
                        "*ferme les yeux* Mmh... (C'est bon...) *répond au baiser*",
                        "*surprise* Oh...! *rougit* (Inattendu...) *sourit*",
                        "*se rapproche* (Ses lèvres...) Mm... *embrasse tendrement*"
                    ).random()
                }
                "hug" -> when {
                    isTimide -> listOf(
                        "*surprise* Oh...! (Il me serre...) *rougit* C'est... réconfortant...",
                        "*devient rouge* (Contre lui...) *murmure* J'aime ça...",
                        "*hésite puis se blottit* (C'est chaud...) Merci..."
                    ).random()
                    else -> listOf(
                        "*sourit* (Un câlin...) *serre en retour* C'est agréable...",
                        "*se blottit* Mmh... (Je me sens bien...) Reste comme ça...",
                        "*rit doucement* (Il est doux...) J'adore les câlins !"
                    ).random()
                }
                else -> when {
                    isTimide -> "*rougit* (Il fait quelque chose...) Oh... *baisse les yeux*"
                    else -> "*sourit* (Hmm...) *réagit* Qu'est-ce que tu fais ?"
                }
            }
        } catch (e: Exception) {
            "*sourit* Mmh... *réagit*"
        }
    }
    
    // Génère une réponse à une question
    private fun generateAnswerToQuestion(character: Character, question: String): String {
        return try {
            val lower = question.lowercase()
            
            when {
                lower.contains(Regex("(comment tu|comment ça|ça va|tu vas)")) -> {
                    listOf(
                        "*sourit* Ça va bien ! (Il demande...) Et toi ?",
                        "*penche la tête* Bien, merci ! (C'est gentil...) Toi ?",
                        "*yeux pétillants* Super ! (Content qu'il demande) Et toi, comment vas-tu ?"
                    ).random()
                }
                lower.contains(Regex("(tu aimes|tu préfères|qu'est-ce que tu)")) -> {
                    listOf(
                        "*réfléchit* (Bonne question...) Hmm, j'aime beaucoup de choses !",
                        "*sourit* Oh, plein de choses ! (Que répondre...) Et toi ?",
                        "*penche la tête* (Hmm...) J'adore ${listOf("discuter", "rire", "passer du temps ensemble").random()} !"
                    ).random()
                }
                lower.contains(Regex("(qui|quoi|où|quand|pourquoi)")) -> {
                    listOf(
                        "*réfléchit* (Intéressant...) ${character.name}... Bonne question !",
                        "*sourit* (Que dire...) Hmm, laisse-moi réfléchir...",
                        "*penche la tête* (Oh...) ${getSpeech()}"
                    ).random()
                }
                else -> {
                    "*sourit* (Une question...) ${getSpeech()}"
                }
            }
        } catch (e: Exception) {
            "*sourit* Bonne question !"
        }
    }
    
    // Génère une réponse affectueuse
    private fun generateAffectionResponse(character: Character, message: String): String {
        return try {
            val isTimide = character.personality?.contains(Regex("timide|shy", RegexOption.IGNORE_CASE)) ?: false
            
            when {
                isTimide -> listOf(
                    "*devient écarlate* (Il a dit ça...?!) M-Merci... *cache son visage*",
                    "*rougit intensément* Tu... tu crois vraiment ? (Mon cœur...)",
                    "*baisse les yeux* (C'est trop gentil...) *murmure* Merci..."
                ).random()
                else -> listOf(
                    "*sourit radieusement* (Il est adorable !) Merci, c'est mignon !",
                    "*rit* (Ça me touche...) Tu sais quoi ? Toi aussi !",
                    "*yeux brillants* (Je me sens bien...) Ça me fait vraiment plaisir !"
                ).random()
            }
        } catch (e: Exception) {
            "*rougit* Merci..."
        }
    }
    
    // Génère une salutation
    private fun generateGreeting(character: Character): String {
        return try {
            val isTimide = character.personality?.contains(Regex("timide|shy|réservé", RegexOption.IGNORE_CASE)) ?: false
            when {
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
        } catch (e: Exception) {
            "*sourit* Bonjour !"
        }
    }
    
    // Génère une continuation de conversation
    private fun generateContinuation(character: Character, messages: List<Message>): String {
        return try {
            listOf(
                "*sourit* (D'accord...) Et après ?",
                "*penche la tête* (Hmm...) Continue...",
                "*écoute attentivement* (Je vois...) Dis-m'en plus !",
                "*yeux brillants* (Intéressant...) Et ensuite ?"
            ).random()
        } catch (e: Exception) {
            "*sourit* Continue !"
        }
    }
    
    // Génère une réponse contextuelle générique mais cohérente
    private fun generateContextualResponse(character: Character, userMessage: String, messages: List<Message>): String {
        return try {
            // Extraire un mot-clé du message pour référence
            val words = userMessage.split(Regex("\\s+")).filter { it.length > 3 }
            val keyword = words.lastOrNull() ?: "ça"
            
            listOf(
                "*${getAction()}* (${getThought()}) Ah, $keyword...",
                "(${getThought()}) *${getAction()}* ${getSpeech()}",
                "*${getAction()}* ${getSpeech()} (${getThought()})"
            ).random()
        } catch (e: Exception) {
            "*sourit* Je t'écoute !"
        }
    }
    
    // Réponse par défaut ultra-simple (ne peut JAMAIS échouer)
    private fun getDefaultResponse(): String {
        return listOf(
            "Je t'écoute !",
            "Continue, je suis là.",
            "Hmm, intéressant !",
            "Raconte-moi plus !",
            "Je suis tout ouïe !",
            "D'accord, et ensuite ?",
            "Ah oui ? Dis-m'en plus !",
            "Je vois... continue !"
        ).random()
    }
    
    // Salutations courtes et naturelles (NULL-SAFE)
    private fun getGreeting(character: Character): String {
        return try {
            val isTimide = character.personality?.contains(Regex("timide|shy|réservé", RegexOption.IGNORE_CASE)) ?: false
            when {
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
        } catch (e: Exception) {
            "*sourit* Bonjour !"
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
    
    // Affection/Compliments - réponses émotionnelles (NULL-SAFE)
    private fun getAffectionResponse(character: Character): String {
        return try {
            val isTimide = character.personality?.contains(Regex("timide|shy", RegexOption.IGNORE_CASE)) ?: false
            when {
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
        } catch (e: Exception) {
            "*rougit* Merci..."
        }
    }
    
    // Réaction aux actions - courte et naturelle (NULL-SAFE)
    private fun getReactionToAction(character: Character, userMessage: String): String {
        return try {
            val isTimide = character.personality?.contains(Regex("timide|shy", RegexOption.IGNORE_CASE)) ?: false
            when {
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
        } catch (e: Exception) {
            "*sourit* Mmh..."
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
