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
        GEMINI_NANO,    // Gemini Nano (on-device, Android 14+)
        LLAMA_CPP,      // llama.cpp (modèles GGUF locaux)
        TOGETHER_AI,    // Together AI (API gratuite)
        SMART_LOCAL;    // SmartLocalAI (templates intelligents, fallback ultime)
        
        fun getDisplayName(): String = when(this) {
            GROQ -> "Groq API (Cloud)"
            GEMINI_NANO -> "Gemini Nano (Local)"
            LLAMA_CPP -> "llama.cpp (Local)"
            TOGETHER_AI -> "Together AI (Cloud)"
            SMART_LOCAL -> "IA Locale Simple"
        }
        
        fun getDescription(): String = when(this) {
            GROQ -> "Réponses ultra-rapides (1-2s), qualité maximale. Nécessite Internet."
            GEMINI_NANO -> "IA Google locale. Android 14+ uniquement. Excellente qualité."
            LLAMA_CPP -> "Modèles locaux GGUF. Phi-3, Gemma, TinyLlama, etc."
            TOGETHER_AI -> "API gratuite de secours. Qualité correcte."
            SMART_LOCAL -> "Réponses locales basées sur templates. Toujours disponible."
        }
        
        fun isLocal(): Boolean = when(this) {
            GROQ, TOGETHER_AI -> false
            GEMINI_NANO, LLAMA_CPP, SMART_LOCAL -> true
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
        
        // Dernier recours absolu : SmartLocalAI (ne peut pas échouer)
        Log.w(TAG, "🆘 Utilisation fallback ultime: SmartLocalAI")
        val smartAI = SmartLocalAI(context, character, character.id, config.nsfwMode)
        val response = smartAI.generateResponse(
            messages.lastOrNull { it.isUser }?.content ?: "",
            messages,
            username
        )
        
        val duration = System.currentTimeMillis() - startTime
        
        return@withContext GenerationResult(
            response = response,
            usedEngine = AIEngine.SMART_LOCAL,
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
            
            AIEngine.GEMINI_NANO -> {
                val geminiEngine = GeminiNanoEngine(context, config.nsfwMode)
                if (!geminiEngine.isAvailable()) {
                    throw Exception("Gemini Nano non disponible (nécessite Android 14+)")
                }
                geminiEngine.generateResponse(character, messages, username, userGender, memoryContext)
            }
            
            AIEngine.LLAMA_CPP -> {
                val modelPath = config.llamaCppModelPath 
                    ?: throw Exception("Chemin du modèle llama.cpp manquant")
                
                val llamaEngine = LlamaCppEngine(context, modelPath, config.nsfwMode)
                
                // Charger le modèle si pas déjà chargé
                if (!llamaEngine.isModelLoaded()) {
                    Log.d(TAG, "Chargement du modèle llama.cpp...")
                    val loaded = llamaEngine.loadModel()
                    if (!loaded) {
                        throw Exception("Échec du chargement du modèle llama.cpp")
                    }
                }
                
                llamaEngine.generateResponse(character, messages, username, userGender, memoryContext)
            }
            
            AIEngine.TOGETHER_AI -> {
                val togetherEngine = TogetherAIEngine(
                    apiKey = "",  // Gratuit
                    model = "mistralai/Mistral-7B-Instruct-v0.2",
                    nsfwMode = config.nsfwMode
                )
                togetherEngine.generateResponse(character, messages, username, userGender, memoryContext)
            }
            
            AIEngine.SMART_LOCAL -> {
                val smartAI = SmartLocalAI(context, character, character.id, config.nsfwMode)
                smartAI.generateResponse(
                    messages.lastOrNull { it.isUser }?.content ?: "",
                    messages,
                    username
                )
            }
        }
    }
    
    /**
     * Détermine la cascade de fallbacks selon le moteur principal
     */
    private fun getFallbackCascade(primaryEngine: AIEngine): List<AIEngine> {
        return when (primaryEngine) {
            AIEngine.GROQ -> listOf(
                AIEngine.TOGETHER_AI,
                AIEngine.GEMINI_NANO,
                AIEngine.LLAMA_CPP
            )
            AIEngine.GEMINI_NANO -> listOf(
                AIEngine.LLAMA_CPP,
                AIEngine.TOGETHER_AI
            )
            AIEngine.LLAMA_CPP -> listOf(
                AIEngine.GEMINI_NANO,
                AIEngine.TOGETHER_AI
            )
            AIEngine.TOGETHER_AI -> listOf(
                AIEngine.GEMINI_NANO,
                AIEngine.LLAMA_CPP
            )
            AIEngine.SMART_LOCAL -> emptyList()
        }
    }
    
    /**
     * Vérifie si un moteur est disponible sur cet appareil
     */
    suspend fun isEngineAvailable(engine: AIEngine, config: GenerationConfig): Boolean {
        return try {
            when (engine) {
                AIEngine.GROQ -> config.groqApiKey?.isNotBlank() == true
                
                AIEngine.GEMINI_NANO -> {
                    val geminiEngine = GeminiNanoEngine(context, false)
                    geminiEngine.isAvailable()
                }
                
                AIEngine.LLAMA_CPP -> {
                    val llamaEngine = LlamaCppEngine(context, "", false)
                    llamaEngine.getAvailableModels().isNotEmpty()
                }
                
                AIEngine.TOGETHER_AI -> true  // Toujours disponible (gratuit)
                AIEngine.SMART_LOCAL -> true  // Toujours disponible
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur vérification disponibilité ${engine.name}", e)
            false
        }
    }
}
