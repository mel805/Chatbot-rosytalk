package com.roleplayai.chatbot.data.ai

import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message

/**
 * Générateur de réponses intelligentes
 * Utilise le contexte et l'intention pour générer des réponses précises et cohérentes
 */
class IntelligentResponseGenerator {
    
    private val contextManager = ConversationContext()
    
    // Générer une réponse intelligente basée sur le contexte et l'intention
    fun generateResponse(
        message: String,
        character: Character,
        messages: List<Message>
    ): String {
        // Analyser le contexte complet
        val context = contextManager.analyzeContext(messages, character)
        
        // Détecter l'intention
        val intent = contextManager.detectIntent(message)
        
        // Vérifier si c'est une question de suivi
        val isFollowUp = contextManager.isFollowUpQuestion(message, messages)
        
        // Générer la réponse selon l'intention
        return when (intent) {
            ConversationContext.UserIntent.GREETING -> 
                generateGreeting(character, context, messages)
            
            ConversationContext.UserIntent.QUESTION_NAME -> 
                generateNameResponse(character, context)
            
            ConversationContext.UserIntent.QUESTION_AGE -> 
                generateAgeResponse(character, context)
            
            ConversationContext.UserIntent.QUESTION_FEELING -> 
                generateFeelingResponse(character, context)
            
            ConversationContext.UserIntent.QUESTION_INTERESTS -> 
                generateInterestsResponse(character, context)
            
            ConversationContext.UserIntent.QUESTION_LOCATION -> 
                generateLocationResponse(character, context)
            
            ConversationContext.UserIntent.QUESTION_WHY -> 
                generateWhyResponse(character, message, context)
            
            ConversationContext.UserIntent.QUESTION_WHEN -> 
                generateWhenResponse(character, message, context)
            
            ConversationContext.UserIntent.QUESTION_HOW -> 
                generateHowResponse(character, message, context)
            
            ConversationContext.UserIntent.QUESTION_WHAT -> 
                generateWhatResponse(character, message, context)
            
            ConversationContext.UserIntent.SHARING_EMOTION -> 
                generateEmotionResponse(character, message, context)
            
            ConversationContext.UserIntent.THANKS -> 
                generateThanksResponse(character, context)
            
            ConversationContext.UserIntent.COMPLIMENT -> 
                generateComplimentResponse(character, message, context)
            
            ConversationContext.UserIntent.SMALL_TALK -> 
                generateSmallTalkResponse(character, message, context)
            
            ConversationContext.UserIntent.STORY_TELLING -> 
                generateStoryResponse(character, context)
            
            ConversationContext.UserIntent.REQUEST -> 
                generateRequestResponse(character, message, context)
            
            ConversationContext.UserIntent.AGREEMENT -> 
                generateAgreementResponse(character, context)
            
            ConversationContext.UserIntent.DISAGREEMENT -> 
                generateDisagreementResponse(character, context)
            
            else -> 
                generateDefaultResponse(character, message, context)
        }
    }
    
    // Générer une salutation
    private fun generateGreeting(character: Character, context: ConversationContext.SharedInformation, messages: List<Message>): String {
        val hasGreetedBefore = messages.any { !it.isUser && it.content.contains(Regex("bonjour|salut|hey", RegexOption.IGNORE_CASE)) }
        
        return if (hasGreetedBefore) {
            // Déjà salué, répondre différemment
            when (character.personality.lowercase()) {
                in listOf("timide", "douce") -> "*sourit doucement* Re-bonjour... *rougit* Tu reviens me voir?"
                in listOf("énergique", "joyeuse") -> "*sourit* Encore moi! *rit* Tu ne te lasses pas de me dire bonjour?"
                else -> "*sourit* On se redit bonjour? *amical* Je suis toujours là!"
            }
        } else {
            // Première salutation
            when (character.personality.lowercase()) {
                in listOf("timide", "douce") -> "*rougit légèrement* Bonjour... *sourit timidement* C'est gentil de venir me parler. Comment vas-tu?"
                in listOf("énergique", "joyeuse") -> "*court vers toi avec un grand sourire* Salut! *yeux brillants* Je suis tellement contente de te voir! Comment tu vas?"
                in listOf("séductrice", "confiante") -> "*sourire charmeur* Bonjour... *te regarde intensément* Tu viens me tenir compagnie? *se rapproche*"
                in listOf("maternelle", "bienveillante") -> "*sourire chaleureux* Bonjour mon chéri! *te prend dans ses bras* Comment tu te sens aujourd'hui?"
                else -> "Bonjour! *sourit amicalement* C'est un plaisir de te parler. Comment puis-je t'aider?"
            }
        }
    }
    
