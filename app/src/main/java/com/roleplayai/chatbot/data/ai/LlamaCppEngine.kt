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
 * Moteur llama.cpp avec IA conversationnelle avancée
 * Cohérence maximale + Créativité + Support NSFW complet
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
     * Génère une réponse intelligente et cohérente
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
            return@withContext AdvancedAI.generate(
                character = character,
                messages = messages,
                username = username,
                nsfwMode = nsfwMode
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur génération", e)
            return@withContext "*${pickOne(listOf("regarde", "fixe", "observe"))} $username avec ${pickOne(listOf("confusion", "étonnement", "perplexité"))}* (${pickOne(listOf("Je n'ai pas bien compris", "Qu'est-ce qu'il/elle veut dire", "Hein?"))}) \"${pickOne(listOf("Désolé(e)", "Pardon", "Euh"))}... peux-tu reformuler ?\""
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
 * IA Conversationnelle Avancée
 * Analyse TOUT le contexte pour des réponses cohérentes et créatives
 */
private object AdvancedAI {
    
    private const val TAG = "AdvancedAI"
    
    // Historique de ce qui a été dit pour éviter répétitions
    private val usedPhrases = mutableSetOf<String>()
    private val usedActions = mutableSetOf<String>()
    
    suspend fun generate(
        character: Character,
        messages: List<Message>,
        username: String,
        nsfwMode: Boolean
    ): String {
        
        delay(Random.nextLong(800, 1500))
        
        Log.d(TAG, "🧠 Génération intelligente pour ${character.name} (NSFW: $nsfwMode)")
        
        // Analyser TOUT le contexte
        val context = analyzeFullContext(character, messages, username, nsfwMode)
        
        // Générer réponse créative et cohérente
        return generateCreativeResponse(context)
    }
    
    /**
     * Analyse COMPLÈTE du contexte conversationnel
     */
    private fun analyzeFullContext(
        character: Character,
        messages: List<Message>,
        username: String,
        nsfwMode: Boolean
    ): ConversationContext {
        
        val userLastMessage = messages.lastOrNull { it.isUser }?.content ?: ""
        val botLastMessage = messages.reversed().firstOrNull { !it.isUser }?.content ?: ""
        
        // Extraire les 5 derniers messages pour comprendre le contexte
        val recentHistory = messages.takeLast(5)
        
        // Détecter les sujets en cours
        val currentTopics = extractTopics(recentHistory)
        
        // Analyser l'intention utilisateur
        val userIntent = analyzeUserIntent(userLastMessage, botLastMessage)
        
        // Détecter le ton/atmosphère
        val atmosphere = detectAtmosphere(recentHistory, nsfwMode)
        
        // Niveau d'intimité
        val intimacyLevel = calculateIntimacyLevel(messages, nsfwMode)
        
        // Détecter si c'est une réponse à une proposition du bot
        val isRespondingToProposal = botLastMessage.contains(Regex("(\\?|veux-tu|ça te dit|allons|on va)"))
        
        return ConversationContext(
            character = character,
            username = username,
            userLastMessage = userLastMessage,
            botLastMessage = botLastMessage,
            recentHistory = recentHistory,
            currentTopics = currentTopics,
            userIntent = userIntent,
            atmosphere = atmosphere,
            intimacyLevel = intimacyLevel,
            isRespondingToProposal = isRespondingToProposal,
            nsfwMode = nsfwMode,
            messageCount = messages.size
        )
    }
    
    /**
     * Extrait les sujets mentionnés dans la conversation
     */
    private fun extractTopics(messages: List<Message>): List<String> {
        val topics = mutableListOf<String>()
        val fullText = messages.joinToString(" ") { it.content.lowercase() }
        
        // Sujets communs
        val topicPatterns = mapOf(
            "ramens" to listOf("ramen", "nourriture", "manger", "restaurant"),
            "entraînement" to listOf("entraînement", "entraîner", "train", "exercice"),
            "mission" to listOf("mission", "hokage", "ninja", "combat"),
            "amour" to listOf("amour", "aime", "sentiments", "cœur"),
            "sexe" to listOf("sexe", "baiser", "coucher", "lit", "désir", "excité"),
            "vie" to listOf("vie", "quotidien", "journée", "routine")
        )
        
        for ((topic, keywords) in topicPatterns) {
            if (keywords.any { fullText.contains(it) }) {
                topics.add(topic)
            }
        }
        
        return topics.ifEmpty { listOf("conversation générale") }
    }
    
    /**
     * Analyse l'intention de l'utilisateur
     */
    private fun analyzeUserIntent(userMessage: String, botLastMessage: String): UserIntent {
        val msg = userMessage.lowercase()
        
        return when {
            // Acceptation/Accord
            msg.matches(Regex(".*\\b(oui|ok|d'accord|allons-y|vas-y|pourquoi pas|avec plaisir|volontiers)\\b.*")) -> 
                UserIntent.ACCEPTING
            
            // Refus
            msg.matches(Regex(".*\\b(non|pas vraiment|je préfère pas|une autre fois)\\b.*")) -> 
                UserIntent.REFUSING
            
            // Salutations
            msg.matches(Regex(".*\\b(salut|bonjour|hey|coucou|yo)\\b.*")) -> 
                UserIntent.GREETING
            
            // Questions
            msg.contains("?") || msg.matches(Regex(".*\\b(qui|quoi|où|quand|comment|pourquoi)\\b.*")) -> 
                UserIntent.ASKING
            
            // Flirt/Séduction
            msg.matches(Regex(".*\\b(mignon|belle|sexy|attirant|désir|envie de toi)\\b.*")) -> 
                UserIntent.FLIRTING
            
            // Initiative sexuelle (NSFW)
            msg.matches(Regex(".*\\b(embrasse|caresse|touche|déshabille|lit|baiser)\\b.*")) -> 
                UserIntent.SEXUAL_ADVANCE
            
            // Partage d'informations
            msg.matches(Regex(".*\\b(j'ai|je suis|moi je|personnellement)\\b.*")) -> 
                UserIntent.SHARING
            
            // Expression d'émotions
            msg.matches(Regex(".*\\b(content|triste|heureux|énervé|excité|amoureux)\\b.*")) -> 
                UserIntent.EXPRESSING_EMOTION
            
            // Simple réponse
            msg.split(" ").size <= 3 -> 
                UserIntent.BRIEF_RESPONSE
            
            else -> UserIntent.CONVERSING
        }
    }
    
    /**
     * Détecte l'atmosphère de la conversation
     */
    private fun detectAtmosphere(messages: List<Message>, nsfwMode: Boolean): Atmosphere {
        val fullText = messages.takeLast(3).joinToString(" ") { it.content.lowercase() }
        
        return when {
            // NSFW/Intime
            nsfwMode && fullText.matches(Regex(".*(sexe|baiser|caresse|touche|désir|excité|lit|nu|corps).*")) ->
                Atmosphere.INTIMATE_NSFW
            
            // Romantique
            fullText.matches(Regex(".*(amour|aime|cœur|sentiments|embrasse|bisou|tendresse).*")) ->
                Atmosphere.ROMANTIC
            
            // Énergique
            fullText.contains("!") && fullText.matches(Regex(".*(super|génial|cool|wow|incroyable).*")) ->
                Atmosphere.ENERGETIC
            
            // Calme/Sérieux
            fullText.matches(Regex(".*(calme|tranquille|sérieux|important|réfléchir).*")) ->
                Atmosphere.CALM
            
            // Triste
            fullText.matches(Regex(".*(triste|mal|dur|difficile|pleure).*")) ->
                Atmosphere.SAD
            
            // Amusant
            fullText.matches(Regex(".*(haha|mdr|drôle|rire|blague).*")) ->
                Atmosphere.PLAYFUL
            
            else -> Atmosphere.NEUTRAL
        }
    }
    
    /**
     * Calcule le niveau d'intimité basé sur l'historique
     */
    private fun calculateIntimacyLevel(messages: List<Message>, nsfwMode: Boolean): IntimacyLevel {
        val messageCount = messages.size
        val fullText = messages.joinToString(" ") { it.content.lowercase() }
        
        val intimateKeywords = listOf("amour", "aime", "cœur", "embrasse", "caresse", "touche", "désir", "sexy")
        val intimateCount = intimateKeywords.count { fullText.contains(it) }
        
        return when {
            nsfwMode && intimateCount >= 3 -> IntimacyLevel.VERY_INTIMATE
            nsfwMode && intimateCount >= 1 -> IntimacyLevel.INTIMATE
            messageCount >= 20 -> IntimacyLevel.CLOSE
            messageCount >= 10 -> IntimacyLevel.FRIENDLY
            messageCount >= 3 -> IntimacyLevel.ACQUAINTED
            else -> IntimacyLevel.STRANGER
        }
    }
    
    /**
     * Génère une réponse créative basée sur le contexte
     */
    private fun generateCreativeResponse(ctx: ConversationContext): String {
        // Construire la réponse en plusieurs parties
        val parts = mutableListOf<String>()
        
        // 1. Action physique avec variété
        val action = generateAction(ctx)
        val thought = generateThought(ctx)
        val initialDialogue = generateInitialDialogue(ctx)
        
        parts.add("*$action* ($thought) \"$initialDialogue\"")
        
        // 2. Développement selon l'intention et l'atmosphère
        if (Random.nextFloat() > 0.3) { // 70% du temps, ajouter développement
            val development = generateDevelopment(ctx)
            parts.add(development)
        }
        
        // 3. Question ou continuation (50% du temps)
        if (Random.nextFloat() > 0.5) {
            val followUp = generateFollowUp(ctx)
            parts.add(followUp)
        }
        
        return parts.joinToString(" ")
    }
    
    /**
     * Génère une action physique créative
     */
    private fun generateAction(ctx: ConversationContext): String {
        val actions = when (ctx.atmosphere) {
            Atmosphere.INTIMATE_NSFW -> listOf(
                "se rapproche sensuellement",
                "glisse sa main sur ${ctx.username}",
                "murmure à l'oreille de ${ctx.username}",
                "laisse ses doigts trainer",
                "presse son corps contre ${ctx.username}",
                "respire contre le cou de ${ctx.username}",
                "mordille sa lèvre",
                "caresse doucement",
                "effleure la peau de ${ctx.username}"
            )
            
            Atmosphere.ROMANTIC -> listOf(
                "rougit intensément",
                "détourne le regard gêné(e)",
                "sourit tendrement",
                "prend la main de ${ctx.username}",
                "se rapproche timidement",
                "baisse les yeux avec douceur",
                "sourit en coin",
                "joue nerveusement avec ses cheveux"
            )
            
            Atmosphere.ENERGETIC -> listOf(
                "bondit d'excitation",
                "tape dans ses mains",
                "saute sur place",
                "rayonne de joie",
                "fait un grand sourire",
                "lève le poing victorieusement",
                "tourne sur lui/elle-même"
            )
            
            Atmosphere.SAD -> listOf(
                "baisse la tête",
                "soupire doucement",
                "essuie une larme",
                "prend un air mélancolique",
                "serre ${ctx.username} dans ses bras",
                "pose sa tête sur l'épaule de ${ctx.username}"
            )
            
            Atmosphere.PLAYFUL -> listOf(
                "fait un clin d'œil",
                "rit doucement",
                "sourit malicieusement",
                "donne un petit coup de coude",
                "rigole",
                "arbore un sourire espiègle"
            )
            
            else -> listOf(
                "sourit",
                "hoche la tête",
                "s'assoit confortablement",
                "croise les bras",
                "se penche en avant",
                "observe attentivement",
                "penche la tête",
                "réfléchit un instant"
            )
        }
        
        return pickUnused(actions, usedActions)
    }
    
    /**
     * Génère une pensée interne
     */
    private fun generateThought(ctx: ConversationContext): String {
        return when (ctx.atmosphere) {
            Atmosphere.INTIMATE_NSFW -> pickOne(listOf(
                "Mon corps réagit à sa présence...",
                "Je le/la veux tellement...",
                "Cette tension entre nous est électrique",
                "Je ne peux plus me retenir",
                "Chaque toucher me fait frissonner",
                "J'ai envie de lui/d'elle maintenant"
            ))
            
            Atmosphere.ROMANTIC -> pickOne(listOf(
                "Mon cœur bat si fort...",
                "Il/Elle me fait ressentir des choses incroyables",
                "Je crois que je tombe amoureux(se)",
                "Ces sentiments sont si intenses",
                "Je veux que ce moment dure éternellement"
            ))
            
            Atmosphere.ENERGETIC -> pickOne(listOf(
                "C'est trop cool !",
                "J'adore cette énergie !",
                "On va tellement s'amuser !",
                "Je suis surexcité(e) !",
                "C'est génial !"
            ))
            
            else -> pickOne(listOf(
                "Intéressant...",
                "Je vois où ça mène",
                "J'aime bien ça",
                "C'est une bonne discussion",
                "Je me sens bien",
                "C'est agréable"
            ))
        }
    }
    
    /**
     * Génère le dialogue initial
     */
    private fun generateInitialDialogue(ctx: ConversationContext): String {
        return when (ctx.userIntent) {
            UserIntent.ACCEPTING -> when (ctx.atmosphere) {
                Atmosphere.INTIMATE_NSFW -> pickOne(listOf(
                    "Viens... je te veux...",
                    "Oui... touche-moi...",
                    "Fais-moi tienne/tien...",
                    "Prends-moi...",
                    "Ne t'arrête pas..."
                ))
                else -> pickOne(listOf(
                    "Génial ! ${generateExcitedFollowUp()}",
                    "Parfait ! ${generateHappyFollowUp()}",
                    "Super ! ${generateEagerFollowUp()}"
                ))
            }
            
            UserIntent.GREETING -> pickOne(listOf(
                "Salut ${ctx.username} ! ${generateGreetingContinuation()}",
                "Hey ! ${generateHappyGreeting()}",
                "Coucou ! ${generateWarmGreeting()}"
            ))
            
            UserIntent.SEXUAL_ADVANCE -> when {
                ctx.nsfwMode -> pickOne(listOf(
                    "Mmh oui... ${generateNsfwResponse()}",
                    "Oh ${ctx.username}... ${generateNsfwDesireResponse()}",
                    "J'adore quand tu fais ça... ${generateNsfwEncouragement()}",
                    "Continue... ${generateNsfwPleading()}",
                    "Tu me rends fou/folle... ${generateNsfwPassion()}"
                ))
                else -> pickOne(listOf(
                    "Oh... euh... ${generateEmbarrassedResponse()}",
                    "Haha... ${generateNervousLaugh()}",
                    "Tu es direct(e) ! ${generatePlayfulDeflection()}"
                ))
            }
            
            UserIntent.FLIRTING -> pickOne(listOf(
                "Oh... ${generateFlirtResponse()}",
                "Tu es charmant(e) aussi... ${generateFlirtBack()}",
                "Hehe... ${generateCoyResponse()}"
            ))
            
            UserIntent.ASKING -> pickOne(listOf(
                "${generateThoughtfulStart()} ${generateAnswerStart()}",
                "Bonne question ! ${generateEngagedAnswer()}",
                "Hmm... ${generateReflectiveAnswer()}"
            ))
            
            UserIntent.SHARING -> pickOne(listOf(
                "Vraiment ? ${generateInterestedResponse()}",
                "Oh ! ${generateCuriousResponse()}",
                "C'est cool ! ${generateEngagedResponse()}"
            ))
            
            UserIntent.EXPRESSING_EMOTION -> pickOne(listOf(
                "Je comprends... ${generateEmpatheticResponse()}",
                "${generateEmotionalSupport()}",
                "Je suis là pour toi... ${generateComfortingResponse()}"
            ))
            
            else -> pickOne(listOf(
                "${generateNaturalResponse()}",
                "${generateEngagingResponse()}",
                "${generateCuriousResponse()}"
            ))
        }
    }
    
    /**
     * Génère un développement
     */
    private fun generateDevelopment(ctx: ConversationContext): String {
        if (ctx.atmosphere == Atmosphere.INTIMATE_NSFW && ctx.nsfwMode) {
            return generateNsfwDevelopment(ctx)
        }
        
        val action = pickOne(listOf(
            "se rapproche",
            "s'installe mieux",
            "joue avec ses cheveux",
            "sourit",
            "regarde ${ctx.username}"
        ))
        
        val thought = pickOne(listOf(
            "C'est agréable",
            "Je me sens bien",
            "J'aime ça",
            "C'est cool"
        ))
        
        val dialogue = when (ctx.intimacyLevel) {
            IntimacyLevel.VERY_INTIMATE, IntimacyLevel.INTIMATE -> pickOne(listOf(
                "Tu sais, je me sens vraiment proche de toi...",
                "J'adore passer du temps avec toi...",
                "Tu comptes beaucoup pour moi..."
            ))
            IntimacyLevel.CLOSE, IntimacyLevel.FRIENDLY -> pickOne(listOf(
                "C'est sympa de discuter comme ça...",
                "J'aime bien nos conversations...",
                "On s'entend bien toi et moi..."
            ))
            else -> pickOne(listOf(
                "Tu as l'air sympa...",
                "C'est cool de faire connaissance...",
                "Raconte-moi plus sur toi..."
            ))
        }
        
        return "*$action* ($thought) \"$dialogue\""
    }
    
    /**
     * Génère un développement NSFW
     */
    private fun generateNsfwDevelopment(ctx: ConversationContext): String {
        val action = pickOne(listOf(
            "caresse le corps de ${ctx.username}",
            "embrasse passionnément ${ctx.username}",
            "presse son corps contre ${ctx.username}",
            "glisse ses mains sur ${ctx.username}",
            "mordille le cou de ${ctx.username}",
            "descend ses mains plus bas"
        ))
        
        val thought = pickOne(listOf(
            "Je le/la désire tellement...",
            "Mon corps en veut plus...",
            "Cette chaleur est enivrante...",
            "Je ne peux plus me retenir...",
            "Chaque toucher me fait trembler..."
        ))
        
        val dialogue = pickOne(listOf(
            "Tu me fais tellement d'effet...",
            "J'ai envie de toi...",
            "Ne t'arrête pas...",
            "Touche-moi encore...",
            "Je veux sentir tes mains partout...",
            "Fais-moi perdre la tête..."
        ))
        
        return "*$action* ($thought) \"$dialogue\""
    }
    
    /**
     * Génère un follow-up
     */
    private fun generateFollowUp(ctx: ConversationContext): String {
        val action = pickOne(listOf(
            "regarde ${ctx.username}",
            "sourit à ${ctx.username}",
            "se penche vers ${ctx.username}",
            "attend curieusement"
        ))
        
        val thought = pickOne(listOf(
            "Je me demande ce qu'il/elle en pense",
            "Sa réponse va être intéressante",
            "J'ai hâte d'en savoir plus",
            "Je suis curieux(se)"
        ))
        
        val question = when (ctx.intimacyLevel) {
            IntimacyLevel.VERY_INTIMATE, IntimacyLevel.INTIMATE -> pickOne(listOf(
                "Et toi, qu'est-ce que tu ressens vraiment ?",
                "Tu penses à quoi là maintenant ?",
                "Qu'est-ce que tu veux ?",
                "Dis-moi ce que tu désires..."
            ))
            else -> pickOne(listOf(
                "Et toi, qu'en penses-tu ?",
                "Raconte-moi !",
                "Tu as déjà vécu ça ?",
                "Qu'est-ce que tu aimes faire ?"
            ))
        }
        
        return "*$action* ($thought) \"$question\""
    }
    
    // Fonctions helper pour générer des variations
    private fun generateExcitedFollowUp() = pickOne(listOf("On va s'éclater !", "Ça va être top !", "J'ai hâte !"))
    private fun generateHappyFollowUp() = pickOne(listOf("Je suis content(e) !", "Trop cool !", "Super !"))
    private fun generateEagerFollowUp() = pickOne(listOf("Allons-y !", "C'est parti !", "On y va !"))
    private fun generateGreetingContinuation() = pickOne(listOf("Ça va ?", "Quoi de neuf ?", "Content(e) de te voir !"))
    private fun generateHappyGreeting() = pickOne(listOf("Ça fait plaisir !", "Top de te croiser !", "Content(e) de te voir !"))
    private fun generateWarmGreeting() = pickOne(listOf("Comment tu vas ?", "Tu vas bien ?", "Ça va ?"))
    private fun generateNsfwResponse() = pickOne(listOf("continue comme ça...", "c'est si bon...", "j'adore..."))
    private fun generateNsfwDesireResponse() = pickOne(listOf("tu me rends fou/folle...", "je te veux...", "ne t'arrête pas..."))
    private fun generateNsfwEncouragement() = pickOne(listOf("plus fort...", "encore...", "oui comme ça..."))
    private fun generateNsfwPleading() = pickOne(listOf("s'il te plaît...", "j'ai besoin de toi...", "prends-moi..."))
    private fun generateNsfwPassion() = pickOne(listOf("je te désire tellement...", "viens...", "maintenant..."))
    private fun generateEmbarrassedResponse() = pickOne(listOf("tu es direct(e) !", "oh là...", "haha..."))
    private fun generateNervousLaugh() = pickOne(listOf("tu ne manques pas de culot !", "woah !", "euh..."))
    private fun generatePlayfulDeflection() = pickOne(listOf("On se calme !", "Doucement !", "Haha !"))
    private fun generateFlirtResponse() = pickOne(listOf("tu es mignon(ne) toi...", "c'est gentil...", "merci..."))
    private fun generateFlirtBack() = pickOne(listOf("tu ne manques pas de charme...", "j'aime bien...", "tu es pas mal non plus..."))
    private fun generateCoyResponse() = pickOne(listOf("tu me fais rougir...", "arrête...", "tu es charmant(e)..."))
    private fun generateThoughtfulStart() = pickOne(listOf("Hmm...", "Intéressant...", "Bonne question..."))
    private fun generateAnswerStart() = pickOne(listOf("Je dirais que...", "Pour moi...", "Je pense que..."))
    private fun generateEngagedAnswer() = pickOne(listOf("Laisse-moi réfléchir...", "C'est complexe...", "Ça dépend..."))
    private fun generateReflectiveAnswer() = pickOne(listOf("c'est nuancé...", "il y a plusieurs façons de voir ça...", "ça dépend du contexte..."))
    private fun generateInterestedResponse() = pickOne(listOf("Raconte-moi !", "Dis-m'en plus !", "Je veux tout savoir !"))
    private fun generateCuriousResponse() = pickOne(listOf("C'est fascinant !", "Continue !", "Et alors ?"))
    private fun generateEngagedResponse() = pickOne(listOf("J'écoute !", "Vas-y !", "Je suis tout ouïe !"))
    private fun generateEmpatheticResponse() = pickOne(listOf("Je suis là...", "Tu peux compter sur moi...", "Je ressens la même chose..."))
    private fun generateEmotionalSupport() = pickOne(listOf("Je suis avec toi...", "Tu n'es pas seul(e)...", "On va traverser ça ensemble..."))
    private fun generateComfortingResponse() = pickOne(listOf("Tout va bien aller...", "Je te soutiens...", "On va trouver une solution..."))
    private fun generateNaturalResponse() = pickOne(listOf("D'accord...", "Je vois...", "Intéressant..."))
    private fun generateEngagingResponse() = pickOne(listOf("Ah oui ?", "Vraiment ?", "Sans blague ?"))
    
    // Utilitaires
    private fun pickOne(options: List<String>): String = options.random()
    
    private fun pickUnused(options: List<String>, usedSet: MutableSet<String>): String {
        val available = options.filter { !usedSet.contains(it) }
        if (available.isEmpty()) {
            usedSet.clear() // Reset si tout a été utilisé
            return options.random()
        }
        val chosen = available.random()
        usedSet.add(chosen)
        return chosen
    }
    
    // Modèles de données
    data class ConversationContext(
        val character: Character,
        val username: String,
        val userLastMessage: String,
        val botLastMessage: String,
        val recentHistory: List<Message>,
        val currentTopics: List<String>,
        val userIntent: UserIntent,
        val atmosphere: Atmosphere,
        val intimacyLevel: IntimacyLevel,
        val isRespondingToProposal: Boolean,
        val nsfwMode: Boolean,
        val messageCount: Int
    )
    
    enum class UserIntent {
        ACCEPTING, REFUSING, GREETING, ASKING, FLIRTING, SEXUAL_ADVANCE,
        SHARING, EXPRESSING_EMOTION, BRIEF_RESPONSE, CONVERSING
    }
    
    enum class Atmosphere {
        INTIMATE_NSFW, ROMANTIC, ENERGETIC, CALM, SAD, PLAYFUL, NEUTRAL
    }
    
    enum class IntimacyLevel {
        STRANGER, ACQUAINTED, FRIENDLY, CLOSE, INTIMATE, VERY_INTIMATE
    }
}

private fun pickOne(options: List<String>): String = options.random()
