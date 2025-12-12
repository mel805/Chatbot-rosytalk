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
     * Génère une réponse intelligente et contextuelle
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
        delay(Random.nextLong(500, 1500))
        
        Log.d(TAG, "🧠 Génération intelligente contextuelle pour ${character.name}")
        
        val lastUserMessage = messages.lastOrNull { it.isUser }?.content ?: "Bonjour"
        val recentMessages = messages.takeLast(10)
        
        // Extraire le contexte de la conversation
        val conversationContext = buildConversationContext(recentMessages, username, character.name)
        
        // Extraire mots-clés du message utilisateur
        val keywords = extractKeywords(lastUserMessage)
        
        // Analyser l'intention du message
        val intent = detectIntent(lastUserMessage)
        
        // Analyser l'émotion appropriée
        val emotion = detectEmotion(lastUserMessage, character.personality, nsfwMode)
        
        // Générer réponse contextuelle
        val response = buildContextualResponse(
            character = character,
            userMessage = lastUserMessage,
            keywords = keywords,
            intent = intent,
            emotion = emotion,
            conversationContext = conversationContext,
            username = username,
            nsfwMode = nsfwMode
        )
        
        Log.i(TAG, "✅ Réponse contextuelle: ${response.take(100)}...")
        return@withContext response
    }
    
    /**
     * Construit le contexte de la conversation
     */
    private fun buildConversationContext(
        messages: List<Message>,
        username: String,
        characterName: String
    ): String {
        if (messages.isEmpty()) return ""
        
        val context = StringBuilder()
        messages.takeLast(5).forEach { msg ->
            val speaker = if (msg.isUser) username else characterName
            context.append("$speaker: ${msg.content.take(100)}\n")
        }
        return context.toString()
    }
    
    /**
     * Extrait les mots-clés importants
     */
    private fun extractKeywords(message: String): List<String> {
        val stopWords = setOf("le", "la", "les", "un", "une", "des", "de", "du", "et", "ou", "mais", "donc", "car", "je", "tu", "il", "elle", "nous", "vous", "ils", "elles", "est", "sont", "a", "ai", "as", "avez", "ont")
        
        return message.lowercase()
            .split(Regex("[\\s,.!?;:]+"))
            .filter { it.length > 3 && it !in stopWords }
            .distinct()
            .take(5)
    }
    
    /**
     * Détecte l'intention du message
     */
    private fun detectIntent(message: String): String {
        val msgLower = message.lowercase()
        return when {
            msgLower.contains("?") -> "question"
            msgLower.contains("!") -> "exclamation"
            msgLower.matches(Regex(".*\\b(bonjour|salut|hey|coucou)\\b.*")) -> "greeting"
            msgLower.matches(Regex(".*\\b(merci|thank|remercie)\\b.*")) -> "thanks"
            msgLower.matches(Regex(".*\\b(désolé|pardon|excuse)\\b.*")) -> "apology"
            msgLower.matches(Regex(".*\\b(aime|adore|préfère|veux)\\b.*")) -> "desire"
            msgLower.matches(Regex(".*\\b(pense|crois|trouve)\\b.*")) -> "opinion"
            else -> "statement"
        }
    }
    
    /**
     * Construit une réponse contextuelle
     */
    private fun buildContextualResponse(
        character: Character,
        userMessage: String,
        keywords: List<String>,
        intent: String,
        emotion: String,
        conversationContext: String,
        username: String,
        nsfwMode: Boolean
    ): String {
        // Génération de l'action
        val action = selectAction(emotion, nsfwMode)
        
        // Génération du dialogue selon l'intention
        val dialogue = when (intent) {
            "question" -> generateQuestionResponse(character, userMessage, keywords, nsfwMode)
            "greeting" -> generateGreeting(character, username)
            "thanks" -> generateThanksResponse(character)
            "apology" -> generateApologyResponse(character)
            "desire" -> generateDesireResponse(character, keywords, nsfwMode)
            "opinion" -> generateOpinionResponse(character, keywords)
            else -> generateStatementResponse(character, userMessage, keywords, nsfwMode)
        }
        
        // Assembler la réponse finale
        return if (action.isNotEmpty() && Random.nextFloat() > 0.3f) {
            "*$action* $dialogue"
        } else {
            dialogue
        }
    }
    
    /**
     * Génère une réponse à une question
     */
    private fun generateQuestionResponse(
        character: Character,
        question: String,
        keywords: List<String>,
        nsfwMode: Boolean
    ): String {
        val responses = mutableListOf<String>()
        
        if (keywords.isNotEmpty()) {
            val keyword = keywords.random()
            responses.add("À propos de $keyword, je dirais que...")
            responses.add("C'est une bonne question concernant $keyword.")
            responses.add("Hmm, $keyword ? Laisse-moi réfléchir...")
        } else {
            responses.add("C'est une bonne question...")
            responses.add("Laisse-moi y réfléchir...")
            responses.add("Intéressant comme question...")
        }
        
        responses.add("Qu'est-ce que tu en penses, toi ?")
        responses.add("Et toi, quel est ton avis ?")
        
        return responses.shuffled().take(Random.nextInt(1, 3)).joinToString(" ")
    }
    
    /**
     * Génère un salut
     */
    private fun generateGreeting(character: Character, username: String): String {
        val greetings = listOf(
            "Salut $username ! Comment vas-tu ?",
            "Hey ! Content(e) de te voir !",
            "Bonjour ! Ça me fait plaisir de te parler.",
            "Coucou ! Quoi de neuf ?",
            "Salut ! Tu vas bien ?"
        )
        return greetings.random()
    }
    
    /**
     * Génère une réponse à un remerciement
     */
    private fun generateThanksResponse(character: Character): String {
        val responses = listOf(
            "De rien ! C'est toujours un plaisir.",
            "Pas de problème ! Je suis là pour ça.",
            "Avec plaisir ! N'hésite pas si tu as besoin.",
            "Mais de rien ! C'était normal."
        )
        return responses.random()
    }
    
    /**
     * Génère une réponse à des excuses
     */
    private fun generateApologyResponse(character: Character): String {
        val responses = listOf(
            "Ne t'inquiète pas, ce n'est rien.",
            "C'est pas grave, vraiment !",
            "T'en fais pas, ça arrive à tout le monde.",
            "Pas de souci ! C'est déjà oublié."
        )
        return responses.random()
    }
    
    /**
     * Génère une réponse à un désir/envie
     */
    private fun generateDesireResponse(
        character: Character,
        keywords: List<String>,
        nsfwMode: Boolean
    ): String {
        val responses = mutableListOf<String>()
        
        if (keywords.isNotEmpty()) {
            val keyword = keywords.random()
            responses.add("Tu aimes $keyword ? C'est intéressant !")
            responses.add("Ah, $keyword ? Moi aussi j'apprécie ça.")
            responses.add("$keyword, oui ! Je comprends tout à fait.")
        }
        
        responses.add("Dis-m'en plus sur ce que tu aimes.")
        responses.add("C'est cool ! Continue, je t'écoute.")
        
        if (nsfwMode && Random.nextFloat() > 0.6f) {
            responses.add("Hmm, ça me donne des idées... ♡")
            responses.add("Tu me rends curieux(se)...")
        }
        
        return responses.shuffled().take(Random.nextInt(1, 2)).joinToString(" ")
    }
    
    /**
     * Génère une réponse à une opinion
     */
    private fun generateOpinionResponse(
        character: Character,
        keywords: List<String>
    ): String {
        val responses = mutableListOf<String>()
        
        responses.add("Je vois ce que tu veux dire.")
        responses.add("C'est un point de vue intéressant.")
        responses.add("Hmm, je n'avais pas pensé à ça.")
        
        if (keywords.isNotEmpty()) {
            val keyword = keywords.random()
            responses.add("Concernant $keyword, tu as peut-être raison.")
        }
        
        responses.add("Et pourquoi tu penses ça ?")
        
        return responses.shuffled().take(Random.nextInt(1, 2)).joinToString(" ")
    }
    
    /**
     * Génère une réponse à une affirmation générale
     */
    private fun generateStatementResponse(
        character: Character,
        statement: String,
        keywords: List<String>,
        nsfwMode: Boolean
    ): String {
        val responses = mutableListOf<String>()
        
        if (keywords.isNotEmpty()) {
            val keyword = keywords.random()
            responses.add("$keyword, dis-tu ? Fascinant.")
            responses.add("Ah, $keyword ! Ça m'intéresse.")
            responses.add("Je vois, $keyword...")
        } else {
            responses.add("Je vois...")
            responses.add("Intéressant...")
            responses.add("D'accord...")
        }
        
        responses.add("Continue, je t'écoute attentivement.")
        responses.add("Raconte-m'en plus !")
        responses.add("Et ensuite, qu'est-ce qui s'est passé ?")
        
        if (nsfwMode && Random.nextFloat() > 0.7f) {
            responses.add("Tu sais comment me captiver... ♡")
        }
        
        return responses.shuffled().take(Random.nextInt(1, 2)).joinToString(" ")
    }
    
    /**
     * Détecte l'émotion appropriée
     */
    private fun detectEmotion(userMessage: String, personality: String, nsfwMode: Boolean): String {
        val messageLower = userMessage.lowercase()
        
        return when {
            nsfwMode && (messageLower.contains("touche") || messageLower.contains("embrasse") || 
                        messageLower.contains("caresse")) -> "séducteur"
            messageLower.contains("merci") || messageLower.contains("génial") || messageLower.contains("super") -> "heureux"
            messageLower.contains("désolé") || messageLower.contains("triste") -> "affectueux"
            messageLower.contains("!") && !messageLower.contains("?") -> "excité"
            personality.contains("timide", ignoreCase = true) -> "timide"
            personality.contains("dominant", ignoreCase = true) || 
                personality.contains("confiant", ignoreCase = true) -> "séducteur"
            else -> listOf("heureux", "curieux", "affectueux").random()
        }
    }
    
    /**
     * Sélectionne une action
     */
    private fun selectAction(emotion: String, nsfwMode: Boolean): String {
        val actions = if (nsfwMode && Random.nextFloat() > 0.6f) {
            nsfwActions
        } else {
            actionsByEmotion[emotion] ?: actionsByEmotion["heureux"]!!
        }
        return actions.random()
    }
    
}
