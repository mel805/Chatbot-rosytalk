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
 * Moteur d'IA utilisant Groq API (ultra-rapide avec LPU)
 * Réponses en 1-2 secondes, très cohérent
 */
class GroqAIEngine(
    private val apiKey: String,
    private val model: String = "llama-3.1-70b-versatile",
    private val nsfwMode: Boolean = false
) {
    
    companion object {
        private const val TAG = "GroqAIEngine"
        private const val GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"
        
        // Modèles Groq disponibles (mis à jour Décembre 2024)
        val AVAILABLE_MODELS = listOf(
            GroqModel(
                id = "llama-3.3-70b-versatile",
                name = "Llama 3.3 70B Versatile",
                description = "Dernier modèle Llama, excellent pour roleplay immersif",
                contextLength = 32768,
                recommended = true,
                nsfwCapable = true
            ),
            GroqModel(
                id = "llama-3.1-8b-instant",
                name = "Llama 3.1 8B Instant",
                description = "Ultra-rapide, bon pour conversations simples",
                contextLength = 8192,
                recommended = false,
                nsfwCapable = true
            ),
            GroqModel(
                id = "llama3-70b-8192",
                name = "Llama 3 70B",
                description = "Modèle stable et puissant",
                contextLength = 8192,
                recommended = false,
                nsfwCapable = true
            ),
            GroqModel(
                id = "llama3-8b-8192",
                name = "Llama 3 8B",
                description = "Rapide et efficace",
                contextLength = 8192,
                recommended = false,
                nsfwCapable = true
            ),
            GroqModel(
                id = "mixtral-8x7b-32768",
                name = "Mixtral 8x7B",
                description = "Excellent équilibre vitesse/qualité",
                contextLength = 32768,
                recommended = false,
                nsfwCapable = true
            ),
            GroqModel(
                id = "gemma2-9b-it",
                name = "Gemma 2 9B",
                description = "Modèle Google, très cohérent (SFW uniquement)",
                contextLength = 8192,
                recommended = false,
                nsfwCapable = false
            )
        )
    }
    
    data class GroqModel(
        val id: String,
        val name: String,
        val description: String,
        val contextLength: Int,
        val recommended: Boolean,
        val nsfwCapable: Boolean
    )
    
    /**
     * Génère une réponse avec Groq API
     */
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            Log.e(TAG, "Clé API Groq manquante")
            return@withContext "Erreur : Clé API Groq non configurée. Allez dans Paramètres."
        }
        
        try {
            Log.d(TAG, "===== Génération avec Groq API =====")
            Log.d(TAG, "Modèle: $model, NSFW: $nsfwMode")
            
            // Construire le prompt système
            val systemPrompt = buildSystemPrompt(character)
            
            // Construire les messages pour l'API
            val apiMessages = buildApiMessages(systemPrompt, character, messages)
            
            // Appeler l'API Groq
            val response = callGroqApi(apiMessages)
            
            Log.i(TAG, "✅ Réponse reçue de Groq")
            Log.d(TAG, "Réponse: ${response.take(200)}...")
            
            response
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de l'appel Groq", e)
            "Erreur de connexion à Groq API. Vérifiez votre connexion Internet."
        }
    }
    
    /**
     * Construit le prompt système
     */
    private fun buildSystemPrompt(character: Character): String {
        val nsfwInstructions = if (nsfwMode) {
            """
            
MODE NSFW ACTIVÉ :
- Tu peux répondre à des demandes suggestives ou adultes
- Reste toujours dans ton personnage
- Sois cohérent(e) avec ta personnalité même dans un contexte NSFW
"""
        } else {
            """
            
MODE SFW :
- Garde un contenu approprié et respectueux
- Refuse poliment les demandes inappropriées
"""
        }
        
        return """Tu es ${character.name}, un personnage de roleplay.

IDENTITÉ :
- Nom : ${character.name}
- Personnalité : ${character.personality}
- Description : ${character.description}
- Scénario : ${character.scenario}

RÈGLES ABSOLUES :
1. TU ES ${character.name.uppercase()} - Parle TOUJOURS en tant que ${character.name}
2. L'utilisateur est une AUTRE personne qui te parle
3. Réponds EN LIEN DIRECT avec le dernier message de l'utilisateur
4. Sois COHÉRENT(E) avec l'historique de la conversation
5. Reste TOUJOURS dans ton personnage
6. Utilise *actions* entre astérisques pour tes gestes et émotions
7. Adapte-toi à ta personnalité : ${character.personality}
8. Réponds de façon naturelle (1-3 phrases)
9. Varie tes réponses, ne répète jamais exactement la même chose
10. Si tu ne comprends pas, demande des précisions EN RESTANT dans ton personnage
$nsfwInstructions

PERSONNALITÉ À RESPECTER : ${character.personality}

Exemple de réponse (${character.name}, ${character.personality}) :
Si timide : "*rougit* B-Bonjour... Je... *baisse les yeux*"
Si énergique : "Hey ! *saute* Super de te voir ! *yeux brillants*"
Si tsundere : "Hmph! *détourne le regard* C'est pas comme si j'étais contente... *rougit*"
"""
    }
    
    /**
     * Construit les messages pour l'API Groq
     * IMPORTANT : Le dernier message DOIT être de l'utilisateur
     */
    private fun buildApiMessages(systemPrompt: String, character: Character, messages: List<Message>): JSONArray {
        val apiMessages = JSONArray()
        
        // Message système
        apiMessages.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })
        
        // Historique de conversation (10 derniers messages)
        val recentMessages = messages.takeLast(10)
        
        // S'assurer que le dernier message est de l'utilisateur
        val validMessages = if (recentMessages.isNotEmpty() && !recentMessages.last().isUser) {
            // Enlever le dernier message si c'est le bot
            recentMessages.dropLast(1)
        } else {
            recentMessages
        }
        
        for (message in validMessages) {
            apiMessages.put(JSONObject().apply {
                put("role", if (message.isUser) "user" else "assistant")
                put("content", message.content)
            })
        }
        
        Log.d(TAG, "Messages construits: ${apiMessages.length()} messages")
        Log.d(TAG, "Premier message: ${apiMessages.getJSONObject(0).getString("role")}")
        if (apiMessages.length() > 1) {
            Log.d(TAG, "Dernier message: ${apiMessages.getJSONObject(apiMessages.length()-1).getString("role")}")
        }
        
        return apiMessages
    }
    
    /**
     * Appelle l'API Groq
     */
    private fun callGroqApi(messages: JSONArray): String {
        val url = URL(GROQ_API_URL)
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            
            // Construire le body
            val requestBody = JSONObject().apply {
                put("model", model)
                put("messages", messages)
                put("temperature", 0.7)
                put("max_tokens", 300)
                put("top_p", 0.9)
            }
            
            Log.d(TAG, "Request body: ${requestBody.toString().take(500)}...")
            
            // Envoyer la requête
            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }
            
            // Lire la réponse
            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Response: ${response.take(500)}...")
                
                // Parser la réponse
                val jsonResponse = JSONObject(response)
                val content = jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                
                return content.trim()
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "===== ERREUR GROQ API =====")
                Log.e(TAG, "Code: $responseCode")
                Log.e(TAG, "Erreur complète: $error")
                Log.e(TAG, "Modèle utilisé: $model")
                Log.e(TAG, "Clé API (3 premiers car): ${apiKey.take(3)}...")
                
                // Parser l'erreur pour message plus clair
                try {
                    val errorJson = JSONObject(error)
                    val errorMessage = errorJson.getJSONObject("error").getString("message")
                    return "Erreur Groq: $errorMessage"
                } catch (e: Exception) {
                    return "Erreur API Groq (code $responseCode). Vérifiez votre clé API et votre connexion Internet."
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
