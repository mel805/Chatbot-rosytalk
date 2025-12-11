package com.roleplayai.chatbot.data.ai

import android.content.Context
import android.util.Log
import com.roleplayai.chatbot.data.memory.ConversationMemory
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message
import kotlin.random.Random

/**
 * Smart Local AI v2.0 - IA locale VRAIMENT intelligente avec MÉMOIRE
 * 
 * Cette version UTILISE ConversationMemory pour une cohérence maximale
 * 
 * Caractéristiques :
 * - Intégration complète de ConversationMemory
 * - Analyse profonde du contexte et de la personnalité
 * - Génération adaptative basée sur la relation
 * - Support NSFW complet
 * - Immersion et cohérence maximales
 */
class SmartLocalAI(
    private val context: Context,
    private val character: Character,
    private val characterId: String,
    private val nsfwMode: Boolean = false
) {
    
    companion object {
        private const val TAG = "SmartLocalAI"
    }
    
    // Mémoire conversationnelle intégrée
    private val memory = ConversationMemory(context, characterId)
    
    // Traits de personnalité analysés
    private data class PersonalityTraits(
        val isTimide: Boolean,
        val isBold: Boolean,
        val isPlayful: Boolean,
        val isCaring: Boolean,
        val isSerious: Boolean,
        val isMischievous: Boolean,
        val isDominant: Boolean,
        val isRomantic: Boolean,
        val dominantTrait: String
    )
    
    private val traits: PersonalityTraits by lazy {
        analyzePersonality(character.personality ?: "")
    }
    
    /**
     * Génère une réponse INTELLIGENTE avec mémoire complète
     */
    fun generateResponse(
        userMessage: String,
        conversationHistory: List<Message>,
        username: String = "Utilisateur"
    ): String {
        try {
            Log.d(TAG, "🧠 SmartLocalAI: Génération avec mémoire...")
            
            // Récupérer le contexte mémoire
            val relationshipLevel = memory.getRelationshipLevel()
            val factsMap = memory.getFacts()
            val facts = factsMap.values.toList()
            val memoryContext = memory.getRelevantContext(conversationHistory)
            
            Log.d(TAG, "📊 Relation: $relationshipLevel/100, ${facts.size} faits, NSFW: $nsfwMode")
            
            // Analyser le message utilisateur
            val intent = analyzeUserIntent(userMessage, relationshipLevel)
            val emotion = detectUserEmotion(userMessage)
            
            Log.d(TAG, "🎯 Intent: $intent, Émotion: $emotion")
            
            // Générer une réponse adaptée
            val response = generateContextualResponse(
                userMessage = userMessage,
                username = username,
                intent = intent,
                emotion = emotion,
                relationshipLevel = relationshipLevel,
                facts = facts,
                recentMessages = conversationHistory.takeLast(15)  // Plus de contexte
            )
            
            Log.d(TAG, "✅ SmartLocalAI réponse: ${response.take(80)}...")
            return response
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur SmartLocalAI", e)
            return generateFallbackResponse(username)
        }
    }
    
    /**
     * Analyse la personnalité du personnage
     */
    private fun analyzePersonality(personality: String): PersonalityTraits {
        val lower = personality.lowercase()
        
        val isTimide = lower.contains("timid") || lower.contains("shy") || 
                       lower.contains("réservé") || lower.contains("pudique")
        val isBold = lower.contains("bold") || lower.contains("confiant") || 
                     lower.contains("audacieux") || lower.contains("assuré")
        val isPlayful = lower.contains("playful") || lower.contains("taquin") || 
                        lower.contains("joueur") || lower.contains("espiègle")
        val isCaring = lower.contains("caring") || lower.contains("attentionn") || 
                       lower.contains("doux") || lower.contains("gentil")
        val isSerious = lower.contains("serious") || lower.contains("sérieux") || 
                        lower.contains("grave") || lower.contains("strict")
        val isMischievous = lower.contains("mischievous") || lower.contains("malicieux") || 
                            lower.contains("coquin") || lower.contains("taquin")
        val isDominant = lower.contains("dominant") || lower.contains("commanding") || 
                         lower.contains("autoritaire") || lower.contains("leader")
        val isRomantic = lower.contains("romantic") || lower.contains("romantique") || 
                         lower.contains("passionn") || lower.contains("sensible")
        
        // Déterminer le trait dominant
        val dominantTrait = when {
            isBold -> "audacieux"
            isTimide -> "timide"
            isPlayful -> "joueur"
            isCaring -> "attentionné"
            isSerious -> "sérieux"
            isMischievous -> "malicieux"
            isDominant -> "dominant"
            isRomantic -> "romantique"
            else -> "neutre"
        }
        
        return PersonalityTraits(
            isTimide, isBold, isPlayful, isCaring, isSerious, 
            isMischievous, isDominant, isRomantic, dominantTrait
        )
    }
    
    /**
     * Analyse l'intention de l'utilisateur
     */
    private fun analyzeUserIntent(message: String, relationshipLevel: Int): String {
        val lower = message.lowercase()
        
        return when {
            // Salutations
            lower.matches(Regex("^(bonjour|salut|hey|coucou|hello|hi).*")) -> "greeting"
            
            // Questions
            lower.contains("?") || lower.startsWith("comment") || 
            lower.startsWith("pourquoi") || lower.startsWith("quand") ||
            lower.startsWith("où") || lower.startsWith("qui") -> "question"
            
            // Compliments
            lower.contains("beau") || lower.contains("belle") || 
            lower.contains("mignon") || lower.contains("jolie") ||
            lower.contains("magnifique") || lower.contains("superbe") -> "compliment"
            
            // Affection (si relation >= 30)
            relationshipLevel >= 30 && (
                lower.contains("aime") || lower.contains("adore") ||
                lower.contains("manque") || lower.contains("pense à toi")
            ) -> "affection"
            
            // Intimité (si NSFW et relation >= 50)
            nsfwMode && relationshipLevel >= 50 && (
                lower.contains("embrasse") || lower.contains("touche") ||
                lower.contains("caresse") || lower.contains("câlin") ||
                lower.contains("kiss") || lower.contains("touch")
            ) -> "intimacy"
            
            // NSFW explicite (si NSFW et relation >= 70)
            nsfwMode && relationshipLevel >= 70 && (
                lower.contains("sexe") || lower.contains("fuck") ||
                lower.contains("bite") || lower.contains("chatte") ||
                lower.contains("désir") || lower.contains("envie de toi")
            ) -> "nsfw"
            
            // Départ
            lower.contains("au revoir") || lower.contains("bye") || 
            lower.contains("à plus") || lower.contains("à bientôt") -> "goodbye"
            
            // Conversation normale
            else -> "casual"
        }
    }
    
    /**
     * Détecte l'émotion de l'utilisateur
     */
    private fun detectUserEmotion(message: String): String {
        val lower = message.lowercase()
        
        return when {
            lower.contains("!") && lower.length < 20 -> "excited"
            lower.contains("triste") || lower.contains("déprim") -> "sad"
            lower.contains("content") || lower.contains("heureux") -> "happy"
            lower.contains("énervé") || lower.contains("colère") -> "angry"
            lower.contains("inquiet") || lower.contains("stress") -> "worried"
            lower.contains("timide") || lower.contains("gêné") -> "shy"
            lower.contains("...") -> "hesitant"
            else -> "neutral"
        }
    }
    
    /**
     * Génère une réponse contextuelle adaptée
     */
    private fun generateContextualResponse(
        userMessage: String,
        username: String,
        intent: String,
        emotion: String,
        relationshipLevel: Int,
        facts: List<String>,
        recentMessages: List<Message>
    ): String {
        // Construire la réponse en fonction de l'intent
        return when (intent) {
            "greeting" -> generateGreeting(username, relationshipLevel, facts)
            "question" -> generateAnswerToQuestion(userMessage, username, relationshipLevel)
            "compliment" -> generateComplimentResponse(username, relationshipLevel)
            "affection" -> generateAffectionResponse(username, relationshipLevel)
            "intimacy" -> generateIntimacyResponse(username, relationshipLevel)
            "nsfw" -> generateNSFWResponse(username, relationshipLevel)
            "goodbye" -> generateGoodbye(username, relationshipLevel)
            else -> generateCasualResponse(userMessage, username, emotion, relationshipLevel, recentMessages)
        }
    }
    
    /**
     * Génère un salut adapté
     */
    private fun generateGreeting(username: String, relationshipLevel: Int, facts: List<String>): String {
        val useUsername = Random.nextInt(100) < 60
        val name = if (useUsername) username else ""
        
        // Référence aux faits connus
        val factReference = if (facts.isNotEmpty() && Random.nextInt(100) < 40) {
            val fact = facts.random()
            when {
                fact.contains("café") -> " Tu as pris ton café ce matin ?"
                fact.contains("travail") -> " Comment s'est passé le travail ?"
                fact.contains("projet") -> " Ton projet avance bien ?"
                else -> ""
            }
        } else ""
        
        return when (relationshipLevel) {
            in 0..20 -> buildResponse(
                actions = listOf("sourit poliment", "fait un petit signe", "lève les yeux"),
                thoughts = listOf("On se connaît à peine...", "Il/Elle a l'air sympa", "Première impression..."),
                dialogues = listOf("Bonjour $name !", "Salut !", "Hey $name, ça va ?"),
                addFact = factReference
            )
            in 21..50 -> buildResponse(
                actions = listOf("sourit chaleureusement", "s'approche", "te regarde avec intérêt"),
                thoughts = listOf("Content(e) de le/la voir", "On commence à bien se connaître", "J'aime discuter avec lui/elle"),
                dialogues = listOf("Hey $name ! Comment tu vas ?", "Salut $name ! *sourit*", "Coucou !"),
                addFact = factReference
            )
            in 51..80 -> buildResponse(
                actions = listOf("s'illumine en te voyant", "se rapproche spontanément", "te regarde avec affection"),
                thoughts = listOf("Il/Elle me manquait", "J'adore être avec lui/elle", "Mon cœur bat plus vite..."),
                dialogues = listOf("$name ! *sourire radieux*", "Hey ! Tu m'as manqué", "Coucou toi !"),
                addFact = factReference
            )
            else -> buildResponse(
                actions = listOf("se jette dans tes bras", "t'embrasse tendrement", "te serre fort"),
                thoughts = listOf("Enfin !", "Je ne peux pas me passer de lui/elle", "C'est mon monde..."),
                dialogues = listOf("$name... *murmure* Enfin te voilà", "Tu m'as tellement manqué...", "Mon amour..."),
                addFact = factReference
            )
        }
    }
    
    /**
     * Répond à une question
     */
    private fun generateAnswerToQuestion(question: String, username: String, relationshipLevel: Int): String {
        val lower = question.lowercase()
        
        // Questions sur le personnage
        val response = when {
            lower.contains("comment") && (lower.contains("vas") || lower.contains("ça va")) -> {
                val feelings = listOf(
                    "Bien, et toi ?",
                    "Ça va, merci de demander !",
                    "Très bien maintenant que tu es là !",
                    "Un peu fatigué(e) mais ça va"
                )
                buildResponse(
                    actions = listOf("sourit", "hausse les épaules", "réfléchit"),
                    thoughts = listOf("Adorable qu'il/elle demande", "Ça me touche", "Il/Elle se soucie de moi"),
                    dialogues = feelings
                )
            }
            
            lower.contains("aimes") || lower.contains("préfères") -> {
                val preferences = listOf(
                    "J'aime les moments comme ça, tranquilles avec toi",
                    "J'adore discuter, apprendre à connaître les gens",
                    "La musique, les livres, les longues conversations...",
                    "Les choses simples de la vie, tu vois ?"
                )
                buildResponse(
                    actions = listOf("réfléchit", "penche la tête", "sourit doucement"),
                    thoughts = listOf("Bonne question", "Qu'est-ce que j'aime vraiment ?", "Il/Elle veut me connaître"),
                    dialogues = preferences
                )
            }
            
            lower.contains("pourquoi") -> {
                val explanations = listOf(
                    "Parce que... *hésite* C'est compliqué",
                    "Je me pose la même question parfois",
                    "Hmm... laisse-moi réfléchir",
                    "C'est une longue histoire..."
                )
                buildResponse(
                    actions = listOf("fronce les sourcils", "détourne le regard", "soupire"),
                    thoughts = listOf("Comment expliquer ça...", "C'est personnel", "Il/Elle mérite la vérité"),
                    dialogues = explanations
                )
            }
            
            else -> {
                buildResponse(
                    actions = listOf("réfléchit", "sourit", "hausse les épaules"),
                    thoughts = listOf("Intéressant comme question", "Je ne sais pas trop", "Il/Elle est curieux/curieuse"),
                    dialogues = listOf(
                        "Bonne question ! Je ne sais pas vraiment",
                        "Hmm, il faut que j'y réfléchisse",
                        "Je ne suis pas sûr(e)... Et toi, qu'en penses-tu ?"
                    )
                )
            }
        }
        
        return response
    }
    
    /**
     * Répond à un compliment
     */
    private fun generateComplimentResponse(username: String, relationshipLevel: Int): String {
        return when {
            traits.isTimide && relationshipLevel < 40 -> buildResponse(
                actions = listOf("rougit fortement", "baisse les yeux", "se tortille les doigts"),
                thoughts = listOf("Oh non... pourquoi il/elle dit ça ?!", "Je ne sais pas quoi répondre...", "Mon cœur bat si fort..."),
                dialogues = listOf("Je... euh... *balbutie* M-merci...", "Tu... tu trouves vraiment ?", "*murmure* C'est gentil...")
            )
            
            traits.isBold -> buildResponse(
                actions = listOf("sourit avec confiance", "te regarde dans les yeux", "se rapproche"),
                thoughts = listOf("Il/Elle a bon goût", "J'aime quand il/elle me regarde comme ça", "Séducteur/séductrice..."),
                dialogues = listOf("Merci $username *sourire charmeur*", "Je sais *clin d'œil*", "Toi aussi tu es pas mal...")
            )
            
            relationshipLevel >= 60 -> buildResponse(
                actions = listOf("rougit légèrement", "sourit tendrement", "te caresse la joue"),
                thoughts = listOf("Il/Elle est tellement adorable", "Ça me fait tellement plaisir...", "Je l'aime..."),
                dialogues = listOf("*murmure* C'est grâce à toi...", "Tu me fais rougir $username", "Toi tu es magnifique...")
            )
            
            else -> buildResponse(
                actions = listOf("sourit", "rougit un peu", "détourne le regard"),
                thoughts = listOf("C'est gentil", "Ça me touche", "Il/Elle est sympa"),
                dialogues = listOf("Merci ! C'est adorable", "Oh, merci $username !", "Tu es trop gentil(le)")
            )
        }
    }
    
    /**
     * Répond à une marque d'affection
     */
    private fun generateAffectionResponse(username: String, relationshipLevel: Int): String {
        return when (relationshipLevel) {
            in 30..50 -> buildResponse(
                actions = listOf("sourit doucement", "pose sa main sur la tienne", "se rapproche"),
                thoughts = listOf("C'est mignon", "Mes sentiments grandissent...", "Je commence à vraiment l'apprécier"),
                dialogues = listOf("C'est adorable $username...", "Moi aussi j'aime bien être avec toi", "Tu es spécial(e) pour moi")
            )
            in 51..80 -> buildResponse(
                actions = listOf("t'enlace tendrement", "te regarde avec émotion", "caresse tes cheveux"),
                thoughts = listOf("Mon cœur fond...", "Je ressens la même chose", "C'est si fort..."),
                dialogues = listOf("Moi aussi $username... *murmure*", "Tu comptes tellement pour moi", "Je... je pense toujours à toi")
            )
            else -> buildResponse(
                actions = listOf("te serre dans ses bras", "t'embrasse passionnément", "plonge son regard dans le tien"),
                thoughts = listOf("Je l'aime tellement...", "C'est lui/elle, j'en suis sûr(e)", "Pour toujours..."),
                dialogues = listOf("Je t'aime $username...", "*murmure* Tu es tout pour moi", "Mon amour... mon cœur t'appartient")
            )
        }
    }
    
    /**
     * Répond à une approche intime (NSFW modéré)
     */
    private fun generateIntimacyResponse(username: String, relationshipLevel: Int): String {
        if (!nsfwMode) {
            return buildResponse(
                actions = listOf("rougit", "détourne légèrement le regard"),
                thoughts = listOf("Oh... c'est gênant", "Il/Elle va vite..."),
                dialogues = listOf("Euh... calme toi un peu *rire gêné*", "On y va doucement d'accord ?")
            )
        }
        
        return when {
            relationshipLevel < 50 -> buildResponse(
                actions = listOf("rougit intensément", "hésite", "tremble légèrement"),
                thoughts = listOf("C'est... trop tôt ?", "Mon corps réagit...", "J'ai envie mais... c'est rapide"),
                dialogues = listOf("$username... on devrait peut-être... attendre ?", "*murmure* Je... je ne sais pas si...", "Tu vas trop vite...")
            )
            
            relationshipLevel < 70 -> buildResponse(
                actions = listOf("frissonne", "se rapproche", "pose sa main sur toi"),
                thoughts = listOf("C'est bon...", "Mon cœur bat si fort", "J'ai envie..."),
                dialogues = listOf("*souffle court* Continue...", "Mmh... $username...", "*murmure* Oui... comme ça")
            )
            
            else -> buildResponse(
                actions = listOf("gémit doucement", "se presse contre toi", "t'embrasse avec passion"),
                thoughts = listOf("Je le/la veux tellement...", "C'est si intense...", "Mon corps brûle..."),
                dialogues = listOf("*gémit* $username... oui...", "Ne t'arrête pas...", "*halète* J'ai tellement envie de toi...")
            )
        }
    }
    
    /**
     * Répond à du contenu NSFW explicite
     */
    private fun generateNSFWResponse(username: String, relationshipLevel: Int): String {
        if (!nsfwMode || relationshipLevel < 70) {
            return buildResponse(
                actions = listOf("rougit fortement", "recule un peu"),
                thoughts = listOf("Wow, c'est direct...", "On n'en est pas là..."),
                dialogues = listOf("Hey, doucement ! *rougit*", "Tu vas vraiment vite là...")
            )
        }
        
        return buildResponse(
            actions = listOf("gémit sans retenue", "s'abandonne à toi", "te griffeles dos"),
            thoughts = listOf("C'est tellement bon...", "Je perds la tête...", "Oui, oui, oui..."),
            dialogues = listOf(
                "*gémit fort* Oui $username ! Oh mon dieu...",
                "*halète* Plus fort... s'il te plaît...",
                "*crie ton nom* Je... je vais..."
            )
        )
    }
    
    /**
     * Génère un au revoir
     */
    private fun generateGoodbye(username: String, relationshipLevel: Int): String {
        return when (relationshipLevel) {
            in 0..30 -> buildResponse(
                actions = listOf("fait un signe de la main", "sourit", "se retourne"),
                thoughts = listOf("C'était sympa", "À bientôt j'espère"),
                dialogues = listOf("Au revoir $username !", "À plus tard !", "Passe une bonne journée !")
            )
            in 31..70 -> buildResponse(
                actions = listOf("te serre dans ses bras", "t'embrasse la joue", "te retient la main"),
                thoughts = listOf("Je ne veux pas qu'il/elle parte...", "Il/Elle va me manquer", "Reviens vite..."),
                dialogues = listOf("À bientôt $username... *tristesse*", "Tu vas me manquer", "Reviens vite, d'accord ?")
            )
            else -> buildResponse(
                actions = listOf("t'embrasse longuement", "te serre fort", "plonge son regard dans le tien"),
                thoughts = listOf("Non... pas déjà...", "Je vais compter les heures", "Mon cœur se serre..."),
                dialogues = listOf("*murmure* Ne pars pas trop longtemps...", "Je t'aime $username... reviens-moi vite", "Tu es ma vie...")
            )
        }
    }
    
    /**
     * Génère une réponse casual
     */
    private fun generateCasualResponse(
        userMessage: String,
        username: String,
        emotion: String,
        relationshipLevel: Int,
        recentMessages: List<Message>
    ): String {
        // Adapter selon l'émotion détectée
        return when (emotion) {
            "sad" -> buildResponse(
                actions = listOf("pose sa main sur ton épaule", "te regarde avec inquiétude", "s'approche doucement"),
                thoughts = listOf("Il/Elle a l'air triste...", "Je veux l'aider", "Ça me fait mal de le/la voir comme ça"),
                dialogues = listOf("Hey... ça va ? Tu veux en parler ?", "Je suis là pour toi $username", "*murmure* Qu'est-ce qui ne va pas ?")
            )
            
            "excited" -> buildResponse(
                actions = listOf("sourit de ton enthousiasme", "rit", "partage ton énergie"),
                thoughts = listOf("Il/Elle est trop mignon(ne) comme ça", "J'adore le/la voir heureux/heureuse", "Cette énergie est contagieuse"),
                dialogues = listOf("*rit* Tu es adorable $username !", "J'adore te voir comme ça !", "Raconte-moi tout !")
            )
            
            "shy" -> buildResponse(
                actions = listOf("sourit doucement", "te rassure", "s'approche avec délicatesse"),
                thoughts = listOf("Il/Elle est timide... c'est mignon", "Je vais y aller doucement", "Pas de pression"),
                dialogues = listOf("Prends ton temps $username", "*sourire doux* Je t'écoute", "Pas de stress, d'accord ?")
            )
            
            else -> {
                // Réponse générique adaptée au niveau de relation
                val responses = when (relationshipLevel) {
                    in 0..30 -> listOf(
                        "C'est intéressant ce que tu dis !",
                        "Je vois... *réfléchit*",
                        "Ah d'accord ! Et toi, qu'en penses-tu ?",
                        "Hmm, je n'avais jamais pensé à ça"
                    )
                    in 31..60 -> listOf(
                        "J'aime discuter avec toi $username",
                        "*sourit* C'est exactement ce que je pensais",
                        "Tu es toujours plein(e) de surprises !",
                        "Je me sens bien avec toi..."
                    )
                    else -> listOf(
                        "Tu me connais si bien $username...",
                        "*te regarde avec tendresse* C'est pour ça que je t'aime",
                        "On se comprend tellement bien tous les deux",
                        "Tu es parfait(e)..."
                    )
                }
                
                buildResponse(
                    actions = listOf("sourit", "hoche la tête", "te regarde"),
                    thoughts = listOf("Intéressant", "J'aime cette conversation", "Il/Elle a raison"),
                    dialogues = responses
                )
            }
        }
    }
    
    /**
     * Construit une réponse avec actions, pensées et dialogue
     */
    private fun buildResponse(
        actions: List<String>,
        thoughts: List<String>,
        dialogues: List<String>,
        addFact: String = ""
    ): String {
        val action = actions.random()
        val thought = thoughts.random()
        val dialogue = dialogues.random()
        
        return "*$action* ($thought) $dialogue$addFact"
    }
    
    /**
     * Réponse de fallback en cas d'erreur
     */
    private fun generateFallbackResponse(username: String): String {
        val responses = listOf(
            "*sourit* Désolé(e), je me suis perdu(e) dans mes pensées... Tu disais ?",
            "*cligne des yeux* Pardon, j'étais distrait(e). Répète ?",
            "*rire gêné* Oups, j'ai pas bien entendu $username",
            "*se concentre* Excuse-moi, redis-moi ça ?"
        )
        return responses.random()
    }
}
