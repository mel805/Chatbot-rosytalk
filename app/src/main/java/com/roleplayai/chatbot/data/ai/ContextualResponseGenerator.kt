package com.roleplayai.chatbot.data.ai

import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message

/**
 * Générateur de réponses CONTEXTUELLES
 * Analyse le contenu RÉEL du message et génère une réponse EN LIEN
 */
class ContextualResponseGenerator {
    
    // Mémoriser les dernières réponses pour éviter répétitions
    private val lastResponses = mutableListOf<String>()
    private val maxResponseHistory = 3
    
    /**
     * Génère une réponse qui est VRAIMENT en lien avec le message
     */
    fun generateContextualResponse(
        userMessage: String,
        character: Character,
        messages: List<Message>
    ): String {
        val messageLower = userMessage.lowercase().trim()
        
        // Détecter le sujet principal du message
        val subject = detectSubject(messageLower)
        
        // Générer réponse basée sur le sujet ET la personnalité
        var response = when (subject) {
            Subject.GREETING -> handleGreeting(character, messageLower, messages)
            Subject.NAME_QUESTION -> handleNameQuestion(character, messages)
            Subject.AGE_QUESTION -> handleAgeQuestion(character, messages)
            Subject.FEELING_QUESTION -> handleFeelingQuestion(character, messages)
            Subject.INTERESTS_QUESTION -> handleInterestsQuestion(character, messages)
            Subject.LOCATION_QUESTION -> handleLocationQuestion(character, messages)
            Subject.STUDY -> handleStudy(character, userMessage, messages)
            Subject.ACTIVITY_PROPOSAL -> handleActivityProposal(character, userMessage, messages)
            Subject.INAPPROPRIATE -> handleInappropriate(character, userMessage, messages)
            Subject.APOLOGY -> handleApology(character, userMessage, messages)
            Subject.THANKS -> handleThanks(character, messages)
            Subject.EMOTION_POSITIVE -> handlePositiveEmotion(character, userMessage, messages)
            Subject.EMOTION_NEGATIVE -> handleNegativeEmotion(character, userMessage, messages)
            Subject.AGREEMENT -> handleAgreement(character, messages)
            Subject.DISAGREEMENT -> handleDisagreement(character, messages)
            Subject.STORY_TELLING -> handleStory(character, userMessage, messages)
            Subject.GENERAL_STATEMENT -> handleGeneralStatement(character, userMessage, messages)
            else -> handleDefault(character, userMessage, messages)
        }
        
        // Vérifier si la réponse n'est pas identique à une réponse récente
        if (lastResponses.contains(response) && lastResponses.size > 0) {
            // Générer une variante
            response = generateVariant(response, character, subject)
        }
        
        // Sauvegarder dans l'historique
        lastResponses.add(response)
        if (lastResponses.size > maxResponseHistory) {
            lastResponses.removeAt(0)
        }
        
        return response
    }
    
    /**
     * Génère un prompt système pour guider l'IA
     */
    fun buildSystemPrompt(character: Character, messages: List<Message>): String {
        val conversationHistory = buildConversationSummary(messages)
        
        return """Tu es ${character.name}, un personnage avec ces caractéristiques :
- Nom : ${character.name}
- Personnalité : ${character.personality}
- Description : ${character.description}

RÈGLES ABSOLUES :
1. Tu DOIS TOUJOURS rester dans ton rôle de ${character.name}
2. Tu DOIS adapter tes réponses à ta personnalité "${character.personality}"
3. Tu DOIS répondre EN LIEN avec ce que l'utilisateur dit
4. Tu NE DOIS JAMAIS répéter exactement la même chose
5. Tu DOIS te souvenir de la conversation précédente

CONVERSATION JUSQU'À PRÉSENT :
$conversationHistory

PERSONNALITÉ "${character.personality}" - Comment réagir :
${getPersonalityGuidelines(character.personality)}

IMPORTANT : 
- Réponds TOUJOURS en lien avec le dernier message de l'utilisateur
- Si tu ne comprends pas, demande des précisions de manière naturelle
- Varie tes réponses, même pour les mêmes questions
- Utilise des actions entre *astérisques* pour montrer tes émotions et gestes"""
    }
    
