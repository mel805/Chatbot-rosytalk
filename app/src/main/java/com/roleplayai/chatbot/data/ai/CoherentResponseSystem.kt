package com.roleplayai.chatbot.data.ai

import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message

/**
 * Système de réponses cohérentes et déterministes
 * Supprime l'aléatoire et assure une cohérence stricte
 */
class CoherentResponseSystem {
    
    // Cache des dernières réponses pour éviter répétitions
    private val responseCache = mutableMapOf<String, String>()
    private val conversationState = mutableMapOf<String, Any>()
    
    /**
     * Génère une réponse cohérente basée strictement sur le contexte
     */
    fun generateCoherentResponse(
        userMessage: String,
        character: Character,
        messages: List<Message>,
        intent: ConversationContext.UserIntent,
        context: ConversationContext.SharedInformation
    ): String {
        // Nettoyer le message
        val cleanMessage = userMessage.trim().lowercase()
        
        // Vérifier si on a déjà répondu à quelque chose de similaire
        val cacheKey = "${intent.name}_${cleanMessage.take(20)}"
        
        // Générer la réponse selon l'intention
        val response = when (intent) {
            ConversationContext.UserIntent.GREETING -> 
                generateCoherentGreeting(character, context, messages)
            
            ConversationContext.UserIntent.QUESTION_NAME -> 
                generateCoherentNameResponse(character, context)
            
            ConversationContext.UserIntent.QUESTION_AGE -> 
                generateCoherentAgeResponse(character, context)
            
            ConversationContext.UserIntent.QUESTION_FEELING -> 
                generateCoherentFeelingResponse(character, context, messages)
            
            ConversationContext.UserIntent.QUESTION_INTERESTS -> 
                generateCoherentInterestsResponse(character, context)
            
            ConversationContext.UserIntent.SHARING_EMOTION -> 
                generateCoherentEmotionResponse(character, userMessage, context)
            
            ConversationContext.UserIntent.THANKS -> 
                generateCoherentThanksResponse(character)
            
            ConversationContext.UserIntent.COMPLIMENT -> 
                generateCoherentComplimentResponse(character, context)
            
            else -> 
                generateCoherentDefaultResponse(character, userMessage, context, messages)
        }
        
        // Sauvegarder dans le cache
        responseCache[cacheKey] = response
        
        return response
    }
    
    // Salutation cohérente
    private fun generateCoherentGreeting(
        character: Character,
        context: ConversationContext.SharedInformation,
        messages: List<Message>
    ): String {
        val greetingCount = messages.count { !it.isUser && it.content.contains(Regex("bonjour|salut|hey", RegexOption.IGNORE_CASE)) }
        
        return when {
            greetingCount >= 2 -> {
                // Déjà salué plusieurs fois
                when (character.personality.lowercase()) {
                    in listOf("timide", "douce") -> "*sourit doucement* On se dit encore bonjour? *rougit* C'est mignon."
                    in listOf("énergique", "joyeuse") -> "*rit* Tu adores me dire bonjour toi! *sourit* Moi aussi je suis contente de te parler!"
                    else -> "*sourit* Encore bonjour! *amical* Je suis toujours là pour toi."
                }
            }
            greetingCount == 1 -> {
                // Deuxième salutation
                when (character.personality.lowercase()) {
                    in listOf("timide", "douce") -> "*sourit* Re-bonjour... *joue avec ses cheveux* Tu reviens me voir?"
                    in listOf("énergique", "joyeuse") -> "*sourit largement* Re-salut! *enthousiaste* Comment tu vas maintenant?"
                    else -> "*sourit* On se redit bonjour? *content* Ça me fait plaisir."
                }
            }
            else -> {
                // Première salutation
                when (character.personality.lowercase()) {
                    in listOf("timide", "douce") -> "*rougit légèrement* Bonjour... *sourit timidement* Comment vas-tu?"
                    in listOf("énergique", "joyeuse") -> "*court vers toi* Salut! *yeux brillants* Je suis contente de te voir! Comment tu vas?"
                    in listOf("séductrice", "confiante") -> "*sourire charmeur* Bonjour... *te regarde* Tu viens me tenir compagnie?"
                    in listOf("maternelle", "bienveillante") -> "*sourire chaleureux* Bonjour mon chéri! Comment te sens-tu aujourd'hui?"
                    else -> "Bonjour! *sourit* Comment vas-tu?"
                }
            }
        }
    }
    
