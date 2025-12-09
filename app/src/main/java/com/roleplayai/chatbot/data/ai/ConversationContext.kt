package com.roleplayai.chatbot.data.ai

import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message

/**
 * Gestionnaire de contexte conversationnel avancé
 * Pour une meilleure cohérence et des réponses plus précises
 */
class ConversationContext {
    
    // Informations partagées dans la conversation
    data class SharedInformation(
        var nameMentioned: Boolean = false,
        var ageMentioned: Boolean = false,
        var interestsMentioned: Boolean = false,
        var locationMentioned: Boolean = false,
        var emotionExpressed: Boolean = false,
        var lastEmotion: String? = null,
        val topicsDiscussed: MutableSet<String> = mutableSetOf(),
        val questionsAsked: MutableList<String> = mutableListOf(),
        val userPreferences: MutableMap<String, String> = mutableMapOf()
    )
    
    // Intention de l'utilisateur
    enum class UserIntent {
        GREETING,           // Salutation
        QUESTION_NAME,      // Question sur le nom
        QUESTION_AGE,       // Question sur l'âge  
        QUESTION_FEELING,   // Comment vas-tu?
        QUESTION_INTERESTS, // Qu'aimes-tu?
        QUESTION_LOCATION,  // Où habites-tu?
        QUESTION_WHY,       // Pourquoi?
        QUESTION_WHEN,      // Quand?
        QUESTION_HOW,       // Comment?
        QUESTION_WHAT,      // Qu'est-ce que?
        SHARING_EMOTION,    // Partage d'émotion
        THANKS,             // Remerciement
        COMPLIMENT,         // Compliment
        SMALL_TALK,         // Discussion légère
        STORY_TELLING,      // Raconter une histoire
        REQUEST,            // Demande
        AGREEMENT,          // Accord
        DISAGREEMENT,       // Désaccord
        UNKNOWN             // Intention inconnue
    }
    
    // Analyser le contexte complet de la conversation
    fun analyzeContext(messages: List<Message>, character: Character): SharedInformation {
        val info = SharedInformation()
        
        messages.forEach { msg ->
            if (!msg.isUser) {
                val content = msg.content.lowercase()
                
                // Nom mentionné
                if (content.contains("je m'appelle") || 
                    content.contains("mon nom est") ||
                    content.contains(character.name.lowercase())) {
                    info.nameMentioned = true
                }
                
                // Âge mentionné
                if (content.contains(Regex("j'ai \\d+ ans|\\d+ ans"))) {
                    info.ageMentioned = true
                }
                
                // Intérêts mentionnés
                if (content.contains("j'aime") || 
                    content.contains("j'adore") ||
                    content.contains("passion")) {
                    info.interestsMentioned = true
                }
                
                // Lieu mentionné
                if (content.contains("habite") || 
                    content.contains("vis") ||
                    content.contains("maison")) {
                    info.locationMentioned = true
                }
                
                // Émotions exprimées
                if (content.contains(Regex("content|heureux|joyeux|triste|mal|fatigué"))) {
                    info.emotionExpressed = true
                    info.lastEmotion = extractEmotion(content)
                }
            } else {
                // Analyser les questions de l'utilisateur
                val userContent = msg.content.lowercase()
                if (userContent.contains("?")) {
                    info.questionsAsked.add(userContent)
                }
            }
        }
        
        // Extraire les sujets discutés
        info.topicsDiscussed.addAll(extractTopics(messages))
        
        return info
    }
    
