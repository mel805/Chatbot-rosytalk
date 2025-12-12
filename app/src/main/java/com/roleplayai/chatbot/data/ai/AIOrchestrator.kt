package com.roleplayai.chatbot.data.ai

import android.content.Context
import android.util.Log
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AI Orchestrator - Routeur intelligent des moteurs d'IA
 * 
 * Gère automatiquement la cascade des moteurs d'IA :
 * 1. Moteur principal (Groq / Gemini Nano / llama.cpp selon config)
 * 2. Fallbacks automatiques si échec
 * 3. SmartLocalAI en dernier recours (ne peut jamais échouer)
 * 
 * Architecture :
 * - AIEngine : Enum des moteurs disponibles
 * - Cascade intelligente avec fallbacks
 * - Logs détaillés pour debug
 * - Support NSFW sur tous les moteurs
 */
class AIOrchestrator(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "AIOrchestrator"
    }
    
    /**
     * Moteurs d'IA disponibles
     */
    enum class AIEngine {
        GROQ,           // API Groq (ultra-rapide, cloud)
        GEMINI,         // Google Gemini (cloud, excellente qualité)
        LLAMA_CPP;      // llama.cpp (modèles GGUF locaux)
        
        fun getDisplayName(): String = when(this) {
            GROQ -> "Groq API (Cloud)"
            GEMINI -> "Google Gemini (Cloud)"
            LLAMA_CPP -> "llama.cpp (Local)"
        }
        
        fun getDescription(): String = when(this) {
            GROQ -> "Ultra-rapide (1-2s), excellente qualité. Nécessite clé API gratuite."
            GEMINI -> "Intelligence Google de haute qualité. Très cohérent. Nécessite clé API."
            LLAMA_CPP -> "Modèles locaux GGUF. 100% privé, fonctionne hors-ligne."
        }
        
        fun isLocal(): Boolean = when(this) {
            GROQ, GEMINI -> false
            LLAMA_CPP -> true
        }
        
        fun requiresInternet(): Boolean = !isLocal()
    }
    
    /**
     * Configuration de génération
     */
    data class GenerationConfig(
        val primaryEngine: AIEngine,
        val enableFallbacks: Boolean = true,
        val nsfwMode: Boolean = false,
        val groqApiKey: String? = null,
        val groqModelId: String? = null,
        val geminiApiKey: String? = null,
        val geminiModelId: String? = null,
        val llamaCppModelPath: String? = null
    )
    
    /**
     * Résultat de génération
     */
    data class GenerationResult(
        val response: String,
        val usedEngine: AIEngine,
        val generationTimeMs: Long,
        val hadFallback: Boolean
    )
    
    /**
     * Génère une réponse avec le moteur configuré et fallbacks automatiques
     */
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>,
        username: String,
        userGender: String,
        memoryContext: String,
        config: GenerationConfig
    ): GenerationResult = withContext(Dispatchers.IO) {
        
        Log.i(TAG, "===== AI Orchestrator =====")
        Log.d(TAG, "Moteur principal: ${config.primaryEngine.getDisplayName()}")
        Log.d(TAG, "Fallbacks: ${config.enableFallbacks}, NSFW: ${config.nsfwMode}")
        
        val startTime = System.currentTimeMillis()
        
        // Essayer le moteur principal
        try {
            val response = generateWithEngine(
                engine = config.primaryEngine,
                character = character,
                messages = messages,
                username = username,
                userGender = userGender,
                memoryContext = memoryContext,
                config = config
            )
            
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "✅ Succès avec ${config.primaryEngine.name} en ${duration}ms")
            
            return@withContext GenerationResult(
                response = response,
                usedEngine = config.primaryEngine,
                generationTimeMs = duration,
                hadFallback = false
            )
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Échec moteur principal (${config.primaryEngine.name}): ${e.message}")
            
            if (!config.enableFallbacks) {
                throw e
            }
        }
        
        // Cascade de fallbacks
        val fallbackEngines = getFallbackCascade(config.primaryEngine)
        
        for (fallbackEngine in fallbackEngines) {
            try {
                Log.d(TAG, "🔄 Tentative fallback: ${fallbackEngine.getDisplayName()}")
                
                val response = generateWithEngine(
                    engine = fallbackEngine,
                    character = character,
                    messages = messages,
                    username = username,
                    userGender = userGender,
                    memoryContext = memoryContext,
                    config = config
                )
                
                val duration = System.currentTimeMillis() - startTime
                Log.i(TAG, "✅ Succès avec fallback ${fallbackEngine.name} en ${duration}ms")
                
                return@withContext GenerationResult(
                    response = response,
                    usedEngine = fallbackEngine,
                    generationTimeMs = duration,
                    hadFallback = true
                )
                
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Échec fallback ${fallbackEngine.name}: ${e.message}")
            }
        }
        
        // Dernier recours : llama.cpp en mode Kotlin pur (ne peut jamais échouer)
        Log.w(TAG, "🆘 Fallback ultime: llama.cpp (générateur intelligent Kotlin)")
        val llamaEngine = LlamaCppEngine(context)
        if (config.llamaCppModelPath != null) {
            llamaEngine.setModelPath(config.llamaCppModelPath)
        }
        val response = llamaEngine.generateResponse(character, messages, username, userGender, memoryContext, config.nsfwMode)
        val duration = System.currentTimeMillis() - startTime
        
        return@withContext GenerationResult(
            response = response,
            usedEngine = AIEngine.LLAMA_CPP,
            generationTimeMs = duration,
            hadFallback = true
        )
    }
    
    /**
     * Génère avec un moteur spécifique
     */
    private suspend fun generateWithEngine(
        engine: AIEngine,
        character: Character,
        messages: List<Message>,
        username: String,
        userGender: String,
        memoryContext: String,
        config: GenerationConfig
    ): String {
        return when (engine) {
            AIEngine.GROQ -> {
                val apiKey = config.groqApiKey ?: throw Exception("Clé API Groq manquante")
                val modelId = config.groqModelId ?: "llama-3.1-8b-instant"
                
                val groqEngine = GroqAIEngine(apiKey, modelId, config.nsfwMode)
                groqEngine.generateResponse(character, messages, username, userGender, memoryContext)
            }
            
            AIEngine.GEMINI -> {
                val apiKey = config.geminiApiKey ?: throw Exception("Clé API Gemini manquante")
                val modelId = config.geminiModelId ?: "gemini-pro"
                
                val geminiEngine = GeminiEngine(apiKey, modelId, config.nsfwMode)
                geminiEngine.generateResponse(character, messages, username, userGender, memoryContext)
            }
            
            AIEngine.LLAMA_CPP -> {
                val llamaEngine = LlamaCppEngine(context)
                if (config.llamaCppModelPath != null) {
                    llamaEngine.setModelPath(config.llamaCppModelPath)
                }
                llamaEngine.generateResponse(character, messages, username, userGender, memoryContext, config.nsfwMode)
            }
            
        }
    }
    
    /**
     * Détermine la cascade de fallbacks selon le moteur principal
     */
    private fun getFallbackCascade(primaryEngine: AIEngine): List<AIEngine> {
        return when (primaryEngine) {
            AIEngine.GROQ -> listOf(
                AIEngine.GEMINI,
                AIEngine.LLAMA_CPP
            )
            AIEngine.GEMINI -> listOf(
                AIEngine.GROQ,
                AIEngine.LLAMA_CPP
            )
            AIEngine.LLAMA_CPP -> listOf(
                AIEngine.GROQ,
                AIEngine.GEMINI
            )
        }
    }
    
    /**
     * Vérifie si un moteur est disponible sur cet appareil
     */
    suspend fun isEngineAvailable(engine: AIEngine, config: GenerationConfig): Boolean {
        return try {
            when (engine) {
                AIEngine.GROQ -> config.groqApiKey?.isNotBlank() == true
                
                AIEngine.GEMINI -> config.geminiApiKey?.isNotBlank() == true
                
                AIEngine.LLAMA_CPP -> {
                    val llamaEngine = LlamaCppEngine(context)
                    llamaEngine.isAvailable()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur vérification disponibilité ${engine.name}", e)
            false
        }
    }
}
