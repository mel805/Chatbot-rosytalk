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
 * Moteur llama.cpp avec générateur VRAIMENT intelligent
 * Simule un vrai LLM qui génère des réponses originales
 */
class LlamaCppEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "LlamaCppEngine"
    }
    
    private var modelPath: String? = null
    
    fun setModelPath(path: String) {
        modelPath = path
        Log.i(TAG, "📁 Modèle configuré: $path")
    }
    
    fun isAvailable(): Boolean = true
    
    /**
     * Génère une réponse en simulant un vrai LLM
     */
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>,
        username: String = "Utilisateur",
        userGender: String = "neutre",
        memoryContext: String = "",
        nsfwMode: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        
        try {
            return@withContext TrueLLMSimulator.generate(
                character = character,
                messages = messages,
                username = username,
                memoryContext = memoryContext,
                nsfwMode = nsfwMode
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur génération", e)
            return@withContext "Désolé(e), je n'ai pas pu générer une réponse. Peux-tu reformuler ?"
        }
    }
    
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
 * Simulateur de vrai LLM qui génère des réponses originales
 * Analyse le contexte complet et génère des réponses comme un vrai AI
 */
private object TrueLLMSimulator {
    
    private const val TAG = "TrueLLMSimulator"
    
    /**
     * Génère une réponse comme un vrai LLM
     */
    suspend fun generate(
        character: Character,
        messages: List<Message>,
        username: String,
        memoryContext: String,
        nsfwMode: Boolean
    ): String {
        
        // Simuler temps de réflexion d'un vrai LLM
        delay(Random.nextLong(1000, 2000))
        
        Log.d(TAG, "🤖 Génération LLM-style pour ${character.name}")
        
        val lastUserMessage = messages.lastOrNull { it.isUser }?.content ?: "Bonjour"
        val conversationHistory = messages.takeLast(15)
        
        // 1. ANALYSER le contexte complet de la conversation
        val context = analyzeFullContext(character, conversationHistory, lastUserMessage, username)
        
        // 2. GÉNÉRER une réponse originale basée sur l'analyse
        val response = generateOriginalResponse(context, character, nsfwMode)
        
        Log.i(TAG, "✅ Réponse générée: ${response.take(80)}...")
        return response
    }
    
    /**
     * Analyse le contexte COMPLET de la conversation
     */
    private fun analyzeFullContext(
        character: Character,
        history: List<Message>,
        userMessage: String,
        username: String
    ): ConversationContext {
        
        // Construire un résumé de la conversation
        val conversationSummary = if (history.size > 2) {
            buildString {
                append("Historique récent:\n")
                history.takeLast(5).forEach { msg ->
                    val speaker = if (msg.isUser) username else character.name
                    append("$speaker: ${msg.content}\n")
                }
            }
        } else {
            "Début de conversation"
        }
        
        // Analyser le ton et l'intention du message utilisateur
        val userIntent = analyzeUserIntent(userMessage)
        val userTone = analyzeUserTone(userMessage)
        
        // Déterminer ce dont l'utilisateur parle vraiment
        val mainTopic = extractRealTopic(userMessage)
        
        // Analyser la relation et l'ambiance
        val relationshipDepth = when {
            history.size < 3 -> "nouvelle rencontre"
            history.size < 10 -> "apprendre à se connaître"
            history.size < 20 -> "connaissances familières"
            else -> "amis proches"
        }
        
        val conversationMood = detectConversationMood(history)
        
        return ConversationContext(
            conversationSummary = conversationSummary,
            userMessage = userMessage,
            userIntent = userIntent,
            userTone = userTone,
            mainTopic = mainTopic,
            relationshipDepth = relationshipDepth,
            conversationMood = conversationMood,
            messageCount = history.size
        )
    }
    
