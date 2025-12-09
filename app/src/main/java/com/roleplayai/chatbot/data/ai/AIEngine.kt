package com.roleplayai.chatbot.data.ai

import android.content.Context
import android.util.Log
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AIEngine(private val context: Context) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // API endpoints - these can be configured
    private var apiEndpoint = "https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.2"
    private var apiKey = "" // Will use HuggingFace free tier
    
    // Alternative: Local API endpoint if user runs a local LLM server
    private var useLocalAPI = false
    private var localAPIEndpoint = "http://localhost:8080/v1/chat/completions"
    
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>
    ): String = withContext(Dispatchers.IO) {
        try {
            if (useLocalAPI) {
                return@withContext generateWithLocalAPI(character, messages)
            } else {
                return@withContext generateWithHuggingFace(character, messages)
            }
        } catch (e: Exception) {
            Log.e("AIEngine", "Error generating response", e)
            return@withContext generateFallbackResponse(character)
        }
    }
    
    private fun generateWithHuggingFace(
        character: Character,
        messages: List<Message>
    ): String {
        val prompt = buildPrompt(character, messages)
        
        val jsonObject = JSONObject().apply {
            put("inputs", prompt)
            put("parameters", JSONObject().apply {
                put("max_new_tokens", 500)
                put("temperature", 0.9)
                put("top_p", 0.95)
                put("return_full_text", false)
            })
        }
        
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(apiEndpoint)
            .post(requestBody)
            .apply {
                if (apiKey.isNotEmpty()) {
                    addHeader("Authorization", "Bearer $apiKey")
                }
            }
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e("AIEngine", "HuggingFace API error: ${response.code}")
                return generateFallbackResponse(character)
            }
            
            val responseBody = response.body?.string() ?: return generateFallbackResponse(character)
            
            return try {
                val jsonArray = JSONArray(responseBody)
                if (jsonArray.length() > 0) {
                    val firstResult = jsonArray.getJSONObject(0)
                    val generatedText = firstResult.getString("generated_text")
                    cleanResponse(generatedText)
                } else {
                    generateFallbackResponse(character)
                }
            } catch (e: Exception) {
                // Might be a single object instead of array
                try {
                    val jsonObject = JSONObject(responseBody)
                    val generatedText = jsonObject.getString("generated_text")
                    cleanResponse(generatedText)
                } catch (e2: Exception) {
                    Log.e("AIEngine", "Error parsing response", e2)
                    generateFallbackResponse(character)
                }
            }
        }
    }
    
    private fun generateWithLocalAPI(
        character: Character,
        messages: List<Message>
    ): String {
        val chatMessages = buildChatMessages(character, messages)
        
        val jsonObject = JSONObject().apply {
            put("model", "local-model")
            put("messages", JSONArray().apply {
                chatMessages.forEach { msg ->
                    put(JSONObject().apply {
                        put("role", msg.first)
                        put("content", msg.second)
                    })
                }
            })
            put("temperature", 0.9)
            put("max_tokens", 500)
        }
        
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(localAPIEndpoint)
            .post(requestBody)
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e("AIEngine", "Local API error: ${response.code}")
                return generateFallbackResponse(character)
            }
            
            val responseBody = response.body?.string() ?: return generateFallbackResponse(character)
            
            return try {
                val jsonObject = JSONObject(responseBody)
                val choices = jsonObject.getJSONArray("choices")
                if (choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    val message = firstChoice.getJSONObject("message")
                    val content = message.getString("content")
                    cleanResponse(content)
                } else {
                    generateFallbackResponse(character)
                }
            } catch (e: Exception) {
                Log.e("AIEngine", "Error parsing local API response", e)
                generateFallbackResponse(character)
            }
        }
    }
    
    private fun buildPrompt(character: Character, messages: List<Message>): String {
        val systemPrompt = """
            Tu es ${character.name}.
            Description: ${character.description}
            Personnalité: ${character.personality}
            Scénario: ${character.scenario}
            
            Tu dois répondre en restant dans le personnage à tout moment.
            Sois naturel, engageant et cohérent avec ta personnalité.
            N'utilise pas d'émojis excessifs, reste authentique.
        """.trimIndent()
        
        val conversationHistory = messages.takeLast(10).joinToString("\n") { msg ->
            if (msg.isUser) {
                "Utilisateur: ${msg.content}"
            } else {
                "${character.name}: ${msg.content}"
            }
        }
        
        return """
            $systemPrompt
            
            Conversation:
            $conversationHistory
            
            ${character.name}:
        """.trimIndent()
    }
    
    private fun buildChatMessages(
        character: Character,
        messages: List<Message>
    ): List<Pair<String, String>> {
        val chatMessages = mutableListOf<Pair<String, String>>()
        
        // System message
        val systemPrompt = """
            Tu es ${character.name}.
            Description: ${character.description}
            Personnalité: ${character.personality}
            Scénario: ${character.scenario}
            
            Tu dois répondre en restant dans le personnage à tout moment.
            Sois naturel, engageant et cohérent avec ta personnalité.
        """.trimIndent()
        
        chatMessages.add("system" to systemPrompt)
        
        // Add conversation history
        messages.takeLast(10).forEach { msg ->
            val role = if (msg.isUser) "user" else "assistant"
            chatMessages.add(role to msg.content)
        }
        
        return chatMessages
    }
    
    private fun cleanResponse(response: String): String {
        return response
            .trim()
            .replace(Regex("^(${"|Assistant:|AI:"})", RegexOption.IGNORE_CASE), "")
            .trim()
    }
    
    private fun generateFallbackResponse(character: Character): String {
        val fallbackResponses = listOf(
            "Je réfléchis à ce que tu viens de dire...",
            "C'est intéressant, continue...",
            "Hmm, dis-m'en plus.",
            "Je t'écoute attentivement.",
            "Et toi, qu'en penses-tu?",
            "C'est une perspective intéressante.",
            "Je comprends ce que tu veux dire.",
            "Raconte-moi en plus.",
            "*sourit* Continue, je suis tout ouïe.",
            "Fascinant... et ensuite?"
        )
        return fallbackResponses.random()
    }
    
    fun setAPIEndpoint(endpoint: String) {
        this.apiEndpoint = endpoint
    }
    
    fun setAPIKey(key: String) {
        this.apiKey = key
    }
    
    fun setUseLocalAPI(use: Boolean, endpoint: String = "http://localhost:8080/v1/chat/completions") {
        this.useLocalAPI = use
        this.localAPIEndpoint = endpoint
    }
}