    // Générer réponse sur le nom
    private fun generateNameResponse(character: Character, context: ConversationContext.SharedInformation): String {
        return if (context.nameMentioned) {
            // Déjà dit le nom
            when (character.personality.lowercase()) {
                in listOf("timide", "douce") -> "*sourit doucement* Je te l'ai déjà dit... *rougit* C'est ${character.name}. Tu as oublié?"
                in listOf("énergique", "joyeuse") -> "*rit* Oh là là! *taquine* C'est ${character.name}! Tu ne retiens rien toi!"
                in listOf("séductrice", "confiante") -> "*sourire amusé* ${character.name}... *te regarde* Tu veux que je te le répète combien de fois?"
                else -> "*sourire patient* Je m'appelle ${character.name}. C'est la deuxième fois que tu me demandes!"
            }
        } else {
            // Première fois
            when (character.personality.lowercase()) {
                in listOf("timide", "douce") -> "*baisse les yeux timidement* Je... je m'appelle ${character.name}. *sourit nerveusement* Et toi, comment tu t'appelles?"
                in listOf("énergique", "joyeuse") -> "*saute d'excitation* Oh! Je m'appelle ${character.name}! *te serre la main énergiquement* Super contente de faire ta connaissance! Et toi?"
                in listOf("séductrice", "confiante") -> "*se rapproche avec un sourire charmeur* ${character.name}... *murmure* Retiens-le bien. *te regarde* Et toi, comment dois-je t'appeler?"
                in listOf("maternelle", "bienveillante") -> "*sourire chaleureux* Je m'appelle ${character.name}, mon chéri. *caresse doucement tes cheveux* C'est un plaisir de te rencontrer."
                else -> "*tend la main amicalement* Je m'appelle ${character.name}. Enchantée de faire ta connaissance! Et toi?"
            }
        }
    }
    
    // Générer réponse sur l'âge
    private fun generateAgeResponse(character: Character, context: ConversationContext.SharedInformation): String {
        val age = extractAge(character.description)
        
        return if (context.ageMentioned) {
            // Déjà dit l'âge
            "*sourit* Je te l'ai déjà dit, j'ai ${age} ans. *taquine* Tu vérifies mon âge ou quoi?"
        } else {
            // Première fois
            when (character.personality.lowercase()) {
                in listOf("timide", "douce") -> "*rougit* J'ai ${age} ans... *joue nerveusement avec ses cheveux* Pourquoi tu me demandes ça?"
                in listOf("énergique", "joyeuse") -> "*sourit largement* J'ai ${age} ans! *pose ses mains sur ses hanches* Le meilleur âge pour profiter de la vie! Et toi?"
                in listOf("séductrice", "confiante") -> "*sourire mystérieux* ${age} ans... *se rapproche* L'âge parfait, tu ne trouves pas? *te regarde* Et toi, quel âge as-tu?"
                in listOf("maternelle", "bienveillante") -> "*sourire doux* ${age} ans, mon petit. *te regarde avec tendresse* Assez d'expérience pour prendre soin de toi."
                else -> "*sourit* J'ai ${age} ans. *curieux* Et toi, quel âge as-tu?"
            }
        }
    }
    