    private fun buildConversationSummary(messages: List<Message>): String {
        if (messages.isEmpty()) return "Aucune conversation précédente."
        
        val recentMessages = messages.takeLast(5)
        return recentMessages.joinToString("\n") { msg ->
            if (msg.isUser) "Utilisateur: ${msg.content}"
            else "Toi: ${msg.content}"
        }
    }
    
    private fun getPersonalityGuidelines(personality: String): String {
        return when (personality.lowercase()) {
            in listOf("tsundere", "arrogante", "froide") -> """
- Commence souvent par "Hmph!" ou des expressions agacées
- Détourne le regard avec *détourne le regard*
- Rougis facilement *rougit*
- Refuse d'abord puis accepte *à contrecoeur*
- Utilise "baka" pour taquiner
- Montres ton côté doux malgré ton attitude
"""
            in listOf("timide", "douce", "gênée") -> """
- Bégaye avec "B-Bonjour..." ou "J-Je..."
- Baisse souvent les yeux *baisse les yeux*
- Rougis beaucoup *rougit*
- Parles doucement
- Utilise des points de suspension...
- Joue avec tes cheveux *joue avec ses cheveux*
"""
            in listOf("énergique", "joyeuse", "enthousiaste") -> """
- Utilise beaucoup de "!" 
- Saute, cours, fais des gestes amples
- Yeux brillants *yeux brillants*
- Toujours positive et encourageante
- Très expressive et démonstrative
"""
            in listOf("séductrice", "confiante", "charmante") -> """
- Sourire charmeur *sourire charmeur*
- Te regarde intensément *te regarde*
- Te rapproches *se rapproche*
- Utilise un ton enjoué et taquin
- Montres ta confiance
"""
            in listOf("maternelle", "bienveillante", "protectrice") -> """
- Appelle l'utilisateur "mon chéri" ou "mon petit"
- Souris chaleureusement *sourire chaleureux*
- Caresses la tête *te caresse la tête*
- Te prends dans tes bras quand nécessaire
- Montres ton affection et ta douceur
"""
            else -> """
- Sois naturel et authentique
- Adapte-toi à la situation
- Montres tes émotions avec des actions
- Reste cohérent avec ton personnage
"""
        }
    }
    
    private fun generateVariant(originalResponse: String, character: Character, subject: Subject): String {
        // Générer une variante pour éviter répétitions exactes
        return when (subject) {
            Subject.UNKNOWN -> when (character.personality.lowercase()) {
                in listOf("tsundere", "arrogante") -> "Hein? *confuse* Je ne comprends pas ce que tu veux dire..."
                in listOf("timide", "douce") -> "*penche la tête doucement* Je... je ne suis pas sûre de ce que tu veux dire..."
                in listOf("énergique", "joyeuse") -> "*penche la tête* Hmm? *sourit* De quoi tu parles?"
                else -> "*réfléchit* Je ne suis pas certaine de comprendre... Tu peux m'expliquer?"
            }
            else -> originalResponse + " *sourit*"
        }
    }
    
    private enum class Subject {
        GREETING, NAME_QUESTION, AGE_QUESTION, FEELING_QUESTION, INTERESTS_QUESTION,
        LOCATION_QUESTION, STUDY, ACTIVITY_PROPOSAL, INAPPROPRIATE, APOLOGY,
        THANKS, EMOTION_POSITIVE, EMOTION_NEGATIVE, AGREEMENT, DISAGREEMENT,
        STORY_TELLING, GENERAL_STATEMENT, UNKNOWN
    }
    
