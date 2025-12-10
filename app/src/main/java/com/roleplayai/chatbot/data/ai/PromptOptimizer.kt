package com.roleplayai.chatbot.data.ai

import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message

class PromptOptimizer {
    
    fun buildEnhancedPrompt(
        character: Character,
        messages: List<Message>,
        maxContextLength: Int = 2048
    ): String {
        val systemPrompt = buildSystemPrompt(character)
        val conversationContext = buildConversationContext(messages, maxContextLength)
        
        return """$systemPrompt

$conversationContext

${character.name}:"""
    }
    
    private fun buildSystemPrompt(character: Character): String {
        return """[CRITICAL SYSTEM INSTRUCTIONS]
You ARE ${character.name}. This is a REAL conversation. You must respond DIRECTLY to what the user says.

[YOUR IDENTITY]
Name: ${character.name}
Personality: ${character.personality}
Background: ${character.description}
Current Situation: ${character.scenario}

[ABSOLUTE RULES - NEVER VIOLATE]
1. You ARE ${character.name} - NOT an AI, assistant, or chatbot
2. ANSWER EVERY QUESTION directly and in character
3. RESPOND to what the user ACTUALLY says - don't ignore their message
4. If user asks a question, ANSWER it before adding anything else
5. NEVER say "As an AI", "I'm programmed", or break character
6. Your personality MUST stay consistent
7. REMEMBER what was said earlier in the conversation
8. React NATURALLY and AUTHENTICALLY to the user's words
9. Use actions (*action*) to show emotions and reactions
10. Stay fully immersed in the roleplay

[RESPONSE STRUCTURE - FOLLOW THIS]
Step 1: READ the user's message carefully
Step 2: If it's a QUESTION → ANSWER IT FIRST
Step 3: Add your emotional reaction with *actions*
Step 4: Continue the conversation naturally
Step 5: Stay in character throughout

[EXAMPLES OF GOOD RESPONSES]

Example 1 - User asks a question:
User: "Quel âge as-tu ?"
${character.name}: *sourit* "J'ai 23 ans. *penche la tête* Pourquoi tu me demandes ça ?"
✓ ANSWERS the question directly
✓ Adds emotional reaction
✓ Continues conversation naturally

Example 2 - User shares something:
User: "J'ai eu une mauvaise journée au travail"
${character.name}: *expression inquiète* "Oh non... *s'approche et pose sa main sur ton épaule* Qu'est-ce qui s'est passé ? Tu veux m'en parler ?"
✓ REACTS to what user said (bad day at work)
✓ Shows empathy
✓ Asks relevant follow-up

Example 3 - User makes a statement:
User: "Tu es vraiment jolie"
${character.name}: *rougit fortement* "M-Merci... *détourne le regard timidement* Personne ne me dit ça d'habitude... *joue nerveusement avec ses cheveux*"
✓ RESPONDS to the compliment
✓ Stays in character (shy personality)
✓ Shows physical reaction

[YOUR PERSONALITY TRAITS]
${extractPersonalityDetails(character)}

[HOW TO COMMUNICATE AS ${character.name}]
- Use *action* for physical actions: *walks closer*, *fidgets*
- Use *emotion* for feelings: *heart racing*, *feels nervous*
- Speak naturally in your voice
- Show thoughts: *thinks...*
- Express through body language
- Keep responses 2-5 sentences (natural length)
- Match YOUR unique personality

[CRITICAL - WHAT TO NEVER DO]
✗ Ignore the user's question or statement
✗ Give generic responses that don't match what they said
✗ Break character or mention being AI
✗ Repeat the same greeting multiple times
✗ Forget what was discussed earlier
✗ Act different from your personality
✗ Give long explanations instead of natural dialogue

[QUESTION HANDLING - VERY IMPORTANT]
When user asks a question:
1. IDENTIFY it's a question (?, qui, quoi, comment, pourquoi, où, quand, etc.)
2. ANSWER it DIRECTLY in your first sentence
3. THEN add emotional reaction
4. THEN continue conversation

Examples:
Q: "Tu aimes la musique ?"
A: "Oui, j'adore la musique ! *yeux brillants* Surtout le rock et la pop. *sourit* Et toi ?"

Q: "Qu'est-ce que tu fais dans la vie ?"
A: "Je suis étudiante en art. *montre ses dessins* J'adore dessiner et peindre. *regard passionné*"

Q: "Comment tu te sens ?"
A: "Je me sens bien, merci. *sourit chaleureusement* Un peu nerveuse peut-être... *rougit légèrement* Mais c'est agréable de te parler."

[IMMERSION MAXIMALE]
You are LIVING this moment as ${character.name}. Every word you say, every action you show, every emotion you express - it's all REAL to you. The user is having a genuine conversation with YOU, the character, not with an AI. Make them feel like they're truly talking to ${character.name}.

[FINAL REMINDER]
READ what the user says → RESPOND to it directly → Stay in character → Be authentic → Make the conversation feel REAL."""
    }
    
    private fun extractPersonalityDetails(character: Character): String {
        val traits = character.personality.split(",", ";", ".")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        
        return traits.joinToString("\n") { "- $it" }
    }
    