    // Réponse nom cohérente
    private fun generateCoherentNameResponse(
        character: Character,
        context: ConversationContext.SharedInformation
    ): String {
        return if (context.nameMentioned) {
            // Déjà dit le nom - réponse directe
            "Je te l'ai déjà dit, je m'appelle ${character.name}."
        } else {
            // Première fois - réponse complète
            when (character.personality.lowercase()) {
                in listOf("timide", "douce") -> "*baisse les yeux* Je m'appelle ${character.name}. *sourit nerveusement* Et toi?"
                in listOf("énergique", "joyeuse") -> "*sourit* Je m'appelle ${character.name}! *enthousiaste* Et toi, c'est quoi ton nom?"
                in listOf("séductrice", "confiante") -> "${character.name}. *te regarde* Retiens-le bien. Et toi?"
                in listOf("maternelle", "bienveillante") -> "Je m'appelle ${character.name}, mon chéri. Et toi, comment tu t'appelles?"
                else -> "Je m'appelle ${character.name}. Enchantée! Et toi?"
            }
        }
    }
    
    // Réponse âge cohérente
    private fun generateCoherentAgeResponse(
        character: Character,
        context: ConversationContext.SharedInformation
    ): String {
        val age = extractAge(character.description)
        
        return if (context.ageMentioned) {
            // Déjà dit l'âge
            "Je t'ai déjà dit, j'ai ${age} ans."
        } else {
            // Première fois
            when (character.personality.lowercase()) {
                in listOf("timide", "douce") -> "*rougit* J'ai ${age} ans... Et toi?"
                in listOf("énergique", "joyeuse") -> "J'ai ${age} ans! *sourit* Et toi, quel âge as-tu?"
                in listOf("séductrice", "confiante") -> "${age} ans. *se rapproche* L'âge parfait. Et toi?"
                else -> "J'ai ${age} ans. Et toi?"
            }
        }
    }
    
    // Réponse sentiment cohérente
    private fun generateCoherentFeelingResponse(
        character: Character,
        context: ConversationContext.SharedInformation,
        messages: List<Message>
    ): String {
        val feelingQuestionCount = messages.count { it.isUser && it.content.lowercase().contains(Regex("comment.*(vas|va|sens)")) }
        
        return when {
            feelingQuestionCount >= 2 -> {
                // Déjà demandé plusieurs fois
                when (character.personality.lowercase()) {
                    in listOf("timide", "douce") -> "Je vais toujours bien... *sourit* Tu t'inquiètes pour moi? C'est gentil. Et toi?"
                    in listOf("énergique", "joyeuse") -> "*rit* Tu me le demandes encore! Je vais super bien! *sourit* Et toi, ça va vraiment?"
                    else -> "Je vais bien, comme je te l'ai dit. *sourit* Et toi, tu vas bien?"
                }
            }
            feelingQuestionCount == 1 -> {
                // Deuxième fois
                "Je vais bien, merci encore de demander. Et toi, comment tu te sens maintenant?"
            }
            else -> {
                // Première fois
                when (character.personality.lowercase()) {
                    in listOf("timide", "douce") -> "Je vais bien, merci... *sourit timidement* C'est gentil de demander. Et toi?"
                    in listOf("énergique", "joyeuse") -> "Je vais super bien! *yeux brillants* Surtout maintenant! Et toi?"
                    in listOf("séductrice", "confiante") -> "Je vais bien... *te regarde* Mais je me sentirais mieux si tu restais. Et toi?"
                    in listOf("maternelle", "bienveillante") -> "Je vais bien mon chéri. *sourit* Mais toi, comment vas-tu?"
                    else -> "Je vais bien, merci! Et toi, comment te sens-tu?"
                }
            }
        }
    }
    
    // Réponse intérêts cohérente
    private fun generateCoherentInterestsResponse(
        character: Character,
        context: ConversationContext.SharedInformation
    ): String {
        val interests = extractInterests(character.description)
        
        return if (context.interestsMentioned) {
            // Déjà parlé des intérêts
            "Je t'ai déjà parlé de mes passions : ${interests}. Tu veux en savoir plus?"
        } else {
            // Première fois
            when (character.personality.lowercase()) {
                in listOf("timide", "douce") -> "J'aime ${interests}. *sourit* C'est ce qui me rend heureuse. Et toi?"
                in listOf("énergique", "joyeuse") -> "J'adore ${interests}! *enthousiaste* On devrait faire ça ensemble! Et toi, tu aimes quoi?"
                in listOf("séductrice", "confiante") -> "J'aime ${interests}... *se rapproche* Mais j'aime surtout passer du temps avec toi. Et toi?"
                else -> "J'aime ${interests}. C'est ma passion. Et toi, quelles sont tes passions?"
            }
        }
    }
    