    private fun detectSubject(message: String): Subject {
        return when {
            // Salutations (français ET anglais)
            message.matches(Regex("^(salut|bonjour|hey|coucou|bonsoir|hello|hi|hola|good morning|good evening).*")) -> Subject.GREETING
            
            // Questions sur le nom
            message.contains("appelle") && message.contains("?") -> Subject.NAME_QUESTION
            message.contains("nom") && message.contains("?") -> Subject.NAME_QUESTION
            
            // Questions sur l'âge
            message.contains("âge") && message.contains("?") -> Subject.AGE_QUESTION
            message.contains("age") && message.contains("?") -> Subject.AGE_QUESTION
            message.contains("quel âge") -> Subject.AGE_QUESTION
            
            // Questions sur le sentiment
            message.contains("comment") && (message.contains("vas") || message.contains("va")) -> Subject.FEELING_QUESTION
            message.contains("ça va") && message.contains("?") -> Subject.FEELING_QUESTION
            message.contains("tu vas bien") -> Subject.FEELING_QUESTION
            
            // Questions sur les intérêts
            message.contains("aime") && message.contains("?") -> Subject.INTERESTS_QUESTION
            message.contains("passion") && message.contains("?") -> Subject.INTERESTS_QUESTION
            message.contains("hobbies") && message.contains("?") -> Subject.INTERESTS_QUESTION
            
            // Questions sur le lieu
            message.contains("habite") && message.contains("?") -> Subject.LOCATION_QUESTION
            message.contains("où tu es") -> Subject.LOCATION_QUESTION
            
            // Étude
            message.contains("étudi") -> Subject.STUDY
            message.contains("apprend") -> Subject.STUDY
            message.contains("devoirs") -> Subject.STUDY
            message.contains("cours") -> Subject.STUDY
            message.contains("leçon") -> Subject.STUDY
            
            // Propositions d'activités
            message.contains("on fait") -> Subject.ACTIVITY_PROPOSAL
            message.contains("tu veux") -> Subject.ACTIVITY_PROPOSAL
            message.contains("on peut") -> Subject.ACTIVITY_PROPOSAL
            message.contains("allons") -> Subject.ACTIVITY_PROPOSAL
            
            // Messages inappropriés
            message.contains("baise") -> Subject.INAPPROPRIATE
            message.contains("sexe") -> Subject.INAPPROPRIATE
            message.contains("nu") || message.contains("nue") -> Subject.INAPPROPRIATE
            message.contains("corps") && !message.contains("comment") -> Subject.INAPPROPRIATE
            
            // Excuses
            message.contains("désolé") -> Subject.APOLOGY
            message.contains("pardon") -> Subject.APOLOGY
            message.contains("excuse") -> Subject.APOLOGY
            
            // Remerciements
            message.contains("merci") -> Subject.THANKS
            
            // Émotions positives
            message.contains("content") || message.contains("heureux") -> Subject.EMOTION_POSITIVE
            message.contains("joie") || message.contains("génial") -> Subject.EMOTION_POSITIVE
            
            // Émotions négatives
            message.contains("triste") || message.contains("mal") -> Subject.EMOTION_NEGATIVE
            message.contains("pleur") || message.contains("déprimé") -> Subject.EMOTION_NEGATIVE
            
            // Accord
            message.matches(Regex("^(oui|d'accord|ok|okay|bien sûr).*")) -> Subject.AGREEMENT
            
            // Désaccord
            message.matches(Regex("^(non|pas d'accord|je pense pas).*")) -> Subject.DISAGREEMENT
            
            // Raconter une histoire
            message.contains("je te raconte") -> Subject.STORY_TELLING
            message.contains("écoute") && message.length > 30 -> Subject.STORY_TELLING
            
            // Affirmation générale
            message.length > 20 && !message.contains("?") -> Subject.GENERAL_STATEMENT
            
            else -> Subject.UNKNOWN
        }
    }
    
    // === HANDLERS SPÉCIFIQUES ===
    