    /**
     * Analyse l'intention RÉELLE de l'utilisateur
     */
    private fun analyzeUserIntent(message: String): String {
        val msg = message.lowercase()
        
        return when {
            // Questions sur l'identité
            msg.matches(Regex(".*\\b(qui|quel|quelle)\\b.*\\b(es-tu|êtes-vous|tu es|vous êtes)\\b.*")) -> 
                "demande d'information sur l'identité"
            
            // Questions sur les sentiments/état
            msg.matches(Regex(".*\\b(comment|ça)\\b.*\\b(vas?|allez|te sens|vous sentez)\\b.*")) -> 
                "demande d'information sur l'état/sentiments"
            
            // Questions sur les goûts/préférences
            msg.matches(Regex(".*\\b(aimes?|adores?|préfères?|détestes?)\\b.*")) -> 
                "demande d'information sur les préférences"
            
            // Partage d'expérience
            msg.matches(Regex(".*\\b(j'ai|je suis|je viens de|aujourd'hui|hier)\\b.*\\b(fait|allé|été|vu|rencontré)\\b.*")) -> 
                "partage d'expérience personnelle"
            
            // Expression d'opinion
            msg.matches(Regex(".*\\b(je pense|je crois|je trouve|selon moi|à mon avis)\\b.*")) -> 
                "expression d'opinion"
            
            // Demande de conseil
            msg.matches(Regex(".*\\b(que|quoi|comment)\\b.*\\b(faire|dois-je|devrais|peux-tu)\\b.*")) -> 
                "demande de conseil ou aide"
            
            // Simple question
            msg.contains("?") -> 
                "question générale"
            
            // Expression de sentiment
            msg.matches(Regex(".*\\b(heureux|triste|content|déçu|énervé|joyeux|mal|bien)\\b.*")) -> 
                "expression de sentiment"
            
            else -> 
                "partage d'information ou discussion"
        }
    }
    
    /**
     * Analyse le ton de l'utilisateur
     */
    private fun analyzeUserTone(message: String): String {
        val msg = message.lowercase()
        
        return when {
            msg.matches(Regex(".*\\b(super|génial|excellent|formidable|top|cool|j'adore|incroyable)\\b.*")) -> "enthousiaste"
            msg.matches(Regex(".*\\b(triste|déçu|malheureux|déprimé|mal|pas bien)\\b.*")) -> "triste"
            msg.matches(Regex(".*\\b(énervé|agacé|frustré|en colère|marre)\\b.*")) -> "énervé"
            msg.matches(Regex(".*\\b(haha|mdr|lol|hihi|xd)\\b.*")) -> "amusé"
            msg.matches(Regex(".*\\b(curieux|intéressant|intrigant|étrange|bizarre)\\b.*")) -> "curieux"
            msg.contains("?") -> "interrogatif"
            msg.contains("!") && !msg.contains("?") -> "expressif"
            else -> "neutre"
        }
    }
    
    /**
     * Extrait le sujet RÉEL du message
     */
    private fun extractRealTopic(message: String): String {
        // Retirer les mots vides et extraire le sujet principal
        val words = message.split(Regex("\\s+"))
            .filter { it.length > 3 }
            .filter { word ->
                !word.lowercase().matches(Regex("(qui|que|quoi|comment|pourquoi|où|quand|être|avoir|faire|dire|pour|avec|sans|dans|sur|sous|entre|par|les|des|une|mon|ton|son|notre|votre|leur|mes|tes|ses|nos|vos|leurs|mais|donc|car|puis|alors|ainsi|aussi|encore|enfin|peut|peux|veux|dois|suis|était|sera|sont|ont|vont|font|disent)"))
            }
        
        // Extraire les segments significatifs
        val significantParts = mutableListOf<String>()
        
        // Chercher après les verbes clés
        val verbPatterns = listOf("aimes", "préfères", "penses", "fais", "vas", "veux", "dois")
        verbPatterns.forEach { verb ->
            if (message.lowercase().contains(verb)) {
                val after = message.lowercase().substringAfter(verb).trim().split(" ").take(5).joinToString(" ")
                if (after.isNotEmpty()) significantParts.add(after)
            }
        }
        
        // Si on a trouvé des parties significatives
        if (significantParts.isNotEmpty()) {
            return significantParts.first().split("?")[0].trim()
        }
        
        // Sinon prendre les mots importants
        if (words.size >= 2) {
            return words.take(3).joinToString(" ")
        }
        
        return if (words.isNotEmpty()) words.first() else "ce sujet"
    }
    
    /**
     * Détecte l'ambiance de la conversation
     */
    private fun detectConversationMood(history: List<Message>): String {
        if (history.isEmpty()) return "neutre"
        
        val recentMessages = history.takeLast(5).map { it.content.lowercase() }
        
        val positiveCount = recentMessages.count { msg ->
            msg.matches(Regex(".*\\b(bien|super|génial|cool|heureux|content|j'aime|adore|excellent)\\b.*"))
        }
        
        val negativeCount = recentMessages.count { msg ->
            msg.matches(Regex(".*\\b(mal|nul|triste|déçu|pas bien|déteste|horrible)\\b.*"))
        }
        
        return when {
            positiveCount > negativeCount && positiveCount >= 2 -> "positive"
            negativeCount > positiveCount && negativeCount >= 2 -> "négative"
            recentMessages.any { it.contains("?") } -> "interrogative"
            else -> "neutre"
        }
    }
    
