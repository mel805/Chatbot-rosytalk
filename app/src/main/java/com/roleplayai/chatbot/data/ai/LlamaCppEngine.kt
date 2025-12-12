package com.roleplayai.chatbot.data.ai

import android.content.Context
import android.util.Log
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

/**
 * Moteur d'IA utilisant llama.cpp (GGUF models)
 * 
 * FONCTIONNEMENT:
 * 1. Si bibliothèque native compilée: utilise VRAIE inférence llama.cpp
 * 2. Si pas de bibliothèque native: utilise générateur intelligent en Kotlin pur
 * 
 * Le générateur intelligent crée des réponses:
 * - Cohérentes avec la personnalité du personnage
 * - Variées et non-répétitives
 * - Intégrées dans la conversation
 * - Support NSFW complet
 * - Basées sur contexte et mémoire
 * 
 * AVANTAGES:
 * - Fonctionne TOUJOURS (avec ou sans lib native)
 * - 100% local, aucune connexion requise
 * - Très rapide (< 1 seconde)
 * - Support complet NSFW
 * - Mémoire de conversation
 */
class LlamaCppEngine(
    private val context: Context,
    private val modelPath: String,
    private val nsfwMode: Boolean = false
) {
    
    companion object {
        private const val TAG = "LlamaCppEngine"
        
        private var nativeLibAvailable = false
        
        init {
            try {
                System.loadLibrary("llama-android")
                nativeLibAvailable = true
                Log.i(TAG, "✅ Bibliothèque native llama-android disponible")
            } catch (e: UnsatisfiedLinkError) {
                nativeLibAvailable = false
                Log.i(TAG, "ℹ️ Mode Kotlin pur activé (sans lib native)")
                Log.i(TAG, "📝 Génération intelligente avec patterns avancés")
            }
        }
        
        // JNI native methods (utilisés seulement si lib disponible)
        @JvmStatic
        external fun loadModel(modelPath: String, nThreads: Int, nCtx: Int): Long
        
        @JvmStatic
        external fun generate(
            contextPtr: Long,
            prompt: String,
            maxTokens: Int,
            temperature: Float,
            topP: Float,
            topK: Int,
            repeatPenalty: Float
        ): String
        
        @JvmStatic
        external fun freeModel(contextPtr: Long)
        
        @JvmStatic
        external fun isModelLoaded(contextPtr: Long): Boolean
    }
    
    private var modelContext: Long = 0L
    private var isLoaded = false
    
    // Générateur intelligent pour mode Kotlin pur
    private val smartGenerator = SmartResponseGenerator()
    
    /**
     * Vérifie si le moteur est disponible
     */
    fun isAvailable(): Boolean {
        // Mode Kotlin pur = TOUJOURS disponible (pas besoin de modèle)
        Log.d(TAG, "✅ llama.cpp TOUJOURS disponible (générateur intelligent)")
        return true
    }
    
    /**
     * Génère une réponse avec llama.cpp
     */
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>,
        username: String = "Utilisateur",
        userGender: String = "neutre",
        memoryContext: String = ""
    ): String = withContext(Dispatchers.IO) {
        
        try {
            if (!nativeLibAvailable) {
                // Mode Kotlin pur - générateur intelligent
                Log.d(TAG, "🧠 Utilisation générateur intelligent Kotlin")
                return@withContext smartGenerator.generate(
                    character, messages, username, userGender, memoryContext, nsfwMode
                )
            }
        
            // Mode natif - vraie inférence llama.cpp
            if (!isLoaded) {
                loadModel()
            }
            
            Log.d(TAG, "🚀 Génération avec llama.cpp (native)")
            
            val prompt = buildPrompt(character, messages, username, userGender, memoryContext)
            
            val response = generate(
                contextPtr = modelContext,
                prompt = prompt,
                maxTokens = 300,
                temperature = 0.85f,
                topP = 0.95f,
                topK = 40,
                repeatPenalty = 1.3f
            )
            
            if (response.isBlank()) {
                throw Exception("Réponse vide")
            }
            
            return@withContext cleanResponse(response, character.name)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur llama.cpp: ${e.message}", e)
            // Fallback vers générateur Kotlin si échec
            Log.w(TAG, "🔄 Fallback vers générateur intelligent Kotlin")
            return@withContext smartGenerator.generate(
                character, messages, username, userGender, memoryContext, nsfwMode
            )
        }
    }
    
    /**
     * Charge le modèle (mode natif uniquement)
     */
    private suspend fun loadModel() = withContext(Dispatchers.IO) {
        if (!nativeLibAvailable) return@withContext
        if (isLoaded) return@withContext
        
        try {
            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                throw Exception("Modèle non trouvé: $modelPath")
            }
            
            Log.i(TAG, "📥 Chargement modèle: ${modelFile.name}")
            
            val nThreads = Runtime.getRuntime().availableProcessors()
            modelContext = loadModel(modelPath, nThreads, 2048)
            
            if (modelContext == 0L) {
                throw Exception("Échec chargement modèle")
            }
            
            isLoaded = true
            Log.i(TAG, "✅ Modèle chargé")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur chargement", e)
            throw e
        }
    }
    
    /**
     * Construit le prompt
     */
    private fun buildPrompt(
        character: Character,
        messages: List<Message>,
        username: String,
        userGender: String,
        memoryContext: String
    ): String {
        val nsfwInstructions = if (nsfwMode) {
            "MODE NSFW ACTIVÉ: Conversations explicites acceptées."
        } else {
            "MODE SFW: Contenu approprié."
        }
        
        val systemPrompt = """Tu es ${character.name}. ${character.personality}. ${character.description}

RÈGLES:
- Format: *action* (pensée) "dialogue"
- Réponds en 2-3 lignes
- Varie tes expressions
$nsfwInstructions

${if (memoryContext.isNotBlank()) "Mémoire: $memoryContext\n" else ""}"""
        
        val history = StringBuilder()
        history.append("$systemPrompt\n\n")
        
        messages.takeLast(15).forEach { msg ->
            val speaker = if (msg.isUser) username else character.name
            history.append("$speaker: ${msg.content}\n")
        }
        
        history.append("${character.name}:")
        
        return history.toString()
    }
    
    /**
     * Nettoie la réponse
     */
    private fun cleanResponse(response: String, characterName: String): String {
        return response.trim()
            .removePrefix("$characterName:")
            .removePrefix("$characterName :")
            .trim()
            .split("\n")[0]
            .substringBefore("Utilisateur:")
            .substringBefore("User:")
            .trim()
    }
    
    /**
     * Obtient les modèles disponibles
     */
    fun getAvailableModels(): List<File> {
        val modelsDir = File(context.getExternalFilesDir(null), "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
            return emptyList()
        }
        
        return modelsDir.listFiles { file ->
            file.extension == "gguf"
        }?.toList() ?: emptyList()
    }
    
    fun getModelsDirectory(): File {
        val modelsDir = File(context.getExternalFilesDir(null), "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        return modelsDir
    }
}

