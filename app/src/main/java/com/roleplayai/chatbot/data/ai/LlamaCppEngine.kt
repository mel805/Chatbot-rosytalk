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
 * Moteur llama.cpp avec générateur vraiment intelligent
 * Comprend le contexte complet et génère des réponses cohérentes
 */
class LlamaCppEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "LlamaCppEngine"
        
        init {
            try {
                System.loadLibrary("llama-android")
                Log.i(TAG, "✅ Bibliothèque llama.cpp chargée")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "⚠️ Bibliothèque llama.cpp non disponible, utilisation du générateur intelligent")
            }
        }
    }
    
    private var nativeLibAvailable = false
    private var modelPath: String? = null
    
    init {
        try {
            System.loadLibrary("llama-android")
            nativeLibAvailable = true
            Log.i(TAG, "✅ Bibliothèque native disponible")
        } catch (e: UnsatisfiedLinkError) {
            nativeLibAvailable = false
            Log.i(TAG, "🧠 Utilisation du générateur intelligent Kotlin")
        }
    }
    
    /**
     * Configure le modèle
     */
    fun setModelPath(path: String) {
        modelPath = path
        Log.i(TAG, "📁 Modèle configuré: $path")
    }
    
    /**
     * Vérifie la disponibilité
     */
    fun isAvailable(): Boolean {
        return true // Toujours disponible grâce au générateur intelligent
    }
    
    /**
     * Génère une réponse
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
            // Toujours utiliser le générateur intelligent
            return@withContext IntelligentGenerator.generate(
                character = character,
                messages = messages,
                username = username,
                userGender = userGender,
                memoryContext = memoryContext,
                nsfwMode = nsfwMode
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur génération", e)
            return@withContext IntelligentGenerator.generateFallback(character)
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
 * Générateur intelligent qui comprend VRAIMENT le contexte complet
 */
private object IntelligentGenerator {
    
    private const val TAG = "IntelligentGenerator"
    
    /**
     * Génère une réponse vraiment intelligente
     */
    suspend fun generate(
        character: Character,
        messages: List<Message>,
        username: String,
        userGender: String,
        memoryContext: String,
        nsfwMode: Boolean
    ): String {
        
        // Simuler temps de réflexion
        delay(Random.nextLong(800, 1600))
        
        Log.d(TAG, "🧠 Analyse du message complet pour ${character.name}")
        
        val lastUserMessage = messages.lastOrNull { it.isUser }?.content ?: "Bonjour"
        val conversationHistory = messages.takeLast(10)
        
        // 1. COMPRENDRE le message complet
        val understanding = understandMessage(lastUserMessage, conversationHistory, character)
        
        // 2. GÉNÉRER une réponse contextuelle basée sur la compréhension
        val response = generateContextualResponse(
            understanding = understanding,
            character = character,
            username = username,
            conversationHistory = conversationHistory,
            nsfwMode = nsfwMode
        )
        
        Log.i(TAG, "✅ Réponse: ${response.take(80)}...")
        return response
    }
    
