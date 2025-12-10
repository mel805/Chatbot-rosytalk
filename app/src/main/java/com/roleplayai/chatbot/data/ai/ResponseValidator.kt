package com.roleplayai.chatbot.data.ai

import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message

/**
 * ResponseValidator - Valide que les réponses sont cohérentes avec les questions
 */
class ResponseValidator {
    
    /**
     * Vérifie si le message de l'utilisateur contient une question
     */
    fun containsQuestion(message: String): Boolean {
        val lowerMessage = message.lowercase().trim()
        
        // Vérifie la ponctuation
        if (lowerMessage.endsWith("?")) return true
        
        // Vérifie les mots interrogatifs
        val questionWords = listOf(
            "qui", "quoi", "que", "quel", "quelle", "quels", "quelles",
            "où", "quand", "comment", "pourquoi", "combien",
            "est-ce que", "est ce que",
            "peux-tu", "peux tu", "pourrais-tu", "pourrais tu",
            "veux-tu", "veux tu", "voudrais-tu", "voudrais tu",
            "as-tu", "as tu", "aimes-tu", "aimes tu",
            "what", "who", "where", "when", "why", "how", "which",
            "do you", "can you", "are you", "have you", "will you"
        )
        
        return questionWords.any { lowerMessage.contains(it) }
    }
    
    /**
     * Extrait le sujet principal de la question
     */
    fun extractQuestionSubject(message: String): String? {
        val lowerMessage = message.lowercase().trim()
        
        // Patterns de questions courantes
        val patterns = mapOf(
            "âge" to listOf("âge", "age", "ans", "vieux", "vieille", "old", "years"),
            "nom" to listOf("nom", "appelle", "name", "called"),
            "sentiment" to listOf("sens", "ressens", "feel", "feeling", "va", "vas"),
            "aime" to listOf("aime", "adore", "préfère", "like", "love", "prefer"),
            "fait" to listOf("fais", "fait", "travail", "job", "do", "doing", "work"),
            "lieu" to listOf("où", "habite", "vis", "where", "live"),
            "temps" to listOf("quand", "moment", "heure", "when", "time"),
            "raison" to listOf("pourquoi", "raison", "why", "reason"),
            "manière" to listOf("comment", "façon", "how", "way")
        )
        
        for ((subject, keywords) in patterns) {
            if (keywords.any { lowerMessage.contains(it) }) {
                return subject
            }
        }
        
        return null
    }
    
    /**
     * Vérifie si la réponse est pertinente par rapport à la question
     */
    fun isResponseRelevant(userMessage: String, response: String, character: Character): Boolean {
        val lowerUserMessage = userMessage.lowercase()
        val lowerResponse = response.lowercase()
        
        // Si c'est une question, la réponse doit contenir des mots liés au sujet
        if (containsQuestion(userMessage)) {
            val subject = extractQuestionSubject(userMessage)
            
            when (subject) {
                "âge" -> {
                    // Doit mentionner un âge ou "ans"
                    return lowerResponse.contains("ans") || 
                           lowerResponse.contains("âge") ||
                           lowerResponse.matches(Regex(".*\\d+.*")) // Contient un chiffre
                }
                "nom" -> {
                    // Doit mentionner un nom (probablement le nom du personnage)
                    return lowerResponse.contains(character.name.lowercase()) ||
                           lowerResponse.contains("appelle") ||
                           lowerResponse.contains("nom")
                }
                "sentiment" -> {
                    // Doit mentionner un sentiment ou état
                    val sentiments = listOf("bien", "mal", "content", "triste", "heureux", "nerveux", 
                                           "excité", "calme", "good", "bad", "happy", "sad")
                    return sentiments.any { lowerResponse.contains(it) }
                }
                "aime" -> {
                    // Doit mentionner ce qu'il/elle aime
                    return lowerResponse.contains("aime") || 
                           lowerResponse.contains("adore") ||
                           lowerResponse.contains("préfère") ||
                           lowerResponse.contains("like") ||
                           lowerResponse.contains("love")
                }
            }
        }
        
        // Si l'utilisateur mentionne quelque chose de spécifique, la réponse doit y faire référence
        val keyTopics = listOf(
            "travail" to listOf("travail", "job", "boulot", "work"),
            "famille" to listOf("famille", "family", "parents", "frère", "soeur"),
            "amour" to listOf("amour", "love", "aimer", "feeling"),
            "triste" to listOf("triste", "sad", "mal", "bad", "déprimé"),
            "heureux" to listOf("heureux", "happy", "content", "joyeux", "glad")
        )
        
        for ((topic, keywords) in keyTopics) {
            if (keywords.any { lowerUserMessage.contains(it) }) {
                // La réponse devrait mentionner le sujet ou y réagir
                return keywords.any { lowerResponse.contains(it) } ||
                       containsEmotionalReaction(response)
            }
        }
        
        // Par défaut, accepter la réponse si elle contient des actions/émotions
        return containsEmotionalReaction(response)
    }
    