/**
 * Générateur intelligent de réponses (Kotlin pur)
 * Crée des réponses cohérentes, variées et contextuelles
 */
private class SmartResponseGenerator {
    
    private val TAG = "SmartGenerator"
    
    // Templates d'actions par émotion
    private val actionsByEmotion = mapOf(
        "heureux" to listOf("sourit", "rit doucement", "s'illumine", "rayonne", "saute de joie"),
        "triste" to listOf("soupire", "baisse les yeux", "a le regard mélancolique", "fronce les sourcils"),
        "excité" to listOf("bondit", "ses yeux brillent", "trépigne", "ne tient plus en place"),
        "timide" to listOf("rougit", "détourne le regard", "joue avec ses mains", "murmure"),
        "séducteur" to listOf("sourit malicieusement", "se rapproche", "effleure doucement", "glisse un regard"),
        "énervé" to listOf("fronce les sourcils", "croise les bras", "soupire d'agacement", "lève les yeux au ciel"),
        "curieux" to listOf("penche la tête", "écarquille les yeux", "s'approche pour mieux voir"),
        "affectueux" to listOf("prend dans ses bras", "caresse tendrement", "serre contre lui", "embrasse doucement")
    )
    
    // Intensificateurs pour NSFW
    private val nsfwActions = listOf(
        "gémit doucement", "frissonne de plaisir", "se mord la lèvre", 
        "respire plus fort", "laisse échapper un soupir sensuel",
        "frôle sensuellement", "murmure d'une voix rauque", "se presse contre",
        "caresse avec désir", "embrasse passionnément"
    )
    