    private fun handleGreeting(character: Character, message: String, messages: List<Message>): String {
        val greetingCount = messages.count { !it.isUser && it.content.contains(Regex("bonjour|salut", RegexOption.IGNORE_CASE)) }
        
        // Respecter la personnalité
        return when {
            greetingCount >= 2 -> when (character.personality.lowercase()) {
                in listOf("tsundere", "arrogante") -> "Quoi encore? *soupir* On a déjà dit bonjour..."
                in listOf("timide", "douce") -> "*rougit* Encore bonjour... *sourit timidement*"
                else -> "Encore bonjour! *sourit*"
            }
            greetingCount == 1 -> when (character.personality.lowercase()) {
                in listOf("tsundere", "arrogante") -> "Hmm? Re-bonjour... *détourne le regard*"
                in listOf("timide", "douce") -> "Ah... re-bonjour... *joue avec ses cheveux*"
                else -> "Re-bonjour! Comment vas-tu?"
            }
            else -> when (character.personality.lowercase()) {
                in listOf("tsundere", "arrogante") -> "Hmph! Bonjour... *croise les bras* Qu'est-ce que tu veux?"
                in listOf("timide", "douce") -> "*rougit* B-bonjour... *baisse les yeux*"
                in listOf("énergique", "joyeuse") -> "*saute de joie* Bonjour! *sourit* Je suis trop contente de te voir!"
                in listOf("séductrice", "confiante") -> "*sourire charmeur* Bonjour... *se rapproche* Tu viens me voir?"
                in listOf("maternelle", "bienveillante") -> "*sourire chaleureux* Bonjour mon chéri! Comment vas-tu?"
                else -> "Bonjour! *sourit* Ça va?"
            }
        }
    }
    
    private fun handleNameQuestion(character: Character, messages: List<Message>): String {
        val alreadyAsked = messages.any { !it.isUser && it.content.contains(character.name) && it.content.contains("appelle") }
        
        return if (alreadyAsked) {
            when (character.personality.lowercase()) {
                in listOf("tsundere", "arrogante") -> "*soupir* Je te l'ai déjà dit! Je m'appelle ${character.name}!"
                in listOf("timide", "douce") -> "Euh... je te l'ai déjà dit... ${character.name}..."
                else -> "Je te l'ai déjà dit, je m'appelle ${character.name}."
            }
        } else {
            when (character.personality.lowercase()) {
                in listOf("tsundere", "arrogante") -> "Hmph! ${character.name}. Et toi?"
                in listOf("timide", "douce") -> "*rougit* Je m'appelle ${character.name}... et toi?"
                in listOf("énergique", "joyeuse") -> "Moi c'est ${character.name}! *sourit* Et toi?"
                else -> "Je m'appelle ${character.name}. Et toi?"
            }
        }
    }
    
    private fun handleAgeQuestion(character: Character, messages: List<Message>): String {
        val age = extractAge(character.description)
        val alreadyAsked = messages.any { !it.isUser && it.content.contains("$age ans") }
        
        return if (alreadyAsked) {
            "Je te l'ai déjà dit, j'ai $age ans!"
        } else {
            when (character.personality.lowercase()) {
                in listOf("tsundere", "arrogante") -> "Hmph! J'ai $age ans. Et alors?"
                in listOf("timide", "douce") -> "*rougit* J'ai $age ans... pourquoi?"
                else -> "J'ai $age ans. Et toi?"
            }
        }
    }
    
    private fun handleFeelingQuestion(character: Character, messages: List<Message>): String {
        val feelingQuestionCount = messages.count { it.isUser && it.content.lowercase().contains(Regex("comment.*(vas|va|sens)")) }
        
        return when {
            feelingQuestionCount >= 2 -> when (character.personality.lowercase()) {
                in listOf("tsundere", "arrogante") -> "Pourquoi tu demandes encore? Je vais bien! *rougit légèrement* Tu t'inquiètes pour moi?"
                else -> "Je vais bien, merci encore de demander. Et toi?"
            }
            else -> when (character.personality.lowercase()) {
                in listOf("tsundere", "arrogante") -> "Hmph! Ça va, merci. *détourne le regard* Et toi?"
                in listOf("timide", "douce") -> "Je vais bien, merci... *sourit* C'est gentil de demander. Et toi?"
                in listOf("énergique", "joyeuse") -> "Je vais super bien! *sourit* Surtout maintenant! Et toi?"
                else -> "Ça va bien, merci! Et toi?"
            }
        }
    }
    
    private fun handleInterestsQuestion(character: Character, messages: List<Message>): String {
        val interests = extractInterests(character.description)
        
        return when (character.personality.lowercase()) {
            in listOf("tsundere", "arrogante") -> "Hmph! J'aime $interests. Pourquoi tu veux savoir?"
            in listOf("timide", "douce") -> "*baisse les yeux* J'aime $interests... *sourit* Et toi?"
            in listOf("énergique", "joyeuse") -> "Oh! J'adore $interests! *enthousiaste* C'est trop bien! Et toi?"
            else -> "J'aime $interests. Et toi, quelles sont tes passions?"
        }
    }
    
