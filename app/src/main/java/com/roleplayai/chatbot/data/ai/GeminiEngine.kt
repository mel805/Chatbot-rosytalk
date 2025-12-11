package com.roleplayai.chatbot.data.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * Moteur d'IA utilisant Google Gemini API (cloud)
 * 
 * Gemini offre :
 * - Excellente qualité (équivalent GPT-4)
 * - Très cohérent dans les conversations
 * - Support contexte long (32k tokens)
 * - API gratuite avec quotas généreux
 * - Support NSFW modéré
 * 
 * Obtenir une clé API gratuite :
 * https://makersuite.google.com/app/apikey
 */
class GeminiEngine(
    private val apiKey: String,
    private val model: String = "gemini-pro",
    private val nsfwMode: Boolean = false
) {
    
    companion object {
        private const val TAG = "GeminiEngine"
        
        // Modèles Gemini disponibles
        val AVAILABLE_MODELS = listOf(
            GeminiModel(
                id = "gemini-pro",
                name = "Gemini Pro",
                description = "Haute qualité, rapide",
                contextLength = 32768,
                recommended = true
            ),
            GeminiModel(
                id = "gemini-1.5-pro",
                name = "Gemini 1.5 Pro",
                description = "Dernière version, contexte 1M",
                contextLength = 1048576,
                recommended = false
            ),
            GeminiModel(
                id = "gemini-1.5-flash",
                name = "Gemini 1.5 Flash",
                description = "Ultra-rapide, léger",
                contextLength = 32768,
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
    
    private val generativeModel: GenerativeModel = GenerativeModel(
        modelName = model,
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.9f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 500
        }
    )
    
    /**
     * Génère une réponse
     */
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>,
        username: String = "Utilisateur",
        userGender: String = "neutre",
        memoryContext: String = ""
    ): String = withContext(Dispatchers.IO) {
        
        try {
            Log.d(TAG, "🚀 Génération avec Gemini ($model)")
            
            // Construire le prompt
            val systemPrompt = buildSystemPrompt(character, username, userGender, memoryContext)
            val conversationHistory = buildConversationHistory(messages, username, character.name)
            
            val fullPrompt = """$systemPrompt

$conversationHistory

${character.name}:"""
            
            Log.d(TAG, "📝 Prompt: ${fullPrompt.take(200)}...")
            
            // Générer avec Gemini
            val response = generativeModel.generateContent(fullPrompt)
            val generatedText = response.text ?: throw Exception("Réponse vide de Gemini")
            
            Log.i(TAG, "✅ Réponse générée: ${generatedText.take(100)}...")
            
            return@withContext generatedText.trim()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur Gemini", e)
            throw Exception("Gemini API erreur: ${e.message}")
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
3. Sois créatif et détaillé dans tes descriptions
4. Maintiens la cohérence de ta personnalité
5. Fais évoluer la relation naturellement
6. N'utilise JAMAIS les pensées de ${username}

👤 TON PARTENAIRE:
- Nom: $username
- Genre: $userGender$nsfwInstructions$memorySection"""
    }
    
    /**
     * Construit l'historique de conversation
     */
    private fun buildConversationHistory(
        messages: List<Message>,
        username: String,
        characterName: String
    ): String {
        val history = StringBuilder()
        
        messages.takeLast(20).forEach { msg ->
            val speaker = if (msg.isUser) username else characterName
            history.append("$speaker: ${msg.content}\n\n")
        }
        
        return history.toString().trim()
    }
    
    /**
     * Vérifie si le moteur est disponible
     */
    fun isAvailable(): Boolean {
        return apiKey.isNotBlank()
    }
}