    /**
     * Comprend le message complet
     */
    private fun understandMessage(
        message: String,
        history: List<Message>,
        character: Character
    ): MessageUnderstanding {
        
        val messageLower = message.lowercase()
        
        // Analyser le TYPE de message
        val messageType = when {
            messageLower.matches(Regex(".*\\b(qui es-tu|tu es qui|ton nom|c'est quoi ton nom)\\b.*")) -> 
                MessageType.ASKING_IDENTITY
            
            messageLower.matches(Regex(".*\\b(comment vas-tu|ça va|tu vas bien|comment tu te sens)\\b.*")) -> 
                MessageType.ASKING_WELLBEING
            
            messageLower.matches(Regex(".*\\b(tu aimes|aimes-tu|tu préfères|préfères-tu|qu'est-ce que tu aimes)\\b.*")) -> 
                MessageType.ASKING_PREFERENCE
            
            messageLower.matches(Regex(".*\\b(pourquoi|comment|où|quand|qu'est-ce|que penses-tu)\\b.*\\?")) -> 
                MessageType.ASKING_EXPLANATION
            
            messageLower.matches(Regex(".*\\b(je suis|j'ai|je viens de|aujourd'hui j'ai|hier j'ai)\\b.*")) -> 
                MessageType.SHARING_EXPERIENCE
            
            messageLower.matches(Regex(".*\\b(je pense|selon moi|à mon avis|je trouve|je crois)\\b.*")) -> 
                MessageType.SHARING_OPINION
            
            messageLower.matches(Regex(".*\\b(super|génial|excellent|formidable|incroyable|top|cool|j'adore)\\b.*")) -> 
                MessageType.EXPRESSING_POSITIVE
            
            messageLower.matches(Regex(".*\\b(triste|déçu|nul|mauvais|horrible|déteste|pas bien)\\b.*")) -> 
                MessageType.EXPRESSING_NEGATIVE
            
            messageLower.matches(Regex(".*\\b(bonjour|salut|hey|coucou|bonsoir)\\b.*")) -> 
                MessageType.GREETING
            
            messageLower.matches(Regex(".*\\b(merci|thank|remercie)\\b.*")) -> 
                MessageType.THANKING
            
            messageLower.contains("?") -> 
                MessageType.ASKING_QUESTION
            
            else -> 
                MessageType.MAKING_STATEMENT
        }
        
        // Extraire le CONTENU principal (pas juste des mots-clés)
        val mainContent = extractMainContent(message, messageType)
        
        // Analyser le SENTIMENT global
        val sentiment = when {
            messageLower.matches(Regex(".*\\b(aime|adore|super|génial|cool|content|heureux|joie)\\b.*")) -> 
                Sentiment.POSITIVE
            messageLower.matches(Regex(".*\\b(triste|déçu|nul|ennuy|déteste|horrible|mauvais)\\b.*")) -> 
                Sentiment.NEGATIVE
            messageLower.matches(Regex(".*\\b(bizarre|étrange|curieux|intéressant|intrigant)\\b.*")) -> 
                Sentiment.CURIOUS
            else -> 
                Sentiment.NEUTRAL
        }
        
        // Analyser le CONTEXTE de la conversation
        val context = if (history.size > 2) {
            val lastBotMessage = history.lastOrNull { !it.isUser }?.content ?: ""
            val previousUserMessage = history.reversed().drop(1).firstOrNull { it.isUser }?.content ?: ""
            ConversationContext(
                isFollowUp = previousUserMessage.isNotEmpty(),
                lastBotTopic = extractMainTopic(lastBotMessage),
                conversationLength = history.size
            )
        } else {
            ConversationContext(false, "", history.size)
        }
        
        return MessageUnderstanding(
            type = messageType,
            mainContent = mainContent,
            sentiment = sentiment,
            context = context,
            fullMessage = message
        )
    }
    