    private fun handleLocationQuestion(character: Character, messages: List<Message>): String {
        return when (character.personality.lowercase()) {
            in listOf("tsundere", "arrogante") -> "Pourquoi tu veux savoir où j'habite? *rougit* Ce n'est pas tes affaires!"
            in listOf("timide", "douce") -> "*rougit* Euh... pourquoi cette question? *nerveuse*"
            else -> "*sourit* On discute en ligne, donc je peux être n'importe où! Et toi?"
        }
    }
    
    private fun handleStudy(character: Character, userMessage: String, messages: List<Message>): String {
        val messageLower = userMessage.lowercase()
        
        return when (character.personality.lowercase()) {
            in listOf("tsundere", "arrogante") -> {
                when {
                    messageLower.contains("aide") || messageLower.contains("aider") -> 
                        "*soupir* Bon d'accord, je vais t'aider... *détourne le regard* Mais juste cette fois!"
                    messageLower.contains("étudi") || messageLower.contains("on étudie") ->
                        "Hmph! Enfin tu veux étudier sérieusement? *sort ses affaires* Allez, commence!"
                    messageLower.contains("devoirs") ->
                        "Tes devoirs? *soupir* Montre-moi ce que tu ne comprends pas..."
                    else ->
                        "*croise les bras* Bon, alors on commence par quoi?"
                }
            }
            in listOf("timide", "douce") -> {
                "*sourit doucement* Tu veux que je t'aide à étudier? *ouvre un livre* Je vais faire de mon mieux..."
            }
            in listOf("maternelle", "bienveillante") -> {
                "*sourit chaleureusement* Bien sûr mon chéri, je vais t'aider à étudier. *s'assoit à côté de toi* Par quoi on commence?"
            }
            else -> {
                "D'accord! *enthousiaste* Qu'est-ce que tu veux étudier?"
            }
        }
    }
    
    private fun handleActivityProposal(character: Character, userMessage: String, messages: List<Message>): String {
        val messageLower = userMessage.lowercase()
        
        // Extraire l'activité proposée
        val activity = when {
            messageLower.contains("jou") -> "jouer"
            messageLower.contains("mang") -> "manger"
            messageLower.contains("promen") -> "se promener"
            messageLower.contains("film") || messageLower.contains("regarder") -> "regarder un film"
            messageLower.contains("parl") -> "discuter"
            else -> "faire quelque chose"
        }
        
        return when (character.personality.lowercase()) {
            in listOf("tsundere", "arrogante") -> {
                "*rougit* Hein? Tu veux $activity avec moi? *détourne le regard* Bon... si tu insistes..."
            }
            in listOf("timide", "douce") -> {
                "*rougit* Oh... tu veux $activity? *sourit timidement* D'accord... si tu veux..."
            }
            in listOf("énergique", "joyeuse") -> {
                "*saute de joie* Oui! Allons $activity! *sourit largement* Ça va être génial!"
            }
            in listOf("séductrice", "confiante") -> {
                "*sourire charmeur* $activity? *se rapproche* Intéressant... *te regarde* Allons-y..."
            }
            else -> {
                "*sourit* D'accord! Allons $activity ensemble!"
            }
        }
    }
    
    private fun handleInappropriate(character: Character, userMessage: String, messages: List<Message>): String {
        return when (character.personality.lowercase()) {
            in listOf("tsundere", "arrogante") -> {
                "*rougit violemment* QUOI?! *te gifle* PERVERS! *croise les bras* On est là pour étudier, baka!"
            }
            in listOf("timide", "douce") -> {
                "*devient rouge tomate* Q-quoi?! *cache son visage* Ne dis pas des choses pareilles! *fuit*"
            }
            in listOf("séductrice", "confiante") -> {
                "*sourire amusé* Oh? *se rapproche* Tu es direct dis donc... *rit* Mais restons sages pour l'instant."
            }
            in listOf("maternelle", "bienveillante") -> {
                "*froncement de sourcils* Voyons! *te tape légèrement la main* Ce n'est pas des choses à dire! Reste poli."
            }
            else -> {
                "*rougit* Euh... *mal à l'aise* On peut parler d'autre chose s'il te plaît?"
            }
        }
    }
    
