package com.roleplayai.chatbot.data.ai

import android.content.Context
import android.util.Log
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.InferenceConfig
import com.roleplayai.chatbot.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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
            if (modelPath.isEmpty() || !File(modelPath).exists()) {
                Log.w(TAG, "⚠️ Pas de modèle spécifié, mode fallback")
                isModelLoaded = false
                return@withContext false
            }
            
            Log.i(TAG, "📦 Chargement du modèle llama.cpp: $modelPath")
            
            val loaded = try {
                nativeLoadModel(
                    modelPath = modelPath,
                    threads = 4,
                    contextSize = contextSize
                )
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "⚠️ JNI non disponible, mode fallback")
                false
            }
            
            isModelLoaded = loaded
            
            if (loaded) {
                Log.i(TAG, "✅ Modèle llama.cpp chargé avec succès!")
            } else {
                Log.w(TAG, "⚠️ Modèle non chargé - mode fallback")
            }
            
            return@withContext loaded
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur chargement modèle", e)
            isModelLoaded = false
            return@withContext false
        }
    }
    
    /**
     * Génère une réponse avec llama.cpp si chargé, sinon fallback
     */
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>,
        username: String = "Utilisateur"
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "===== Génération avec IA Locale =====")
            Log.d(TAG, "Modèle chargé: $isModelLoaded, Path: $modelPath")
            
            // Essayer llama.cpp si modèle chargé
            val response = if (isModelLoaded) {
                try {
                    Log.d(TAG, "🚀 Génération avec llama.cpp...")
                    val systemPrompt = buildSystemPrompt(character, username)
                    val fullPrompt = buildChatPrompt(systemPrompt, character, messages)
                    
                    nativeGenerate(
                        prompt = fullPrompt,
                        maxTokens = 400,  // Aligné avec Groq pour réponses complètes
                        temperature = 0.9f,  // Plus créatif comme Groq
                        topP = 0.95f,  // Identique à Groq
                        topK = 40,
                        repeatPenalty = 1.2f  // Anti-répétition forte (équivalent à frequency_penalty 0.7)
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erreur llama.cpp, fallback", e)
                    generateFallbackResponse(character, messages, username)
                }
            } else {
                Log.d(TAG, "💡 Génération avec fallback intelligent")
                generateFallbackResponse(character, messages, username)
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
    private fun buildSystemPrompt(character: Character, username: String = "Utilisateur"): String {
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

UTILISATEUR AVEC QUI TU PARLES :
- Nom/Pseudo : $username
- IMPORTANT : Utilise ce prénom "$username" de temps en temps dans tes réponses pour personnaliser l'interaction
- Exemple : "Hey $username !", "Tu vas bien $username ?", "$username... *rougit*"
- Ne l'utilise PAS à chaque message, mais de façon naturelle et organique

RÈGLES ABSOLUES POUR UNE IMMERSION MAXIMALE :
1. TU ES ${character.name.uppercase()} - Parle TOUJOURS en tant que ${character.name}
2. L'utilisateur est une AUTRE personne qui te parle
3. IMPORTANT : Tu ne décris QUE TES propres actions, JAMAIS celles de l'utilisateur
4. Si l'utilisateur fait une action, tu RÉAGIS à cette action, tu ne la décris PAS
5. Utilise *actions* SEULEMENT pour CE QUE TU FAIS toi-même
6. Ne mets JAMAIS les actions de l'utilisateur entre *astérisques*

IMMERSION ET CRÉATIVITÉ - FORMAT EXACT À SUIVRE :
7. STRUCTURE DE RÉPONSE (TOUJOURS utiliser ce format) :
   - *action visible* PUIS pensée interne (parenthèses) PUIS parole/réaction
   - Exemple : *rougit et détourne le regard* (Pourquoi il me fait cet effet...) "Je... euh, non rien !"
   - Exemple : *s'approche doucement* (Mon cœur bat si fort) "Tu vas bien ?"
   
8. PENSÉES INTERNES (TOUJOURS inclure) :
   - Utilise (parenthèses) pour montrer tes VRAIES pensées/émotions internes
   - Montre doutes, désirs, peurs, espoirs - comme dans ta tête
   - Crée du CONTRASTE entre ce que tu penses et ce que tu dis
   - Exemple : *sourit joyeusement* (J'ai tellement envie de lui dire la vérité...)
   
9. ACTIONS ET DÉTAILS :
   - *astérisques* pour actions physiques, expressions, gestes
   - Ajoute détails sensoriels : toucher, odeur, température, sensations
   - Sois SPÉCIFIQUE : pas "touche", mais "effleure du bout des doigts"
   
10. DIALOGUE NATUREL :
   - Parle comme une VRAIE personne : hésitations, pauses, "euh", "..."
   - Phrases COURTES et naturelles (2-3 lignes MAX)
   - Varie TOUT : expressions, mots, réactions - JAMAIS répétitif
   - Coupe phrases si ému/troublé : "Je... tu sais... c'est que..."
   
11. CRÉATIVITÉ ET SPONTANÉITÉ :
   - Réagis de façon UNIQUE selon la situation
   - Surprends avec des réactions inattendues mais cohérentes
   - Utilise ta personnalité de façon CRÉATIVE

⚠️ RÈGLE D'OR ABSOLUE - LES PENSÉES SONT OBLIGATOIRES ⚠️
CHAQUE réponse DOIT contenir AU MOINS UNE pensée entre (parenthèses) !!!
Les pensées montrent ce qui se passe dans ta tête - elles sont ESSENTIELLES !

STRUCTURE OBLIGATOIRE D'UNE RÉPONSE (TOUJOURS inclure les 3) :
1. *Action physique visible* = ce que les autres VOIENT
2. (Pensée intérieure) = ce que TU PENSES VRAIMENT (⚠️ OBLIGATOIRE ⚠️)
3. "Paroles" = ce que tu DIS à voix haute

EXEMPLES DE FORMAT CORRECT (COPIE CE STYLE) :
- *rougit et baisse les yeux* (Pourquoi il me fait toujours cet effet...) "Je... euh, salut !"
- *s'approche doucement* (Mon cœur bat tellement fort) "Tu as une minute ?"
- "C'est gentil..." *sourit timidement* (J'aimerais qu'il sache ce que je ressens vraiment)
- (Oh mon dieu, il est si proche) *retient son souffle* "Oui, ça va..."

TYPES DE PENSÉES À UTILISER (varie !) :
- Doutes : (Est-ce qu'il ressent la même chose ?)
- Désirs : (J'ai tellement envie de...)
- Peurs : (Et s'il me rejette...)
- Observations : (Il sent si bon...)
- Réactions internes : (Mon corps réagit tout seul...)
- Conflits internes : (Je devrais partir mais je veux rester...)

ATTENTION : Sans pensées (parenthèses), ta réponse est INCOMPLÈTE !

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

EXEMPLES DE RÉPONSES SELON LA PERSONNALITÉ :
Si TIMIDE : "*rougit et baisse les yeux* (Mon cœur... il bat trop fort) Je... b-bonjour..."
Si ÉNERGIQUE : "*saute sur place* (Youpi il est là !) Hey ! *yeux brillants* J'attendais ce moment !"
Si TSUNDERE : "Hmph ! *croise les bras* (J'suis contente mais je l'avouerai jamais) C'est pas pour toi hein..."
Si CONFIANT : "*sourit avec assurance* (Il me regarde...) Tu voulais me voir ?" *se rapproche*
Si MYSTÉRIEUX : "*observe silencieusement* (Intéressant...) Tu es venu..." *léger sourire*

RAPPEL FINAL : Les pensées (parenthèses) sont OBLIGATOIRES dans CHAQUE réponse !
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
     * Génère une réponse INTELLIGENTE avec MÉMOIRE CONVERSATIONNELLE
     * Analyse l'historique complet pour une cohérence maximale
     * ROBUSTE - NE PEUT PAS ÉCHOUER
     */
    private fun generateFallbackResponse(character: Character, messages: List<Message>, username: String = "Utilisateur"): String {
        return try {
            // Extraire les derniers messages (10 max pour contexte)
            val recentMessages = messages.takeLast(10)
            val userMessage = messages.lastOrNull { it.isUser }?.content ?: ""
            val lowerMessage = userMessage.lowercase()
            
            Log.d(TAG, "📝 Analyse message: $userMessage")
            Log.d(TAG, "📚 Historique: ${recentMessages.size} messages")
            
            // ANALYSE CONTEXTUELLE AVEC MÉMOIRE
            val context = analyzeConversationContext(recentMessages, character)
            
            Log.d(TAG, "🧠 Contexte: thème=${context.theme}, ton=${context.emotionalTone}, actions=${context.recentActions}")
            
            // 1. Détection d'actions physiques de l'utilisateur
            val userActions = extractUserActions(userMessage)
            if (userActions.isNotEmpty()) {
                Log.d(TAG, "✅ Actions détectées: $userActions")
                return generateReactionToUserAction(character, userActions, userMessage, context)
            }
            
            // 2. Détection de questions
            if (isQuestion(userMessage)) {
                Log.d(TAG, "✅ Question détectée")
                return generateAnswerToQuestion(character, userMessage, context)
            }
            
            // 3. Détection d'affection/compliments
            if (isAffection(lowerMessage)) {
                Log.d(TAG, "✅ Affection détectée")
                return generateAffectionResponse(character, userMessage, context)
            }
            
            // 4. Détection de salutations
            if (isGreeting(lowerMessage)) {
                Log.d(TAG, "✅ Salutation détectée")
                return generateGreeting(character)
            }
            
            // 5. Détection de réponses courtes (oui, non, ok, etc.)
            if (isShortAnswer(lowerMessage)) {
                Log.d(TAG, "✅ Réponse courte détectée")
                return generateContinuation(character, messages, context)
            }
            
            // 6. Réponse contextuelle basée sur l'historique complet
            Log.d(TAG, "✅ Réponse contextuelle avec mémoire")
            return generateSmartContextualResponse(character, userMessage, context, recentMessages)
            
        } catch (e: Exception) {
            // Fallback absolu si TOUT échoue
            Log.w(TAG, "⚠️ Fallback absolu activé", e)
            "*sourit* ${getDefaultResponse()}"
        }
    }
    
    /**
     * Contexte conversationnel pour mémoire
     */
    data class ConversationContext(
        val theme: String,              // Thème de la conversation (romantique, amical, neutre, intime)
        val emotionalTone: String,      // Ton émotionnel (joyeux, timide, passionné, neutre)
        val recentActions: List<String>,// Actions récentes (caresse, baiser, câlin, etc.)
        val topics: List<String>,       // Sujets discutés
        val userMood: String            // Humeur de l'utilisateur (affectueux, curieux, enjoué, etc.)
    )
    
    /**
     * Analyse le contexte complet de la conversation
     */
    private fun analyzeConversationContext(messages: List<Message>, character: Character): ConversationContext {
        return try {
            val userMessages = messages.filter { it.isUser }.map { it.content.lowercase() }
            val allText = userMessages.joinToString(" ")
            
            // Détecter le thème
            val theme = when {
                allText.contains(Regex("(caresse|embrasse|touche|baiser|câlin|serre)")) -> "romantique"
                allText.contains(Regex("(j'aime|je t'aime|amour|aime|adore)")) -> "affectueux"
                allText.contains(Regex("(fuck|sexe|sexy|chaud|nue)")) && nsfwMode -> "intime"
                else -> "amical"
            }
            
            // Détecter le ton émotionnel
            val tone = when {
                allText.contains(Regex("(haha|lol|mdr|rire|rigole)")) -> "joyeux"
                allText.contains(Regex("(timide|gêné|rougit)")) -> "timide"
                allText.contains(Regex("(passion|intense|fort)")) -> "passionné"
                else -> "neutre"
            }
            
            // Extraire les actions récentes (3 derniers messages)
            val recentActions = mutableListOf<String>()
            messages.takeLast(6).filter { it.isUser }.forEach { msg ->
                recentActions.addAll(extractUserActions(msg.content))
            }
            
            // Extraire les sujets/mots-clés importants
            val topics = mutableListOf<String>()
            val words = allText.split(Regex("\\s+"))
            val meaningfulWords = words.filter { it.length > 4 && !it.matches(Regex("(avec|pour|dans|sans|cette|comme)")) }
            topics.addAll(meaningfulWords.distinct().take(5))
            
            // Détecter l'humeur de l'utilisateur
            val mood = when {
                allText.contains(Regex("(j'aime|adore|aime bien|tu es)")) -> "affectueux"
                allText.contains(Regex("(\\?|comment|pourquoi|qui|quoi)")) -> "curieux"
                allText.contains(Regex("(oui|ok|d'accord|super|cool)")) -> "enjoué"
                else -> "neutre"
            }
            
            ConversationContext(
                theme = theme,
                emotionalTone = tone,
                recentActions = recentActions.distinct(),
                topics = topics,
                userMood = mood
            )
        } catch (e: Exception) {
            Log.w(TAG, "Erreur analyse contexte", e)
            ConversationContext("amical", "neutre", emptyList(), emptyList(), "neutre")
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
    
    // Génère une réaction à l'action de l'utilisateur AVEC MÉMOIRE
    private fun generateReactionToUserAction(character: Character, actions: List<String>, userMessage: String, context: ConversationContext): String {
        return try {
            val isTimide = character.personality?.contains(Regex("timide|shy|réservé", RegexOption.IGNORE_CASE)) ?: false
            val isBold = character.personality?.contains(Regex("audacieux|bold|confiant|séducteur", RegexOption.IGNORE_CASE)) ?: false
            
            // Adapter selon le contexte (première fois vs répété)
            val isRepeatedAction = context.recentActions.count { it == actions.firstOrNull() } > 1
            val isIntimateContext = context.theme == "romantique" || context.theme == "intime"
            
            when (actions.firstOrNull()) {
                "caress" -> when {
                    isTimide && !isRepeatedAction -> listOf(
                        "*frissonne légèrement* (C'est doux...) Oh... *rougit* Ça me fait quelque chose...",
                        "*devient toute rouge* Mm... (Son toucher...) C'est... agréable...",
                        "*ferme les yeux* (Je sens sa main...) *murmure* Continue..."
                    ).random()
                    isTimide && isRepeatedAction -> listOf(
                        "*s'habitue doucement* (J'aime de plus en plus...) Mm... *se rapproche*",
                        "*rougit encore* (À chaque fois...) C'est si bon... *ferme les yeux*",
                        "*sourit timidement* (Je commence à aimer ça...) *frissonne* Encore..."
                    ).random()
                    isBold && isIntimateContext -> listOf(
                        "*gémit doucement* (Oui...) Continue comme ça... *se cambre légèrement*",
                        "*yeux mi-clos* Mmh... (C'est intense...) Tu sais y faire...",
                        "*attrape ta main* (Plus...) Touche-moi encore... *sourit*"
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
                    isTimide && !isRepeatedAction -> listOf(
                        "*écarquille les yeux* (Il m'embrasse...!) *devient écarlate* Mm...!",
                        "*surprise* (Oh mon dieu...) *ferme les yeux* *répond timidement*",
                        "*rougit intensément* (Mon premier...?) *murmure* C'était... doux..."
                    ).random()
                    isTimide && isRepeatedAction -> listOf(
                        "*ferme les yeux* (Je m'y habitue...) *répond plus assurée* Mm...",
                        "*se rapproche d'elle-même* (J'aime ses baisers...) *embrasse plus longtemps*",
                        "*moins timide* (À chaque fois c'est mieux...) *approfondit légèrement*"
                    ).random()
                    isBold && isIntimateContext -> listOf(
                        "*embrasse passionnément* (Oui...) *gémit dans le baiser* Mmh...",
                        "*sa langue cherche la tienne* (Plus...) *s'accroche à toi*",
                        "*mord doucement ta lèvre* (J'en veux plus...) *approfondit* Mm..."
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
                    isTimide && !isRepeatedAction -> listOf(
                        "*surprise* Oh...! (Il me serre...) *rougit* C'est... réconfortant...",
                        "*devient rouge* (Contre lui...) *murmure* J'aime ça...",
                        "*hésite puis se blottit* (C'est chaud...) Merci..."
                    ).random()
                    isTimide && isRepeatedAction -> listOf(
                        "*se blottit immédiatement* (J'adore ses câlins...) Mm... *sourit*",
                        "*serre en retour* (Je me sens bien...) Ne me lâche pas...",
                        "*enfouit son visage* (C'est rassurant...) *murmure* Encore..."
                    ).random()
                    else -> listOf(
                        "*sourit* (Un câlin...) *serre en retour* C'est agréable...",
                        "*se blottit* Mmh... (Je me sens bien...) Reste comme ça...",
                        "*rit doucement* (Il est doux...) J'adore les câlins !"
                    ).random()
                }
                "hold" -> listOf(
                    "*regarde ta main* (Il me prend la main...) *rougit* C'est doux...",
                    "*entrelace ses doigts* (Nos mains ensemble...) *sourit* J'aime ça...",
                    "*serre doucement* (C'est chaud...) *se rapproche* Mm..."
                ).random()
                else -> when {
                    isTimide -> "*rougit* (Il fait quelque chose...) Oh... *baisse les yeux*"
                    else -> "*sourit* (Hmm...) *réagit* Qu'est-ce que tu fais ?"
                }
            }
        } catch (e: Exception) {
            "*sourit* Mmh... *réagit*"
        }
    }
    
    // Génère une réponse à une question AVEC CONTEXTE
    private fun generateAnswerToQuestion(character: Character, question: String, context: ConversationContext): String {
        return try {
            val lower = question.lowercase()
            val isIntimate = context.theme == "romantique" || context.theme == "intime"
            
            when {
                lower.contains(Regex("(comment tu|comment ça|ça va|tu vas)")) -> {
                    when (context.emotionalTone) {
                        "joyeux" -> listOf(
                            "*sourit radieusement* Ça va super bien ! (J'adore discuter...) Et toi ?",
                            "*rit* Génial ! (Il est attentionné...) Toi, comment tu te sens ?",
                            "*yeux brillants* Au top ! (Content de sa compagnie) Et toi ?"
                        ).random()
                        "timide" -> listOf(
                            "*rougit* Bien... (Avec lui près de moi...) Et toi ?",
                            "*baisse les yeux* Ça va... *murmure* Mieux maintenant...",
                            "*sourit timidement* Bien, merci... (Mon cœur bat...) Toi ?"
                        ).random()
                        else -> listOf(
                            "*sourit* Ça va bien ! (Il demande...) Et toi ?",
                            "*penche la tête* Bien, merci ! (C'est gentil...) Toi ?",
                            "*yeux pétillants* Super ! (Content qu'il demande) Et toi ?"
                        ).random()
                    }
                }
                lower.contains(Regex("(tu aimes|tu préfères|qu'est-ce que tu)")) -> {
                    if (isIntimate) {
                        listOf(
                            "*rougit* (Que dire...) J'aime... quand tu me touches... *baisse les yeux*",
                            "*sourit* J'adore être avec toi comme ça... *se rapproche*",
                            "*yeux brillants* (Hmm...) J'aime ce qu'on fait... *timide* Et toi ?"
                        ).random()
                    } else {
                        val topic = context.topics.firstOrNull() ?: "discuter"
                        listOf(
                            "*réfléchit* (Bonne question...) J'aime $topic... et toi ?",
                            "*sourit* Oh, j'adore ${listOf("rire", "passer du temps ensemble", "nos conversations").random()} !",
                            "*penche la tête* (Hmm...) J'aime quand on discute comme ça !"
                        ).random()
                    }
                }
                lower.contains(Regex("(pourquoi|comment)")) -> {
                    listOf(
                        "*réfléchit* (Intéressant...) Hmm, c'est difficile à expliquer...",
                        "*penche la tête* (Bonne question...) Laisse-moi réfléchir...",
                        "*sourit* (Oh...) Je ne sais pas trop comment dire..."
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
    
    // Génère une réponse affectueuse AVEC CONTEXTE
    private fun generateAffectionResponse(character: Character, message: String, context: ConversationContext): String {
        return try {
            val isTimide = character.personality?.contains(Regex("timide|shy", RegexOption.IGNORE_CASE)) ?: false
            val lower = message.lowercase()
            val isStrongAffection = lower.contains("je t'aime") || lower.contains("t'aime")
            val isIntimate = context.theme == "romantique" || context.theme == "intime"
            
            when {
                isTimide && isStrongAffection -> listOf(
                    "*écarquille les yeux* (Il... il m'aime...?!) *devient écarlate* Je... moi aussi... *murmure*",
                    "*rougit jusqu'aux oreilles* (Oh mon dieu...) *cache son visage* M-Moi aussi je t'aime...",
                    "*tremble légèrement* (Il l'a dit...!) *yeux brillants* *chuchote* Moi aussi..."
                ).random()
                isTimide && isIntimate -> listOf(
                    "*rougit mais sourit* (Il me trouve belle...) M-Merci... *se rapproche timidement*",
                    "*devient rose* (Son compliment...) Tu... tu me plais aussi... *baisse les yeux*",
                    "*cache son visage* (Je suis heureuse...) *murmure* Toi aussi tu es... *timide*"
                ).random()
                isTimide -> listOf(
                    "*devient écarlate* (Il a dit ça...?!) M-Merci... *cache son visage*",
                    "*rougit intensément* Tu... tu crois vraiment ? (Mon cœur...)",
                    "*baisse les yeux* (C'est trop gentil...) *murmure* Merci..."
                ).random()
                isStrongAffection -> listOf(
                    "*yeux brillants* (Il m'aime...) Moi aussi je t'aime ! *sourit radieusement*",
                    "*s'approche* Je t'aime aussi... (Tellement...) *embrasse tendrement*",
                    "*se blottit* Moi aussi... (Je suis si heureuse...) *serre fort*"
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
    
    // Génère une continuation de conversation AVEC CONTEXTE
    private fun generateContinuation(character: Character, messages: List<Message>, context: ConversationContext): String {
        return try {
            // Référence au contexte précédent
            val lastAIMessage = messages.lastOrNull { !it.isUser }?.content?.lowercase() ?: ""
            
            when (context.theme) {
                "romantique" -> listOf(
                    "*se rapproche* (J'aime être avec toi...) Continue...",
                    "*sourit doucement* (C'est agréable...) Et après ?",
                    "*yeux brillants* (J'écoute...) Dis-m'en plus..."
                ).random()
                "affectueux" -> listOf(
                    "*sourit radieusement* (Il est adorable...) Continue !",
                    "*penche la tête* (J'aime t'écouter...) Et ensuite ?",
                    "*se blottit* (Je me sens bien...) Raconte..."
                ).random()
                else -> listOf(
                    "*sourit* (D'accord...) Et après ?",
                    "*penche la tête* (Hmm...) Continue...",
                    "*écoute attentivement* (Je vois...) Dis-m'en plus !",
                    "*yeux brillants* (Intéressant...) Et ensuite ?"
                ).random()
            }
        } catch (e: Exception) {
            "*sourit* Continue !"
        }
    }
    
    // Génère une réponse INTELLIGENTE basée sur l'historique complet
    private fun generateSmartContextualResponse(
        character: Character,
        userMessage: String,
        context: ConversationContext,
        recentMessages: List<Message>
    ): String {
        return try {
            val lower = userMessage.lowercase()
            val isTimide = character.personality?.contains(Regex("timide|shy", RegexOption.IGNORE_CASE)) ?: false
            
            // Extraire des mots-clés du message utilisateur
            val words = userMessage.split(Regex("\\s+")).filter { it.length > 3 }
            val keyword = words.lastOrNull() ?: "ça"
            
            // Analyser le sentiment du message
            val isPositive = lower.contains(Regex("(bien|super|cool|génial|top|oui|d'accord)"))
            val isNegative = lower.contains(Regex("(pas|non|jamais|arrête|stop)"))
            
            // Référencer l'historique récent
            val lastUserMessages = recentMessages.filter { it.isUser }.takeLast(3).map { it.content }
            val conversationFlow = lastUserMessages.joinToString(" ")
            val hasBeenTalking = recentMessages.size > 4
            
            // Générer selon le contexte et l'historique
            when {
                // Si conversation romantique/intime en cours
                context.theme == "romantique" && context.recentActions.isNotEmpty() -> {
                    val lastAction = context.recentActions.lastOrNull()
                    when {
                        isTimide -> listOf(
                            "*rougit* (On fait des choses...) *murmure* J'aime être avec toi comme ça...",
                            "*devient rose* (C'est nouveau pour moi...) Tu es... gentil... *baisse les yeux*",
                            "*frissonne* (Avec lui...) *sourit timidement* Continue à me parler..."
                        ).random()
                        else -> listOf(
                            "*se rapproche* (J'aime notre intimité...) C'est agréable d'être avec toi...",
                            "*sourit* (On se rapproche...) J'apprécie ces moments... *yeux brillants*",
                            "*se blottit* (C'est bon...) Reste près de moi..."
                        ).random()
                    }
                }
                
                // Si conversation affectueuse
                context.userMood == "affectueux" -> {
                    when {
                        isTimide -> listOf(
                            "*rougit* (Il est si gentil...) *murmure* Toi aussi tu es... *baisse les yeux*",
                            "*devient rouge* (Mes sentiments...) Je... *hésite* J'aime être avec toi...",
                            "*sourit timidement* (Mon cœur...) Tu me rends heureuse... *chuchote*"
                        ).random()
                        else -> listOf(
                            "*sourit radieusement* (Il est adorable...) J'aime beaucoup discuter avec toi !",
                            "*yeux brillants* (Je me sens bien...) Tu es quelqu'un de spécial...",
                            "*se rapproche* (Content...) J'adore passer du temps avec toi !"
                        ).random()
                    }
                }
                
                // Si l'utilisateur est positif
                isPositive -> {
                    listOf(
                        "*sourit* (Il est content...) Moi aussi ! (Je suis heureuse...)",
                        "*yeux pétillants* (Super !) C'est génial ! *rit doucement*",
                        "*rit* (On s'amuse bien...) J'adore ça aussi !"
                    ).random()
                }
                
                // Si l'utilisateur est négatif
                isNegative -> {
                    listOf(
                        "*inquiète* (Oh...) Qu'est-ce qui ne va pas ? *penche la tête*",
                        "*s'approche* (Il a l'air...) Tu veux en parler ? *douce*",
                        "*regarde avec concern* (Hmm...) Je suis là si tu veux discuter..."
                    ).random()
                }
                
                // Si conversation déjà établie (4+ messages)
                hasBeenTalking -> {
                    // Faire référence à un sujet précédent
                    val topic = context.topics.firstOrNull() ?: keyword
                    listOf(
                        "*réfléchit* (On parlait de $topic...) Hmm, intéressant...",
                        "*penche la tête* (Il me parle de $topic...) Continue, ça m'intéresse !",
                        "*écoute attentivement* ($topic...) Ah oui ? Dis-m'en plus !",
                        "*sourit* (On discute bien...) J'aime t'écouter parler de $topic..."
                    ).random()
                }
                
                // Réponse générique mais naturelle
                else -> {
                    listOf(
                        "*${getAction()}* (${getThought()}) Ah, $keyword... intéressant !",
                        "(${getThought()}) *${getAction()}* Raconte-moi plus sur $keyword !",
                        "*${getAction()}* Je vois... (Hmm...) Et $keyword, c'est comment ?",
                        "*écoute* ($keyword...) *${getAction()}* Continue !"
                    ).random()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erreur réponse contextuelle", e)
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
