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
    
    // Gestion simple de la rotation des clés Groq
    private var currentGroqKeyIndex = 0
    private val failedGroqKeys = mutableSetOf<String>()

    // IMPORTANT: conserver une instance llama.cpp pour éviter de recharger le GGUF à chaque message
    private val llamaEngine: LlamaCppEngine by lazy { LlamaCppEngine(context) }
    private var llamaEnginePath: String? = null
    
    companion object {
        private const val TAG = "AIOrchestrator"
    }

    private fun getOrConfigureLlamaEngine(modelPath: String?): LlamaCppEngine {
        if (modelPath.isNullOrBlank()) {
            // L'appelant gère l'erreur "GGUF non configuré"
            return llamaEngine
        }
        if (llamaEnginePath != modelPath) {
            llamaEngine.setModelPath(modelPath)
            llamaEnginePath = modelPath
        }
        return llamaEngine
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
        
        fun isUsable(engine: AIEngine): Boolean {
            return when (engine) {
                AIEngine.GROQ -> config.groqApiKey?.isNotBlank() == true
                AIEngine.LLAMA_CPP -> !config.llamaCppModelPath.isNullOrBlank()
            }
        }

        var primaryError: Exception? = null

        // Essayer le moteur principal
        try {
            if (!isUsable(config.primaryEngine)) {
                throw Exception(
                    when (config.primaryEngine) {
                        AIEngine.GROQ -> "Aucune clé API Groq configurée."
                        AIEngine.LLAMA_CPP -> "Aucun modèle GGUF sélectionné pour llama.cpp."
                    }
                )
            }
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
            primaryError = e
            Log.w(TAG, "⚠️ Échec moteur principal (${config.primaryEngine.name}): ${e.message}")
            
            if (!config.enableFallbacks) {
                throw e
            }
        }
        
        // Cascade de fallbacks
        val fallbackEngines = getFallbackCascade(config.primaryEngine)
            .filter { isUsable(it) }
        
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

        // Aucun fallback utilisable -> remonter l'erreur primaire (important pour Groq sans GGUF)
        throw primaryError ?: Exception("Aucun moteur IA utilisable (Groq clé manquante et/ou GGUF non configuré).")
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
                // Parser les clés (peuvent être séparées par virgules)
                val keysString = config.groqApiKey ?: ""
                Log.d(TAG, "📥 Clés Groq brutes reçues: ${if (keysString.isBlank()) "(vide)" else "'${keysString.take(50)}...'"}")
                
                val apiKeys = keysString.split(",").map { it.trim() }.filter { it.isNotBlank() }
                
                if (apiKeys.isEmpty()) {
                    Log.e(TAG, "❌ ERREUR: Aucune clé Groq trouvée après parsing!")
                    throw Exception("Aucune clé API Groq configurée. Ajoutez vos clés dans les paramètres.")
                }
                
                Log.d(TAG, "📊 ${apiKeys.size} clé(s) Groq disponible(s) après parsing")
                apiKeys.forEachIndexed { i, key ->
                    Log.d(TAG, "   🔑 Clé ${i + 1}: ${key.take(20)}... (${key.length} caractères)")
                }
                
                val modelId = config.groqModelId ?: "llama-3.1-8b-instant"
                
                // Essayer chaque clé jusqu'à ce qu'une fonctionne
                var lastError: Exception? = null
                for (attempt in 0 until apiKeys.size) {
                    val keyIndex = (currentGroqKeyIndex + attempt) % apiKeys.size
                    val apiKey = apiKeys[keyIndex]
                    
                    // Ignorer les clés qui ont déjà échoué
                    if (failedGroqKeys.contains(apiKey)) {
                        Log.d(TAG, "⏭️ Clé ${keyIndex + 1} déjà en échec, skip")
                        continue
                    }
                    
                    try {
                        Log.d(TAG, "🔑 Essai avec clé ${keyIndex + 1}/${apiKeys.size}")
                        val groqEngine = GroqAIEngine(apiKey, modelId, config.nsfwMode)
                        val response = groqEngine.generateResponse(character, messages, username, userGender, memoryContext)
                        
                        // Succès ! Mettre à jour l'index
                        currentGroqKeyIndex = keyIndex
                        Log.i(TAG, "✅ Clé ${keyIndex + 1} fonctionne")
                        return response
                        
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Clé ${keyIndex + 1} échoue: ${e.message}")
                        
                        // Si rate limit, marquer comme échouée et essayer la suivante
                        if (e.message?.contains("429") == true || 
                            e.message?.contains("rate limit", ignoreCase = true) == true ||
                            e.message?.contains("Request too large", ignoreCase = true) == true) {
                            
                            failedGroqKeys.add(apiKey)
                            Log.w(TAG, "🚫 Clé ${keyIndex + 1} blacklistée (rate limit)")
                        }
                        
                        lastError = e
                    }
                }
                
                // Toutes les clés ont échoué
                throw lastError ?: Exception("Toutes les clés Groq ont échoué")
            }
            
            AIEngine.LLAMA_CPP -> {
                val engineInstance = getOrConfigureLlamaEngine(config.llamaCppModelPath)
                engineInstance.generateResponse(character, messages, username, userGender, memoryContext, config.nsfwMode)
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
                    val engineInstance = getOrConfigureLlamaEngine(config.llamaCppModelPath)
                    engineInstance.isAvailable()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur vérification disponibilité ${engine.name}", e)
            false
        }
    }
}