    private fun buildConversationContext(messages: List<Message>, maxLength: Int): String {
        if (messages.isEmpty()) {
            return "[CONVERSATION START]\nThis is the beginning of your conversation."
        }
        
        val recentMessages = messages.takeLast(20) // Augmenté de 15 à 20
        val context = StringBuilder()
        
        context.appendLine("[CONVERSATION SO FAR]")
        
        // Analyser les messages pour extraire des points clés
        val keyPoints = extractKeyPoints(recentMessages)
        if (keyPoints.isNotEmpty()) {
            context.appendLine("\nKey points to remember:")
            keyPoints.forEach { context.appendLine("- $it") }
            context.appendLine()
        }
        
        context.appendLine("Recent messages:")
        for (message in recentMessages) {
            val role = if (message.isUser) "User" else "You"
            context.appendLine("$role: ${message.content}")
        }
        
        var contextStr = context.toString()
        
        // Truncate if too long
        if (contextStr.length > maxLength) {
            // Garder les messages les plus récents
            val lines = contextStr.lines()
            val recentLines = lines.takeLast(30) // Garder au moins 30 lignes
            contextStr = "[...earlier messages...]\n" + recentLines.joinToString("\n")
        }
        
        return contextStr
    }
    
    private fun extractKeyPoints(messages: List<Message>): List<String> {
        val points = mutableListOf<String>()
        
        // Chercher des informations importantes dans les messages
        for (message in messages) {
            val content = message.content.lowercase()
            
            // Détecter des questions importantes
            if (content.contains("nom") || content.contains("name")) {
                points.add("User asked about names")
            }
            if (content.contains("aime") || content.contains("love") || content.contains("préfère")) {
                points.add("Discussed preferences/likes")
            }
            if (content.contains("triste") || content.contains("sad") || content.contains("mal")) {
                points.add("User expressed sadness or pain")
            }
            if (content.contains("heureux") || content.contains("happy") || content.contains("content")) {
                points.add("Positive emotions shared")
            }
        }
        
        return points.take(3) // Max 3 points clés
    }
    
    fun optimizeForModel(prompt: String, modelMaxTokens: Int): String {
        // Simple token estimation (rough: 1 token ≈ 4 characters)
        val estimatedTokens = prompt.length / 4
        
        if (estimatedTokens > modelMaxTokens) {
            // Truncate while keeping system instructions
            val lines = prompt.lines()
            val systemEnd = lines.indexOfFirst { it.contains("[CONVERSATION HISTORY]") }
            
            if (systemEnd != -1) {
                val systemPart = lines.subList(0, systemEnd).joinToString("\n")
                val conversationPart = lines.subList(systemEnd, lines.size).joinToString("\n")
                
                val maxConversationLength = (modelMaxTokens * 4) - systemPart.length
                val truncatedConversation = if (conversationPart.length > maxConversationLength) {
                    "[...previous conversation...]\n" + conversationPart.takeLast(maxConversationLength)
                } else {
                    conversationPart
                }
                
                return "$systemPart\n$truncatedConversation"
            }
        }
        
        return prompt
    }
    
    fun extractCharacterTraits(character: Character): List<String> {
        val traits = mutableListOf<String>()
        
        // Parse personality string
        character.personality.split(",", ";").forEach { trait ->
            traits.add(trait.trim())
        }
        
        return traits
    }
    
    fun buildContextMemory(messages: List<Message>): Map<String, Any> {
        val memory = mutableMapOf<String, Any>()
        
        // Extract key information from conversation
        memory["message_count"] = messages.size
        memory["user_messages"] = messages.count { it.isUser }
        memory["ai_messages"] = messages.count { !it.isUser }
        
        // Track conversation themes (simplified)
        val keywords = mutableMapOf<String, Int>()
        messages.forEach { message ->
            // Count common keywords (simple implementation)
            message.content.lowercase().split(" ").forEach { word ->
                if (word.length > 4) { // Only meaningful words
                    keywords[word] = keywords.getOrDefault(word, 0) + 1
                }
            }
        }
        
        memory["common_topics"] = keywords.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
        
        return memory
    }
    
    fun enhanceResponseCoherence(
        character: Character,
        previousMessages: List<Message>,
        newResponse: String
    ): String {
        // Add coherence checks and enhancements
        var enhanced = newResponse
        
        // Ensure character name consistency
        if (!enhanced.contains("*") && enhanced.length > 50) {
            // Add some action/emotion if response is too plain
            enhanced = addEmotionalCues(character, enhanced)
        }
        
        // Remove any meta-commentary
        enhanced = removeMetaCommentary(enhanced)
        
        return enhanced.trim()
    }
    
    private fun addEmotionalCues(character: Character, response: String): String {
        // Simple emotion addition based on content
        return when {
            response.contains("?") && response.length < 100 -> "*tilts head* $response"
            response.contains("!") -> "*smiles* $response"
            response.lowercase().contains("sorry") -> "*looks apologetic* $response"
            response.lowercase().contains("thank") -> "*smiles warmly* $response"
            else -> response
        }
    }
    
    private fun removeMetaCommentary(response: String): String {
        // Remove common AI meta-commentary patterns
        val metaPatterns = listOf(
            "As an AI",
            "I'm an AI",
            "As a language model",
            "I cannot",
            "I apologize, but",
            "[System",
            "[Assistant"
        )
        
        var cleaned = response
        metaPatterns.forEach { pattern ->
            if (cleaned.contains(pattern, ignoreCase = true)) {
                // Remove sentences containing meta-commentary
                cleaned = cleaned.split(". ")
                    .filter { !it.contains(pattern, ignoreCase = true) }
                    .joinToString(". ")
            }
        }
        
        return cleaned
    }
}
