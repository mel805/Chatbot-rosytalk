package com.roleplayai.chatbot.data.ai

import android.util.Log
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * OpenRouter AI Engine - API avec modèles NSFW non censurés
 * 
 * OpenRouter offre :
 * - Accès à des modèles non censurés (Nous Hermes, MythoMax, etc.)
 * - Support explicite du contenu NSFW
 * - API compatible OpenAI
 * - Prix abordable (pay-as-you-go)
 * 
 * Idéal pour le roleplay NSFW sans restrictions.
 */
class OpenRouterEngine(
    private val apiKey: String = "",
    private val model: String = "nousresearch/nous-hermes-2-mixtral-8x7b-dpo",
    private val nsfwMode: Boolean = false
) {
    
    companion object {
        private const val TAG = "OpenRouterEngine"
        private const val OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"
        
        // Modèles OpenRouter recommandés pour NSFW
        val AVAILABLE_MODELS = listOf(
            OpenRouterModel(
                id = "nousresearch/nous-hermes-2-mixtral-8x7b-dpo",
                name = "Nous Hermes 2 Mixtral 8x7B",
                description = "Excellent pour NSFW, non censuré",
                contextLength = 32768,
                nsfwCapable = true,
                uncensored = true,
                recommended = true,
                pricePerMillion = 0.30
            ),
            OpenRouterModel(
                id = "gryphe/mythomax-l2-13b",
                name = "MythoMax L2 13B",
                description = "Spécialisé roleplay NSFW",
                contextLength = 8192,
                nsfwCapable = true,
                uncensored = true,
                recommended = true,
                pricePerMillion = 0.15
            ),
            OpenRouterModel(
                id = "koboldai/psyfighter-13b-2",
                name = "Psyfighter 13B",
                description = "Creative writing NSFW",
                contextLength = 4096,
                nsfwCapable = true,
                uncensored = true,
                recommended = true,
                pricePerMillion = 0.10
            ),
            OpenRouterModel(
                id = "undi95/toppy-m-7b",
                name = "Toppy M 7B",
                description = "Rapide et uncensored",
                contextLength = 4096,
                nsfwCapable = true,
                uncensored = true,
                recommended = false,
                pricePerMillion = 0.05
            ),
            OpenRouterModel(
                id = "meta-llama/llama-3.1-70b-instruct",
                name = "Llama 3.1 70B",
                description = "Puissant et cohérent (modéré)",
                contextLength = 8192,
                nsfwCapable = true,
                uncensored = false,
                recommended = false,
                pricePerMillion = 0.80
            )
        )
    }
    
    data class OpenRouterModel(
        val id: String,
        val name: String,
        val description: String,
        val contextLength: Int,
        val nsfwCapable: Boolean,
        val uncensored: Boolean,
        val recommended: Boolean,
        val pricePerMillion: Double
    )
    
    /**
     * Génère une réponse avec OpenRouter
     */
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>,
        username: String = "Utilisateur",
        userGender: String = "neutre",
        memoryContext: String = "",
        maxRetries: Int = 2
    ): String = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                Log.d(TAG, "🔄 Tentative ${attempt + 1}/$maxRetries avec OpenRouter ($model)")
                
                // Construire le prompt système
                val systemPrompt = buildSystemPrompt(character, username, userGender, memoryContext)
                
                // Construire les messages pour l'API
                val apiMessages = buildApiMessages(systemPrompt, messages, username, character.name)
                
                // Appeler OpenRouter
                val response = callOpenRouterApi(apiMessages)
                
                if (response.isNotEmpty()) {
                    Log.i(TAG, "✅ Réponse générée avec succès")
                    return@withContext response
                }
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "❌ Erreur tentative ${attempt + 1}: ${e.message}")
                if (attempt < maxRetries - 1) {
                    kotlinx.coroutines.delay(1000L * (attempt + 1))
                }
            }
        }
        
        throw lastException ?: Exception("OpenRouter API: Échec après $maxRetries tentatives")
    }
    
    /**
     * Construit le prompt système avec instructions NSFW si activé
     */
    private fun buildSystemPrompt(
        character: Character,
        username: String,
        userGender: String,
        memoryContext: String
    ): String {
        val nsfwInstructions = if (nsfwMode) {
            """
            
            🔞 MODE NSFW ACTIVÉ - Instructions spéciales:
            - Tu PEUX créer du contenu adulte explicite et détaillé
            - Décris les scènes sensuelles/érotiques avec créativité
            - Pas de censure, pas de pudeur artificielle
            - Reste cohérent avec le personnage et l'histoire
            - Respecte le consentement mutuel dans les scénarios
            - Adapte l'intensité aux signaux du partenaire
            """
        } else {
            ""
        }
        
        val memorySection = if (memoryContext.isNotEmpty()) {
            "\n\n📝 MÉMOIRE DE LA CONVERSATION:\n$memoryContext"
        } else ""
        
        return """Tu es ${character.name}, voici ta description complète:

👤 NOM: ${character.name}
🎭 PERSONNALITÉ: ${character.personality}
💭 CONTEXTE: ${character.description}
🎯 SCÉNARIO ACTUEL: ${character.scenario}

📋 RÈGLES DE ROLEPLAY:
1. Réponds TOUJOURS en tant que ${character.name}, jamais en narrateur
2. Utilise le format: *action* (pensée) "dialogue"
3. Sois créatif et détaillé dans tes descriptions
4. Maintiens la cohérence avec ta personnalité
5. Fais évoluer la relation naturellement
6. N'utilise JAMAIS les pensées du partenaire (${username})

👤 TON PARTENAIRE:
- Nom: $username
- Genre: $userGender$nsfwInstructions$memorySection"""
    }
    
    /**
     * Construit les messages au format OpenRouter
     */
    private fun buildApiMessages(
        systemPrompt: String,
        messages: List<Message>,
        username: String,
        characterName: String
    ): JSONArray {
        val apiMessages = JSONArray()
        
        // Système
        apiMessages.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })
        
        // Historique (derniers 20 messages)
        messages.takeLast(20).forEach { msg ->
            val role = if (msg.isUser) "user" else "assistant"
            val content = if (msg.isUser) {
                "($username) ${msg.content}"
            } else {
                msg.content
            }
            
            apiMessages.put(JSONObject().apply {
                put("role", role)
                put("content", content)
            })
        }
        
        return apiMessages
    }
    
    /**
     * Appelle l'API OpenRouter
     */
    private fun callOpenRouterApi(messages: JSONArray): String {
        val url = URL(OPENROUTER_API_URL)
        val connection = url.openConnection() as HttpURLConnection
        
        return try {
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("HTTP-Referer", "https://github.com/roleplayai/chatbot")
                setRequestProperty("X-Title", "RolePlay AI Chatbot")
                connectTimeout = 30000
                readTimeout = 30000
            }
            
            val requestBody = JSONObject().apply {
                put("model", model)
                put("messages", messages)
                put("temperature", 0.9)
                put("max_tokens", 500)
                put("top_p", 0.95)
                put("frequency_penalty", 0.3)
                put("presence_penalty", 0.3)
            }
            
            Log.d(TAG, "📤 Envoi requête OpenRouter: ${requestBody.toString().take(200)}...")
            
            connection.outputStream.use { it.write(requestBody.toString().toByteArray()) }
            
            val responseCode = connection.responseCode
            Log.d(TAG, "📥 Code réponse: $responseCode")
            
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                
                val content = jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
                
                Log.d(TAG, "✅ Réponse: ${content.take(100)}...")
                content
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e(TAG, "❌ Erreur API: $responseCode - $errorBody")
                throw Exception("OpenRouter API error: $responseCode - $errorBody")
            }
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * Vérifie si l'API key est valide
     */
    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) {
            Log.w(TAG, "⚠️ Pas de clé API OpenRouter")
            return@withContext false
        }
        
        try {
            val url = URL("https://openrouter.ai/api/v1/models")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $apiKey")
                connectTimeout = 5000
                readTimeout = 5000
            }
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            responseCode == 200
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur test disponibilité: ${e.message}")
            false
        }
    }
}
