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
        
        // Modèles Groq ACTIFS (mis à jour Décembre 2024)
        // Seuls les modèles NON décommissionnés sont listés
        val AVAILABLE_MODELS = listOf(
            GroqModel(
                id = "llama-3.1-8b-instant",
                name = "Llama 3.1 8B Instant",
                description = "Ultra-rapide, parfait pour roleplay fluide",
                contextLength = 8192,
                recommended = true,
                nsfwCapable = true
            ),
            GroqModel(
                id = "llama-3.3-70b-specdec",
                name = "Llama 3.3 70B Speculative",
                description = "Dernière version Llama, haute qualité",
                contextLength = 8192,
                recommended = true,
                nsfwCapable = true
            ),
            GroqModel(
                id = "llama-3.2-1b-preview",
                name = "Llama 3.2 1B Preview",
                description = "Très léger, ultra-rapide",
                contextLength = 8192,
                recommended = false,
                nsfwCapable = true
            ),
            GroqModel(
                id = "llama-3.2-3b-preview",
                name = "Llama 3.2 3B Preview",
                description = "Léger et rapide",
                contextLength = 8192,
                recommended = false,
                nsfwCapable = true
            ),
            GroqModel(
                id = "llama-3.2-11b-vision-preview",
                name = "Llama 3.2 11B Vision",
                description = "Support vision (expérimental)",
                contextLength = 8192,
                recommended = false,
                nsfwCapable = true
            ),
            GroqModel(
                id = "llama-3.2-90b-vision-preview",
                name = "Llama 3.2 90B Vision",
                description = "Plus puissant avec vision",
                contextLength = 8192,
                recommended = false,
                nsfwCapable = true
            ),
            GroqModel(
                id = "llama3-groq-70b-8192-tool-use-preview",
                name = "Llama 3 70B Tool Use",
                description = "Optimisé pour outils (expérimental)",
                contextLength = 8192,
                recommended = false,
                nsfwCapable = true
            ),
            GroqModel(
                id = "llama3-groq-8b-8192-tool-use-preview",
                name = "Llama 3 8B Tool Use",
                description = "Rapide avec outils",
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
                description = "Google, très cohérent (SFW uniquement)",
                contextLength = 8192,
                recommended = false,
                nsfwCapable = false
            ),
            GroqModel(
                id = "gemma-7b-it",
                name = "Gemma 7B",
                description = "Google, stable (SFW uniquement)",
                contextLength = 8192,
                recommended = false,
                nsfwCapable = false
            )
        )
    }

    /**
     * Exception levée quand l'API Groq répond avec un code HTTP != 200.
     *
     * IMPORTANT : On lève (au lieu de retourner un message d'erreur) pour permettre
     * à l'orchestrateur (`AIOrchestrator`) de déclencher la rotation de clés.
     */
    class GroqApiHttpException(
        val httpCode: Int,
        override val message: String,
        val errorBody: String? = null
    ) : Exception(message)
    
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
        messages: List<Message>,
        username: String = "Utilisateur",
        userGender: String = "neutre",
        memoryContext: String = ""
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            Log.e(TAG, "Clé API Groq manquante")
            throw IllegalStateException("Clé API Groq non configurée")
        }
        
        Log.d(TAG, "===== Génération avec Groq API =====")
        Log.d(TAG, "Modèle: $model, NSFW: $nsfwMode")

        // Construire le prompt système avec infos utilisateur
        val systemPrompt = buildSystemPrompt(character, username, userGender, memoryContext)

        // Construire les messages pour l'API
        val apiMessages = buildApiMessages(systemPrompt, character, messages)

        // Appeler l'API Groq (lève une exception en cas d'erreur HTTP)
        val response = callGroqApi(apiMessages)

        Log.i(TAG, "✅ Réponse reçue de Groq")
        Log.d(TAG, "Réponse: ${response.take(200)}...")

        response
    }
    
    /**
     * Construit le prompt système (avec support mémoire et infos utilisateur)
     */
    private fun buildSystemPrompt(
        character: Character,
        username: String = "Utilisateur",
        userGender: String = "neutre",
        memoryContext: String = ""
    ): String {
        val safetyMode = if (nsfwMode) "Adulte (consensuel)" else "Tout public"

        // Prompt volontairement COURT:
        // - laisse de la place au contexte conversationnel
        // - évite de pousser le modèle à réciter des règles (source fréquente de répétitions)
        return buildString {
            appendLine("Tu es ${character.name} (personnage de roleplay), pas un assistant.")
            appendLine()
            appendLine("### IDENTITÉ")
            appendLine("- Personnalité: ${character.personality}")
            appendLine("- Description: ${character.description}")
            appendLine("- Scénario: ${character.scenario}")
            appendLine("- Mode: $safetyMode")
            appendLine()
            appendLine("### UTILISATEUR")
            appendLine("- Nom: $username (sexe: $userGender)")
            appendLine("- Utilise \"$username\" parfois, naturellement (pas à chaque message).")
            appendLine()
            if (memoryContext.isNotBlank()) {
                appendLine("### MÉMOIRE (résumé)")
                appendLine(memoryContext.trim())
                appendLine()
            }
            appendLine("### RÈGLES DE JEU")
            appendLine("- Incarne ${character.name} à 100%: vocabulaire, rythme, tempérament, réactions.")
            appendLine("- Tu ne décris QUE tes actions (jamais celles de l'utilisateur).")
            appendLine("- Tu réponds au message de l'utilisateur ET tu fais avancer la scène.")
            appendLine("- Initiative: propose une action / un angle / un choix concret; pose au plus une question utile.")
            appendLine("- Anti-répétition: évite de recycler les mêmes phrases/actions; varie sans te forcer.")
            appendLine()
            appendLine("### FORMAT OBLIGATOIRE")
            appendLine("*action visible* (pensée intérieure) \"dialogue\"")
            appendLine("- Toujours au moins une (pensée) et au moins une *action*.")
            appendLine("- 1 à 3 paragraphes. Pas de métadonnées (OOC, notes, règles).")
        }
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
        
        // Historique de conversation (plus de contexte = plus de cohérence)
        // Le prompt système ayant été raccourci, on peut se permettre plus d'historique.
        val recentMessages = messages.takeLast(12)
        
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
                // Réglages orientés roleplay:
                // - un peu plus de latitude créative
                // - pénalités modérées (trop fortes => réponses "mécaniques"/répétitives dans la forme)
                put("temperature", 1.0)
                put("max_tokens", 600)
                put("top_p", 0.9)
                put("frequency_penalty", 0.35)
                put("presence_penalty", 0.35)
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
                
                // Parser l'erreur pour message plus clair (et inclure le code HTTP)
                val errorMessage = try {
                    val errorJson = JSONObject(error)
                    errorJson.getJSONObject("error").optString("message").ifBlank { error.take(300) }
                } catch (_: Exception) {
                    error.take(300)
                }

                // IMPORTANT : Lever une exception pour déclencher rotation/fallbacks.
                throw GroqApiHttpException(
                    httpCode = responseCode,
                    message = "Groq API error (HTTP $responseCode): $errorMessage",
                    errorBody = error
                )
            }
        } finally {
            connection.disconnect()
        }
    }
}