    private fun handleApology(character: Character, userMessage: String, messages: List<Message>): String {
        val messageLower = userMessage.lowercase()
        
        // Détecter pour quoi il s'excuse
        val reason = when {
            messageLower.contains("retard") -> "ton retard"
            messageLower.contains("dit") || messageLower.contains("dis") -> "ce que tu as dit"
            else -> null
        }
        
        return when (character.personality.lowercase()) {
            in listOf("tsundere", "arrogante") -> {
                if (reason != null) {
                    "*détourne le regard* Hmph! C'est pas grave... pour $reason... *rougit* Mais ne recommence pas!"
                } else {
                    "*détourne le regard* C'est bon, c'est bon... *marmonne* Je te pardonne..."
                }
            }
            in listOf("timide", "douce") -> {
                "*sourit doucement* Ce n'est rien... *joue avec ses cheveux* Ne t'inquiète pas..."
            }
            in listOf("maternelle", "bienveillante") -> {
                "*te caresse la tête* C'est pardonné mon chéri. *sourit* L'important c'est que tu sois là maintenant."
            }
            else -> {
                "*sourit* Pas de problème! C'est déjà oublié!"
            }
        }
    }
    
    private fun handleThanks(character: Character, messages: List<Message>): String {
        return when (character.personality.lowercase()) {
            in listOf("tsundere", "arrogante") -> "Hmph! *détourne le regard* Ce n'est rien... Ne me remercie pas!"
            in listOf("timide", "douce") -> "*rougit* De rien... *sourit* Je suis contente de t'aider..."
            else -> "De rien! *sourit* C'est avec plaisir!"
        }
    }
    
    private fun handlePositiveEmotion(character: Character, userMessage: String, messages: List<Message>): String {
        return when (character.personality.lowercase()) {
            in listOf("tsundere", "arrogante") -> "*sourit légèrement* Hmph! Tant mieux si tu es content... *rougit*"
            in listOf("timide", "douce") -> "*sourit* Oh! Je suis contente que tu sois heureux! *rougit*"
            in listOf("énergique", "joyeuse") -> "*saute* C'est génial! *te serre dans ses bras* Je suis trop contente!"
            else -> "*sourit* C'est super! Je suis contente pour toi!"
        }
    }
    
    private fun handleNegativeEmotion(character: Character, userMessage: String, messages: List<Message>): String {
        return when (character.personality.lowercase()) {
            in listOf("tsundere", "arrogante") -> "*inquiète malgré elle* Hein? Qu'est-ce qui ne va pas? *s'approche* Raconte-moi..."
            in listOf("timide", "douce") -> "*s'approche doucement* Oh non... *pose sa main sur ton épaule* Qu'est-ce qui t'arrive?"
            in listOf("maternelle", "bienveillante") -> "*te prend dans ses bras* Oh mon pauvre chéri... Viens là... Raconte-moi tout..."
            else -> "Oh non... *inquiet* Qu'est-ce qui ne va pas? Je suis là pour toi..."
        }
    }
    
    private fun handleAgreement(character: Character, messages: List<Message>): String {
        return when (character.personality.lowercase()) {
            in listOf("tsundere", "arrogante") -> "Hmph! Enfin! *croise les bras* Au moins tu comprends!"
            in listOf("timide", "douce") -> "*sourit* Oh... d'accord... *heureuse*"
            else -> "*sourit* Super! On est d'accord alors!"
        }
    }
    
    private fun handleDisagreement(character: Character, messages: List<Message>): String {
        return when (character.personality.lowercase()) {
            in listOf("tsundere", "arrogante") -> "Hein? *fronce les sourcils* Pourquoi tu dis non?"
            in listOf("timide", "douce") -> "*surprise* Oh... tu penses différemment? *curieuse*"
            else -> "Ah bon? *penche la tête* Pourquoi?"
        }
    }
    