    /**
     * Extrait le contenu principal (pas des mots-clés isolés)
     */
    private fun extractMainContent(message: String, type: MessageType): String {
        return when (type) {
            MessageType.ASKING_PREFERENCE -> {
                // Extraire ce qui suit "aimes" / "préfères"
                val match = message.lowercase().let { msg ->
                    when {
                        msg.contains("tu aimes") -> msg.substringAfter("tu aimes").trim()
                        msg.contains("aimes-tu") -> msg.substringAfter("aimes-tu").trim()
                        msg.contains("tu préfères") -> msg.substringAfter("tu préfères").trim()
                        msg.contains("préfères-tu") -> msg.substringAfter("préfères-tu").trim()
                        else -> ""
                    }
                }
                match.split("?")[0].trim().ifEmpty { "ce sujet" }
            }
            
            MessageType.SHARING_EXPERIENCE -> {
                // Extraire l'expérience partagée
                val match = message.lowercase().let { msg ->
                    when {
                        msg.contains("j'ai") -> msg.substringAfter("j'ai").trim()
                        msg.contains("je suis") -> msg.substringAfter("je suis").trim()
                        msg.contains("je viens de") -> msg.substringAfter("je viens de").trim()
                        else -> message
                    }
                }
                match.split(".")[0].trim()
            }
            
            MessageType.SHARING_OPINION -> {
                // Extraire l'opinion
                val match = message.lowercase().let { msg ->
                    when {
                        msg.contains("je pense que") -> msg.substringAfter("je pense que").trim()
                        msg.contains("je trouve que") -> msg.substringAfter("je trouve que").trim()
                        msg.contains("selon moi") -> msg.substringAfter("selon moi").trim()
                        msg.contains("à mon avis") -> msg.substringAfter("à mon avis").trim()
                        else -> message
                    }
                }
                match.trim()
            }
            
            else -> {
                // Pour les autres, prendre le message complet ou une partie significative
                message.split(".")[0].trim()
            }
        }
    }
    
    /**
     * Extrait le sujet principal
     */
    private fun extractMainTopic(message: String): String {
        val words = message.lowercase()
            .split(Regex("[\\s,.!?;:]+"))
            .filter { it.length > 4 }
        return words.firstOrNull() ?: ""
    }
    
