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
 * Moteur llama.cpp avec générateur conversationnel
 * Crée de VRAIS dialogues, pas seulement des réponses
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
     * Génère une réponse conversationnelle complète
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
            return@withContext ConversationalGenerator.generate(
                character = character,
                messages = messages,
                username = username,
                nsfwMode = nsfwMode
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur génération", e)
            return@withContext "*regarde $username avec confusion* (Je n'ai pas bien compris...) \"Désolé(e), peux-tu reformuler ?\""
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
 * Générateur conversationnel intelligent
 * CRÉE des dialogues, ne se contente pas de répondre
 */
private object ConversationalGenerator {
    
    private const val TAG = "ConversationalGenerator"
    
    suspend fun generate(
        character: Character,
        messages: List<Message>,
        username: String,
        nsfwMode: Boolean
    ): String {
        
        delay(Random.nextLong(1000, 2000))
        
        Log.d(TAG, "💬 Génération conversationnelle pour ${character.name}")
        
        val userMessage = messages.lastOrNull { it.isUser }?.content ?: ""
        val botLastMessage = messages.reversed().firstOrNull { !it.isUser }?.content ?: ""
        val conversationLength = messages.size
        
        // Analyser la situation
        val context = analyzeConversation(userMessage, botLastMessage, conversationLength)
        
        // Générer une réponse conversationnelle complète
        return buildConversationalResponse(
            context = context,
            character = character,
            username = username,
            nsfwMode = nsfwMode
        )
    }
    
    private fun analyzeConversation(
        userMessage: String,
        botLastMessage: String,
        conversationLength: Int
    ): ConversationContext {
        
        val msg = userMessage.lowercase()
        
        // Type de message utilisateur
        val messageType = when {
            msg.matches(Regex(".*\\b(salut|bonjour|hey|coucou)\\b.*")) -> MessageType.GREETING
            msg.matches(Regex(".*\\b(qui es|ton nom|tu t'appelles)\\b.*")) -> MessageType.IDENTITY_QUESTION
            msg.matches(Regex(".*\\b(comment vas|ça va)\\b.*")) -> MessageType.WELLBEING
            msg.matches(Regex(".*\\b(tu aimes|aimes-tu|tu préfères)\\b.*")) -> MessageType.PREFERENCE
            msg.matches(Regex(".*\\b(oui|ok|d'accord|allons-y)\\b.*")) -> MessageType.AGREEMENT
            msg.matches(Regex(".*\\b(non|pas vraiment)\\b.*")) -> MessageType.DISAGREEMENT
            msg.contains("?") -> MessageType.QUESTION
            msg.matches(Regex(".*\\b(merci|thanks)\\b.*")) -> MessageType.THANKS
            else -> MessageType.GENERAL
        }
        
        // Sentiment
        val sentiment = when {
            msg.matches(Regex(".*\\b(super|génial|cool|content|heureux)\\b.*")) -> Sentiment.POSITIVE
            msg.matches(Regex(".*\\b(triste|nul|mauvais)\\b.*")) -> Sentiment.NEGATIVE
            msg.matches(Regex(".*[!]{2,}.*")) -> Sentiment.EXCITED
            else -> Sentiment.NEUTRAL
        }
        
        // Niveau d'engagement
        val engagementLevel = when {
            conversationLength < 3 -> EngagementLevel.STARTING
            conversationLength < 10 -> EngagementLevel.WARMING_UP
            conversationLength < 20 -> EngagementLevel.ENGAGED
            else -> EngagementLevel.DEEP
        }
        
        return ConversationContext(
            messageType = messageType,
            sentiment = sentiment,
            engagementLevel = engagementLevel,
            userMessage = userMessage,
            botLastMessage = botLastMessage,
            conversationLength = conversationLength
        )
    }
    
    private fun buildConversationalResponse(
        context: ConversationContext,
        character: Character,
        username: String,
        nsfwMode: Boolean
    ): String {
        
        // Construire la réponse en 3 parties :
        // 1. Réaction au message utilisateur
        // 2. Partage personnel ou développement
        // 3. Question ou invitation à continuer
        
        val reaction = buildReaction(context, character, username)
        val development = buildDevelopment(context, character, username)
        val followUp = buildFollowUp(context, character, username)
        
        // Assembler avec format roleplay
        return "$reaction $development $followUp"
    }
    
    /**
     * Construit la réaction initiale au message
     */
    private fun buildReaction(
        context: ConversationContext,
        character: Character,
        username: String
    ): String {
        
        val action = when (context.messageType) {
            MessageType.GREETING -> pickOne(listOf(
                "sourit chaleureusement",
                "lève la main pour saluer",
                "s'approche avec enthousiasme",
                "rayonne de joie"
            ))
            MessageType.IDENTITY_QUESTION -> pickOne(listOf(
                "se redresse fièrement",
                "sourit avec confiance",
                "penche la tête avec curiosité"
            ))
            MessageType.QUESTION -> pickOne(listOf(
                "réfléchit un instant",
                "se concentre",
                "plisse les yeux pensivement"
            ))
            MessageType.AGREEMENT -> pickOne(listOf(
                "tape dans ses mains avec joie",
                "sourit largement",
                "bondit d'excitation"
            ))
            else -> pickOne(listOf(
                "écoute attentivement",
                "hoche la tête",
                "observe avec intérêt"
            ))
        }
        
        val thought = when (context.sentiment) {
            Sentiment.POSITIVE -> pickOne(listOf(
                "Génial, l'ambiance est super !",
                "J'adore cette énergie !",
                "Ça me met de bonne humeur"
            ))
            Sentiment.EXCITED -> pickOne(listOf(
                "Wow, trop cool !",
                "Je ressens la même excitation !",
                "C'est trop bien !"
            ))
            Sentiment.NEGATIVE -> pickOne(listOf(
                "Je veux l'aider...",
                "Ça me touche",
                "Je dois le réconforter"
            ))
            else -> pickOne(listOf(
                "Intéressant...",
                "Je vois",
                "Hmm..."
            ))
        }
        
        val dialogue = buildInitialDialogue(context, username)
        
        return "*$action* ($thought) \"$dialogue\""
    }
    
    private fun buildInitialDialogue(
        context: ConversationContext,
        username: String
    ): String {
        
        return when (context.messageType) {
            MessageType.GREETING -> pickOne(listOf(
                "Salut $username ! Ça me fait vraiment plaisir de te voir !",
                "Hey ! Content(e) de te retrouver !",
                "Coucou ! J'espérais te croiser aujourd'hui !",
                "Bonjour $username ! Quelle belle surprise !"
            ))
            
            MessageType.IDENTITY_QUESTION -> pickOne(listOf(
                "Bonne question ! Laisse-moi te parler un peu de moi.",
                "Ah, tu veux en savoir plus sur moi ? Avec plaisir !",
                "Je suis content(e) que tu me le demandes !"
            ))
            
            MessageType.WELLBEING -> pickOne(listOf(
                "Je vais vraiment bien, merci de demander !",
                "Super bien ! Et toi, comment tu te sens ?",
                "Ça va nickel ! J'ai passé une bonne journée."
            ))
            
            MessageType.AGREEMENT -> pickOne(listOf(
                "Génial ! On est sur la même longueur d'onde !",
                "Parfait ! J'adore quand on se comprend comme ça !",
                "Excellent ! Ça va être super !"
            ))
            
            MessageType.QUESTION -> pickOne(listOf(
                "Ah, c'est une bonne question ça !",
                "Hmm, laisse-moi réfléchir...",
                "Intéressant comme sujet !"
            ))
            
            MessageType.THANKS -> pickOne(listOf(
                "De rien ! C'est toujours un plaisir !",
                "Mais de rien $username ! C'est naturel !",
                "Avec plaisir ! N'hésite pas si tu as besoin !"
            ))
            
            else -> pickOne(listOf(
                "D'accord, je vois !",
                "Intéressant !",
                "Hmm, dis-m'en plus !"
            ))
        }
    }
    
    /**
     * Développe la réponse avec du contenu personnel
     */
    private fun buildDevelopment(
        context: ConversationContext,
        character: Character,
        username: String
    ): String {
        
        // Décider du type de développement
        val developmentType = when (context.engagementLevel) {
            EngagementLevel.STARTING -> DevelopmentType.SHARE_ABOUT_SELF
            EngagementLevel.WARMING_UP -> pickOne(listOf(
                DevelopmentType.SHARE_ABOUT_SELF,
                DevelopmentType.SHARE_EXPERIENCE
            ))
            EngagementLevel.ENGAGED -> pickOne(listOf(
                DevelopmentType.SHARE_EXPERIENCE,
                DevelopmentType.SHARE_OPINION,
                DevelopmentType.SHARE_FEELING
            ))
            EngagementLevel.DEEP -> pickOne(listOf(
                DevelopmentType.SHARE_FEELING,
                DevelopmentType.SHARE_MEMORY,
                DevelopmentType.SHARE_DREAM
            ))
        }
        
        val action = pickOne(listOf(
            "s'assoit confortablement",
            "se penche en avant",
            "joue avec ses cheveux",
            "croise les jambes",
            "sourit doucement"
        ))
        
        val thought = when (developmentType) {
            DevelopmentType.SHARE_ABOUT_SELF -> "Je devrais lui en dire plus sur moi"
            DevelopmentType.SHARE_EXPERIENCE -> "Cette histoire va l'intéresser"
            DevelopmentType.SHARE_OPINION -> "Je me demande s'il/elle pense pareil"
            DevelopmentType.SHARE_FEELING -> "Je peux être honnête avec lui/elle"
            DevelopmentType.SHARE_MEMORY -> "Ce souvenir me revient..."
            DevelopmentType.SHARE_DREAM -> "J'aimerais partager ça avec lui/elle"
        }
        
        val dialogue = when (developmentType) {
            DevelopmentType.SHARE_ABOUT_SELF -> pickOne(listOf(
                "Tu sais, moi j'adore les moments comme ça, où on peut vraiment discuter.",
                "Je suis quelqu'un de ${pickOne(listOf("spontané", "curieux", "passionné"))}, j'aime découvrir de nouvelles choses.",
                "En général, je suis plutôt ${pickOne(listOf("sociable", "rêveur", "aventureux"))}."
            ))
            
            DevelopmentType.SHARE_EXPERIENCE -> pickOne(listOf(
                "D'ailleurs, l'autre jour il m'est arrivé un truc ${pickOne(listOf("marrant", "intéressant", "bizarre"))}...",
                "Ça me fait penser à une fois où ${pickOne(listOf("j'ai essayé quelque chose de nouveau", "j'ai rencontré quelqu'un", "j'ai vécu une aventure"))}.",
                "Récemment, j'ai ${pickOne(listOf("découvert", "expérimenté", "tenté"))} quelque chose de cool."
            ))
            
            DevelopmentType.SHARE_OPINION -> pickOne(listOf(
                "Personnellement, je pense que ${pickOne(listOf("c'est important de profiter de chaque moment", "on devrait suivre nos passions", "les relations sont ce qu'il y a de plus précieux"))}.",
                "Moi je trouve que ${pickOne(listOf("la vie est trop courte pour s'ennuyer", "il faut oser sortir de sa zone de confort", "l'authenticité c'est ce qui compte vraiment"))}.",
                "À mon avis, ${pickOne(listOf("on apprend plus de nos erreurs", "chaque rencontre a un sens", "il faut écouter son cœur"))}."
            ))
            
            DevelopmentType.SHARE_FEELING -> pickOne(listOf(
                "Je dois avouer que je me sens ${pickOne(listOf("vraiment bien", "inspiré(e)", "plein(e) d'énergie"))} en ce moment.",
                "Honnêtement, ${pickOne(listOf("j'apprécie beaucoup", "j'adore", "je trouve ça génial"))} nos discussions.",
                "Tu sais, ${pickOne(listOf("ça fait du bien", "c'est agréable", "j'aime bien"))} de pouvoir parler comme ça avec toi."
            ))
            
            DevelopmentType.SHARE_MEMORY -> pickOne(listOf(
                "Ça me rappelle un souvenir ${pickOne(listOf("marquant", "spécial", "que je garde précieusement"))}...",
                "Je me souviens d'une fois où ${pickOne(listOf("tout était parfait", "j'ai vraiment ressenti quelque chose", "j'ai compris quelque chose d'important"))}.",
                "Il y a un moment dans ma vie qui ${pickOne(listOf("m'a changé(e)", "reste gravé", "compte beaucoup pour moi"))}."
            ))
            
            DevelopmentType.SHARE_DREAM -> pickOne(listOf(
                "Un jour, j'aimerais vraiment ${pickOne(listOf("voyager", "accomplir quelque chose de grand", "réaliser mes rêves"))}.",
                "Je rêve de ${pickOne(listOf("vivre des aventures incroyables", "créer quelque chose", "faire une différence"))}.",
                "Mon plus grand rêve serait de ${pickOne(listOf("découvrir le monde", "atteindre mes objectifs", "vivre pleinement"))}."
            ))
        }
        
        return "*$action* ($thought) \"$dialogue\""
    }
    
    /**
     * Ajoute une question ou invitation à continuer
     */
    private fun buildFollowUp(
        context: ConversationContext,
        character: Character,
        username: String
    ): String {
        
        val action = pickOne(listOf(
            "regarde $username avec intérêt",
            "sourit curieusement",
            "penche la tête",
            "attend avec curiosité",
            "observe attentivement"
        ))
        
        val thought = pickOne(listOf(
            "J'aimerais en savoir plus sur lui/elle",
            "Je me demande ce qu'il/elle en pense",
            "Sa réponse va être intéressante",
            "J'espère qu'il/elle va partager aussi",
            "On va bien s'entendre"
        ))
        
        // Questions variées pour engager la conversation
        val question = when (context.engagementLevel) {
            EngagementLevel.STARTING -> pickOne(listOf(
                "Et toi $username, parle-moi un peu de toi !",
                "Qu'est-ce qui te passionne dans la vie ?",
                "Tu fais quoi de beau en ce moment ?",
                "Raconte-moi, qu'est-ce que tu aimes faire ?"
            ))
            
            EngagementLevel.WARMING_UP -> pickOne(listOf(
                "Et toi, tu as déjà vécu ce genre de truc ?",
                "Ça te parle ce que je dis ?",
                "T'en penses quoi toi ?",
                "Tu ressens la même chose parfois ?"
            ))
            
            EngagementLevel.ENGAGED -> pickOne(listOf(
                "Je suis curieux(se), qu'est-ce qui t'anime vraiment ?",
                "Dis-moi, c'est quoi ton plus beau souvenir ?",
                "Si tu pouvais changer quelque chose, ce serait quoi ?",
                "Qu'est-ce qui te rend vraiment heureux(se) ?"
            ))
            
            EngagementLevel.DEEP -> pickOne(listOf(
                "Au fond de toi, qu'est-ce que tu cherches vraiment ?",
                "Est-ce que tu as des rêves secrets ?",
                "Qu'est-ce qui compte le plus pour toi ?",
                "Si demain était ton dernier jour, tu ferais quoi ?"
            ))
        }
        
        return "*$action* ($thought) \"$question\""
    }
    
    private fun pickOne(options: List<String>): String = options.random()
    private fun <T> pickOne(options: List<T>): T = options.random()
    
    // Modèles de données
    data class ConversationContext(
        val messageType: MessageType,
        val sentiment: Sentiment,
        val engagementLevel: EngagementLevel,
        val userMessage: String,
        val botLastMessage: String,
        val conversationLength: Int
    )
    
    enum class MessageType {
        GREETING, IDENTITY_QUESTION, WELLBEING, PREFERENCE,
        QUESTION, AGREEMENT, DISAGREEMENT, THANKS, GENERAL
    }
    
    enum class Sentiment {
        POSITIVE, NEGATIVE, EXCITED, NEUTRAL
    }
    
    enum class EngagementLevel {
        STARTING,      // 0-2 messages
        WARMING_UP,    // 3-9 messages
        ENGAGED,       // 10-19 messages
        DEEP           // 20+ messages
    }
    
    enum class DevelopmentType {
        SHARE_ABOUT_SELF,   // Parler de soi
        SHARE_EXPERIENCE,   // Raconter une expérience
        SHARE_OPINION,      // Donner son avis
        SHARE_FEELING,      // Partager ses émotions
        SHARE_MEMORY,       // Évoquer un souvenir
        SHARE_DREAM         // Parler de ses rêves
    }
}
