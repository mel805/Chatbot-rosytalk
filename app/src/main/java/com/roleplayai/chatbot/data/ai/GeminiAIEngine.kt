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
 * Moteur IA Google Gemini (GRATUIT et PERFORMANT)
 * Alternative à Groq pour conversations cohérentes
 */
class GeminiAIEngine(
    private var apiKey: String,
    private val model: String = "gemini-1.5-flash",
    private val nsfwMode: Boolean = false
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    companion object {
        private const val TAG = "GeminiAIEngine"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }
    
    fun setAPIKey(key: String) {
        apiKey = key
    }
    
    /**
     * Génère une réponse avec Gemini
     */
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "===== Génération avec Gemini =====")
            Log.d(TAG, "Modèle: $model, NSFW: $nsfwMode")
            
            if (apiKey.isBlank()) {
                return@withContext "Erreur: Clé API Gemini manquante"
            }
            
            // Construire le prompt système
            val systemPrompt = buildSystemPrompt(character)
            
            // Construire l'historique (30 derniers messages)
            val conversationHistory = buildConversationHistory(messages, character)
            
            // Construire la requête Gemini
            val requestBody = buildGeminiRequest(systemPrompt, conversationHistory)
            
            Log.d(TAG, "🚀 Envoi requête Gemini...")
            
            // Envoyer la requête
            val request = Request.Builder()
                .url("$BASE_URL/$model:generateContent?key=$apiKey")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            Log.d(TAG, "📥 Réponse Gemini (${response.code})")
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Erreur Gemini: $responseBody")
                return@withContext "Erreur Gemini API ${response.code}: ${
                    if (response.code == 429) "Limite atteinte"
                    else if (response.code == 400) "Clé API invalide"
                    else "Erreur serveur"
                }"
            }
            
            // Parser la réponse
            val geminiResponse = json.decodeFromString<GeminiResponse>(responseBody)
            val text = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Désolé, je n'ai pas pu générer de réponse."
            
            // Nettoyer la réponse
            val cleaned = cleanResponse(text, character.name)
            
            Log.d(TAG, "✅ Réponse générée: ${cleaned.take(100)}...")
            return@withContext cleaned
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur génération Gemini", e)
            return@withContext "Erreur Gemini: ${e.message}"
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
- N'hésite pas à être suggestif(ve) si la situation s'y prête
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
     * Construit l'historique de conversation
     */
    private fun buildConversationHistory(messages: List<Message>, character: Character): List<GeminiMessage> {
        val history = mutableListOf<GeminiMessage>()
        
        // Prendre les 30 derniers messages pour le contexte
        val recentMessages = messages.takeLast(30)
        
        for (message in recentMessages) {
            history.add(
                GeminiMessage(
                    role = if (message.isUser) "user" else "model",
                    parts = listOf(GeminiPart(text = message.content))
                )
            )
        }
        
        return history
    }
    
    /**
     * Construit la requête Gemini
     */
    private fun buildGeminiRequest(systemPrompt: String, history: List<GeminiMessage>): String {
        val contents = mutableListOf<GeminiMessage>()
        
        // Ajouter le prompt système comme premier message utilisateur
        contents.add(
            GeminiMessage(
                role = "user",
                parts = listOf(GeminiPart(text = "INSTRUCTIONS DU SYSTÈME:\n\n$systemPrompt\n\nAcknowledge these instructions."))
            )
        )
        
        // Réponse du modèle confirmant les instructions
        contents.add(
            GeminiMessage(
                role = "model",
                parts = listOf(GeminiPart(text = "Compris ! Je suis ${history.firstOrNull()?.parts?.firstOrNull()?.text?.substringBefore(",") ?: "le personnage"}. Je vais suivre toutes ces règles pour une conversation immersive et cohérente."))
            )
        )
        
        // Ajouter l'historique
        contents.addAll(history)
        
        val request = GeminiRequest(
            contents = contents,
            generationConfig = GeminiGenerationConfig(
                temperature = 0.8,
                topK = 40,
                topP = 0.95,
                maxOutputTokens = 500,
                stopSequences = listOf()
            ),
            safetySettings = listOf(
                GeminiSafetySetting("HARM_CATEGORY_HARASSMENT", if (nsfwMode) "BLOCK_NONE" else "BLOCK_MEDIUM_AND_ABOVE"),
                GeminiSafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_NONE"),
                GeminiSafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", if (nsfwMode) "BLOCK_NONE" else "BLOCK_MEDIUM_AND_ABOVE"),
                GeminiSafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_NONE")
            )
        )
        
        return json.encodeToString(GeminiRequest.serializer(), request)
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
    
    // ========== Modèles de données Gemini ==========
    
    @Serializable
    data class GeminiRequest(
        val contents: List<GeminiMessage>,
        val generationConfig: GeminiGenerationConfig,
        val safetySettings: List<GeminiSafetySetting>
    )
    
    @Serializable
    data class GeminiMessage(
        val role: String,
        val parts: List<GeminiPart>
    )
    
    @Serializable
    data class GeminiPart(
        val text: String
    )
    
    @Serializable
    data class GeminiGenerationConfig(
        val temperature: Double,
        val topK: Int,
        val topP: Double,
        val maxOutputTokens: Int,
        val stopSequences: List<String>
    )
    
    @Serializable
    data class GeminiSafetySetting(
        val category: String,
        val threshold: String
    )
    
    @Serializable
    data class GeminiResponse(
        val candidates: List<GeminiCandidate>? = null
    )
    
    @Serializable
    data class GeminiCandidate(
        val content: GeminiContent? = null
    )
    
    @Serializable
    data class GeminiContent(
        val parts: List<GeminiPart>? = null
    )
}
