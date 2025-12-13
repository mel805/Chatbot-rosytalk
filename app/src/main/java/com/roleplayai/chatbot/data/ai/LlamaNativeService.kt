package com.roleplayai.chatbot.data.ai

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

/**
 * Service exécuté dans un process séparé pour isoler llama.cpp.
 * Objectif: si le natif crash (SIGSEGV/OOM), l'app ne tombe pas.
 */
class LlamaNativeService : Service() {
    companion object {
        private const val TAG = "LlamaNativeService"
    }

    private val engine = LocalAIEngine()

    private var loadedModelPath: String? = null
    private var loadedThreads: Int = 0
    private var loadedCtx: Int = 0

    inner class Api : Binder() {
        fun isLoaded(): Boolean = engine.isModelLoaded()

        fun loadModel(modelPath: String, threads: Int, contextSize: Int): Boolean {
            // Si un autre modèle est déjà chargé, on décharge pour éviter une accumulation mémoire.
            val shouldReload = loadedModelPath != modelPath || loadedThreads != threads || loadedCtx != contextSize || !engine.isModelLoaded()
            if (shouldReload && engine.isModelLoaded()) {
                engine.unloadModel()
            }
            val ok = engine.ensureModelLoaded(modelPath, threads, contextSize)
            if (ok) {
                loadedModelPath = modelPath
                loadedThreads = threads
                loadedCtx = contextSize
            }
            return ok
        }

        fun generateChat(
            roles: List<String>,
            contents: List<String>,
            maxTokens: Int,
            temperature: Float,
            topP: Float,
            topK: Int,
            repeatPenalty: Float
        ): String {
            return engine.generateChat(
                roles = roles,
                contents = contents,
                maxTokens = maxTokens,
                temperature = temperature,
                topP = topP,
                topK = topK,
                repeatPenalty = repeatPenalty
            )
        }
    }

    private val binder = Api()

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind()")
        return binder
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() -> unload")
        runCatching { engine.unloadModel() }
        super.onDestroy()
    }
}

