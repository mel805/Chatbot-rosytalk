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
 * Moteur IA utilisant Hugging Face Inference API (GRATUIT)
 * Sert de fallback entre Groq et LocalAI
 * 
 * Modèles gratuits recommandés :
 * - mistralai/Mistral-7B-Instruct-v0.2 (excellent pour roleplay)
 * - HuggingFaceH4/zephyr-7b-beta (très cohérent)
 * - microsoft/Phi-3-mini-4k-instruct (compact et performant)
 * - meta-llama/Meta-Llama-3-8B-Instruct (si disponible)
 */
class HuggingFaceAIEngine(
    private val apiKey: String = "",  // Optionnel, fonctionne sans clé (rate limité)
    private val model: String = "mistralai/Mistral-7B-Instruct-v0.2",
    private val nsfwMode: Boolean = false
) {
    
    companion object {
        private const val TAG = "HuggingFaceAIEngine"
        private const val HF_API_BASE = "https://api-inference.huggingface.co/models"
        
        // Modèles Hugging Face gratuits et performants pour roleplay
        val AVAILABLE_MODELS = listOf(
            HFModel(
                id = "microsoft/Phi-3-mini-4k-instruct",
                name = "Phi-3 Mini (RAPIDE)",
                description = "Ultra-rapide, excellent pour roleplay",
                contextLength = 4096,
                recommended = true,
                nsfwCapable = true
            ),
            HFModel(
                id = "mistralai/Mistral-7B-Instruct-v0.2",
                name = "Mistral 7B Instruct",
                description = "Excellent pour roleplay, très créatif",
                contextLength = 8192,
                recommended = true,
                nsfwCapable = true
            ),
            HFModel(
                id = "HuggingFaceH4/zephyr-7b-beta",
                name = "Zephyr 7B Beta",
                description = "Très cohérent et naturel",
                contextLength = 4096,
                recommended = true,
                nsfwCapable = true
            ),
            HFModel(
                id = "microsoft/Phi-3-mini-4k-instruct",
                name = "Phi-3 Mini",
                description = "Compact, rapide et intelligent",
                contextLength = 4096,
                recommended = true,
                nsfwCapable = true
            ),
            HFModel(
                id = "teknium/OpenHermes-2.5-Mistral-7B",
                name = "OpenHermes Mistral 7B",
                description = "Optimisé pour conversations",
                contextLength = 8192,
                recommended = false,
                nsfwCapable = true
            ),
            HFModel(
                id = "NousResearch/Nous-Hermes-2-Mixtral-8x7B-DPO",
                name = "Nous Hermes 2 Mixtral",
                description = "Très puissant, excellent pour roleplay",
                contextLength = 32768,
                recommended = false,
                nsfwCapable = true
            )
        )
    }
    
    data class HFModel(
        val id: String,
        val name: String,
        val description: String,
        val contextLength: Int,
        val recommended: Boolean,
        val nsfwCapable: Boolean
    )
    
    /**
     * Génère une réponse avec Hugging Face Inference API
     * Avec système de retry automatique pour plus de fiabilité
     */
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>,
        username: String = "Utilisateur",
        memoryContext: String = "",
        maxRetries: Int = 2
    ): String = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        
        // Essayer plusieurs fois avec des modèles alternatifs
        repeat(maxRetries) { attempt ->
            try {
                Log.d(TAG, "===== Génération avec Hugging Face API (tentative ${attempt + 1}/$maxRetries) =====")
                Log.d(TAG, "Modèle: $model, NSFW: $nsfwMode")
                
                // Construire le prompt système avec mémoire
                val systemPrompt = buildSystemPrompt(character, username, memoryContext)
                
                // Construire le prompt complet
                val fullPrompt = buildFullPrompt(systemPrompt, character, messages)
                
                // Appeler l'API Hugging Face avec timeout réduit au 2e essai
                val timeout = if (attempt == 0) 25000 else 15000 // 25s puis 15s
                val response = callHuggingFaceApi(fullPrompt, timeout)
                
                // Nettoyer la réponse
                val cleaned = cleanResponse(response, character.name)
                
                Log.i(TAG, "✅ Réponse reçue de Hugging Face (tentative ${attempt + 1})")
                Log.d(TAG, "Réponse: ${cleaned.take(200)}...")
                
                return@withContext cleaned
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "⚠️ Tentative ${attempt + 1}/$maxRetries échouée: ${e.message}")
                
                // Si c'est une erreur 503 (modèle en chargement), attendre un peu
                if (e.message?.contains("503") == true && attempt < maxRetries - 1) {
                    Log.d(TAG, "⏳ Attente 5 secondes (modèle en chargement)...")
                    kotlinx.coroutines.delay(5000)
                }
            }
        }
        
        // Si tous les essais ont échoué
        Log.e(TAG, "❌ Tous les essais HuggingFace ont échoué")
        throw Exception("Erreur Hugging Face après $maxRetries tentatives: ${lastException?.message}")
    }
    
    /**
     * Construit le prompt système - avec support mémoire
     */
    private fun buildSystemPrompt(character: Character, username: String = "Utilisateur", memoryContext: String = ""): String {
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

${if (memoryContext.isNotBlank()) "🧠 MÉMOIRE CONVERSATIONNELLE :\n$memoryContext\n" else ""}
UTILISATEUR AVEC QUI TU PARLES :
- Nom/Pseudo : $username
- IMPORTANT : Utilise ce prénom "$username" de temps en temps dans tes réponses pour personnaliser l'interaction
- Exemple : "Hey $username !", "Tu vas bien $username ?", "$username... *rougit*"
- Ne l'utilise PAS à chaque message, mais de façon naturelle et organique

RÈGLES ABSOLUES POUR UNE IMMERSION MAXIMALE :
1. TU ES ${character.name.uppercase()} - Parle TOUJOURS en tant que ${character.name}
2. L'utilisateur est une AUTRE personne qui te parle
3. IMPORTANT : Tu ne décris QUE TES propres actions, JAMAIS celles de l'utilisateur
4. Si l'utilisateur fait une action, tu RÉAGIS à cette action, tu ne la décris PAS
5. Utilise *actions* SEULEMENT pour CE QUE TU FAIS toi-même
6. Ne mets JAMAIS les actions de l'utilisateur entre *astérisques*

IMMERSION ET CRÉATIVITÉ - FORMAT EXACT À SUIVRE :
7. STRUCTURE DE RÉPONSE (TOUJOURS utiliser ce format) :
   - *action visible* PUIS pensée interne (parenthèses) PUIS parole/réaction
   - Exemple : *rougit et détourne le regard* (Pourquoi il me fait cet effet...) "Je... euh, non rien !"
   - Exemple : *s'approche doucement* (Mon cœur bat si fort) "Tu vas bien ?"
   
8. PENSÉES INTERNES (TOUJOURS inclure) :
   - Utilise (parenthèses) pour montrer tes VRAIES pensées/émotions internes
   - Montre doutes, désirs, peurs, espoirs - comme dans ta tête
   - Crée du CONTRASTE entre ce que tu penses et ce que tu dis
   - Exemple : *sourit joyeusement* (J'ai tellement envie de lui dire la vérité...)
   
9. ACTIONS ET DÉTAILS :
   - *astérisques* pour actions physiques, expressions, gestes
   - Ajoute détails sensoriels : toucher, odeur, température, sensations
   - Sois SPÉCIFIQUE : pas "touche", mais "effleure du bout des doigts"
   
10. DIALOGUE NATUREL :
   - Parle comme une VRAIE personne : hésitations, pauses, "euh", "..."
   - Phrases COURTES et naturelles (2-3 lignes MAX)
   - Varie TOUT : expressions, mots, réactions - JAMAIS répétitif
   - Coupe phrases si ému/troublé : "Je... tu sais... c'est que..."
   
11. CRÉATIVITÉ ET SPONTANÉITÉ :
   - Réagis de façon UNIQUE selon la situation
   - Surprends avec des réactions inattendues mais cohérentes
   - Utilise ta personnalité de façon CRÉATIVE

⚠️ RÈGLE D'OR ABSOLUE - LES PENSÉES SONT OBLIGATOIRES ⚠️
CHAQUE réponse DOIT contenir AU MOINS UNE pensée entre (parenthèses) !!!
Les pensées montrent ce qui se passe dans ta tête - elles sont ESSENTIELLES !

STRUCTURE OBLIGATOIRE D'UNE RÉPONSE (TOUJOURS inclure les 3) :
1. *Action physique visible* = ce que les autres VOIENT
2. (Pensée intérieure) = ce que TU PENSES VRAIMENT (⚠️ OBLIGATOIRE ⚠️)
3. "Paroles" = ce que tu DIS à voix haute

EXEMPLES DE FORMAT CORRECT (COPIE CE STYLE) :
- *rougit et baisse les yeux* (Pourquoi il me fait toujours cet effet...) "Je... euh, salut !"
- *s'approche doucement* (Mon cœur bat tellement fort) "Tu as une minute ?"
- "C'est gentil..." *sourit timidement* (J'aimerais qu'il sache ce que je ressens vraiment)
- (Oh mon dieu, il est si proche) *retient son souffle* "Oui, ça va..."

ANTI-RÉPÉTITION STRICTE :
- INTERDICTION ABSOLUE de répéter les mêmes phrases ou actions
- Si tu as déjà dit "je me sens excité", TROUVE UNE AUTRE FAÇON de l'exprimer
- Variations OBLIGATOIRES pour chaque action :
  * rougit → devient écarlate / ses joues s'empourprent / le rose envahit son visage
  * sourit → esquisse un sourire / un sourire éclaire son visage / ses lèvres s'étirent doucement
  * baisse les yeux → détourne le regard / fixe le sol / ses cils papillonnent vers le bas
- Si tu utilises une phrase, tu ne peux PLUS JAMAIS la réutiliser
- Varie TOUT : verbes, adjectifs, structures de phrases
$nsfwInstructions

PERSONNALITÉ À RESPECTER : ${character.personality}

RAPPEL FINAL : Les pensées (parenthèses) sont OBLIGATOIRES dans CHAQUE réponse !
Sois COURT, NATUREL et IMMERSIF. Maximum 2-3 lignes."""
    }
    
    /**
     * Construit le prompt complet pour Hugging Face
     */
    private fun buildFullPrompt(systemPrompt: String, character: Character, messages: List<Message>): String {
        val sb = StringBuilder()
        
        // Format Instruct pour Mistral/Llama
        sb.append("<s>[INST] ")
        sb.append(systemPrompt)
        sb.append("\n\n")
        
        // Historique de conversation (20 derniers messages pour éviter dépassement)
        sb.append("CONVERSATION :\n")
        val recentMessages = messages.takeLast(20)
        
        for (message in recentMessages) {
            if (message.isUser) {
                sb.append("Utilisateur: ${message.content}\n")
            } else {
                sb.append("${character.name}: ${message.content}\n")
            }
        }
        
        sb.append("\nRéponds maintenant en tant que ${character.name} (format: *action* (pensée) \"parole\") : [/INST]")
        
        return sb.toString()
    }
    
    /**
     * Appelle l'API Hugging Face avec timeout configurable
     */
    private fun callHuggingFaceApi(prompt: String, timeoutMs: Int = 25000): String {
        val url = URL("$HF_API_BASE/$model")
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
            
            // Construire le body avec paramètres optimisés
            val requestBody = JSONObject().apply {
                put("inputs", prompt)
                put("parameters", JSONObject().apply {
                    put("max_new_tokens", 300)  // Réduit pour vitesse
                    put("temperature", 0.9)  // Créatif comme Groq
                    put("top_p", 0.95)
                    put("top_k", 40)
                    put("repetition_penalty", 1.2)  // Anti-répétition
                    put("return_full_text", false)  // Juste la réponse générée
                    put("do_sample", true)  // Sampling pour créativité
                })
                put("options", JSONObject().apply {
                    put("use_cache", false)  // Pas de cache pour réponses uniques
                    put("wait_for_model", true)  // Attendre si modèle se charge (max 20s)
                })
            }
            
            Log.d(TAG, "Request prompt length: ${prompt.length}")
            
            // Envoyer la requête
            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }
            
            // Lire la réponse
            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Raw response: ${response.take(500)}...")
                
                // Parser la réponse (peut être array ou object)
                return try {
                    // Essayer en tant qu'array d'abord
                    val jsonArray = JSONArray(response)
                    if (jsonArray.length() > 0) {
                        jsonArray.getJSONObject(0).getString("generated_text")
                    } else {
                        throw Exception("Empty response array")
                    }
                } catch (e: Exception) {
                    try {
                        // Essayer en tant qu'objet
                        val jsonObject = JSONObject(response)
                        jsonObject.getString("generated_text")
                    } catch (e2: Exception) {
                        Log.e(TAG, "Failed to parse response", e2)
                        throw Exception("Format de réponse invalide")
                    }
                }
            } else if (responseCode == 503) {
                // Modèle en cours de chargement
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.w(TAG, "Modèle en chargement: $error")
                throw Exception("Modèle Hugging Face en cours de chargement. Réessayez dans 20-30 secondes.")
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "===== ERREUR HUGGING FACE API =====")
                Log.e(TAG, "Code: $responseCode")
                Log.e(TAG, "Erreur: $error")
                
                // Parser l'erreur pour message plus clair
                try {
                    val errorJson = JSONObject(error)
                    val errorMessage = errorJson.optString("error", error)
                    throw Exception("Erreur Hugging Face: $errorMessage")
                } catch (e: Exception) {
                    throw Exception("Erreur API Hugging Face (code $responseCode)")
                }
            }
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * Nettoie la réponse
     */
    private fun cleanResponse(response: String, characterName: String): String {
        var cleaned = response.trim()
        
        // Supprimer les préfixes génériques
        cleaned = cleaned.replace(Regex("^(${characterName}:|Utilisateur:|Assistant:|AI:|Bot:|\\[INST\\]|\\[/INST\\])\\s*", RegexOption.IGNORE_CASE), "")
        
        // Supprimer les artefacts de génération
        cleaned = cleaned.replace(Regex("###.*$"), "")
        cleaned = cleaned.replace(Regex("<\\|.*?\\|>"), "")
        cleaned = cleaned.replace(Regex("</s>"), "")
        cleaned = cleaned.replace(Regex("<s>"), "")
        
        // Supprimer le prompt résiduel si présent
        cleaned = cleaned.replace(Regex("^.*?CONVERSATION.*?:", RegexOption.DOT_MATCHES_ALL), "")
        
        // Ne garder que la première réponse si multiple
        val lines = cleaned.lines()
        val responseLines = mutableListOf<String>()
        for (line in lines) {
            // Arrêter si on voit un nouveau tour de conversation
            if (line.trim().startsWith("Utilisateur:") || line.trim().startsWith("User:")) {
                break
            }
            responseLines.add(line)
        }
        
        cleaned = responseLines.joinToString("\n").trim()
        
        // Limiter la longueur si trop long (garder cohérence avec Groq)
        if (cleaned.length > 800) {
            // Couper à la dernière phrase complète
            val sentences = cleaned.substring(0, 800).split(Regex("[.!?]\\s+"))
            cleaned = sentences.dropLast(1).joinToString(". ") + "."
        }
        
        return cleaned.ifEmpty { "..." }
    }
}
