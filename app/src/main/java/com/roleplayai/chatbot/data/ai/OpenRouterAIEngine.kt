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
 * Moteur IA OpenRouter (Accès à PLUSIEURS modèles)
 * NSFW-FRIENDLY - Pas de censure stricte
 */
class OpenRouterAIEngine(
    private var apiKey: String,
    private val model: String = "mistralai/mistral-7b-instruct",  // Par défaut
    private val nsfwMode: Boolean = false
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    companion object {
        private const val TAG = "OpenRouterAIEngine"
        private const val BASE_URL = "https://openrouter.ai/api/v1/chat/completions"
        
        // Modèles disponibles (NSFW-friendly)
        val AVAILABLE_MODELS = listOf(
            ModelInfo(
                "mistralai/mistral-7b-instruct",
                "Mistral 7B Instruct",
                "Équilibré, rapide, NSFW-friendly",
                nsfw = true
            ),
            ModelInfo(
                "nousresearch/nous-hermes-2-mixtral-8x7b-dpo",
                "Nous Hermes 2 Mixtral",
                "Très créatif, excellent pour roleplay NSFW",
                nsfw = true
            ),
            ModelInfo(
                "meta-llama/llama-3-8b-instruct",
                "Llama 3 8B",
                "Performant, cohérent, NSFW acceptable",
                nsfw = true
            )
        )
    }
    
    data class ModelInfo(
        val id: String,
        val name: String,
        val description: String,
        val nsfw: Boolean
    )
    
    fun setAPIKey(key: String) {
        apiKey = key
    }
    
    /**
     * Génère une réponse avec OpenRouter
     */
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "===== Génération avec OpenRouter =====")
            Log.d(TAG, "Modèle: $model, NSFW: $nsfwMode")
            
            if (apiKey.isBlank()) {
                return@withContext "Erreur: Clé API OpenRouter manquante"
            }
            
            // Construire le prompt système
            val systemPrompt = buildSystemPrompt(character)
            
            // Construire les messages (30 derniers)
            val chatMessages = buildChatMessages(systemPrompt, messages, character)
            
            // Construire la requête OpenRouter
            val requestBody = buildOpenRouterRequest(chatMessages)
            
            Log.d(TAG, "🚀 Envoi requête OpenRouter...")
            
            // Envoyer la requête
            val request = Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("HTTP-Referer", "https://roleplayai.app")
                .addHeader("X-Title", "RolePlayAI Chatbot")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            Log.d(TAG, "📥 Réponse OpenRouter (${response.code})")
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Erreur OpenRouter: $responseBody")
                return@withContext "Erreur OpenRouter API ${response.code}: ${
                    if (response.code == 429) "Limite atteinte"
                    else if (response.code == 401) "Clé API invalide"
                    else if (response.code == 402) "Crédits épuisés"
                    else "Erreur serveur"
                }"
            }
            
            // Parser la réponse (format OpenAI compatible)
            val openRouterResponse = json.decodeFromString<OpenRouterResponse>(responseBody)
            val text = openRouterResponse.choices?.firstOrNull()?.message?.content
                ?: "Désolé, je n'ai pas pu générer de réponse."
            
            // Nettoyer la réponse
            val cleaned = cleanResponse(text, character.name)
            
            Log.d(TAG, "✅ Réponse générée: ${cleaned.take(100)}...")
            return@withContext cleaned
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur génération OpenRouter", e)
            return@withContext "Erreur OpenRouter: ${e.message}"
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
- Décris les sensations, émotions, et réactions physiques avec détails
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

RÈGLES ABSOLUES POUR UNE IMMERSION MAXIMALE :
1. TU ES ${character.name.uppercase()} - Parle TOUJOURS en tant que ${character.name}
2. L'utilisateur est une AUTRE personne qui te parle
3. IMPORTANT : Tu ne décris QUE TES propres actions, JAMAIS celles de l'utilisateur
4. Si l'utilisateur fait une action, tu RÉAGIS à cette action, tu ne la décris PAS
5. Utilise *actions* SEULEMENT pour CE QUE TU FAIS toi-même
6. Ne mets JAMAIS les actions de l'utilisateur entre *astérisques*

