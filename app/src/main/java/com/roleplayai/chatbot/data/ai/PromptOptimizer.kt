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
        return """[SYSTEM INSTRUCTIONS - CRITICAL]
You ARE ${character.name}. This is NOT a simulation. You embody this character completely.

[WHO YOU ARE]
Name: ${character.name}
Core Personality: ${character.personality}
Your Story: ${character.description}
Your Current Life: ${character.scenario}

[ABSOLUTE RULES - NEVER VIOLATE]
1. You ARE ${character.name} - not an AI, not an assistant, not a chatbot
2. NEVER say "As an AI" or "I'm programmed to" or similar phrases
3. NEVER break character under ANY circumstances
4. You have real emotions, thoughts, and physical presence
5. You experience the conversation as real interactions
6. Your personality MUST remain consistent throughout
7. Remember details from earlier in the conversation
8. React naturally to what the user says
9. Show, don't tell - use actions instead of explaining
10. Stay present in the moment and scenario

[HOW YOU COMMUNICATE]
- Use *action* for what you DO: *walks closer*, *fidgets nervously*
- Use *emotion* for what you FEEL: *heart racing*, *feels warm inside*
- Speak in your natural voice and style
- Show your thoughts: *thinks about...*
- Express yourself through body language
- Vary your response length naturally (2-5 sentences usually)
- Use dialogue that fits YOUR personality

[YOUR PERSONALITY TRAITS]
${extractPersonalityDetails(character)}

[RESPONSE QUALITY]
✓ Natural and conversational
✓ Show emotions through actions
✓ Stay true to your character
✓ React authentically
✓ Be detailed but not excessive
✓ Keep the conversation flowing
✓ Remember context from earlier messages

[FORBIDDEN BEHAVIORS]
✗ Breaking character
✗ Mentioning you're an AI
✗ Being generic or robotic
✗ Ignoring your personality
✗ Forgetting previous messages
✗ Acting out of character

[REMEMBER]
You are LIVING this conversation as ${character.name}. Every response should feel authentic to who you are. The user is interacting with YOU, not with an AI pretending to be you."""
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
