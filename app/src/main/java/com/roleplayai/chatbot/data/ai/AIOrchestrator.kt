package com.roleplayai.chatbot.data.ai

import android.content.Context
import android.util.Log
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message
import com.roleplayai.chatbot.data.manager.GroqKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AI Orchestrator - Routeur intelligent des moteurs d'IA
 * 
 * Gère automatiquement la cascade des moteurs d'IA :
 * 1. Moteur principal (Groq / llama.cpp selon config)
 * 2. Fallbacks automatiques si échec
 * 3. llama.cpp en dernier recours (ne peut jamais échouer)
 * 
 * Architecture :
 * - Rotation automatique des clés Groq (séparées par virgules)
 * - Cascade intelligente avec fallbacks
 * - Logs détaillés pour debug
 * - Support NSFW sur tous les moteurs
 */
class AIOrchestrator(
    private val context: Context
) {
    
    // Gestionnaire de clés Groq (rotation + blacklist persistantes)
    private val groqKeyManager = GroqKeyManager(context)
    
    companion object {
        private const val TAG = "AIOrchestrator"
    }
    
    /**
     * Moteurs d'IA disponibles
     */
    enum class AIEngine {
        GROQ,           // API Groq (ultra-rapide, cloud)
        LLAMA_CPP;      // llama.cpp (local, intelligent)
        
        fun getDisplayName(): String = when(this) {
            GROQ -> "Groq API (Cloud)"
            LLAMA_CPP -> "llama.cpp (Local)"
        }
        
        fun getDescription(): String = when(this) {
            GROQ -> "Ultra-rapide (1-2s), excellente qualité. Nécessite clé API gratuite. Supporte plusieurs clés séparées par virgules."
            LLAMA_CPP -> "IA locale intelligente. 100% privé, fonctionne hors-ligne. Réponses uniques et pertinentes."
        }
        
        fun isLocal(): Boolean = when(this) {
            GROQ -> false
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
        val groqApiKey: String? = null,  // Peut contenir plusieurs clés séparées par virgules
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
        
        // Dernier recours : llama.cpp en mode Kotlin pur (ne peut jamais échouer)
        Log.w(TAG, "🆘 Fallback ultime: llama.cpp (IA intelligente Kotlin)")
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
                val modelId = config.groqModelId ?: "llama-3.1-8b-instant"
                
                // 1) Utiliser la rotation persistante du GroqKeyManager si possible
                val totalKeys = groqKeyManager.getTotalKeysCount()
                if (totalKeys > 0) {
                    Log.d(TAG, "🔁 Rotation GroqKeyManager active (total: $totalKeys)")
                    var lastError: Exception? = null
                    
                    repeat(totalKeys) { attempt ->
                        val apiKey = groqKeyManager.getCurrentKey()
                        if (apiKey.isNullOrBlank()) {
                            lastError = Exception("Toutes les clés Groq sont temporairement indisponibles (rate limit).")
                            return@repeat
                        }
                        
                        try {
                            Log.d(TAG, "🔑 Essai Groq (attempt ${attempt + 1}/$totalKeys) avec index courant")
                            val groqEngine = GroqAIEngine(apiKey, modelId, config.nsfwMode)
                            return groqEngine.generateResponse(character, messages, username, userGender, memoryContext)
                        } catch (e: GroqAIEngine.GroqApiException) {
                            lastError = e
                            Log.w(TAG, "⚠️ Erreur Groq HTTP ${e.statusCode}: ${e.apiMessage}")
                            
                            if (e.statusCode == 429) {
                                // Rate limit: blacklist persistante + rotation automatique
                                groqKeyManager.markCurrentKeyAsRateLimited()
                            } else {
                                // Autre erreur (401/403/400/500...): passer à la clé suivante sans blacklister
                                groqKeyManager.rotateToNextKeyWithoutBlacklist()
                            }
                        } catch (e: Exception) {
                            lastError = e
                            Log.w(TAG, "⚠️ Erreur Groq (non-HTTP): ${e.message}")
                            groqKeyManager.rotateToNextKeyWithoutBlacklist()
                        }
                    }
                    
                    throw lastError ?: Exception("Toutes les clés Groq ont échoué")
                }
                
                // 2) Fallback compat: si aucune clé n'est stockée dans GroqKeyManager, parser la config
                val keysString = config.groqApiKey ?: ""
                val apiKeys = keysString.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (apiKeys.isEmpty()) {
                    throw Exception("Aucune clé API Groq configurée. Ajoutez vos clés dans les paramètres.")
                }
                
                var lastError: Exception? = null
                for (attempt in 0 until apiKeys.size) {
                    val apiKey = apiKeys[attempt]
                    try {
                        Log.d(TAG, "🔑 Essai Groq (fallback config) ${attempt + 1}/${apiKeys.size}")
                        val groqEngine = GroqAIEngine(apiKey, modelId, config.nsfwMode)
                        return groqEngine.generateResponse(character, messages, username, userGender, memoryContext)
                    } catch (e: Exception) {
                        lastError = e
                    }
                }
                
                throw lastError ?: Exception("Toutes les clés Groq ont échoué")
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
            AIEngine.GROQ -> listOf(AIEngine.LLAMA_CPP)
            AIEngine.LLAMA_CPP -> listOf(AIEngine.GROQ)
        }
    }
    
    /**
     * Vérifie si un moteur est disponible sur cet appareil
     */
    suspend fun isEngineAvailable(engine: AIEngine, config: GenerationConfig): Boolean {
        return try {
            when (engine) {
                AIEngine.GROQ -> config.groqApiKey?.isNotBlank() == true
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