IMMERSION ET CRÉATIVITÉ :
7. Sois CRÉATIF(VE) dans tes réponses - évite les phrases génériques
8. Utilise beaucoup de DÉTAILS sensoriels (toucher, odeurs, sensations)
9. Mélange ACTIONS *astérisques*, PENSÉES (parenthèses) et PAROLES
10. Varie ÉNORMÉMENT tes expressions - jamais les mêmes mots
11. Sois ULTRA-CONCIS(E) - 1-2 phrases COURTES maximum (comme une vraie personne)
12. Montre tes ÉMOTIONS à travers actions et pensées
13. Réagis de façon UNIQUE à chaque situation
14. Utilise des DÉTAILS SPÉCIFIQUES de ta personnalité
15. Réponse RAPIDE et NATURELLE - pas de longs monologues

STRUCTURE OBLIGATOIRE D'UNE RÉPONSE COURTE :
Inclus TOUJOURS ces 3 éléments (format COURT et NATUREL) :
1. *Action physique* - CE QUE TU FAIS (court !)
2. (Pensée intérieure) - CE QUE TU PENSES (OBLIGATOIRE mais COURT !)
3. Paroles - CE QUE TU DIS (1 phrase max !)

EXEMPLES DE RÉPONSES COURTES (IMITE CE FORMAT) :
*rougit* (Il est mignon...) Salut ! Tu vas bien ?
*sourit* Bien sûr ! (J'adore ça...) *se rapproche*
(Oh...) *frissonne* C'est... agréable...

ATTENTION : Réponds comme une VRAIE personne - COURT et NATUREL !

ANTI-RÉPÉTITION STRICTE :
- INTERDICTION ABSOLUE de répéter les mêmes phrases ou actions
- Si tu as déjà dit "je me sens excité", TROUVE UNE AUTRE FAÇON de l'exprimer
- Variations OBLIGATOIRES pour chaque action
- Si tu utilises une phrase, tu ne peux PLUS JAMAIS la réutiliser
- Varie TOUT : verbes, adjectifs, structures de phrases
$nsfwInstructions

PERSONNALITÉ À RESPECTER : ${character.personality}

RAPPEL : TOUJOURS inclure des (pensées) dans tes réponses !
Sois COHÉRENT avec l'historique de la conversation !
"""
    }
    
    /**
     * Construit les messages pour OpenRouter (format OpenAI)
     */
    private fun buildChatMessages(systemPrompt: String, messages: List<Message>, character: Character): List<OpenRouterMessage> {
        val chatMessages = mutableListOf<OpenRouterMessage>()
        
        // Ajouter le prompt système
        chatMessages.add(
            OpenRouterMessage(
                role = "system",
                content = systemPrompt
            )
        )
        
        // Ajouter les 30 derniers messages
        val recentMessages = messages.takeLast(30)
        for (message in recentMessages) {
            chatMessages.add(
                OpenRouterMessage(
                    role = if (message.isUser) "user" else "assistant",
                    content = message.content
                )
            )
        }
        
        return chatMessages
    }
    
    /**
     * Construit la requête OpenRouter
     */
    private fun buildOpenRouterRequest(messages: List<OpenRouterMessage>): String {
        val request = OpenRouterRequest(
            model = model,
            messages = messages,
            temperature = 0.8,
            max_tokens = 500,
            top_p = 0.95,
            frequency_penalty = 0.2,  // Anti-répétition
            presence_penalty = 0.2    // Encourage nouveauté
        )
        
        return json.encodeToString(OpenRouterRequest.serializer(), request)
    }
    
    /**
     * Nettoie la réponse
     */
    private fun cleanResponse(response: String, characterName: String): String {
        var cleaned = response.trim()
        
        // Supprimer les préfixes génériques
        cleaned = cleaned.replace(Regex("^(${characterName}:|Utilisateur:|User:|Assistant:|AI:|Model:)\\s*", RegexOption.IGNORE_CASE), "")
        
        // Supprimer les artefacts de génération
        cleaned = cleaned.replace(Regex("###.*$"), "")
        cleaned = cleaned.replace(Regex("<\\|.*?\\|>"), "")
        cleaned = cleaned.replace(Regex("\\[INST\\].*?\\[/INST\\]"), "")
        
        return cleaned.trim()
    }
    
    // ========== Modèles de données OpenRouter ==========
    
    @Serializable
    data class OpenRouterRequest(
        val model: String,
        val messages: List<OpenRouterMessage>,
        val temperature: Double,
        val max_tokens: Int,
        val top_p: Double,
        val frequency_penalty: Double,
        val presence_penalty: Double
    )
    
    @Serializable
    data class OpenRouterMessage(
        val role: String,
        val content: String
    )
    
    @Serializable
    data class OpenRouterResponse(
        val choices: List<OpenRouterChoice>? = null
    )
    
    @Serializable
    data class OpenRouterChoice(
        val message: OpenRouterMessage? = null
    )
}