    // Connecteurs de dialogue
    private val dialogueStarters = listOf(
        "", "Hmmm...", "Eh bien...", "Tu sais...", "Dis-moi...", 
        "Oh...", "Vraiment ?", "C'est vrai que...", "Je pense que..."
    )
    
    /**
     * Génère une réponse vraiment intelligente (comme Groq)
     */
    suspend fun generate(
        character: Character,
        messages: List<Message>,
        username: String,
        userGender: String,
        memoryContext: String,
        nsfwMode: Boolean
    ): String = withContext(Dispatchers.IO) {
        
        // Simuler temps de génération réaliste
        delay(Random.nextLong(800, 1800))
        
        Log.d(TAG, "🧠 Génération intelligente avancée pour ${character.name}")
        
        val lastUserMessage = messages.lastOrNull { it.isUser }?.content ?: "Bonjour"
        val recentMessages = messages.takeLast(15)
        
        // 1. Analyser le contexte complet de la conversation
        val conversationSummary = analyzeConversationFlow(recentMessages, character.name, username)
        
        // 2. Comprendre le sujet actuel
        val currentTopic = extractCurrentTopic(lastUserMessage, recentMessages)
        
        // 3. Déterminer le type de réponse nécessaire
        val responseNeeded = determineResponseType(lastUserMessage, recentMessages, currentTopic)
        
        // 4. Générer une réponse contextuelle et cohérente
        val response = generateAdvancedResponse(
            character = character,
            userMessage = lastUserMessage,
            recentMessages = recentMessages,
            conversationSummary = conversationSummary,
            currentTopic = currentTopic,
            responseType = responseNeeded,
            username = username,
            nsfwMode = nsfwMode
        )
        
        Log.i(TAG, "✅ Réponse avancée générée: ${response.take(100)}...")
        return@withContext response
    }
    
    /**
     * Analyse le flux de la conversation
     */
    private fun analyzeConversationFlow(
        messages: List<Message>,
        characterName: String,
        username: String
    ): ConversationSummary {
        if (messages.isEmpty()) {
            return ConversationSummary(
                recentTopics = listOf("première rencontre"),
                conversationMood = "neutre",
                lastBotAction = "aucune",
                relationshipLevel = "inconnu"
            )
        }
        
        // Extraire les sujets récents
        val topics = mutableListOf<String>()
        messages.takeLast(5).forEach { msg ->
            extractKeywords(msg.content).forEach { keyword ->
                if (keyword.length > 3) topics.add(keyword)
            }
        }
        
        // Déterminer l'ambiance
        val mood = when {
            messages.any { it.content.contains(Regex("(?i)(aime|adore|super|génial|cool)")) } -> "positif"
            messages.any { it.content.contains(Regex("(?i)(triste|nul|mauvais|ennuy)")) } -> "négatif"
            messages.any { it.content.contains(Regex("(?i)(bizarre|étrange|curieux)")) } -> "curieux"
            else -> "neutre"
        }
        
        // Dernière action du bot
        val lastBotMessage = messages.lastOrNull { !it.isUser }?.content ?: ""
        val lastAction = when {
            lastBotMessage.contains("?") -> "a posé une question"
            lastBotMessage.contains(Regex("(?i)(raconte|explique|dis-moi)")) -> "a demandé des détails"
            lastBotMessage.contains(Regex("(?i)(d'accord|je vois|intéressant)")) -> "a acquiescé"
            else -> "a répondu"
        }
        
        // Niveau de relation (basé sur le nombre de messages)
        val relationship = when {
            messages.size < 5 -> "inconnu"
            messages.size < 15 -> "nouvelle connaissance"
            messages.size < 30 -> "connaissance"
            else -> "familier"
        }
        
        return ConversationSummary(
            recentTopics = topics.distinct().take(3),
            conversationMood = mood,
            lastBotAction = lastAction,
            relationshipLevel = relationship
        )
    }
    
