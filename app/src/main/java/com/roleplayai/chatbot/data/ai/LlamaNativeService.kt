package com.roleplayai.chatbot.data.ai

import android.app.Service
import android.content.Intent
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
    // Ne pas appeler ça "lastError" : AIDL génère un getter getLastError() et Kotlin crée
    // une propriété synthétique `lastError` (val) dans le Stub -> conflit d'assignation.
    @Volatile private var lastErrorMessage: String = ""

    private val binder = object : ILlamaNativeService.Stub() {
        override fun isLoaded(): Boolean = engine.isModelLoaded()

        override fun loadModel(modelPath: String, threads: Int, contextSize: Int): Boolean {
            lastErrorMessage = ""
            // Si un autre modèle est déjà chargé, on décharge pour éviter une accumulation mémoire.
            val shouldReload =
                loadedModelPath != modelPath ||
                    loadedThreads != threads ||
                    loadedCtx != contextSize ||
                    !engine.isModelLoaded()

            if (shouldReload && engine.isModelLoaded()) {
                runCatching { engine.unloadModel() }.onFailure {
                    lastErrorMessage = "Erreur unload modèle: ${it.message ?: it.javaClass.simpleName}"
                }
            }

            val ok = runCatching { engine.ensureModelLoaded(modelPath, threads, contextSize) }
                .onFailure { lastErrorMessage = "Erreur chargement modèle: ${it.message ?: it.javaClass.simpleName}" }
                .getOrDefault(false)
            if (ok) {
                loadedModelPath = modelPath
                loadedThreads = threads
                loadedCtx = contextSize
            } else if (lastErrorMessage.isBlank()) {
                lastErrorMessage = "Échec chargement modèle (lib native indisponible, fichier invalide ou mémoire insuffisante)."
            }
            return ok
        }

        override fun getLastError(): String = lastErrorMessage

        override fun generateChat(
            roles: Array<out String>,
            contents: Array<out String>,
            maxTokens: Int,
            temperature: Float,
            topP: Float,
            topK: Int,
            repeatPenalty: Float
        ): String {
            lastErrorMessage = ""
            return runCatching {
                engine.generateChat(
                    roles = roles.toList(),
                    contents = contents.toList(),
                    maxTokens = maxTokens,
                    temperature = temperature,
                    topP = topP,
                    topK = topK,
                    repeatPenalty = repeatPenalty
                )
            }.onFailure {
                lastErrorMessage = "Erreur génération: ${it.message ?: it.javaClass.simpleName}"
            }.getOrDefault("")
        }
    }

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