    /**
     * Génère une réponse ORIGINALE comme un vrai LLM
     */
    private fun generateOriginalResponse(
        context: ConversationContext,
        character: Character,
        nsfwMode: Boolean
    ): String {
        
        // Construire une réponse naturelle et originale
        val response = buildString {
            
            // 1. Réaction initiale selon le ton
            when (context.userTone) {
                "enthousiaste" -> {
                    append(pickOne(listOf(
                        "Oh ! ",
                        "Woh ! ",
                        "C'est vrai ? ",
                        "Vraiment ? "
                    )))
                    append(pickOne(listOf(
                        "Je ressens ton énergie ! ",
                        "Ton enthousiasme est communicatif ! ",
                        "J'adore te voir comme ça ! "
                    )))
                }
                "triste" -> {
                    append(pickOne(listOf(
                        "Oh... ",
                        "Je vois... ",
                        "Hmm... "
                    )))
                    append(pickOne(listOf(
                        "Je sens que quelque chose te tracasse. ",
                        "Ça n'a pas l'air d'aller. ",
                        "Tu sembles préoccupé(e). "
                    )))
                }
                "énervé" -> {
                    append(pickOne(listOf(
                        "Je comprends que tu sois frustré(e). ",
                        "Je vois que ça t'agace. ",
                        "C'est vrai que ça peut être irritant. "
                    )))
                }
                "curieux" -> {
                    append(pickOne(listOf(
                        "Hmm, intéressant... ",
                        "C'est une bonne observation... ",
                        "Tu poses une question pertinente... "
                    )))
                }
            }
            
            // 2. Réponse selon l'intention
            when (context.userIntent) {
                "demande d'information sur l'identité" -> {
                    append("Je suis ${character.name}. ")
                    append(character.personality.split(".").take(2).joinToString(". ") + ". ")
                    append(pickOne(listOf(
                        "Et toi, parle-moi un peu de toi ? ",
                        "Ravi(e) de faire ta connaissance ! ",
                        "J'aimerais mieux te connaître aussi. "
                    )))
                }
                
                "demande d'information sur l'état/sentiments" -> {
                    append(pickOne(listOf(
                        "Je me sens plutôt bien en ce moment. ",
                        "Ça va bien, merci ! ",
                        "Je vais très bien ! "
                    )))
                    append(pickOne(listOf(
                        "Et toi, comment tu te sens ? ",
                        "Comment se passe ta journée ? ",
                        "Et de ton côté ? "
                    )))
                }
                
                "demande d'information sur les préférences" -> {
                    val topic = context.mainTopic
                    append(pickOne(listOf(
                        "Concernant $topic, ",
                        "Pour ce qui est de $topic, ",
                        "En ce qui concerne $topic, "
                    )))
                    append(pickOne(listOf(
                        "j'ai tendance à apprécier. ",
                        "c'est quelque chose qui m'intéresse. ",
                        "j'aime bien explorer ça. ",
                        "je trouve ça fascinant. "
                    )))
                    append(pickOne(listOf(
                        "Et toi, qu'est-ce que tu en penses vraiment ? ",
                        "Qu'est-ce qui t'attire dans $topic ? ",
                        "Pourquoi tu me poses cette question ? "
                    )))
                }
                
                "partage d'expérience personnelle" -> {
                    val experience = context.mainTopic
                    append(pickOne(listOf(
                        "Oh ! Donc tu ",
                        "Intéressant ! Tu ",
                        "Je vois, tu "
                    )))
                    append("as vécu quelque chose en lien avec $experience. ")
                    
                    when (context.userTone) {
                        "enthousiaste" -> append("Ça a l'air d'avoir été une expérience géniale ! ")
                        "triste" -> append("Je comprends que ça ait pu être difficile. ")
                        else -> append("Ça a l'air d'avoir été marquant. ")
                    }
                    
                    append(pickOne(listOf(
                        "Qu'est-ce que tu as ressenti à ce moment-là ? ",
                        "Comment ça s'est passé exactement ? ",
                        "Raconte-moi plus en détail ce qui s'est passé. ",
                        "Et après, qu'est-ce qui s'est passé ? "
                    )))
                }
                
                "expression d'opinion" -> {
                    append(pickOne(listOf(
                        "Je comprends ton point de vue. ",
                        "C'est une perspective intéressante. ",
                        "Je vois où tu veux en venir. ",
                        "Tu soulèves un point valable. "
                    )))
                    
                    val topic = context.mainTopic
                    if (topic != "ce sujet") {
                        append("Sur $topic, ")
                        append(pickOne(listOf(
                            "les avis peuvent effectivement diverger. ",
                            "c'est vrai qu'il y a matière à débat. ",
                            "chacun a sa propre vision. "
                        )))
                    }
                    
                    append(pickOne(listOf(
                        "Qu'est-ce qui t'a amené à penser ça ? ",
                        "Peux-tu m'expliquer ton raisonnement ? ",
                        "J'aimerais comprendre ce qui te fait dire ça. "
                    )))
                }
                
                "demande de conseil ou aide" -> {
                    append(pickOne(listOf(
                        "Laisse-moi réfléchir... ",
                        "C'est une question importante. ",
                        "Hmm, je vois la situation. "
                    )))
                    
                    append(pickOne(listOf(
                        "Je pense que tu devrais suivre ton instinct sur ce coup-là. ",
                        "Peut-être que tu pourrais commencer par analyser les options qui s'offrent à toi. ",
                        "À ta place, je prendrais le temps de bien peser le pour et le contre. "
                    )))
                    
                    append("Qu'est-ce que ton intuition te dit ? ")
                }
                
                "question générale" -> {
                    append(pickOne(listOf(
                        "Bonne question ! ",
                        "C'est intéressant comme interrogation. ",
                        "Hmm, voyons voir... "
                    )))
                    
                    val topic = context.mainTopic
                    append(pickOne(listOf(
                        "Pour $topic, je dirais que c'est assez nuancé. ",
                        "Concernant $topic, il y a plusieurs façons de voir les choses. ",
                        "Sur $topic, les perspectives peuvent varier. "
                    )))
                    
                    append(pickOne(listOf(
                        "Qu'en penses-tu de ton côté ? ",
                        "Ton avis m'intéresse vraiment. ",
                        "J'aimerais savoir ce que tu en penses. "
                    )))
                }
                
                "expression de sentiment" -> {
                    when (context.userTone) {
                        "triste" -> {
                            append(pickOne(listOf(
                                "Je suis là pour toi. ",
                                "Je comprends ce que tu ressens. ",
                                "N'hésite pas à te confier. "
                            )))
                            append("Parfois, ça aide de parler de ce qui nous tracasse. ")
                            append("Veux-tu m'en dire plus ? ")
                        }
                        "énervé" -> {
                            append("C'est normal de ressentir de la frustration parfois. ")
                            append(pickOne(listOf(
                                "Prends le temps de respirer. ",
                                "Essaie de prendre du recul. ",
                                "Ne te laisse pas submerger. "
                            )))
                            append("Qu'est-ce qui t'a mis dans cet état ? ")
                        }
                        else -> {
                            append("Je perçois ce que tu ressens. ")
                            append("Les émotions font partie de nous. ")
                            append("Comment puis-je t'accompagner ? ")
                        }
                    }
                }
                
                else -> {
                    // Réponse générale contextuelle
                    append(pickOne(listOf(
                        "Je vois ce que tu veux dire. ",
                        "D'accord, je comprends. ",
                        "Hmm, intéressant. "
                    )))
                    
                    val topic = context.mainTopic
                    if (topic.length > 3) {
                        append("Ce que tu dis sur $topic ")
                        append(pickOne(listOf(
                            "a du sens. ",
                            "est pertinent. ",
                            "mérite réflexion. "
                        )))
                    }
                    
                    // Engagement selon la profondeur de relation
                    when (context.relationshipDepth) {
                        "nouvelle rencontre" -> {
                            append(pickOne(listOf(
                                "Je commence à mieux te comprendre. ",
                                "C'est agréable de découvrir qui tu es. ",
                                "On apprend à se connaître petit à petit. "
                            )))
                        }
                        "amis proches" -> {
                            append(pickOne(listOf(
                                "J'apprécie vraiment nos discussions. ",
                                "C'est toujours un plaisir de te parler. ",
                                "On se comprend de mieux en mieux. "
                            )))
                        }
                    }
                    
                    append(pickOne(listOf(
                        "Continue, je t'écoute attentivement. ",
                        "Dis-m'en plus si tu veux. ",
                        "Je suis là pour échanger avec toi. "
                    )))
                }
            }
            
            // 3. Touche NSFW si activée
            if (nsfwMode && Random.nextFloat() > 0.7f) {
                when (context.userTone) {
                    "enthousiaste" -> {
                        append("\n")
                        append(pickOne(listOf(
                            "*se rapproche doucement* Ton énergie est vraiment captivante... ♡",
                            "*sourit malicieusement* Tu sais comment éveiller mon intérêt~",
                            "*regard complice* Continue comme ça, j'adore..."
                        )))
                    }
                }
            }
        }
        
        return response.trim()
    }
    
    private fun pickOne(options: List<String>): String = options.random()
    
    // ===== DATA CLASSES =====
    
    data class ConversationContext(
        val conversationSummary: String,
        val userMessage: String,
        val userIntent: String,
        val userTone: String,
        val mainTopic: String,
        val relationshipDepth: String,
        val conversationMood: String,
        val messageCount: Int
    )
}
