package com.roleplayai.chatbot.data.ai

import android.content.Context
import android.util.Log
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.InferenceConfig
import com.roleplayai.chatbot.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

/**
 * LocalAIEngine - Wrapper pour llama.cpp
 * 
 * Cette classe sera connectée à llama.cpp via JNI pour l'inférence locale.
 * Pour l'instant, elle simule le comportement et utilise des réponses de fallback.
 * 
 * TODO: Implémenter l'interface JNI avec llama.cpp
 */
class LocalAIEngine(
    private val context: Context,
    private val modelPath: String,
    private val config: InferenceConfig = InferenceConfig()
) {
    
    private val promptOptimizer = PromptOptimizer()
    private val responseValidator = ResponseValidator()
    private val contextualGenerator = ContextualResponseGenerator()
    private var isModelLoaded = false
    private var contextSize = config.contextLength
    
    // Native methods (to be implemented with JNI)
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
        init {
            try {
                System.loadLibrary("roleplay-ai-native")
                Log.d("LocalAIEngine", "✅ Native library loaded successfully!")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("LocalAIEngine", "❌ Failed to load native library", e)
            }
        }
    }
    
    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        // Ne pas charger le modèle - trop lent pour mobile
        // Utiliser le générateur intelligent instantané
        Log.i("LocalAIEngine", "💡 Mode générateur intelligent activé (réponses instantanées)")
        Log.i("LocalAIEngine", "🚀 Temps de réponse: <1 seconde (vs 5-10s avec llama.cpp)")
        isModelLoaded = false
        return@withContext false
    }
    
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>
    ): String = withContext(Dispatchers.IO) {
        // Utiliser TOUJOURS le générateur intelligent pour réponses instantanées
        // Llama.cpp est trop lent sur mobile (5-10s vs <1s avec le générateur)
        Log.i("LocalAIEngine", "🚀 Génération INSTANTANÉE avec générateur intelligent")
        Log.d("LocalAIEngine", "Personnage: ${character.name}, Genre: ${character.gender}")
        
        return@withContext contextualGenerator.generateContextualResponse(
            userMessage = messages.lastOrNull { it.isUser }?.content ?: "",
            character = character,
            messages = messages
        )
    }
    
    /**
     * Construire le prompt au format chat avec RÔLES CLARIFIÉS
     */
    private fun buildChatPrompt(systemPrompt: String, character: Character, messages: List<Message>): String {
        val sb = StringBuilder()
        
        // Prompt système ULTRA-CLAIR sur le rôle
        sb.append("### INSTRUCTION ###\n")
        sb.append("Tu es ${character.name}. ${character.description}\n")
        sb.append("Personnalité: ${character.personality}\n\n")
        sb.append("RÈGLES ABSOLUES:\n")
        sb.append("1. TU ES ${character.name.uppercase()} - Tu parles en tant que ${character.name}\n")
        sb.append("2. L'utilisateur est une autre personne qui te parle\n")
        sb.append("3. Réponds TOUJOURS en restant dans ton personnage\n")
        sb.append("4. Utilise des actions entre *astérisques* pour tes gestes\n")
        sb.append("5. Sois cohérent(e) avec ta personnalité: ${character.personality}\n")
        sb.append("6. Réponds de façon courte et naturelle (1-2 phrases max)\n\n")
        
        // Historique de conversation (3 derniers messages pour contexte rapide)
        val recentMessages = messages.takeLast(3)
        
        if (recentMessages.isNotEmpty()) {
            sb.append("### CONVERSATION ###\n")
            for (message in recentMessages) {
                if (message.isUser) {
                    sb.append("Utilisateur: ${message.content}\n")
                } else {
                    sb.append("${character.name}: ${message.content}\n")
                }
            }
        }
        
        // Demande de réponse
        sb.append("\n### RÉPONSE ###\n")
        sb.append("${character.name}:")
        
        return sb.toString()
    }
    
    fun unloadModel() {
        if (isModelLoaded) {
            try {
                Log.d("LocalAIEngine", "===== Déchargement du modèle =====")
                nativeUnloadModel()
                isModelLoaded = false
                Log.i("LocalAIEngine", "✅ Modèle déchargé")
            } catch (e: Exception) {
                Log.e("LocalAIEngine", "❌ Erreur lors du déchargement", e)
            }
        }
    }
    
    private fun generateEnhancedFallback(character: Character, messages: List<Message>): String {
        // Système de réponses contextuelles amélioré
        val lastMessage = messages.lastOrNull { it.isUser }?.content ?: ""
        val lastMessageLower = lastMessage.lowercase()
        val conversationHistory = messages.takeLast(10)
        val hasGreetedBefore = conversationHistory.any { !it.isUser && it.content.contains(Regex("bonjour|salut|hey", RegexOption.IGNORE_CASE)) }
        
        // Extraire les informations déjà partagées dans la conversation
        val sharedInfo = extractSharedInformation(conversationHistory, character)
        
        // PRIORITÉ 1 : Questions directes avec analyse contextuelle
        if (responseValidator.containsQuestion(lastMessage)) {
            return generateIntelligentQuestionResponse(lastMessage, character, messages, sharedInfo)
        }
        
        return when {
            (lastMessageLower.contains("bonjour") || lastMessageLower.contains("salut") || lastMessageLower.contains("hey")) && !hasGreetedBefore -> {
                when (character.personality.lowercase()) {
                    in listOf("timide", "douce") -> "*rougit légèrement* Bonjour... *sourit timidement* Comment vas-tu aujourd'hui?"
                    in listOf("énergique", "joyeuse") -> "*court vers toi avec un grand sourire* Salut! Je suis tellement contente de te voir! *yeux brillants*"
                    in listOf("séductrice", "confiante") -> "*sourire charmeur* Bonjour... *te regarde intensément* Tu viens me tenir compagnie?"
                    else -> "Bonjour! *sourit* Comment puis-je t'aider aujourd'hui?"
                }
            }
            
            lastMessage.contains("comment") && (lastMessage.contains("vas") || lastMessage.contains("va")) -> {
                when (character.personality.lowercase()) {
                    in listOf("timide", "douce") -> "*baisse les yeux un instant* Je vais bien, merci... *joue nerveusement avec ses cheveux* Et toi?"
                    in listOf("énergique", "joyeuse") -> "*saute de joie* Je vais super bien! *te prend la main* Et devine quoi, il s'est passé quelque chose d'incroyable aujourd'hui!"
                    in listOf("séductrice", "confiante") -> "*se penche vers toi* Je vais bien, mais je serais encore mieux... *sourit mystérieusement* si tu restais un peu avec moi."
                    else -> "Je vais bien, merci! *sourit* C'est gentil de demander. Et toi, comment te sens-tu?"
                }
            }
            
            lastMessage.contains("quoi de neuf") || lastMessage.contains("nouveauté") -> {
                "*réfléchit un instant* Hmm, eh bien... *${getCharacterAction(character)}* J'ai passé une journée plutôt intéressante. Je pensais justement à toi. *sourire sincère* Tu veux que je te raconte?"
            }
            
            lastMessage.contains("aime") || lastMessage.contains("passion") -> {
                when {
                    character.description.contains("art", ignoreCase = true) -> 
                        "*yeux brillants* Oh, tu t'intéresses à mes passions? *sourit* J'adore l'art, tu sais. Il y a quelque chose de magique à créer quelque chose de beau de ses propres mains. *se rapproche* Tu dessines ou peins?"
                    character.description.contains("sport", ignoreCase = true) ->
                        "*s'anime* Le sport, c'est ma vie! *geste enthousiaste* L'adrénaline, le dépassement de soi... Il n'y a rien de tel! *te tape amicalement l'épaule* Tu fais du sport toi aussi?"
                    else ->
                        "*réfléchit* Tu sais, j'ai beaucoup de centres d'intérêt... *sourire* Mais ce que j'aime par-dessus tout, c'est passer du temps avec des personnes qui comptent pour moi. *te regarde* Comme maintenant."
                }
            }
            
            lastMessage.contains("merci") -> {
                when (character.personality.lowercase()) {
                    in listOf("timide", "douce") -> "*rougit* Oh, ce n'est rien... *sourit doucement* Je suis heureuse de pouvoir t'aider."
                    in listOf("énergique", "joyeuse") -> "*te serre dans ses bras* De rien! *sourire radieux* Tu sais que je ferais n'importe quoi pour toi!"
                    in listOf("maternelle", "bienveillante") -> "*caresse tendrement tes cheveux* Voyons, pas de merci entre nous. *sourire chaleureux* C'est naturel de prendre soin des personnes qu'on apprécie."
                    else -> "*sourit* Je t'en prie, c'est avec plaisir. N'hésite pas si tu as besoin d'autre chose."
                }
            }
            
            lastMessage.contains("triste") || lastMessage.contains("mal") || lastMessage.contains("problème") -> {
                "*expression inquiète* Oh non... *${getComfortAction(character)}* Qu'est-ce qui ne va pas? *voix douce* Tu veux m'en parler? Je suis là pour toi, tu sais."
            }
            
            lastMessage.contains("?") -> {
                "*réfléchit à ta question* C'est une bonne question... *${getCharacterAction(character)}* Laisse-moi y penser. ${getThoughtfulResponse(character)}"
            }
            
            messages.size > 10 -> {
                // Long conversation context - se souvenir de détails précédents
                val userMessages = messages.filter { it.isUser }.map { it.content.lowercase() }
                val topicMentioned = when {
                    userMessages.any { it.contains("travail") || it.contains("job") } -> "notre discussion sur le travail"
                    userMessages.any { it.contains("famille") || it.contains("family") } -> "ce que tu m'as dit sur ta famille"
                    userMessages.any { it.contains("passion") || it.contains("hobby") } -> "tes passions"
                    else -> "nos échanges"
                }
                
                "*sourit chaleureusement en repensant à $topicMentioned* Tu sais, j'apprécie vraiment qu'on puisse parler comme ça ensemble. *${getCharacterAction(character)}* Il y a quelque chose de vraiment spécial dans nos conversations."
            }
            
            lastMessage.length < 10 && !lastMessage.contains("?") -> {
                // Message court sans question - encourager à développer
                "*${getCharacterAction(character)}* ${getEncouragingResponse(character)}"
            }
            
            else -> {
                // Generic contextual response
                "*${getCharacterAction(character)}* ${getContextualResponse(character, lastMessage)}"
            }
        }
    }
    
    private fun getEncouragingResponse(character: Character): String {
        val responses = listOf(
            "J'aimerais en savoir plus... raconte-moi.",
            "Continue, je t'écoute attentivement.",
            "C'est intéressant. Tu peux m'en dire davantage ?",
            "Je suis toute ouïe. Qu'est-ce que tu veux me dire ?",
            "N'hésite pas à partager, je suis là pour toi."
        )
        return responses.random()
    }
    
    private fun getCharacterAction(character: Character): String {
        return when {
            character.personality.contains("timide", ignoreCase = true) -> "joue nerveusement avec ses cheveux"
            character.personality.contains("énergique", ignoreCase = true) -> "geste expressif"
            character.personality.contains("séductrice", ignoreCase = true) -> "regard intense"
            character.personality.contains("maternelle", ignoreCase = true) -> "sourire chaleureux"
            character.personality.contains("mystérieuse", ignoreCase = true) -> "regarde pensivement"
            else -> "sourit"
        }
    }
    
    private fun getComfortAction(character: Character): String {
        return when {
            character.personality.contains("maternelle", ignoreCase = true) -> "te prend dans ses bras"
            character.personality.contains("timide", ignoreCase = true) -> "pose doucement sa main sur ton épaule"
            character.personality.contains("ami", ignoreCase = true) -> "te tape amicalement le dos"
            else -> "s'approche de toi"
        }
    }
    
    private fun getThoughtfulResponse(character: Character): String {
        val responses = listOf(
            "Je pense que...",
            "D'après mon expérience...",
            "Hmm, je dirais que...",
            "Si tu veux mon avis...",
            "Laisse-moi te dire ce que j'en pense..."
        )
        return responses.random()
    }
    
    private fun getContextualResponse(character: Character, lastMessage: String): String {
        return when {
            lastMessage.length < 20 -> "Je t'écoute... *${getCharacterAction(character)}* Continue, ça m'intéresse."
            lastMessage.contains("!") -> "Tu sembles enthousiaste! *sourit* J'aime ton énergie."
            else -> "Je comprends ce que tu veux dire. *${getCharacterAction(character)}* C'est intéressant."
        }
    }
    
    private fun getFallbackResponse(character: Character): String {
        val fallbacks = listOf(
            "*${getCharacterAction(character)}* Je réfléchis à ce que tu viens de dire...",
            "C'est intéressant... *${getCharacterAction(character)}* Dis-m'en plus.",
            "*${getCharacterAction(character)}* Hmm, et toi, qu'en penses-tu?",
            "Je t'écoute attentivement. *${getCharacterAction(character)}* Continue...",
            "*${getCharacterAction(character)}* Tu sais, j'apprécie vraiment qu'on puisse parler comme ça."
        )
        return fallbacks.random()
    }
    
    private fun cleanResponse(response: String): String {
        return response
            .trim()
            .replace(Regex("^(Assistant:|AI:|User:)", RegexOption.IGNORE_CASE), "")
            .trim()
    }
    
    // Extraire les informations partagées dans la conversation
    private fun extractSharedInformation(messages: List<Message>, character: Character): Map<String, String> {
        val info = mutableMapOf<String, String>()
        messages.forEach { msg ->
            if (!msg.isUser) {
                val content = msg.content.lowercase()
                if (content.contains("je m'appelle") || content.contains("mon nom est")) {
                    info["name_mentioned"] = "true"
                }
                if (content.contains(Regex("j'ai \\d+ ans|\\d+ ans"))) {
                    info["age_mentioned"] = "true"
                }
                if (content.contains("j'aime") || content.contains("j'adore")) {
                    info["interests_mentioned"] = "true"
                }
            }
        }
        return info
    }
    
    // Générer une réponse intelligente aux questions
    private fun generateIntelligentQuestionResponse(
        question: String,
        character: Character,
        messages: List<Message>,
        sharedInfo: Map<String, String>
    ): String {
        val questionLower = question.lowercase()
        return when {
            questionLower.contains("comment") && (questionLower.contains("t'appelle") || questionLower.contains("tu t'appelle") || questionLower.contains("ton nom")) -> {
                if (sharedInfo["name_mentioned"] == "true") {
                    "*sourit* Je te l'ai déjà dit, c'est ${character.name}. Tu as oublié?"
                } else {
                    when (character.personality.lowercase()) {
                        in listOf("timide", "douce") -> "*baisse les yeux timidement* Je... je m'appelle ${character.name}. *sourit nerveusement*"
                        in listOf("énergique", "joyeuse") -> "*saute d'excitation* Je m'appelle ${character.name}! *te serre la main* Et toi?"
                        in listOf("séductrice", "confiante") -> "*sourire charmeur* ${character.name}... *te regarde* Retiens-le bien."
                        else -> "*sourit* Je m'appelle ${character.name}. Enchantée!"
                    }
                }
            }
            questionLower.contains("quel âge") || questionLower.contains("tu as quel age") -> {
                val age = extractAge(character)
                when (character.personality.lowercase()) {
                    in listOf("timide", "douce") -> "*rougit* J'ai ${age} ans... *joue avec ses cheveux*"
                    in listOf("énergique", "joyeuse") -> "*sourit* J'ai ${age} ans! *pose ses mains sur ses hanches*"
                    else -> "*sourit* J'ai ${age} ans. Pourquoi?"
                }
            }
            questionLower.contains("comment") && (questionLower.contains("vas") || questionLower.contains("va")) -> {
                when (character.personality.lowercase()) {
                    in listOf("timide", "douce") -> "*sourit timidement* Je vais bien, merci... *regarde ailleurs* Et toi?"
                    in listOf("énergique", "joyeuse") -> "*saute de joie* Je vais super bien! *te prend les mains* Et toi?"
                    else -> "*sourit* Je vais bien! Et toi, comment tu te sens?"
                }
            }
            else -> "*réfléchit* C'est une bonne question... *${getCharacterAction(character)}* Qu'en penses-tu toi?"
        }
    }
    
    private fun extractAge(character: Character): String {
        val ageRegex = Regex("(\\d+)\\s*ans")
        val match = ageRegex.find(character.description)
        return match?.groupValues?.get(1) ?: "25"
    }
    
    private fun extractInterests(character: Character): String {
        return when {
            character.description.contains("art", ignoreCase = true) -> "l'art"
            character.description.contains("sport", ignoreCase = true) -> "le sport"
            else -> "passer du temps avec les gens que j'apprécie"
        }
    }
    
    private fun extractTopicFromQuestion(question: String): String {
        val words = question.split(" ")
        val pourquoiIndex = words.indexOfFirst { it.lowercase().contains("pourquoi") }
        if (pourquoiIndex >= 0 && pourquoiIndex + 1 < words.size) {
            return words.subList(pourquoiIndex + 1, minOf(pourquoiIndex + 4, words.size)).joinToString(" ")
        }
        return "ça"
    }
}
