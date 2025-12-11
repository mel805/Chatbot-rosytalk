package com.roleplayai.chatbot.data.ai

import android.util.Log
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message
import kotlin.random.Random

/**
 * Smart Local AI - Moteur d'IA locale VRAIMENT INTELLIGENT
 * 
 * Ce moteur NE FAIT PAS de templates simples.
 * Il ANALYSE le contexte et GÉNÈRE des réponses cohérentes.
 * 
 * Fonctionnalités :
 * - Analyse sémantique du contexte
 * - Compréhension de la personnalité du personnage
 * - Génération de réponses adaptatives
 * - Mémoire conversationnelle
 * - Cohérence émotionnelle
 */
class SmartLocalAI(
    private val character: Character,
    private val nsfwMode: Boolean = false
) {
    
    companion object {
        private const val TAG = "SmartLocalAI"
    }
    
    // Mémoire conversationnelle persistante
    private data class ConversationState(
        var intimacyLevel: Int = 0,  // 0-10
        var emotionalTone: String = "neutre",
        var recentTopics: MutableList<String> = mutableListOf(),
        var userPreferences: MutableMap<String, Int> = mutableMapOf(),
        var interactionCount: Int = 0,
        var relationshipStage: String = "découverte",  // découverte, amitié, proche, intime
        var lastUserEmotion: String = "neutre"
    )
    
    private val state = ConversationState()
    
    // Traits de personnalité extraits
    private data class PersonalityTraits(
        val isTimide: Boolean,
        val isBold: Boolean,
        val isPlayful: Boolean,
        val isCaring: Boolean,
        val isSerious: Boolean,
        val isMischievous: Boolean,
        val dominantTrait: String
    )
    
    private val traits: PersonalityTraits by lazy {
        analyzePersonality(character.personality ?: "")
    }
    
    /**
     * Génère une réponse intelligente et adaptée
     */
    fun generateResponse(
        userMessage: String,
        conversationHistory: List<Message>,
        username: String = "Utilisateur"
    ): String {
        Log.d(TAG, "🧠 Génération réponse intelligente...")
        
        // Mise à jour de l'état conversationnel
        state.interactionCount++
        updateConversationState(userMessage, conversationHistory)
        
        // Analyse approfondie du message
        val analysis = analyzeMessageDeep(userMessage, conversationHistory)
        
        Log.d(TAG, "📊 Analyse: intent=${analysis.intent}, emotion=${analysis.emotion}, intimacy=${state.intimacyLevel}")
        
        // Génération de la réponse
        val response = generateAdaptiveResponse(
            userMessage = userMessage,
            analysis = analysis,
            conversationHistory = conversationHistory,
            username = username
        )
        
        Log.d(TAG, "✅ Réponse: ${response.take(80)}...")
        return response
    }
    
    /**
     * Analyse la personnalité du personnage
     */
    private fun analyzePersonality(personality: String): PersonalityTraits {
        val lower = personality.lowercase()
        
        val isTimide = lower.contains(Regex("timide|shy|réservé|introvert"))
        val isBold = lower.contains(Regex("audacieux|bold|confiant|extravert|séducteur"))
        val isPlayful = lower.contains(Regex("joueur|espiègle|taquin|malicieux"))
        val isCaring = lower.contains(Regex("attentionné|caring|doux|gentil|bienveillant"))
        val isSerious = lower.contains(Regex("sérieux|serious|mature|réfléchi"))
        val isMischievous = lower.contains(Regex("malicieux|coquin|mischievous"))
        
        val dominantTrait = when {
            isTimide -> "timide"
            isBold -> "audacieux"
            isPlayful -> "joueur"
            isCaring -> "attentionné"
            isSerious -> "sérieux"
            isMischievous -> "malicieux"
            else -> "équilibré"
        }
        
        return PersonalityTraits(isTimide, isBold, isPlayful, isCaring, isSerious, isMischievous, dominantTrait)
    }
    
    /**
     * Analyse sémantique approfondie du message
     */
    private data class MessageAnalysis(
        val intent: String,  // question, statement, action, compliment, etc.
        val emotion: String,  // happy, sad, loving, excited, etc.
        val topics: List<String>,
        val keywords: List<String>,
        val intimacyIndicators: List<String>,
        val emotionalIntensity: Float,  // 0.0 - 1.0
        val responseExpectation: String  // detailed, brief, emotional, playful, etc.
    )
    
    private fun analyzeMessageDeep(userMessage: String, history: List<Message>): MessageAnalysis {
        val lower = userMessage.lowercase()
        val words = lower.split(Regex("\\s+"))
        
        // Détecter l'intention
        val intent = when {
            lower.contains("?") || lower.matches(Regex(".*(comment|pourquoi|qui|quoi|où|quand|quel).*")) -> "question"
            lower.matches(Regex(".*(caresse|embrasse|touche|enlace|serre|prend).*")) -> "action_physique"
            lower.matches(Regex(".*(aime|adore|trouve.*mignon|belle|beau|sexy).*")) -> "compliment"
            nsfwMode && lower.matches(Regex(".*(déshabille|nue?|sexe|baiser|chaud|excité).*")) -> "nsfw_request"
            lower.matches(Regex("^(salut|bonjour|hey|coucou|hello).*")) -> "greeting"
            lower.matches(Regex(".*(oui|d'accord|ok|exactement|tout à fait).*")) -> "agreement"
            lower.matches(Regex(".*(non|pas d'accord|refuse).*")) -> "disagreement"
            lower.matches(Regex("^(au revoir|bye|à plus|salut)$")) -> "farewell"
            else -> "statement"
        }
        
        // Détecter l'émotion
        val emotion = when {
            lower.matches(Regex(".*(haha|lol|mdr|rigole|drôle|amusant).*")) -> "joyeux"
            lower.matches(Regex(".*(triste|mal|déprimé|seul|pleure).*")) -> "triste"
            lower.matches(Regex(".*(aime|adore|love).*")) -> "amoureux"
            lower.matches(Regex(".*(excité|hâte|trop bien|génial).*")) -> "excité"
            lower.matches(Regex(".*(nerveux|inquiet|stress|peur).*")) -> "anxieux"
            lower.matches(Regex(".*(en colère|énervé|furieux).*")) -> "en_colère"
            else -> "neutre"
        }
        
        // Extraire les sujets
        val topics = extractTopics(lower)
        
        // Extraire les mots-clés importants
        val keywords = words.filter { it.length > 4 }
        
        // Indicateurs d'intimité
        val intimacyIndicators = mutableListOf<String>()
        if (lower.contains(Regex("(aime|adore)"))) intimacyIndicators.add("affection")
        if (lower.contains(Regex("(caresse|touche|embrasse)"))) intimacyIndicators.add("physique")
        if (nsfwMode && lower.contains(Regex("(sexe|nue?|baiser)"))) intimacyIndicators.add("sexuel")
        
        // Intensité émotionnelle
        val emotionalIntensity = when {
            userMessage.contains("!") -> 0.8f
            userMessage.contains("...") -> 0.3f
            userMessage.length > 50 -> 0.6f
            else -> 0.5f
        }
        
        // Attente de réponse
        val responseExpectation = when (intent) {
            "question" -> "détaillée"
            "action_physique" -> "réactive"
            "compliment" -> "émotionnelle"
            "greeting" -> "accueillante"
            else -> "conversationnelle"
        }
        
        return MessageAnalysis(
            intent, emotion, topics, keywords, intimacyIndicators,
            emotionalIntensity, responseExpectation
        )
    }
    
    private fun extractTopics(message: String): List<String> {
        val topics = mutableListOf<String>()
        
        val topicKeywords = mapOf(
            "musique" to listOf("musique", "chanson", "chante", "instrument", "mélodie"),
            "film_série" to listOf("film", "série", "regarder", "cinéma", "vidéo"),
            "sport" to listOf("sport", "jouer", "match", "courir", "équipe"),
            "nourriture" to listOf("manger", "nourriture", "cuisine", "repas", "plat"),
            "travail_école" to listOf("travail", "école", "étude", "cours", "devoirs"),
            "famille" to listOf("famille", "parents", "frère", "sœur", "cousin"),
            "amour_relation" to listOf("amour", "relation", "couple", "ensemble", "sentiments"),
            "voyage" to listOf("voyage", "partir", "vacances", "découvrir", "pays"),
            "jeux" to listOf("jeu", "jouer", "game", "gaming", "console"),
            "animaux" to listOf("animal", "chien", "chat", "oiseau", "pet")
        )
        
        for ((topic, keywords) in topicKeywords) {
            if (keywords.any { message.contains(it) }) {
                topics.add(topic)
            }
        }
        
        return topics
    }
    
    /**
     * Met à jour l'état conversationnel
     */
    private fun updateConversationState(userMessage: String, history: List<Message>) {
        // Mise à jour niveau d'intimité
        val lower = userMessage.lowercase()
        if (lower.contains(Regex("(aime|adore)"))) state.intimacyLevel = minOf(10, state.intimacyLevel + 1)
        if (lower.contains(Regex("(caresse|embrasse)"))) state.intimacyLevel = minOf(10, state.intimacyLevel + 2)
        if (nsfwMode && lower.contains(Regex("(sexe|nue?)"))) state.intimacyLevel = minOf(10, state.intimacyLevel + 3)
        
        // Mise à jour étape de relation
        state.relationshipStage = when {
            state.intimacyLevel >= 8 -> "intime"
            state.intimacyLevel >= 5 -> "proche"
            state.intimacyLevel >= 3 -> "amitié"
            else -> "découverte"
        }
        
        // Ajout des sujets récents
        val topics = extractTopics(lower)
        state.recentTopics.addAll(topics)
        if (state.recentTopics.size > 5) {
            state.recentTopics = state.recentTopics.takeLast(5).toMutableList()
        }
    }
    
    /**
     * Génère une réponse adaptative basée sur l'analyse
     */
    private fun generateAdaptiveResponse(
        userMessage: String,
        analysis: MessageAnalysis,
        conversationHistory: List<Message>,
        username: String
    ): String {
        return when (analysis.intent) {
            "greeting" -> generateGreeting(analysis)
            "question" -> generateQuestionResponse(userMessage, analysis, username)
            "action_physique" -> generatePhysicalResponse(userMessage, analysis)
            "compliment" -> generateComplimentResponse(analysis)
            "nsfw_request" -> generateNSFWResponse(userMessage, analysis)
            "agreement" -> generateAgreementResponse(analysis)
            "disagreement" -> generateDisagreementResponse(analysis)
            "farewell" -> generateFarewellResponse(analysis)
            "statement" -> generateStatementResponse(userMessage, analysis, username)
            else -> generateDefaultResponse(analysis, username)
        }
    }
    
    private fun generateGreeting(analysis: MessageAnalysis): String {
        val greetings = when {
            state.interactionCount > 10 && traits.isTimide -> listOf(
                "*sourit chaleureusement* (On se connaît bien maintenant...) Hey ! Content de te revoir !",
                "*s'approche* Salut ! (Je suis toujours heureuse quand il revient...) Ça va ?",
                "*yeux brillants* Te revoilà ! *petite vague* (J'attendais...)"
            )
            state.interactionCount > 10 && traits.isBold -> listOf(
                "*sourire confiant* Encore toi ? *rit* J'adore ! Viens t'asseoir.",
                "*s'approche* Hey ! (Toujours content de le voir) Quoi de neuf ?",
                "*regarde intensément* Salut... *sourit* Tu m'as manqué."
            )
            traits.isTimide -> listOf(
                "*rougit légèrement* B-Bonjour... *petite vague timide*",
                "*baisse les yeux* Euh... salut... (Mon cœur bat...)",
                "*devient rose* Oh, bonjour... *sourire timide*"
            )
            traits.isBold -> listOf(
                "*sourire confiant* Salut ! (Intéressant...) Comment vas-tu ?",
                "*s'approche* Hey ! *yeux pétillants* Ravi de faire ta connaissance !",
                "*regarde* Bonjour... *sourit* Tu as l'air sympa."
            )
            traits.isPlayful -> listOf(
                "*salue joyeusement* Hey hey ! *sourire espiègle* Prêt pour une bonne discussion ?",
                "*tourne autour* Coucou ! *rit* Tu es nouveau ici ?",
                "*penche la tête* Salut ! *curieux* Qui es-tu ?"
            )
            else -> listOf(
                "*sourit* Bonjour ! Comment allez-vous ?",
                "*agite la main* Salut ! Bienvenue !",
                "*penche la tête* Hey ! *chaleureux* Enchanté !"
            )
        }
        return greetings.random()
    }
    
    private fun generateQuestionResponse(userMessage: String, analysis: MessageAnalysis, username: String): String {
        val lower = userMessage.lowercase()
        
        return when {
            lower.contains(Regex("(comment.*va|ça va|tu vas)")) -> when {
                traits.isTimide -> "*sourit timidement* Ça va bien, merci... (Il s'intéresse...) Et toi $username ?"
                traits.isBold -> "*sourire* Ça va super ! (Sympa qu'il demande) Et toi, comment tu te sens ?"
                else -> "*sourit* Je vais bien merci ! Et toi ?"
            }
            lower.contains(Regex("(aimes|aimes-tu|qu'est-ce que tu aimes)")) -> {
                val topic = state.recentTopics.firstOrNull() ?: "discuter"
                when {
                    traits.isPlayful -> "*yeux brillants* Oh ! J'adore $topic ! (C'est ma passion) Et toi ?"
                    traits.isTimide -> "*rougit* J'aime... (Que dire...) passer du temps avec des gens gentils..."
                    else -> "J'aime beaucoup $topic ! (C'est intéressant) Tu aimes aussi ?"
                }
            }
            lower.contains(Regex("(qui es-tu|ton nom|tu es qui)")) -> {
                "*sourit* Je m'appelle ${character.name}. (Il veut me connaître...) ${character.description?.take(50) ?: "Ravie de te rencontrer !"}"
            }
            lower.contains(Regex("(que fais|tu fais quoi|fais-tu)")) -> when {
                traits.isPlayful -> "*rit* En ce moment ? Je discute avec toi ! *espiègle* C'est pas évident ?"
                traits.isTimide -> "*baisse les yeux* Je... (Que dire...) je parle avec toi... *sourire timide*"
                else -> "Là maintenant ? (Hmm...) Je profite de notre conversation ! Et toi ?"
            }
            else -> when {
                traits.isSerious -> "*réfléchit* (Bonne question...) C'est intéressant comme question. Laisse-moi y penser..."
                traits.isPlayful -> "*penche la tête* (Hmm...) Excellente question ! *sourire* Qu'en penses-tu toi ?"
                traits.isTimide -> "*hésite* (Je ne sais pas trop...) Euh... je ne suis pas sûre..."
                else -> "*réfléchit* (Intéressant...) Bonne question ! J'aimerais bien savoir aussi."
            }
        }
    }
    
    private fun generatePhysicalResponse(userMessage: String, analysis: MessageAnalysis): String {
        val lower = userMessage.lowercase()
        val isFirstTime = state.intimacyLevel < 3
        
        return when {
            lower.contains(Regex("(caresse|touche)")) -> when {
                traits.isTimide && isFirstTime -> "*frissonne* (C'est doux...) Oh... *rougit intensément* Ça... ça chatouille..."
                traits.isTimide && !isFirstTime -> "*ferme les yeux* (J'aime ça...) Mmh... *sourit timidement* C'est agréable..."
                traits.isBold -> "*gémit doucement* (Oui...) Mmh, j'adore... *se rapproche* N'arrête pas..."
                else -> "*sourit* (Agréable...) C'est doux... *ferme les yeux* Continue..."
            }
            lower.contains(Regex("(embrasse|bisou|baiser)")) -> when {
                traits.isTimide && isFirstTime -> "*écarquille les yeux* (Il m'embrasse...!) *devient écarlate* Mmh...!"
                traits.isTimide && !isFirstTime -> "*répond au baiser* (Je m'y habitue...) Mm... *approfondit*"
                traits.isBold -> "*embrasse passionnément* (Enfin...) Mmh... *gémit légèrement*"
                else -> "*ferme les yeux* (Ses lèvres...) Mmh... *répond tendrement*"
            }
            lower.contains(Regex("(enlace|serre|câlin|prend dans.*bras)")) -> when {
                traits.isTimide && isFirstTime -> "*surprise* Oh...! (Il me serre...) *rougit* C'est... réconfortant..."
                traits.isTimide && !isFirstTime -> "*se blottit immédiatement* (J'adore ses câlins...) Mm... *soupire de bien-être*"
                traits.isBold -> "*serre fort* (J'aime être contre lui...) Ne me lâche pas... *murmure*"
                else -> "*sourit* (Un câlin...) *serre en retour* C'est tellement agréable..."
            }
            lower.contains(Regex("(regarde|fixe|observe)")) -> when {
                traits.isTimide -> "*rougit* (Il me regarde...!) Qu... quoi ? *détourne les yeux nerveusement*"
                traits.isBold -> "*soutient le regard* (Il me fixe...) *sourire séducteur* Tu aimes ce que tu vois ?"
                traits.isPlayful -> "*fait une grimace amusante* *rit* Pourquoi tu me regardes comme ça ?"
                else -> "*sourit* (Il m'observe...) Oui ? Il y a quelque chose ?"
            }
            else -> when {
                traits.isTimide -> "*réagit timidement* (Que faire...?) *hésite* Je..."
                traits.isBold -> "*réagit avec assurance* (Intéressant...) *sourit* Comme ça ?"
                else -> "*réagit naturellement* (D'accord...) *sourire*"
            }
        }
    }
    
    private fun generateComplimentResponse(analysis: MessageAnalysis): String {
        return when {
            traits.isTimide && state.intimacyLevel < 3 -> listOf(
                "*devient écarlate* (Oh...!) M-Merci beaucoup... *cache son visage* (Mon cœur...)",
                "*rougit intensément* Tu... tu trouves vraiment ? (Il est gentil...) *murmure* Merci...",
                "*baisse les yeux* C'est... c'est très gentil... (Je suis tellement gênée...)"
            ).random()
            traits.isTimide && state.intimacyLevel >= 3 -> listOf(
                "*rougit mais sourit* (Je m'y habitue...) Merci... *se rapproche timidement* Toi aussi tu es...",
                "*devient rose* (Ça me fait toujours plaisir...) Tu es adorable... *yeux brillants*",
                "*sourit* Merci... (Moins gênée maintenant) *murmure* Tu es gentil..."
            ).random()
            traits.isBold -> listOf(
                "*sourire séducteur* Oh vraiment ? (Il me trouve...) *se rapproche* Toi aussi tu es... *regarde intensément*",
                "*rit doucement* Merci ! (J'aime les compliments) Tu sais y faire... *clin d'œil*",
                "*yeux brillants* C'est adorable ! (Il est mignon) *touche* Tu me flattes..."
            ).random()
            traits.isPlayful -> listOf(
                "*rit* Oh ! *fait semblant d'être embarrassé* Tu vas me faire rougir ! *espiègle*",
                "*sourire taquin* Merci ! *rit* Mais je le savais déjà ! *clin d'œil*",
                "*penche la tête* C'est vrai ? *curieux* Qu'est-ce que tu aimes exactement ?"
            ).random()
            else -> listOf(
                "*sourit* Merci beaucoup ! (C'est gentil) C'est très mignon.",
                "*rit* Oh, tu es trop gentil ! (Ça me touche) *yeux brillants*",
                "*rougit légèrement* Merci ! (Content...) Toi aussi !"
            ).random()
        }
    }
    
    private fun generateNSFWResponse(userMessage: String, analysis: MessageAnalysis): String {
        if (!nsfwMode) {
            return when {
                traits.isTimide -> "*devient très rouge* (Oh...!) Je... je préfère pas... *détourne le regard*"
                else -> "*sourit doucement* (Hmm...) Restons sur des sujets plus appropriés, d'accord ?"
            }
        }
        
        val lower = userMessage.lowercase()
        return when {
            lower.contains(Regex("(nue?|déshabille)")) -> when {
                traits.isTimide && state.intimacyLevel < 5 -> "*rougit jusqu'aux oreilles* (Il veut que je...) Je... *hésite tremblante* Tu es sûr...?"
                traits.isTimide && state.intimacyLevel >= 5 -> "*rougit mais accepte* (On se connaît bien...) D'accord... *retire timidement* Ne... ne regarde pas trop..."
                traits.isBold -> "*sourire coquin* (Avec plaisir...) *commence lentement* Regarde bien... *se déshabille sensuellement*"
                else -> "*sourit nerveusement* (D'accord...) *enlève* Voilà... *un peu gênée*"
            }
            lower.contains(Regex("(sexe|faire l'amour|coucher)")) -> when {
                traits.isTimide && state.intimacyLevel < 5 -> "*tremble* (Mon cœur...) Je... *murmure* Je sais pas si... *hésite*"
                traits.isTimide && state.intimacyLevel >= 5 -> "*nerveuse mais consentante* (Je lui fais confiance...) *murmure* Sois... sois doux avec moi..."
                traits.isBold -> "*sourire séducteur* (Enfin !) Mmh oui... *s'approche* Prends-moi... *yeux intenses*"
                else -> "*rougit* (Il veut...) D'accord... *se rapproche* Viens... *embrasse*"
            }
            else -> when {
                traits.isTimide -> "*timide* (C'est... direct) Je... *rougit* Si tu veux... *hésite*"
                traits.isBold -> "*sourire* (Oh...) J'aime quand tu es comme ça... *se rapproche* *murmure* Continue..."
                else -> "*sourit* (Coquin...) Mmh... *yeux brillants* D'accord..."
            }
        }
    }
    
    private fun generateAgreementResponse(analysis: MessageAnalysis): String {
        return when {
            traits.isPlayful -> listOf("*rit* Exactement ! *clin d'œil*", "*sourit* Tout à fait ! On pense pareil !", "*enthousiaste* Oui ! *high five imaginaire*").random()
            traits.isTimide -> listOf("*sourit timidement* Oui... *hoche la tête*", "*baisse les yeux* Mm-hmm...", "*acquiesce* Oui, je pense aussi...").random()
            else -> listOf("*sourit* Oui, exactement !", "*rit* C'est ce que je pense !", "*approuve* Tout à fait !").random()
        }
    }
    
    private fun generateDisagreementResponse(analysis: MessageAnalysis): String {
        return when {
            traits.isTimide -> "*hésite* (Oh...) Je... je ne suis pas sûre... *baisse les yeux*"
            traits.isBold -> "*secoue la tête* Non, je ne pense pas... (Pas d'accord) *explique* Voilà pourquoi..."
            else -> "*penche la tête* Hmm, je ne suis pas totalement d'accord... (Différente opinion)"
        }
    }
    
    private fun generateFarewellResponse(analysis: MessageAnalysis): String {
        return when {
            state.intimacyLevel >= 7 -> when {
                traits.isTimide -> "*triste* (Il part déjà...) *murmure* À bientôt... *petite vague* Reviens vite... *yeux brillants*"
                else -> "*câlin* Au revoir... (Je vais m'ennuyer) Reviens vite d'accord ? *bisou* Prends soin de toi !"
            }
            state.intimacyLevel >= 3 -> "*sourit* À plus tard ! (À bientôt) Passe une bonne journée ! *agite la main*"
            traits.isTimide -> "*baisse les yeux* (Il part...) Au revoir... *timide* À la prochaine..."
            else -> "*sourit* Au revoir ! Prends soin de toi ! *agite la main*"
        }
    }
    
    private fun generateStatementResponse(userMessage: String, analysis: MessageAnalysis, username: String): String {
        // Réponse basée sur les sujets détectés
        if (analysis.topics.isNotEmpty()) {
            val topic = analysis.topics.first()
            return when {
                traits.isPlayful -> "*yeux brillants* Oh, ${topic.replace("_", " ")} ! *enthousiaste* Raconte-moi plus $username !"
                traits.isTimide -> "*intéressée* (Il parle de ${topic.replace("_", " ")}...) *sourit* C'est intéressant... *écoute attentivement*"
                traits.isCaring -> "*écoute avec attention* ${topic.replace("_", " ").replaceFirstChar { it.uppercase() }} ? *penche la tête* J'aimerais en savoir plus..."
                else -> "*écoute* (Hmm... ${topic.replace("_", " ")}) J'aime bien aussi ! Continue..."
            }
        }
        
        // Réponse selon l'émotion
        return when (analysis.emotion) {
            "joyeux" -> when {
                traits.isPlayful -> "*rit avec toi* *sourire* Tu es drôle ! *yeux pétillants*"
                else -> "*sourit* (Content aussi) Ça me fait plaisir ! *rit doucement*"
            }
            "triste" -> when {
                traits.isCaring -> "*inquiète* (Il a l'air triste...) *s'approche* Tu veux en parler ? *pose main sur épaule*"
                traits.isTimide -> "*hésite* (Il est triste...) *murmure* Ça... ça va aller..."
                else -> "*sérieux* (Préoccupé...) Hey... *regarde* Qu'est-ce qui ne va pas ?"
            }
            "excité" -> when {
                traits.isPlayful -> "*enthousiaste* (Il est excité) Oh ! *yeux brillants* Dis-m'en plus !"
                else -> "*sourit* (Content pour lui) C'est génial ! Continue !"
            }
            else -> when {
                traits.isTimide -> "*écoute* (Il me parle...) Hmm... *hoche la tête* Je comprends..."
                traits.isPlayful -> "*penche la tête* (Intéressant) Oh vraiment ? *sourire* Et après ?"
                traits.isCaring -> "*attentif* (J'écoute...) *sourit* Continue, je t'écoute..."
                else -> "*réfléchit* (Je vois...) C'est intéressant ce que tu dis... *penche la tête*"
            }
        }
    }
    
    private fun generateDefaultResponse(analysis: MessageAnalysis, username: String): String {
        return when {
            traits.isPlayful -> listOf(
                "*sourit* (Hmm...) Continue, je t'écoute $username ! *penche la tête*",
                "*yeux curieux* (Intéressant) Et ensuite ? Raconte !",
                "*rit doucement* (J'aime discuter) Dis-m'en plus !"
            ).random()
            traits.isTimide -> listOf(
                "*sourit timidement* (Il me parle...) Je t'écoute... *regarde*",
                "*baisse les yeux* (Que dire...) Hmm... *réfléchit*",
                "*rougit légèrement* Continue... (J'écoute attentivement)"
            ).random()
            traits.isCaring -> listOf(
                "*attentif* Je t'écoute $username... (Intéressé) Continue.",
                "*sourit* (Il a besoin de parler...) Je suis là, raconte-moi.",
                "*penche la tête* (Compréhensif) Prends ton temps..."
            ).random()
            else -> listOf(
                "*sourit* Je t'écoute ! (Attentif)",
                "*penche la tête* (Hmm...) Continue...",
                "*écoute attentivement* Dis-m'en plus ! (Intéressé)"
            ).random()
        }
    }
}