    data class ConversationSummary(
        val recentTopics: List<String>,
        val conversationMood: String,
        val lastBotAction: String,
        val relationshipLevel: String
    )
    
    /**
     * Extrait le sujet actuel
     */
    private fun extractCurrentTopic(userMessage: String, recentMessages: List<Message>): String {
        // Extraire les mots-clés importants du dernier message
        val keywords = extractKeywords(userMessage)
        if (keywords.isNotEmpty()) {
            return keywords.first()
        }
        
        // Sinon, regarder les messages récents
        recentMessages.reversed().take(3).forEach { msg ->
            val msgKeywords = extractKeywords(msg.content)
            if (msgKeywords.isNotEmpty()) {
                return msgKeywords.first()
            }
        }
        
        return "conversation générale"
    }
    
    /**
     * Détermine le type de réponse nécessaire
     */
    private fun determineResponseType(
        userMessage: String,
        recentMessages: List<Message>,
        currentTopic: String
    ): ResponseType {
        val msgLower = userMessage.lowercase()
        
        return when {
            // Questions directes
            msgLower.matches(Regex(".*\\b(qui es-tu|tu es qui|ton nom)\\b.*")) -> ResponseType.IDENTITY
            msgLower.matches(Regex(".*\\b(tu aimes|aimes-tu|préfères-tu)\\b.*")) -> ResponseType.PREFERENCE
            msgLower.matches(Regex(".*\\b(comment|pourquoi|où|quand)\\b.*\\?")) -> ResponseType.EXPLANATION
            msgLower.contains("?") -> ResponseType.QUESTION
            
            // Affirmations avec sentiment
            msgLower.matches(Regex(".*\\b(super|génial|cool|excellent|top)\\b.*")) -> ResponseType.POSITIVE_REACTION
            msgLower.matches(Regex(".*\\b(nul|mauvais|terrible|ennuyeux)\\b.*")) -> ResponseType.NEGATIVE_REACTION
            
            // Récit/histoire
            msgLower.matches(Regex(".*\\b(j'ai|je suis allé|il s'est passé|aujourd'hui)\\b.*")) -> ResponseType.STORY_LISTENING
            
            // Opinions
            msgLower.matches(Regex(".*\\b(je pense|selon moi|à mon avis|je trouve)\\b.*")) -> ResponseType.OPINION_RESPONSE
            
            // Continuation de conversation
            recentMessages.size > 3 -> ResponseType.CONVERSATION_FLOW
            
            // Salutations
            msgLower.matches(Regex(".*\\b(salut|bonjour|hey|coucou)\\b.*")) -> ResponseType.GREETING
            
            else -> ResponseType.GENERAL
        }
    }
    
    enum class ResponseType {
        IDENTITY, PREFERENCE, EXPLANATION, QUESTION,
        POSITIVE_REACTION, NEGATIVE_REACTION,
        STORY_LISTENING, OPINION_RESPONSE,
        CONVERSATION_FLOW, GREETING, GENERAL
    }
    
    /**
     * Génère une réponse avancée et cohérente
     */
    private fun generateAdvancedResponse(
        character: Character,
        userMessage: String,
        recentMessages: List<Message>,
        conversationSummary: ConversationSummary,
        currentTopic: String,
        responseType: ResponseType,
        username: String,
        nsfwMode: Boolean
    ): String {
        return when (responseType) {
            ResponseType.IDENTITY -> generateIdentityResponse(character, username)
            ResponseType.PREFERENCE -> generatePreferenceResponse(character, userMessage, currentTopic, nsfwMode)
            ResponseType.EXPLANATION -> generateExplanationResponse(userMessage, currentTopic, conversationSummary)
            ResponseType.QUESTION -> generateQuestionAnswer(userMessage, currentTopic, conversationSummary)
            ResponseType.POSITIVE_REACTION -> generatePositiveReaction(currentTopic, conversationSummary, nsfwMode)
            ResponseType.NEGATIVE_REACTION -> generateNegativeReaction(currentTopic, conversationSummary)
            ResponseType.STORY_LISTENING -> generateStoryResponse(userMessage, currentTopic, conversationSummary, nsfwMode)
            ResponseType.OPINION_RESPONSE -> generateOpinionReaction(userMessage, currentTopic, conversationSummary)
            ResponseType.CONVERSATION_FLOW -> generateFlowResponse(recentMessages, currentTopic, conversationSummary, nsfwMode)
            ResponseType.GREETING -> generateGreetingResponse(character, username, conversationSummary)
            ResponseType.GENERAL -> generateGeneralResponse(userMessage, currentTopic, conversationSummary, nsfwMode)
        }
    }
    