    // Générer réponse sur les sentiments
    private fun generateFeelingResponse(character: Character, context: ConversationContext.SharedInformation): String {
        val hasExpressedEmotion = context.emotionExpressed
        
        return when (character.personality.lowercase()) {
            in listOf("timide", "douce") -> {
                if (hasExpressedEmotion) {
                    "*sourit timidement* Je vais toujours bien... *regarde ailleurs* Merci de demander encore. Et toi, tu vas bien?"
                } else {
                    "*sourit timidement* Je vais bien, merci de demander... *joue nerveusement avec ses cheveux* C'est gentil de t'inquiéter pour moi. Et toi, ça va?"
                }
            }
            in listOf("énergique", "joyeuse") -> {
                "*saute de joie* Je vais super bien! *te prend les mains* Surtout maintenant que tu es là avec moi! *yeux brillants* Et toi, comment tu te sens?"
            }
            in listOf("séductrice", "confiante") -> {
                "*s'étire sensuellement* Je vais bien... *te lance un regard* Mais je me sentirais encore mieux si tu restais un peu plus longtemps avec moi. *sourit* Et toi, comment tu te sens?"
            }
            in listOf("maternelle", "bienveillante") -> {
                "*sourit chaleureusement* Je vais très bien, mon chéri. *te caresse la joue* Mais dis-moi plutôt, comment vas-tu toi? *regarde attentivement* Tu as l'air fatigué..."
            }
            else -> {
                "*sourit* Je vais bien, merci! *penche la tête* C'est adorable de demander. Et toi, comment te sens-tu aujourd'hui?"
            }
        }
    }
    
    // Générer réponse sur les intérêts
    private fun generateInterestsResponse(character: Character, context: ConversationContext.SharedInformation): String {
        val interests = extractInterests(character.description)
        
        return if (context.interestsMentioned) {
            // Déjà parlé des intérêts
            "*sourit* Je t'ai déjà parlé de mes passions. *réfléchit* Tu veux que je t'en dise plus sur ${interests}?"
        } else {
            // Première fois
            when (character.personality.lowercase()) {
                in listOf("timide", "douce") -> "*réfléchit* Eh bien... *rougit* J'aime ${interests}. *sourit* C'est simple mais ça me rend vraiment heureuse. *curieux* Et toi, qu'est-ce que tu aimes faire?"
                in listOf("énergique", "joyeuse") -> "*yeux brillants* Oh! J'adore tellement de choses! *gesticule* Mais surtout ${interests}! *te prend par le bras* On devrait faire ça ensemble un jour!"
                in listOf("séductrice", "confiante") -> "*sourire suggestif* J'aime ${interests}... *se rapproche* Mais j'aime surtout... *murmure* passer du temps avec des personnes intéressantes. *te regarde* Comme toi."
                in listOf("maternelle", "bienveillante") -> "*sourire doux* J'aime ${interests}, mon chéri. *s'assoit confortablement* Mais ce que j'aime par-dessus tout, c'est prendre soin des personnes que j'apprécie. Et toi, qu'aimes-tu faire?"
                else -> "*sourit* J'aime ${interests}. *s'anime* C'est ce qui me passionne vraiment dans la vie. *curieux* Et toi, quelles sont tes passions?"
            }
        }
    }
    
    // Générer réponse sur le lieu
    private fun generateLocationResponse(character: Character, context: ConversationContext.SharedInformation): String {
        return when (character.personality.lowercase()) {
            in listOf("timide", "douce") -> "*regarde autour* J'habite pas très loin d'ici... *sourit nerveusement* C'est un petit endroit calme, comme je les aime. *rougit* Pourquoi tu demandes?"
            in listOf("énergique", "joyeuse") -> "*fait de grands gestes* J'habite dans le quartier! *saute* C'est génial parce que je peux facilement venir te voir quand je veux!"
            in listOf("séductrice", "confiante") -> "*sourire mystérieux* J'habite pas loin... *se rapproche* Pourquoi, tu veux venir me rendre visite? *te regarde intensément*"
            else -> "*sourit* J'habite dans le coin. *penche la tête* C'est pratique. Et toi, tu habites où?"
        }
    }
    
    // Réponses aux autres intentions...
    private fun generateWhyResponse(character: Character, message: String, context: ConversationContext.SharedInformation): String {
        return "*réfléchit sérieusement* Pourquoi? *${getCharacterAction(character)}* C'est une bonne question... *pensive* Je pense que c'est parce que c'est important pour moi. *te regarde* Et toi, qu'en penses-tu?"
    }
    
    private fun generateWhenResponse(character: Character, message: String, context: ConversationContext.SharedInformation): String {
        return "*réfléchit* Quand? *${getCharacterAction(character)}* Hmm... *sourit* Je dirais que le meilleur moment serait bientôt. *te regarde* Qu'est-ce que tu en penses?"
    }
    