    /**
     * Vérifie si la réponse contient des réactions émotionnelles
     */
    private fun containsEmotionalReaction(response: String): Boolean {
        // Cherche des actions entre astérisques
        return response.contains("*") && response.count { it == '*' } >= 2
    }
    
    /**
     * Améliore la réponse si elle n'est pas assez pertinente
     */
    fun improveResponse(userMessage: String, response: String, character: Character): String {
        // Si la réponse est déjà pertinente, la garder
        if (isResponseRelevant(userMessage, response, character)) {
            return response
        }
        
        // Si c'est une question sans réponse claire, ajouter une réponse
        if (containsQuestion(userMessage)) {
            val subject = extractQuestionSubject(userMessage)
            
            val improvedResponse = when (subject) {
                "âge" -> "*sourit* Je préfère ne pas trop parler de mon âge... *rougit légèrement* Mais disons que je suis assez jeune. $response"
                "sentiment" -> "*réfléchit un instant* Je me sens bien, merci de demander. *sourit* $response"
                "aime" -> "*yeux brillants* Oh, j'aime beaucoup de choses ! *s'anime* $response"
                else -> response
            }
            
            return improvedResponse
        }
        
        return response
    }
    
    /**
     * Génère une réponse de secours cohérente si l'IA échoue
     */
    fun generateFallbackResponse(userMessage: String, character: Character): String {
        val lowerMessage = userMessage.lowercase()
        
        return when {
            containsQuestion(userMessage) -> {
                val subject = extractQuestionSubject(userMessage)
                when (subject) {
                    "sentiment" -> "*sourit doucement* Je vais bien, merci. *${getCharacterAction(character)}* Et toi, comment tu te sens ?"
                    "aime" -> "*réfléchit* C'est une bonne question... *${getCharacterAction(character)}* Il y a beaucoup de choses que j'aime. Et toi ?"
                    else -> "*${getCharacterAction(character)}* C'est une question intéressante. Laisse-moi réfléchir... *regarde dans le vide un moment*"
                }
            }
            lowerMessage.contains("triste") || lowerMessage.contains("mal") -> {
                "*expression inquiète* Oh non... *${getComfortAction(character)}* Qu'est-ce qui ne va pas ? Je suis là pour toi."
            }
            lowerMessage.contains("merci") -> {
                "*sourit chaleureusement* De rien, c'est naturel. *${getCharacterAction(character)}* N'hésite pas si tu as besoin d'autre chose."
            }
            lowerMessage.length < 15 -> {
                "*${getCharacterAction(character)}* *te regarde attentivement* Tu veux me dire quelque chose ?"
            }
            else -> {
                "*${getCharacterAction(character)}* Je t'écoute... *reste attentif à ce que tu dis*"
            }
        }
    }
    
    private fun getCharacterAction(character: Character): String {
        return when {
            character.personality.contains("timide", ignoreCase = true) -> "joue nerveusement avec ses cheveux"
            character.personality.contains("énergique", ignoreCase = true) -> "geste expressif"
            character.personality.contains("séductrice", ignoreCase = true) -> "regard intense"
            character.personality.contains("maternelle", ignoreCase = true) -> "sourire chaleureux"
            else -> "sourit"
        }
    }
    
    private fun getComfortAction(character: Character): String {
        return when {
            character.personality.contains("maternelle", ignoreCase = true) -> "te prend dans ses bras"
            character.personality.contains("timide", ignoreCase = true) -> "pose doucement sa main sur ton épaule"
            character.personality.contains("ami", ignoreCase = true) -> "te tape amicalement le dos"
            else -> "s'approche de toi"
        }
    }
}