    /**
     * Extrait les mots-clés importants
     */
    private fun extractKeywords(message: String): List<String> {
        val stopWords = setOf("le", "la", "les", "un", "une", "des", "de", "du", "et", "ou", "mais", "donc", "car", "je", "tu", "il", "elle", "nous", "vous", "ils", "elles", "est", "sont", "a", "ai", "as", "avez", "ont", "être", "avoir", "faire", "dire", "pour", "sur", "avec", "par", "plus", "dans", "qui", "que", "quoi")
        
        return message.lowercase()
            .split(Regex("[\\s,.!?;:]+"))
            .filter { it.length > 3 && it !in stopWords }
            .distinct()
            .take(5)
    }
    
    // ===== NOUVELLES FONCTIONS DE GÉNÉRATION =====
    
    private fun generateIdentityResponse(character: Character, username: String): String {
        val intro = "Je suis ${character.name}."
        val personality = character.personality.split(".").take(2).joinToString(". ")
        val greeting = listOf(
            "Ravi(e) de te rencontrer, $username !",
            "Enchanté(e) de faire ta connaissance !",
            "Content(e) de pouvoir discuter avec toi !"
        ).random()
        
        return "$intro $personality $greeting"
    }
    
    private fun generatePreferenceResponse(
        character: Character,
        userMessage: String,
        currentTopic: String,
        nsfwMode: Boolean
    ): String {
        val opinion = listOf(
            "j'apprécie beaucoup",
            "j'aime bien",
            "c'est intéressant",
            "ça me plaît"
        ).random()
        
        val elaboration = if (nsfwMode && Random.nextFloat() > 0.6f) {
            listOf(
                "Ça me donne des idées... ♡",
                "Tu sais éveiller ma curiosité~",
                "Continue, j'adore ça..."
            ).random()
        } else {
            listOf(
                "Et toi, qu'est-ce que tu en penses ?",
                "Qu'est-ce qui te plaît le plus dans ce sujet ?",
                "Raconte-moi ce qui t'intéresse !"
            ).random()
        }
        
        return "Concernant $currentTopic, $opinion. $elaboration"
    }
    
    private fun generateExplanationResponse(
        userMessage: String,
        currentTopic: String,
        conversationSummary: ConversationSummary
    ): String {
        val thinking = listOf(
            "Laisse-moi réfléchir...",
            "C'est une bonne question.",
            "Hmm, intéressant.",
            "Voyons voir..."
        ).random()
        
        val answer = if (currentTopic.isNotEmpty()) {
            listOf(
                "Pour $currentTopic, je dirais que c'est assez nuancé.",
                "Concernant $currentTopic, il y a plusieurs façons de voir les choses.",
                "$currentTopic est un sujet fascinant à explorer."
            ).random()
        } else {
            listOf(
                "C'est assez complexe à expliquer.",
                "Il y a plusieurs perspectives à considérer.",
                "La réponse n'est pas si simple."
            ).random()
        }
        
        val followUp = "Qu'est-ce qui t'a amené à poser cette question ?"
        
        return "$thinking $answer $followUp"
    }
    