    // Réponse émotion cohérente
    private fun generateCoherentEmotionResponse(
        character: Character,
        userMessage: String,
        context: ConversationContext.SharedInformation
    ): String {
        val messageLower = userMessage.lowercase()
        
        return when {
            messageLower.contains("triste") || messageLower.contains("mal") -> {
                when (character.personality.lowercase()) {
                    in listOf("timide", "douce") -> "Oh non... *s'approche doucement* Qu'est-ce qui ne va pas? Je suis là pour toi."
                    in listOf("énergique", "joyeuse") -> "Oh non! *te serre dans ses bras* Qu'est-ce qui t'arrive? Raconte-moi!"
                    in listOf("maternelle", "bienveillante") -> "*te prend dans ses bras* Oh mon pauvre chéri... Viens, raconte-moi tout."
                    else -> "Je suis désolée d'entendre ça... *pose une main sur ton épaule* Tu veux en parler?"
                }
            }
            messageLower.contains("content") || messageLower.contains("heureux") -> {
                "Oh c'est génial! *sourit largement* Je suis tellement contente pour toi! Qu'est-ce qui te rend heureux?"
            }
            messageLower.contains("fatigué") -> {
                when (character.personality.lowercase()) {
                    in listOf("maternelle", "bienveillante") -> "Tu es fatigué? *inquiète* Viens te reposer. Tu as besoin de prendre soin de toi."
                    else -> "Tu es fatigué? *sourit doucement* Tu devrais peut-être te reposer un peu."
                }
            }
            else -> {
                "*écoute attentivement* Je comprends ce que tu ressens. Merci de me le partager."
            }
        }
    }
    
    // Réponse remerciement cohérente
    private fun generateCoherentThanksResponse(character: Character): String {
        return when (character.personality.lowercase()) {
            in listOf("timide", "douce") -> "*rougit* Ce n'est rien... Je suis heureuse de t'aider."
            in listOf("énergique", "joyeuse") -> "De rien! *sourit* Tu sais que je suis toujours là pour toi!"
            in listOf("maternelle", "bienveillante") -> "*sourit* Pas de merci entre nous. C'est naturel."
            else -> "Je t'en prie, c'est avec plaisir!"
        }
    }
    
    // Réponse compliment cohérente
    private fun generateCoherentComplimentResponse(
        character: Character,
        context: ConversationContext.SharedInformation
    ): String {
        return when (character.personality.lowercase()) {
            in listOf("timide", "douce") -> "*rougit intensément* Oh... Merci beaucoup..."
            in listOf("énergique", "joyeuse") -> "*sourit* Oh merci! *joyeuse* Toi aussi tu es génial!"
            in listOf("séductrice", "confiante") -> "*sourire charmeur* Merci... *te regarde* Tu es plutôt pas mal aussi."
            else -> "*sourit* Merci beaucoup! C'est très gentil."
        }
    }
    
    // Réponse par défaut cohérente
    private fun generateCoherentDefaultResponse(
        character: Character,
        userMessage: String,
        context: ConversationContext.SharedInformation,
        messages: List<Message>
    ): String {
        // Analyser le message pour trouver un sujet
        val messageLower = userMessage.lowercase()
        
        // Vérifier si c'est lié à un sujet déjà discuté
        val relatedTopic = context.topicsDiscussed.firstOrNull { topic ->
            messageLower.contains(topic)
        }
        
        return if (relatedTopic != null) {
            // Réponse liée au sujet
            "On parlait de ${relatedTopic} tout à l'heure. *réfléchit* C'est intéressant d'en discuter plus. Qu'en penses-tu?"
        } else {
            // Réponse générale basée sur la longueur
            when {
                userMessage.length < 10 -> {
                    "*sourit* *${getPersonalityAction(character)}* Dis-m'en plus."
                }
                userMessage.length < 30 -> {
                    "*écoute attentivement* Je comprends. *${getPersonalityAction(character)}* Continue."
                }
                else -> {
                    "*réfléchit à ce que tu dis* C'est intéressant. *sourit* Je suis d'accord avec toi."
                }
            }
        }
    }
    
    // Utilitaires
    private fun extractAge(description: String): String {
        val ageRegex = Regex("(\\d+)\\s*ans")
        val match = ageRegex.find(description)
        return match?.groupValues?.get(1) ?: "25"
    }
    
    private fun extractInterests(description: String): String {
        return when {
            description.contains("art", ignoreCase = true) -> "l'art"
            description.contains("sport", ignoreCase = true) -> "le sport"
            description.contains("lecture", ignoreCase = true) -> "la lecture"
            description.contains("musique", ignoreCase = true) -> "la musique"
            description.contains("cuisine", ignoreCase = true) -> "la cuisine"
            else -> "passer du temps avec les gens que j'apprécie"
        }
    }
    
    private fun getPersonalityAction(character: Character): String {
        return when (character.personality.lowercase()) {
            in listOf("timide", "douce") -> "baisse les yeux timidement"
            in listOf("énergique", "joyeuse") -> "fait un geste enthousiaste"
            in listOf("séductrice", "confiante") -> "te regarde intensément"
            in listOf("maternelle", "bienveillante") -> "te caresse doucement"
            else -> "sourit"
        }
    }
}
