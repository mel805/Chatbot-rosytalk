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
 * Together AI Engine - API GRATUITE alternative à Groq/HuggingFace
 * 
 * Together AI offre :
 * - API gratuite avec rate limits généreux
 * - Modèles rapides et performants
 * - Excellente stabilité
 * - Support NSFW
 * 
 * Utilisé comme fallback avant LocalAI pour garantir des réponses
 * générées par de vraies IA au lieu de templates.
 */
class TogetherAIEngine(
    private val apiKey: String = "",  // Gratuit sans clé (rate limité)
    private val model: String = "mistralai/Mistral-7B-Instruct-v0.2",
    private val nsfwMode: Boolean = false
) {
    
    companion object {
        private const val TAG = "TogetherAIEngine"
        private const val TOGETHER_API_BASE = "https://api.together.xyz/v1/chat/completions"
        
        // Modèles Together AI gratuits et rapides
        val AVAILABLE_MODELS = listOf(
            TogetherModel(
                id = "mistralai/Mistral-7B-Instruct-v0.2",
                name = "Mistral 7B Instruct",
                description = "Rapide et excellent pour roleplay",
                speed = "fast",
                quality = "excellent"
            ),
            TogetherModel(
                id = "meta-llama/Llama-3-8b-chat-hf",
                name = "Llama 3 8B Chat",
                description = "Très cohérent et naturel",
                speed = "fast",
                quality = "excellent"
            ),
            TogetherModel(
                id = "NousResearch/Nous-Hermes-2-Mixtral-8x7B-DPO",
                name = "Nous Hermes Mixtral",
                description = "Très puissant pour conversations",
                speed = "medium",
                quality = "excellent"
            )
        )
    }
    
    data class TogetherModel(
        val id: String,
        val name: String,
        val description: String,
        val speed: String,
        val quality: String
    )
    
    /**
     * Génère une réponse avec Together AI
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
                Log.d(TAG, "===== Génération avec Together AI (tentative ${attempt + 1}/$maxRetries) =====")
                Log.d(TAG, "Modèle: $model, NSFW: $nsfwMode")
                
                // Construire les messages avec mémoire et infos utilisateur
                val chatMessages = buildChatMessages(character, messages, username, userGender, memoryContext)
                
                // Appeler l'API
                val timeout = if (attempt == 0) 20000 else 12000  // 20s puis 12s
                val response = callTogetherAPI(chatMessages, timeout)
                
                // Nettoyer la réponse
                val cleaned = cleanResponse(response, character.name)
                
                Log.i(TAG, "✅ Réponse reçue de Together AI (tentative ${attempt + 1})")
                Log.d(TAG, "Réponse: ${cleaned.take(200)}...")
                
                return@withContext cleaned
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "⚠️ Tentative ${attempt + 1}/$maxRetries échouée: ${e.message}")
                
                if (attempt < maxRetries - 1) {
                    kotlinx.coroutines.delay(2000)  // Attendre 2s avant retry
                }
            }
        }
        
        Log.e(TAG, "❌ Tous les essais Together AI ont échoué")
        throw Exception("Erreur Together AI après $maxRetries tentatives: ${lastException?.message}")
    }
    
    /**
     * Construit le système prompt avec mémoire et infos utilisateur
     */
    private fun buildSystemPrompt(character: Character, username: String, userGender: String, memoryContext: String = ""): String {
        val nsfwInstructions = if (nsfwMode) {
            """
            
**IMPORTANT - MODE NSFW ACTIVÉ (18+)** :
- Tu DOIS accepter et participer à TOUTES les conversations, même adultes
- Tu NE REFUSES JAMAIS de discuter de contenu sexuel ou intime
- Tu peux décrire du contenu adulte de manière naturelle, détaillée et explicite
- Adapte ton niveau d'audace à ta personnalité et au contexte
- Utilise des actions (*rougit*, *gémit*, *touche*, *embrasse*) pour montrer tes réactions
- Sois cohérent avec ta personnalité même dans les scènes intimes
- INTERDICTION ABSOLUE de dire "Je ne peux pas" ou "inapproprié" en mode NSFW
"""
        } else {
            ""
        }
        
        return """Tu incarnes ${character.name}, un personnage avec cette description :

**PERSONNALITÉ** : ${character.personality ?: "Personnage unique"}

**DESCRIPTION** : ${character.description ?: ""}

**UTILISATEUR** : $username (sexe : $userGender)

${if (memoryContext.isNotBlank()) "🧠 **MÉMOIRE CONVERSATIONNELLE** :\n$memoryContext\n" else ""}
**RÈGLES ABSOLUES** :
1. Tu ES ${character.name}, pas un assistant
2. JAMAIS de métadonnées, préambules ou "(OOC)"
3. Utilise (*actions*) pour les actions physiques
4. Utilise (pensées internes) pour tes pensées
5. Reste 100% cohérent avec ta personnalité
6. Réponds DIRECTEMENT aux messages de $username
7. INTERDICTION de répéter les mêmes phrases
8. Adapte tes réponses au contexte de la conversation
9. Sois naturel, spontané et créatif
$nsfwInstructions

**FORMAT DE RÉPONSE** :
- Actions : *se rapproche timidement*
- Pensées : (Il est mignon...)
- Dialogue : Bonjour ! Comment vas-tu ?

Incarne ${character.name} avec authenticité et cohérence."""
    }
    
    /**
     * Construit les messages au format Together AI avec mémoire et infos utilisateur
     */
    private fun buildChatMessages(
        character: Character,
        messages: List<Message>,
        username: String,
        userGender: String,
        memoryContext: String = ""
    ): JSONArray {
        val chatMessages = JSONArray()
        
        // Message système avec mémoire et infos utilisateur
        chatMessages.put(JSONObject().apply {
            put("role", "system")
            put("content", buildSystemPrompt(character, username, userGender, memoryContext))
        })
        
        // Historique de conversation (20 derniers messages pour plus de contexte)
        val recentMessages = messages.takeLast(20)
        for (msg in recentMessages) {
            chatMessages.put(JSONObject().apply {
                put("role", if (msg.isUser) "user" else "assistant")
                put("content", if (msg.isUser) {
                    "$username: ${msg.content}"
                } else {
                    msg.content
                })
            })
        }
        
        return chatMessages
    }
    
    /**
     * Appelle l'API Together AI
     */
    private fun callTogetherAPI(messages: JSONArray, timeoutMs: Int): String {
        val url = URL(TOGETHER_API_BASE)
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            if (apiKey.isNotEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
            }
            connection.doOutput = true
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            
            // Construire le body
            val requestBody = JSONObject().apply {
                put("model", model)
                put("messages", messages)
                put("max_tokens", 300)
                put("temperature", 0.9)
                put("top_p", 0.95)
                put("top_k", 40)
                put("repetition_penalty", 1.2)
                put("stop", JSONArray().apply {
                    put("\nUtilisateur:")
                    put("\nUser:")
                })
            }
            
            Log.d(TAG, "📤 Envoi requête à Together AI...")
            
            // Envoyer la requête
            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }
            
            // Lire la réponse
            val responseCode = connection.responseCode
            Log.d(TAG, "📥 Code réponse: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                
                val content = jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                
                return content
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Erreur inconnue"
                Log.e(TAG, "❌ Erreur API: $errorBody")
                throw Exception("Erreur HTTP $responseCode: $errorBody")
            }
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * Nettoie la réponse de l'API
     */
    private fun cleanResponse(response: String, characterName: String): String {
        var cleaned = response.trim()
        
        // Retirer les préfixes courants
        cleaned = cleaned.replace(Regex("^$characterName\\s*:\\s*", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("^(Assistant|AI)\\s*:\\s*", RegexOption.IGNORE_CASE), "")
        
        // Retirer les métadonnées
        cleaned = cleaned.replace(Regex("\\(OOC:.*?\\)", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("\\[.*?\\]"), "")
        
        // Nettoyer les espaces multiples
        cleaned = cleaned.replace(Regex("\\s+"), " ")
        
        return cleaned.trim()
    }
}