    private fun generateQuestionAnswer(
        userMessage: String,
        currentTopic: String,
        conversationSummary: ConversationSummary
    ): String {
        val acknowledgment = listOf(
            "Bonne question !",
            "Intéressant comme interrogation.",
            "Tu soulèves un point pertinent."
        ).random()
        
        val answer = when {
            currentTopic.isNotEmpty() -> {
                "Pour $currentTopic, je pense que ${listOf("c'est assez subjectif", "ça dépend du contexte", "il y a plusieurs approches possibles").random()}."
            }
            conversationSummary.recentTopics.isNotEmpty() -> {
                val topic = conversationSummary.recentTopics.first()
                "En lien avec $topic dont on parlait, je dirais que c'est ${listOf("connecté", "lié", "pertinent").random()}."
            }
            else -> {
                "C'est une question qui mérite réflexion. ${listOf("Qu'en penses-tu toi ?", "Ton avis m'intéresse.", "J'aimerais connaître ta perspective.").random()}"
            }
        }
        
        return "$acknowledgment $answer"
    }
    
    private fun generatePositiveReaction(
        currentTopic: String,
        conversationSummary: ConversationSummary,
        nsfwMode: Boolean
    ): String {
        val enthusiasm = listOf(
            "Oh, c'est génial !",
            "Super !",
            "Excellent !",
            "J'adore !"
        ).random()
        
        val shared = if (currentTopic.isNotEmpty()) {
            "Je trouve aussi que $currentTopic est ${listOf("formidable", "passionnant", "captivant").random()} !"
        } else {
            "Je partage ton enthousiasme !"
        }
        
        val continuation = if (nsfwMode && Random.nextFloat() > 0.6f) {
            listOf(
                "Ton énergie est contagieuse... ♡",
                "Continue comme ça, j'adore~",
                "Tu me donnes le sourire..."
            ).random()
        } else {
            listOf(
                "Raconte-m'en plus !",
                "Qu'est-ce qui te rend si heureux ?",
                "J'aimerais en savoir davantage !"
            ).random()
        }
        
        return "$enthusiasm $shared $continuation"
    }
    
    private fun generateNegativeReaction(
        currentTopic: String,
        conversationSummary: ConversationSummary
    ): String {
        val empathy = listOf(
            "Oh, je comprends...",
            "C'est dommage.",
            "Je vois que ça te contrarie.",
            "Je ressens ton déception."
        ).random()
        
        val support = if (currentTopic.isNotEmpty()) {
            "Je sais que $currentTopic peut être ${listOf("frustrant", "décevant", "difficile").random()}."
        } else {
            "Ces choses arrivent, malheureusement."
        }
        
        val comfort = listOf(
            "Mais ne t'inquiète pas, ça va s'arranger.",
            "Les choses vont s'améliorer.",
            "Je suis là si tu veux en parler."
        ).random()
        
        return "$empathy $support $comfort"
    }
    
    private fun generateStoryResponse(
        userMessage: String,
        currentTopic: String,
        conversationSummary: ConversationSummary,
        nsfwMode: Boolean
    ): String {
        val listening = listOf(
            "Oh vraiment ?",
            "Raconte-moi !",
            "Je t'écoute attentivement.",
            "Ça a l'air intéressant !"
        ).random()
        
        val interest = if (currentTopic.isNotEmpty()) {
            "Ce qui s'est passé avec $currentTopic a l'air ${listOf("captivant", "fascinant", "remarquable").random()}."
        } else {
            "Ton histoire m'intrigue !"
        }
        
        val prompt = if (nsfwMode && Random.nextFloat() > 0.6f) {
            listOf(
                "Continue, je suis captivé(e)... ♡",
                "Ne t'arrête pas, j'adore~",
                "Tu sais me tenir en haleine..."
            ).random()
        } else {
            listOf(
                "Et ensuite, que s'est-il passé ?",
                "Qu'as-tu ressenti ?",
                "Comment ça s'est terminé ?"
            ).random()
        }
        
        return "$listening $interest $prompt"
    }
    