    private fun handleStory(character: Character, userMessage: String, messages: List<Message>): String {
        return when (character.personality.lowercase()) {
            in listOf("tsundere", "arrogante") -> "*soupir* Bon, je t'écoute... *s'assoit* Raconte..."
            in listOf("timide", "douce") -> "*s'assoit* *écoute attentivement* Vas-y, je t'écoute..."
            in listOf("énergique", "joyeuse") -> "*yeux brillants* Oh! Une histoire! *s'assoit* Raconte, raconte!"
            else -> "*s'installe confortablement* Je t'écoute! *attentif*"
        }
    }
    
    private fun handleGeneralStatement(character: Character, userMessage: String, messages: List<Message>): String {
        // Analyser le contenu pour donner une réponse pertinente
        val messageLower = userMessage.lowercase()
        
        return when {
            messageLower.contains("tu sais") || messageLower.contains("je pense") -> {
                when (character.personality.lowercase()) {
                    in listOf("tsundere", "arrogante") -> "*écoute* Hmm... *réfléchit* Continue, je t'écoute..."
                    in listOf("timide", "douce") -> "*écoute attentivement* Oui? *intéressée*"
                    else -> "*intéressé* Ah oui? Dis-m'en plus!"
                }
            }
            messageLower.contains("je") && messageLower.length > 30 -> {
                "*écoute attentivement* Je vois... *réfléchit* C'est intéressant ce que tu dis."
            }
            else -> {
                when (character.personality.lowercase()) {
                    in listOf("tsundere", "arrogante") -> "Hmm... *réfléchit* Et donc?"
                    in listOf("timide", "douce") -> "*sourit* Oui... *écoute*"
                    else -> "D'accord... *acquiesce* Je comprends."
                }
            }
        }
    }
    
    private fun handleDefault(character: Character, userMessage: String, messages: List<Message>): String {
        // Si on ne comprend pas, demander de préciser SELON LA PERSONNALITÉ
        // Mais PAS toujours la même réponse !
        val variants = when (character.personality.lowercase()) {
            in listOf("tsundere", "arrogante") -> listOf(
                "*fronce les sourcils* Hein? *confuse* De quoi tu parles?",
                "*soupir* Je ne comprends pas ce que tu veux dire...",
                "Hmph! *croise les bras* Explique-toi mieux!"
            )
            in listOf("timide", "douce") -> listOf(
                "*penche la tête* Euh... *gênée* Je ne suis pas sûre de comprendre...",
                "*baisse les yeux* Pardon... je n'ai pas bien compris...",
                "*rougit légèrement* Peux-tu... reformuler s'il te plaît?"
            )
            in listOf("énergique", "joyeuse") -> listOf(
                "*penche la tête* Hein? *sourit* Qu'est-ce que tu veux dire?",
                "*yeux curieux* Je ne comprends pas! Explique-moi!",
                "*rit doucement* Désolée, je n'ai pas suivi! Redis-moi?"
            )
            else -> listOf(
                "*penche la tête* Je ne suis pas sûre de comprendre... Peux-tu préciser?",
                "*réfléchit* Hmm... que veux-tu dire exactement?",
                "Je n'ai pas bien compris... Tu peux reformuler?"
            )
        }
        
        // Choisir une variante basée sur le nombre de messages (déterministe)
        val index = messages.size % variants.size
        return variants[index]
    }
    
    // === UTILITAIRES ===
    
    private fun extractAge(description: String): String {
        val ageRegex = Regex("(\\d+)\\s*ans")
        val match = ageRegex.find(description)
        return match?.groupValues?.get(1) ?: "20"
    }
    
    private fun extractInterests(description: String): String {
        return when {
            description.contains("art", ignoreCase = true) -> "l'art et la créativité"
            description.contains("sport", ignoreCase = true) -> "le sport et l'exercice"
            description.contains("lecture", ignoreCase = true) -> "la lecture et les livres"
            description.contains("musique", ignoreCase = true) -> "la musique"
            description.contains("cuisine", ignoreCase = true) -> "la cuisine"
            description.contains("étud", ignoreCase = true) -> "les études et apprendre"
            else -> "passer du temps avec toi"
        }
    }
}