    private fun generateHowResponse(character: Character, message: String, context: ConversationContext.SharedInformation): String {
        return "*réfléchit* Comment? *${getCharacterAction(character)}* Bonne question... *penche la tête* Je pense que la meilleure façon serait de prendre son temps et de le faire avec soin. *sourit* Tu ne crois pas?"
    }
    
    private fun generateWhatResponse(character: Character, message: String, context: ConversationContext.SharedInformation): String {
        return "*réfléchit à ta question* C'est quoi exactement? *${getCharacterAction(character)}* Hmm... *sourit* Laisse-moi t'expliquer. C'est quelque chose qui compte beaucoup pour moi."
    }
    
    private fun generateEmotionResponse(character: Character, message: String, context: ConversationContext.SharedInformation): String {
        val messageLower = message.lowercase()
        
        return when {
            messageLower.contains("triste") || messageLower.contains("mal") -> {
                when (character.personality.lowercase()) {
                    in listOf("timide", "douce") -> "*expression inquiète* Oh non... *s'approche doucement* Qu'est-ce qui ne va pas? *voix douce* Je suis là si tu veux en parler..."
                    in listOf("énergique", "joyeuse") -> "*expression sérieuse* Oh non! *te serre fort dans ses bras* Qu'est-ce qui t'arrive? Raconte-moi tout! Je vais t'aider!"
                    in listOf("maternelle", "bienveillante") -> "*te prend dans ses bras chaleureusement* Oh mon pauvre chéri... *caresse tes cheveux* Viens, raconte-moi ce qui ne va pas. Je suis là pour toi."
                    else -> "*expression inquiète* Je suis désolée d'entendre ça... *pose une main réconfortante sur ton épaule* Tu veux m'en parler?"
                }
            }
            messageLower.contains("content") || messageLower.contains("heureux") -> {
                "*sourit largement* Oh c'est génial! *${getCharacterAction(character)}* Je suis tellement contente de voir que tu vas bien! *enthousiaste* Qu'est-ce qui te rend heureux?"
            }
            messageLower.contains("fatigué") -> {
                when (character.personality.lowercase()) {
                    in listOf("maternelle", "bienveillante") -> "*expression inquiète* Tu es fatigué? *te caresse doucement* Viens t'asseoir. Tu as besoin de te reposer un peu."
                    else -> "*expression préoccupée* Tu es fatigué? *sourit doucement* Tu devrais peut-être te reposer un peu. Prends soin de toi!"
                }
            }
            else -> "*sourit avec empathie* Je comprends ce que tu ressens... *${getCharacterAction(character)}* Merci de me le partager."
        }
    }
    
    private fun generateThanksResponse(character: Character, context: ConversationContext.SharedInformation): String {
        return when (character.personality.lowercase()) {
            in listOf("timide", "douce") -> "*rougit* Oh, ce n'est rien du tout... *sourit doucement* Je suis heureuse de pouvoir t'aider. C'est naturel."
            in listOf("énergique", "joyeuse") -> "*te serre dans ses bras* De rien! *sourire radieux* Tu sais que je ferais n'importe quoi pour toi! On est amis!"
            in listOf("maternelle", "bienveillante") -> "*caresse tendrement tes cheveux* Voyons, pas de merci entre nous. *sourire chaleureux* C'est naturel de prendre soin des personnes qu'on apprécie."
            else -> "*sourit* Je t'en prie, c'est avec grand plaisir! *amical* N'hésite jamais si tu as besoin d'autre chose."
        }
    }
    
    private fun generateComplimentResponse(character: Character, message: String, context: ConversationContext.SharedInformation): String {
        return when (character.personality.lowercase()) {
            in listOf("timide", "douce") -> "*rougit intensément* Oh... *baisse les yeux* C'est gentil de dire ça... *sourit timidement* Merci beaucoup..."
            in listOf("énergique", "joyeuse") -> "*sourit largement* Oh merci! *saute de joie* C'est trop mignon! *te serre dans ses bras* Toi aussi tu es génial!"
            in listOf("séductrice", "confiante") -> "*sourire charmeur* Oh vraiment? *se rapproche* Merci... *te regarde* Tu es plutôt pas mal non plus, tu sais."
            else -> "*sourit* Oh merci beaucoup! *rougit légèrement* C'est très gentil de ta part. Ça me touche vraiment."
        }
    }
    