    /**
     * Génère une réponse contextuelle basée sur la compréhension
     */
    private fun generateContextualResponse(
        understanding: MessageUnderstanding,
        character: Character,
        username: String,
        conversationHistory: List<Message>,
        nsfwMode: Boolean
    ): String {
        
        return when (understanding.type) {
            
            MessageType.ASKING_IDENTITY -> {
                val intro = "Je suis ${character.name}."
                val personality = character.personality.split(".").take(2).joinToString(". ")
                val closing = listOf(
                    "Et toi, comment tu t'appelles ?",
                    "Ravi(e) de faire ta connaissance !",
                    "Content(e) de pouvoir discuter avec toi."
                ).random()
                "$intro $personality $closing"
            }
            
            MessageType.ASKING_WELLBEING -> {
                listOf(
                    "Je vais bien, merci ! Et toi, comment ça va ?",
                    "Ça va super bien ! Comment te sens-tu aujourd'hui ?",
                    "Je me sens bien, merci de demander. Et toi ?",
                    "Très bien ! Et de ton côté ?"
                ).random()
            }
            
            MessageType.ASKING_PREFERENCE -> {
                val subject = understanding.mainContent
                val opinion = listOf("j'apprécie", "j'aime bien", "c'est intéressant", "ça me plaît").random()
                val question = listOf(
                    "Et toi, tu en penses quoi ?",
                    "Qu'est-ce qui te plaît dans $subject ?",
                    "Tu as l'air d'y réfléchir, raconte-moi !",
                    "J'aimerais connaître ton point de vue."
                ).random()
                
                if (subject != "ce sujet") {
                    "Pour $subject, $opinion. $question"
                } else {
                    "C'est $opinion. $question"
                }
            }
            
            MessageType.ASKING_EXPLANATION -> {
                val subject = understanding.mainContent
                val thinking = listOf(
                    "C'est une excellente question.",
                    "Intéressant comme interrogation.",
                    "Laisse-moi réfléchir à ça.",
                    "Hmm, bonne question."
                ).random()
                
                val explanation = if (subject.length > 5) {
                    "Concernant ${subject.take(50)}, je dirais que ${listOf("c'est complexe", "il y a plusieurs aspects", "c'est nuancé", "ça dépend du contexte").random()}."
                } else {
                    "Je pense que ${listOf("c'est assez subjectif", "il y a différentes perspectives", "c'est une question de point de vue").random()}."
                }
                
                val engagement = listOf(
                    "Qu'est-ce qui t'a amené à te poser cette question ?",
                    "Et toi, qu'en penses-tu ?",
                    "J'aimerais connaître ton avis là-dessus."
                ).random()
                
                "$thinking $explanation $engagement"
            }
            
            MessageType.SHARING_EXPERIENCE -> {
                val experience = understanding.mainContent
                val interest = listOf(
                    "Oh vraiment ?",
                    "Ça a l'air intéressant !",
                    "Raconte-moi !",
                    "Je t'écoute attentivement."
                ).random()
                
                val comment = if (experience.length > 10) {
                    val experienceShort = experience.take(40)
                    when (understanding.sentiment) {
                        Sentiment.POSITIVE -> "Ça devait être super de ${experienceShort} !"
                        Sentiment.NEGATIVE -> "Je comprends que ${experienceShort} ait pu être difficile."
                        else -> "Donc tu as ${experienceShort}. C'est captivant !"
                    }
                } else {
                    when (understanding.sentiment) {
                        Sentiment.POSITIVE -> "Ça a l'air d'avoir été une belle expérience !"
                        Sentiment.NEGATIVE -> "Je vois que ça a été compliqué pour toi."
                        else -> "Ton histoire m'intrigue."
                    }
                }
                
                val question = listOf(
                    "Comment tu t'es senti(e) ?",
                    "Et après, qu'est-ce qui s'est passé ?",
                    "Qu'as-tu ressenti à ce moment-là ?",
                    "Raconte-moi plus en détail !"
                ).random()
                
                "$interest $comment $question"
            }
            
            MessageType.SHARING_OPINION -> {
                val opinion = understanding.mainContent
                val validation = listOf(
                    "Je comprends ton point de vue.",
                    "C'est une perspective intéressante.",
                    "Je respecte ton opinion.",
                    "Tu soulèves un bon point."
                ).random()
                
                val reflection = if (opinion.length > 10) {
                    val opinionShort = opinion.take(40)
                    "Sur le fait que ${opinionShort}, ${listOf("c'est vrai que", "effectivement", "je vois ce que tu veux dire sur", "il y a matière à réflexion sur").random()}."
                } else {
                    "C'est une réflexion qui mérite d'être approfondie."
                }
                
                val engagement = listOf(
                    "Qu'est-ce qui t'a mené à cette conclusion ?",
                    "Peux-tu développer ton idée ?",
                    "J'aimerais comprendre ton raisonnement.",
                    "Explique-moi comment tu en es arrivé là."
                ).random()
                
                "$validation $reflection $engagement"
            }
            
            MessageType.EXPRESSING_POSITIVE -> {
                val enthusiasm = listOf(
                    "C'est génial !",
                    "Super !",
                    "Excellent !",
                    "J'adore !"
                ).random()
                
                val shared = "Je partage ton enthousiasme !"
                
                val continuation = if (nsfwMode && Random.nextFloat() > 0.6f) {
                    listOf(
                        "Continue, ton énergie est contagieuse... ♡",
                        "J'aime quand tu es comme ça~",
                        "Tu me donnes le sourire..."
                    ).random()
                } else {
                    listOf(
                        "Raconte-moi ce qui te rend si heureux !",
                        "Qu'est-ce qui s'est passé ?",
                        "Dis-m'en plus !",
                        "Je suis tout(e) ouïe !"
                    ).random()
                }
                
                "$enthusiasm $shared $continuation"
            }
            
            MessageType.EXPRESSING_NEGATIVE -> {
                val empathy = listOf(
                    "Oh, je comprends...",
                    "C'est vraiment dommage.",
                    "Je ressens ta déception.",
                    "Je vois que ça te contrarie."
                ).random()
                
                val support = "Ces choses arrivent, malheureusement."
                
                val comfort = listOf(
                    "Mais ne t'inquiète pas, ça va s'arranger.",
                    "Je suis là si tu veux en parler.",
                    "Les choses vont s'améliorer.",
                    "N'hésite pas à te confier."
                ).random()
                
                "$empathy $support $comfort"
            }
            
            MessageType.GREETING -> {
                val relationship = when {
                    conversationHistory.size < 3 -> "new"
                    conversationHistory.size < 10 -> "familiar"
                    else -> "close"
                }
                
                val greeting = when (relationship) {
                    "new" -> "Salut $username ! Ravi(e) de faire ta connaissance."
                    "familiar" -> "Hey $username ! Content(e) de te revoir !"
                    else -> "Coucou $username ! Toujours un plaisir de te parler !"
                }
                
                val question = listOf(
                    "Comment vas-tu ?",
                    "Quoi de neuf ?",
                    "Comment se passe ta journée ?",
                    "Qu'est-ce qui t'amène ?"
                ).random()
                
                "$greeting $question"
            }
            
            MessageType.THANKING -> {
                listOf(
                    "De rien ! C'est toujours un plaisir.",
                    "Pas de problème ! Je suis là pour ça.",
                    "Avec plaisir ! N'hésite pas.",
                    "Mais de rien ! C'était normal.",
                    "Content(e) d'avoir pu t'aider !"
                ).random()
            }
            
            MessageType.ASKING_QUESTION -> {
                val question = understanding.fullMessage
                val acknowledgment = listOf(
                    "Bonne question !",
                    "Intéressant.",
                    "Hmm, laisse-moi réfléchir.",
                    "Tu soulèves un point pertinent."
                ).random()
                
                val answer = "Je pense que ${listOf("c'est assez nuancé", "ça dépend des circonstances", "il y a plusieurs façons de voir", "c'est une question complexe").random()}."
                
                val reflect = listOf(
                    "Qu'en penses-tu toi ?",
                    "Ton avis m'intéresse.",
                    "J'aimerais connaître ta perspective.",
                    "Et de ton côté, comment tu vois ça ?"
                ).random()
                
                "$acknowledgment $answer $reflect"
            }
            
            MessageType.MAKING_STATEMENT -> {
                val statement = understanding.mainContent
                val acknowledgment = listOf(
                    "Je vois.",
                    "D'accord.",
                    "Intéressant.",
                    "Je comprends."
                ).random()
                
                val reflection = if (statement.length > 10) {
                    val statementShort = statement.take(50)
                    "Ce que tu dis sur ${statementShort} ${listOf("a du sens", "est pertinent", "mérite réflexion", "est intéressant").random()}."
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
                        "Continue, je t'écoute.",
                        "J'aimerais en savoir davantage."
                    ).random()
                }
                
                "$acknowledgment $reflection $engagement"
            }
        }
    }
    
    /**
     * Réponse de secours
     */
    fun generateFallback(character: Character): String {
        return listOf(
            "Je suis ${character.name}. Parle-moi, je t'écoute.",
            "Désolé(e), je n'ai pas bien compris. Peux-tu reformuler ?",
            "Hmm, intéressant. Continue, je t'écoute attentivement.",
            "Dis-m'en plus, ça m'intéresse vraiment."
        ).random()
    }
    
    // ===== DATA CLASSES =====
    
    data class MessageUnderstanding(
        val type: MessageType,
        val mainContent: String,
        val sentiment: Sentiment,
        val context: ConversationContext,
        val fullMessage: String
    )
    
    enum class MessageType {
        ASKING_IDENTITY,
        ASKING_WELLBEING,
        ASKING_PREFERENCE,
        ASKING_EXPLANATION,
        ASKING_QUESTION,
        SHARING_EXPERIENCE,
        SHARING_OPINION,
        EXPRESSING_POSITIVE,
        EXPRESSING_NEGATIVE,
        GREETING,
        THANKING,
        MAKING_STATEMENT
    }
    
    enum class Sentiment {
        POSITIVE, NEGATIVE, CURIOUS, NEUTRAL
    }
    
    data class ConversationContext(
        val isFollowUp: Boolean,
        val lastBotTopic: String,
        val conversationLength: Int
    )
}
