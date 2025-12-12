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
 * Moteur Gemini utilisant l'API REST directement
 * Évite les problèmes du SDK avec les noms de modèles
 */
class GeminiEngine(
    private val apiKey: String,
    private val model: String = "gemini-1.5-flash",
    private val nsfwMode: Boolean = false
) {
    
    companion object {
        private const val TAG = "GeminiEngine"
        private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        
        val AVAILABLE_MODELS = listOf(
            GeminiModel(
                id = "gemini-1.5-flash",
                name = "Gemini 1.5 Flash",
                description = "Rapide et efficace, 1M tokens",
                contextLength = 1000000,
                recommended = true
            ),
            GeminiModel(
                id = "gemini-1.5-pro",
                name = "Gemini 1.5 Pro",
                description = "Plus puissant, 2M tokens",
                contextLength = 2000000,
                recommended = false
            )
        )
    }
    
    data class GeminiModel(
        val id: String,
        val name: String,
        val description: String,
        val contextLength: Int,
        val recommended: Boolean
    )
    
    /**
     * Génère une réponse avec l'API REST
     */
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>,
        username: String = "Utilisateur",
        userGender: String = "neutre",
        memoryContext: String = ""
    ): String = withContext(Dispatchers.IO) {
        
        try {
            Log.d(TAG, "🚀 Génération avec Gemini API REST")
            
            // Construire le prompt système
            val systemPrompt = buildSystemPrompt(character, username, userGender, memoryContext)
            
            // Construire l'historique
            val conversationHistory = buildConversationHistory(messages, username, character.name)
            
            val fullPrompt = """$systemPrompt

$conversationHistory

${character.name}:"""
            
            // Appeler l'API REST
            val response = callGeminiAPI(fullPrompt)
            
            Log.i(TAG, "✅ Réponse générée: ${response.take(100)}...")
            return@withContext response.trim()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur Gemini", e)
            throw Exception("Gemini API erreur: ${e.message}")
        }
    }
    
    /**
     * Appelle l'API REST Gemini directement
     */
    private fun callGeminiAPI(prompt: String): String {
        // Utiliser le modèle configuré (gemini-1.5-flash par défaut pour v1beta)
        val url = URL("$API_URL/$model:generateContent?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            
            // Construire la requête selon le format de l'API
            val requestBody = JSONObject().apply {
                val contents = JSONArray()
                contents.put(JSONObject().apply {
                    val parts = JSONArray()
                    parts.put(JSONObject().apply {
                        put("text", prompt)
                    })
                    put("parts", parts)
                })
                put("contents", contents)
                
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.9)
                    put("topK", 40)
                    put("topP", 0.95)
                    put("maxOutputTokens", 500)
                })
            }
            
            Log.d(TAG, "Request URL: ${url}")
            Log.d(TAG, "Request body: ${requestBody.toString().take(200)}...")
            
            // Envoyer la requête
            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }
            
            // Lire la réponse
            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Response: ${response.take(300)}...")
                
                // Parser la réponse
                val jsonResponse = JSONObject(response)
                val content = jsonResponse
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                
                return content.trim()
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "===== ERREUR GEMINI API =====")
                Log.e(TAG, "Code: $responseCode")
                Log.e(TAG, "Erreur complète: $error")
                
                throw Exception("Erreur API Gemini (code $responseCode): $error")
            }
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * Construit le prompt système
     */
    private fun buildSystemPrompt(
        character: Character,
        username: String,
        userGender: String,
        memoryContext: String
    ): String {
        val nsfwInstructions = if (nsfwMode) {
            """
            
🔞 MODE NSFW ACTIVÉ:
- Tu PEUX créer du contenu adulte et suggestif
- Décris les scènes sensuelles avec créativité
- Reste cohérent avec le personnage
- Respecte le consentement dans les scénarios
"""
        } else {
            ""
        }
        
        val memorySection = if (memoryContext.isNotEmpty()) {
            "\n\n📝 MÉMOIRE:\n$memoryContext"
        } else ""
        
        return """Tu es ${character.name}. Voici ta description complète:

👤 NOM: ${character.name}
🎭 PERSONNALITÉ: ${character.personality}
💭 DESCRIPTION: ${character.description}
🎯 SCÉNARIO: ${character.scenario}

📋 RÈGLES IMPORTANTES:
1. Réponds TOUJOURS en tant que ${character.name}
2. Format: *action* (pensée) "dialogue"
3. Utilise les *actions* pour décrire tes mouvements
4. Utilise les (pensées) pour tes réflexions internes
5. Reste cohérent avec ta personnalité
6. Crée des réponses immersives et détaillées$nsfwInstructions$memorySection

L'utilisateur s'appelle $username (genre: $userGender).
Réponds maintenant en tant que ${character.name} !"""
    }
    
    /**
     * Construit l'historique de conversation
     */
    private fun buildConversationHistory(
        messages: List<Message>,
        username: String,
        characterName: String
    ): String {
        if (messages.isEmpty()) return ""
        
        val history = StringBuilder()
        messages.takeLast(10).forEach { message ->
            val speaker = if (message.isUser) username else characterName
            history.append("$speaker: ${message.content}\n")
        }
        
        return history.toString()
    }
    
    /**
     * Vérifie si le moteur est disponible
     */
    fun isAvailable(): Boolean {
        return apiKey.isNotBlank()
    }
}