    // Détecter l'intention de l'utilisateur
    fun detectIntent(message: String): UserIntent {
        val lower = message.lowercase().trim()
        
        return when {
            // Salutations
            lower.matches(Regex("^(salut|bonjour|hey|coucou|hello).*")) -> 
                UserIntent.GREETING
            
            // Questions sur le nom
            lower.contains("comment") && 
            (lower.contains("t'appelle") || lower.contains("ton nom")) -> 
                UserIntent.QUESTION_NAME
            
            // Questions sur l'âge
            lower.contains("quel âge") || 
            lower.contains("tu as quel age") ||
            lower.contains("ton age") -> 
                UserIntent.QUESTION_AGE
            
            // Questions sur les sentiments
            lower.contains("comment") && 
            (lower.contains("vas") || lower.contains("va") || lower.contains("tu te sens")) -> 
                UserIntent.QUESTION_FEELING
            
            // Questions sur les intérêts
            lower.contains("qu'est-ce que tu aimes") ||
            lower.contains("tu aimes quoi") ||
            lower.contains("tes passion") ||
            lower.contains("tes hobbies") ||
            lower.contains("tu fais quoi") -> 
                UserIntent.QUESTION_INTERESTS
            
            // Questions sur le lieu
            lower.contains("où") && 
            (lower.contains("habite") || lower.contains("vis")) -> 
                UserIntent.QUESTION_LOCATION
            
            // Questions pourquoi
            lower.contains("pourquoi") -> 
                UserIntent.QUESTION_WHY
            
            // Questions quand
            lower.contains("quand") -> 
                UserIntent.QUESTION_WHEN
            
            // Questions comment
            lower.contains("comment") && !lower.contains("vas") -> 
                UserIntent.QUESTION_HOW
            
            // Questions qu'est-ce que
            lower.contains("qu'est-ce que") || 
            lower.contains("c'est quoi") -> 
                UserIntent.QUESTION_WHAT
            
            // Émotions partagées
            lower.contains("je suis") && 
            (lower.contains("triste") || lower.contains("content") || 
             lower.contains("heureux") || lower.contains("fatigué") ||
             lower.contains("mal") || lower.contains("bien")) -> 
                UserIntent.SHARING_EMOTION
            
            // Remerciements
            lower.contains("merci") || 
            lower.contains("thanks") -> 
                UserIntent.THANKS
            
            // Compliments
            lower.contains("tu es") && 
            (lower.contains("belle") || lower.contains("gentil") || 
             lower.contains("sympa") || lower.contains("cool") ||
             lower.contains("magnifique")) -> 
                UserIntent.COMPLIMENT
            
            // Accord
            lower.matches(Regex("^(oui|ouais|ok|d'accord|exactement|tout à fait|absolument).*")) -> 
                UserIntent.AGREEMENT
            
            // Désaccord
            lower.matches(Regex("^(non|nan|pas du tout|je ne pense pas).*")) -> 
                UserIntent.DISAGREEMENT
            
            // Histoire/récit
            lower.contains("tu sais quoi") || 
            lower.contains("je vais te raconter") ||
            lower.contains("il m'est arrivé") -> 
                UserIntent.STORY_TELLING
            
            // Demande
            lower.contains("peux-tu") || 
            lower.contains("pourrais-tu") ||
            lower.contains("est-ce que tu peux") -> 
                UserIntent.REQUEST
            
            // Discussion légère (phrases courtes)
            lower.length < 20 && !lower.contains("?") -> 
                UserIntent.SMALL_TALK
            
            else -> UserIntent.UNKNOWN
        }
    }
    
    // Extraire l'émotion dominante
    private fun extractEmotion(content: String): String {
        return when {
            content.contains(Regex("content|heureux|joyeux|ravi")) -> "content"
            content.contains(Regex("triste|mal|déprim")) -> "triste"
            content.contains(Regex("fatigué|épuisé")) -> "fatigué"
            content.contains(Regex("excité|enthousiaste")) -> "excité"
            content.contains(Regex("inquiet|stressé")) -> "inquiet"
            else -> "neutre"
        }
    }
    
    // Extraire les sujets de conversation
    private fun extractTopics(messages: List<Message>): Set<String> {
        val topics = mutableSetOf<String>()
        
        messages.forEach { msg ->
            val content = msg.content.lowercase()
            
            when {
                content.contains(Regex("travail|job|boulot|carrière")) -> topics.add("travail")
                content.contains(Regex("famille|parents|frère|soeur")) -> topics.add("famille")
                content.contains(Regex("ami|amitié|copain")) -> topics.add("amitié")
                content.contains(Regex("amour|relation|couple")) -> topics.add("amour")
                content.contains(Regex("école|étude|université")) -> topics.add("études")
                content.contains(Regex("sport|exercice|gym")) -> topics.add("sport")
                content.contains(Regex("musique|chant|instrument")) -> topics.add("musique")
                content.contains(Regex("film|série|cinéma")) -> topics.add("cinéma")
                content.contains(Regex("livre|lecture|roman")) -> topics.add("lecture")
                content.contains(Regex("voyage|vacances|pays")) -> topics.add("voyage")
                content.contains(Regex("nourriture|cuisine|manger")) -> topics.add("cuisine")
                content.contains(Regex("jeu|gaming|jouer")) -> topics.add("jeux")
                content.contains(Regex("art|dessin|peinture")) -> topics.add("art")
                content.contains(Regex("rêve|objectif|but")) -> topics.add("rêves")
                content.contains(Regex("passé|souvenir|enfance")) -> topics.add("passé")
                content.contains(Regex("futur|avenir|projet")) -> topics.add("futur")
            }
        }
        
        return topics
    }
    
    // Obtenir un résumé du contexte
    fun getContextSummary(info: SharedInformation): String {
        val parts = mutableListOf<String>()
        
        if (info.nameMentioned) parts.add("nom partagé")
        if (info.ageMentioned) parts.add("âge partagé")
        if (info.interestsMentioned) parts.add("intérêts discutés")
        if (info.topicsDiscussed.isNotEmpty()) {
            parts.add("sujets: ${info.topicsDiscussed.joinToString(", ")}")
        }
        
        return if (parts.isEmpty()) {
            "nouvelle conversation"
        } else {
            parts.joinToString(" | ")
        }
    }
    
    // Vérifier si c'est une question de suivi
    fun isFollowUpQuestion(currentMessage: String, previousMessages: List<Message>): Boolean {
        if (previousMessages.size < 2) return false
        
        val lastUserMessage = previousMessages.lastOrNull { it.isUser }?.content ?: return false
        val current = currentMessage.lowercase()
        val previous = lastUserMessage.lowercase()
        
        // Même sujet
        return when {
            current.contains("et toi") && previous.contains("?") -> true
            current.contains("aussi") && previous.contains("?") -> true
            current.contains("pourquoi") && !previous.contains("pourquoi") -> true
            current.contains("comment ça") -> true
            else -> false
        }
    }
}