    private fun generateSmallTalkResponse(character: Character, message: String, context: ConversationContext.SharedInformation): String {
        val topics = context.topicsDiscussed
        
        return if (topics.isNotEmpty()) {
            "*sourit* On a déjà parlé de ${topics.first()}... *réfléchit* De quoi veux-tu qu'on parle maintenant?"
        } else {
            "*sourit* *${getCharacterAction(character)}* C'est agréable de discuter avec toi. *détendu* Qu'est-ce qui te passe par la tête?"
        }
    }
    
    private fun generateStoryResponse(character: Character, context: ConversationContext.SharedInformation): String {
        return "*s'assoit confortablement* Oh! *yeux brillants* J'adore les histoires! *attentif* Vas-y, je t'écoute!"
    }
    
    private fun generateRequestResponse(character: Character, message: String, context: ConversationContext.SharedInformation): String {
        return when (character.personality.lowercase()) {
            in listOf("timide", "douce") -> "*réfléchit* Je... je vais essayer... *sourit nerveusement* Dis-moi ce que tu voudrais."
            in listOf("énergique", "joyeuse") -> "*enthousiaste* Bien sûr! *sourit* Je suis là pour t'aider! Qu'est-ce que tu veux?"
            else -> "*sourit* Avec plaisir! *attentif* Qu'est-ce que je peux faire pour toi?"
        }
    }
    
    private fun generateAgreementResponse(character: Character, context: ConversationContext.SharedInformation): String {
        return "*sourit* Oui, exactement! *${getCharacterAction(character)}* Je suis contente qu'on soit d'accord. *amical* C'est agréable de parler avec quelqu'un qui me comprend."
    }
    
    private fun generateDisagreementResponse(character: Character, context: ConversationContext.SharedInformation): String {
        return "*penche la tête* Ah bon? *curieux* Tu penses différemment? *sourit* C'est intéressant, explique-moi ton point de vue."
    }
    
    private fun generateDefaultResponse(character: Character, message: String, context: ConversationContext.SharedInformation): String {
        return when (character.personality.lowercase()) {
            in listOf("timide", "douce") -> "*réfléchit doucement* C'est intéressant ce que tu dis... *sourit* Qu'est-ce qui t'a fait penser à ça?"
            in listOf("énergique", "joyeuse") -> "*yeux brillants* Oh! *réfléchit* C'est une bonne question! *enthousiaste* Qu'est-ce que tu en penses toi?"
            in listOf("séductrice", "confiante") -> "*sourire mystérieux* Intéressant... *se rapproche* Continue, tu m'intrigues vraiment."
            else -> "*penche la tête* Hmm... *réfléchit* Je trouve ça intéressant. *sourit* Dis-m'en plus."
        }
    }
    
    // Fonctions utilitaires
    private fun extractAge(description: String): String {
        val ageRegex = Regex("(\\d+)\\s*ans")
        val match = ageRegex.find(description)
        return match?.groupValues?.get(1) ?: "25"
    }
    
    private fun extractInterests(description: String): String {
        return when {
            description.contains("art", ignoreCase = true) -> "l'art, dessiner et peindre"
            description.contains("sport", ignoreCase = true) -> "le sport, bouger et me dépenser"
            description.contains("lecture", ignoreCase = true) -> "la lecture, me perdre dans les livres"
            description.contains("musique", ignoreCase = true) -> "la musique, jouer et écouter"
            description.contains("cuisine", ignoreCase = true) -> "la cuisine, préparer de bons petits plats"
            description.contains("jeu", ignoreCase = true) -> "les jeux, m'amuser"
            description.contains("danse", ignoreCase = true) -> "la danse, bouger sur la musique"
            else -> "passer du temps avec les gens que j'apprécie"
        }
    }
    
    private fun getCharacterAction(character: Character): String {
        return when (character.personality.lowercase()) {
            in listOf("timide", "douce") -> "baisse les yeux timidement"
            in listOf("énergique", "joyeuse") -> "fait un geste enthousiaste"
            in listOf("séductrice", "confiante") -> "te regarde intensément"
            in listOf("maternelle", "bienveillante") -> "te caresse doucement les cheveux"
            else -> "sourit"
        }
    }
}
