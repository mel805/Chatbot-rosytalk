package com.roleplayai.chatbot.data.ai

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

/**
 * Client (process app) -> Service (process :llama_native).
 * Si le process natif crash, la connexion tombe sans crasher l'app.
 */
class LlamaNativeClient(private val context: Context) {
    companion object {
        private const val TAG = "LlamaNativeClient"
        private const val CONNECT_TIMEOUT_MS = 1500L
        // Certaines générations (Phi) peuvent dépasser 45s sur mobile
        private const val CALL_TIMEOUT_MS = 90000L
    }

    @Volatile private var api: LlamaNativeService.Api? = null
    @Volatile private var connectDeferred: CompletableDeferred<LlamaNativeService.Api>? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? LlamaNativeService.Api
            if (binder == null) {
                connectDeferred?.completeExceptionally(IllegalStateException("Binder invalide"))
                connectDeferred = null
                return
            }
            api = binder
            connectDeferred?.complete(binder)
            connectDeferred = null
            Log.d(TAG, "Service connecté")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "Service déconnecté (crash natif probable)")
            api = null
        }

        override fun onBindingDied(name: ComponentName?) {
            Log.w(TAG, "Binding died")
            api = null
        }

        override fun onNullBinding(name: ComponentName?) {
            Log.e(TAG, "Null binding")
            api = null
        }
    }

    private suspend fun getApi(): LlamaNativeService.Api {
        api?.let { return it }

        // Déjà en cours de connexion ?
        connectDeferred?.let { return withTimeout(CONNECT_TIMEOUT_MS) { it.await() } }

        val deferred = CompletableDeferred<LlamaNativeService.Api>()
        connectDeferred = deferred

        val ok = context.bindService(
            Intent(context, LlamaNativeService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )

        if (!ok) {
            connectDeferred = null
            throw IllegalStateException("Impossible de binder le service llama")
        }

        return withTimeout(CONNECT_TIMEOUT_MS) { deferred.await() }
    }

    suspend fun generateChat(
        modelPath: String,
        threads: Int,
        contextSize: Int,
        roles: List<String>,
        contents: List<String>,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int,
        repeatPenalty: Float
    ): String {
        return withTimeout(CALL_TIMEOUT_MS) {
            val api = getApi()
            try {
                val loaded = api.loadModel(modelPath, threads, contextSize)
                if (!loaded) return@withTimeout ""
                api.generateChat(roles, contents, maxTokens, temperature, topP, topK, repeatPenalty)
            } catch (e: RemoteException) {
                Log.e(TAG, "RemoteException: ${e.message}")
                ""
            } catch (e: Throwable) {
                Log.e(TAG, "Erreur appel service: ${e.message}")
                ""
            }
        }
    }
}

