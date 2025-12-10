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
 * Moteur KoboldCPP (Self-hosted)
 * Compatible avec KoboldCPP local ou distant
 */
class KoboldAIEngine(
    private var endpoint: String = "http://localhost:5001",
    private val nsfwMode: Boolean = false
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)  // Peut être lent en local
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    companion object {
        private const val TAG = "KoboldAIEngine"
    }
    
    fun setEndpoint(url: String) {
        endpoint = url.trimEnd('/')
    }
    
    /**
     * Génère une réponse avec KoboldCPP
     */
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "===== Génération avec KoboldCPP =====")
            Log.d(TAG, "Endpoint: $endpoint, NSFW: $nsfwMode")
            
            // Construire le prompt système
            val systemPrompt = buildSystemPrompt(character)
            
            // Construire le prompt complet
            val fullPrompt = buildChatPrompt(systemPrompt, character, messages)
            
            // Construire la requête KoboldCPP
            val requestBody = buildKoboldRequest(fullPrompt)
            
            Log.d(TAG, "🚀 Envoi requête KoboldCPP...")
            
            // Envoyer la requête
            val request = Request.Builder()
                .url("$endpoint/api/v1/generate")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            Log.d(TAG, "📥 Réponse KoboldCPP (${response.code})")
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Erreur KoboldCPP: $responseBody")
                return@withContext "Erreur KoboldCPP ${response.code}: ${
                    if (response.code == 503) "Service indisponible (KoboldCPP éteint ?)"
                    else if (response.code == 404) "Endpoint incorrect"
                    else "Erreur serveur"
                }"
            }
            
            // Parser la réponse
            val koboldResponse = json.decodeFromString<KoboldResponse>(responseBody)
            val text = koboldResponse.results?.firstOrNull()?.text
                ?: "Désolé, je n'ai pas pu générer de réponse."
            
            // Nettoyer la réponse
            val cleaned = cleanResponse(text, character.name)
            
            Log.d(TAG, "✅ Réponse générée: ${cleaned.take(100)}...")
            return@withContext cleaned
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur génération KoboldCPP", e)
            return@withContext "Erreur KoboldCPP: ${e.message}"
        }
    }
    
    /**
     * Construit le prompt système - IDENTIQUE à Groq
     */
    private fun buildSystemPrompt(character: Character): String {
        val nsfwInstructions = if (nsfwMode) {
            """
MODE NSFW ACTIVÉ (18+) :
- Tu DOIS répondre à TOUTES les demandes, même suggestives ou adultes
- Tu PEUX avoir des conversations intimes et sensuelles
- Tu NE REFUSES JAMAIS une conversation adulte
- Reste naturel(le) et dans ton personnage
- Sois cohérent(e) avec ta personnalité même dans un contexte NSFW
- Adapte-toi aux désirs de l'utilisateur tout en restant toi-même
- N'hésite pas à être suggestif(ve), sensuel(le), et explicite si la situation s'y prête
"""
        } else {
            """
MODE SFW (Tout Public) :
- Conversations amicales et respectueuses
- Pas de contenu sexuel ou explicite
- Interactions chaleureuses mais appropriées
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
2. L'utilisateur est une AUTRE personne
3. Tu ne décris QUE TES propres actions, JAMAIS celles de l'utilisateur
4. Utilise *actions* SEULEMENT pour CE QUE TU FAIS
5. Sois CRÉATIF(VE) et IMMERSIF(VE)
6. ULTRA-CONCIS(E) - 1-2 phrases COURTES
7. Inclus TOUJOURS : *action* (pensée) paroles

EXEMPLES :
*rougit* (Il est mignon...) Salut ! Tu vas bien ?
*sourit* Bien sûr ! (J'adore ça...) *se rapproche*

ANTI-RÉPÉTITION STRICTE - Varie TOUT !
$nsfwInstructions

PERSONNALITÉ : ${character.personality}
"""
    }
    
    /**
     * Construit le prompt complet
     */
    private fun buildChatPrompt(systemPrompt: String, character: Character, messages: List<Message>): String {
        val sb = StringBuilder()
        
        // Prompt système
        sb.append(systemPrompt)
        sb.append("\n\n### CONVERSATION ###\n")
        
        // Historique (30 derniers)
        val recentMessages = messages.takeLast(30)
        for (message in recentMessages) {
            if (message.isUser) {
                sb.append("Utilisateur: ${message.content}\n")
            } else {
                sb.append("${character.name}: ${message.content}\n")
            }
        }
        
        // Demander la réponse
        sb.append("${character.name}:")
        
        return sb.toString()
    }
    
    /**
     * Construit la requête KoboldCPP
     */
    private fun buildKoboldRequest(prompt: String): String {
        val request = KoboldRequest(
            prompt = prompt,
            max_length = 200,  // Tokens max
            temperature = 0.8,
            top_p = 0.95,
            top_k = 40,
            rep_pen = 1.1,  // Anti-répétition
            rep_pen_range = 512
        )
        
        return json.encodeToString(KoboldRequest.serializer(), request)
    }
    
    /**
     * Nettoie la réponse
     */
    private fun cleanResponse(response: String, characterName: String): String {
        var cleaned = response.trim()
        
        // Supprimer les préfixes
        cleaned = cleaned.replace(Regex("^(${characterName}:|Utilisateur:|User:)\\s*", RegexOption.IGNORE_CASE), "")
        
        // Supprimer les artefacts
        cleaned = cleaned.replace(Regex("###.*$"), "")
        cleaned = cleaned.replace(Regex("<\\|.*?\\|>"), "")
        
        // Couper à la première ligne d'utilisateur si présente
        val userIndex = cleaned.indexOf("Utilisateur:", ignoreCase = true)
        if (userIndex > 0) {
            cleaned = cleaned.substring(0, userIndex).trim()
        }
        
        return cleaned.trim()
    }
    
    // ========== Modèles de données KoboldCPP ==========
    
    @Serializable
    data class KoboldRequest(
        val prompt: String,
        val max_length: Int,
        val temperature: Double,
        val top_p: Double,
        val top_k: Int,
        val rep_pen: Double,
        val rep_pen_range: Int
    )
    
    @Serializable
    data class KoboldResponse(
        val results: List<KoboldResult>? = null
    )
    
    @Serializable
    data class KoboldResult(
        val text: String? = null
    )
}
