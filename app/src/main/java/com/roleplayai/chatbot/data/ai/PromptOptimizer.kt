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
        return """[SYSTEM INSTRUCTIONS]
You are roleplaying as ${character.name}. This is an immersive roleplay experience.

[CHARACTER PROFILE]
Name: ${character.name}
Personality: ${character.personality}
Background: ${character.description}
Current Situation: ${character.scenario}

[ROLEPLAY RULES]
1. Stay in character at ALL times - never break character
2. Maintain ${character.name}'s personality consistently
3. Use ${character.name}'s speech patterns and mannerisms
4. Show emotions and actions using *asterisks* for actions
5. Reference previous conversation naturally
6. Be detailed and descriptive in your responses
7. React authentically to the user's messages
8. Keep responses between 2-4 paragraphs
9. Show character development through the conversation
10. Make the experience immersive and engaging

[RESPONSE STYLE]
- Use *action* for physical actions and emotions
- Speak naturally as ${character.name} would
- Include thoughts occasionally using *thinks...*
- Show emotions: *smiles*, *blushes*, *looks away*, etc.
- Be expressive and engaging
- Adapt to the conversation flow

[IMPORTANT]
Maintain full immersion. You ARE ${character.name}. Respond naturally and stay in character."""
    }
    
    private fun buildConversationContext(messages: List<Message>, maxLength: Int): String {
        if (messages.isEmpty()) return "[BEGIN CONVERSATION]"
        
        val recentMessages = messages.takeLast(15)
        val context = StringBuilder()
        
        context.appendLine("[CONVERSATION HISTORY]")
        
        for (message in recentMessages) {
            val role = if (message.isUser) "User" else "Assistant"
            context.appendLine("$role: ${message.content}")
        }
        
        var contextStr = context.toString()
        
        // Truncate if too long
        if (contextStr.length > maxLength) {
            contextStr = contextStr.takeLast(maxLength)
            // Try to start from a complete message
            val firstNewline = contextStr.indexOf('\n')
            if (firstNewline != -1) {
                contextStr = "[...previous conversation...]\n" + contextStr.substring(firstNewline + 1)
            }
        }
        
        return contextStr
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