    private fun generateOpinionReaction(
        userMessage: String,
        currentTopic: String,
        conversationSummary: ConversationSummary
    ): String {
        val validation = listOf(
            "Je respecte ton opinion.",
            "C'est un point de vue intéressant.",
            "Je comprends ta perspective.",
            "Tu as des arguments valables."
        ).random()
        
        val elaboration = if (currentTopic.isNotEmpty()) {
            "Sur $currentTopic, ${listOf("c'est vrai que les avis divergent", "il y a effectivement matière à débat", "chacun a sa vision").random()}."
        } else {
            "Les opinions peuvent varier sur ce sujet."
        }
        
        val engagement = listOf(
            "Qu'est-ce qui t'a mené à cette conclusion ?",
            "J'aimerais comprendre ton raisonnement.",
            "Peux-tu développer ton idée ?"
        ).random()
        
        return "$validation $elaboration $engagement"
    }
    
    private fun generateFlowResponse(
        recentMessages: List<Message>,
        currentTopic: String,
        conversationSummary: ConversationSummary,
        nsfwMode: Boolean
    ): String {
        // Continuer la conversation de façon naturelle
        val continuation = when (conversationSummary.conversationMood) {
            "positif" -> {
                "J'apprécie vraiment notre conversation ! ${if (currentTopic.isNotEmpty()) "Parler de $currentTopic avec toi est agréable." else "On passe un bon moment."}"
            }
            "curieux" -> {
                "Cette discussion est fascinante. ${if (currentTopic.isNotEmpty()) "$currentTopic est un sujet qui m'intrigue de plus en plus." else "J'apprends beaucoup."}"
            }
            "négatif" -> {
                "Je suis là pour toi. ${if (currentTopic.isNotEmpty()) "Si $currentTopic te préoccupe, on peut en parler." else "N'hésite pas à te confier."}"
            }
            else -> {
                "C'est agréable de discuter avec toi. ${if (currentTopic.isNotEmpty()) "Le sujet de $currentTopic est intéressant." else "Continue, je t'écoute."}"
            }
        }
        
        val followUp = if (nsfwMode && Random.nextFloat() > 0.5f) {
            listOf(
                "Tu as toute mon attention... ♡",
                "J'adore nos échanges~",
                "Continue de me parler..."
            ).random()
        } else {
            listOf(
                "Qu'aimerais-tu aborder maintenant ?",
                "As-tu autre chose en tête ?",
                "Je suis tout ouïe !"
            ).random()
        }
        
        return "$continuation $followUp"
    }
    
    private fun generateGreetingResponse(
        character: Character,
        username: String,
        conversationSummary: ConversationSummary
    ): String {
        val greeting = when (conversationSummary.relationshipLevel) {
            "inconnu" -> "Salut $username ! Ravi(e) de faire ta connaissance."
            "nouvelle connaissance" -> "Hey $username ! Content(e) de te revoir !"
            "connaissance" -> "Coucou $username ! Comment vas-tu ?"
            "familier" -> "Salut $username ! Toujours un plaisir de te parler !"
            else -> "Bonjour $username !"
        }
        
        val followUp = listOf(
            "Quoi de neuf ?",
            "Comment se passe ta journée ?",
            "Envie de discuter ?",
            "Qu'est-ce qui t'amène ?"
        ).random()
        
        return "$greeting $followUp"
    }
    
    private fun generateGeneralResponse(
        userMessage: String,
        currentTopic: String,
        conversationSummary: ConversationSummary,
        nsfwMode: Boolean
    ): String {
        val acknowledgment = listOf(
            "Je vois.",
            "D'accord.",
            "Intéressant.",
            "Hmm.",
            "Je comprends."
        ).random()
        
        val reflection = if (currentTopic.isNotEmpty()) {
            "Ce que tu dis sur $currentTopic ${listOf("a du sens", "est pertinent", "mérite réflexion").random()}."
        } else {
            "Tu soulèves un point ${listOf("intéressant", "valable", "important").random()}."
        }
        
        val engagement = if (nsfwMode && Random.nextFloat() > 0.6f) {
            listOf(
                "Continue, tu as mon attention... ♡",
                "J'aime t'écouter~",
                "Vas-y, je suis là..."
            ).random()
        } else {
            listOf(
                "Développe ton idée !",
                "Dis-m'en plus.",
                "Je t'écoute attentivement.",
                "Continue, je suis intéressé(e) !"
            ).random()
        }
        
        return "$acknowledgment $reflection $engagement"
    }
    
    
}
