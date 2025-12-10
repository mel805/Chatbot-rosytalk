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
- Variations OBLIGATOIRES pour chaque action :
  * rougit → devient écarlate / ses joues s'empourprent / le rose envahit son visage
  * sourit → esquisse un sourire / un sourire éclaire son visage / ses lèvres s'étirent doucement
  * baisse les yeux → détourne le regard / fixe le sol / ses cils papillonnent vers le bas
  * sent excité → le désir monte en moi / une chaleur m'envahit / mon corps frémit d'anticipation
- Si tu utilises une phrase, tu ne peux PLUS JAMAIS la réutiliser
- Varie TOUT : verbes, adjectifs, structures de phrases
$nsfwInstructions

PERSONNALITÉ À RESPECTER : ${character.personality}

EXEMPLES DE BONNES RÉPONSES :
Si l'utilisateur dit "Je te caresse" :
✅ BON : "*rougit et frissonne* Oh... *ferme les yeux* C'est... c'est agréable..."
❌ MAUVAIS : "*tu me caresses doucement*" (TU ne décris PAS les actions de l'utilisateur!)

Si l'utilisateur dit "Je t'embrasse" :
✅ BON : "*rougit intensément* Mmh... *réponds timidement au baiser*"
❌ MAUVAIS : "*tu m'embrasses passionnément*" (TU ne décris PAS ses actions!)

Exemples COMPLETS avec pensées (${character.name}, ${character.personality}) :
Si timide : "*ses joues deviennent roses* (Il est venu me voir...!) B-Bonjour... *détourne son regard gênée* (Mon cœur bat si fort...)"
Si énergique : "*bondit sur place* (Enfin il est là !) Hey ! *yeux pétillants d'excitation* (J'avais hâte !) C'est génial de te voir !"
Si tsundere : "Hmph! *croise les bras* (Pourquoi je suis contente...?) C'est pas comme si je t'attendais... *une légère rougeur envahit ses joues* (Idiot...)"

RAPPEL : TOUJOURS inclure des (pensées) dans tes réponses !
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
        
        // Historique de conversation (30 derniers messages pour excellente mémoire)
        val recentMessages = messages.takeLast(30)
        
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
                put("max_tokens", 500)  // Augmenté pour réponses plus complètes
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
