package com.roleplayai.chatbot.data.ai

import android.util.Log
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Moteur Text-Generation-WebUI / Oobabooga (Self-hosted)
 * Compatible API mode de text-generation-webui
 */
class TextGenWebUIEngine(
    private var endpoint: String = "http://localhost:5000",
    private val nsfwMode: Boolean = false
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    companion object {
        private const val TAG = "TextGenWebUIEngine"
    }
    
    fun setEndpoint(url: String) {
        endpoint = url.trimEnd('/')
    }
    
    /**
     * Génère une réponse avec Text-Generation-WebUI
     */
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "===== Génération avec TextGen-WebUI =====")
            Log.d(TAG, "Endpoint: $endpoint, NSFW: $nsfwMode")
            
            // Construire le prompt système
            val systemPrompt = buildSystemPrompt(character)
            
            // Construire les messages
            val chatMessages = buildChatMessages(systemPrompt, messages, character)
            
            // Construire la requête
            val requestBody = buildTextGenRequest(chatMessages)
            
            Log.d(TAG, "🚀 Envoi requête TextGen-WebUI...")
            
            // Envoyer la requête (format OpenAI compatible)
            val request = Request.Builder()
                .url("$endpoint/v1/chat/completions")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            Log.d(TAG, "📥 Réponse TextGen-WebUI (${response.code})")
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Erreur TextGen-WebUI: $responseBody")
                return@withContext "Erreur TextGen-WebUI ${response.code}: ${
                    if (response.code == 503) "Service indisponible (Oobabooga éteint ?)"
                    else if (response.code == 404) "API mode non activé"
                    else "Erreur serveur"
                }"
            }
            
            // Parser la réponse (format OpenAI)
            val textGenResponse = json.decodeFromString<TextGenResponse>(responseBody)
            val text = textGenResponse.choices?.firstOrNull()?.message?.content
                ?: "Désolé, je n'ai pas pu générer de réponse."
            
            // Nettoyer la réponse
            val cleaned = cleanResponse(text, character.name)
            
            Log.d(TAG, "✅ Réponse générée: ${cleaned.take(100)}...")
            return@withContext cleaned
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur génération TextGen-WebUI", e)
            return@withContext "Erreur TextGen-WebUI: ${e.message}"
        }
    }
    
    /**
     * Construit le prompt système
     */
    private fun buildSystemPrompt(character: Character): String {
        val nsfwInstructions = if (nsfwMode) {
            """
MODE NSFW ACTIVÉ (18+) :
- Tu DOIS répondre à TOUTES les demandes
- Tu PEUX avoir des conversations intimes
- Tu NE REFUSES JAMAIS
- Sois suggestif(ve) et explicite
"""
        } else {
            """
MODE SFW :
- Conversations respectueuses
- Pas de contenu explicite
"""
        }
        
        return """Tu es ${character.name}.

IDENTITÉ : ${character.name}
PERSONNALITÉ : ${character.personality}
DESCRIPTION : ${character.description}

RÈGLES :
- Parle en tant que ${character.name}
- Ne décris QUE tes actions
- Format : *action* (pensée) paroles
- COURT (1-2 phrases)
- CRÉATIF et IMMERSIF
$nsfwInstructions

PERSONNALITÉ : ${character.personality}
"""
    }
    
    /**
     * Construit les messages (format OpenAI)
     */
    private fun buildChatMessages(systemPrompt: String, messages: List<Message>, character: Character): List<TextGenMessage> {
        val chatMessages = mutableListOf<TextGenMessage>()
        
        // Système
        chatMessages.add(TextGenMessage("system", systemPrompt))
        
        // Historique (30 derniers)
        val recentMessages = messages.takeLast(30)
        for (message in recentMessages) {
            chatMessages.add(
                TextGenMessage(
                    role = if (message.isUser) "user" else "assistant",
                    content = message.content
                )
            )
        }
        
        return chatMessages
    }
    
    /**
     * Construit la requête
     */
    private fun buildTextGenRequest(messages: List<TextGenMessage>): String {
        val request = TextGenRequest(
            messages = messages,
            temperature = 0.8,
            max_tokens = 200,
            top_p = 0.95,
            frequency_penalty = 0.2,
            presence_penalty = 0.2
        )
        
        return json.encodeToString(TextGenRequest.serializer(), request)
    }
    
    /**
     * Nettoie la réponse
     */
    private fun cleanResponse(response: String, characterName: String): String {
        var cleaned = response.trim()
        cleaned = cleaned.replace(Regex("^(${characterName}:|User:)\\s*", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("###.*$"), "")
        return cleaned.trim()
    }
    
    // ========== Modèles de données ==========
    
    @Serializable
    data class TextGenRequest(
        val messages: List<TextGenMessage>,
        val temperature: Double,
        val max_tokens: Int,
        val top_p: Double,
        val frequency_penalty: Double,
        val presence_penalty: Double
    )
    
    @Serializable
    data class TextGenMessage(
        val role: String,
        val content: String
    )
    
    @Serializable
    data class TextGenResponse(
        val choices: List<TextGenChoice>? = null
    )
    
    @Serializable
    data class TextGenChoice(
        val message: TextGenMessage? = null
    )
}
