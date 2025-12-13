package com.roleplayai.chatbot.data.ai

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.DeadObjectException
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Client (process app) -> Service (process :llama_native).
 * Si le process natif crash, la connexion tombe sans crasher l'app.
 */
class LlamaNativeClient(private val context: Context) {
    companion object {
        private const val TAG = "LlamaNativeClient"
        private const val CONNECT_TIMEOUT_MS = 2500L
        // Certaines générations (Phi) peuvent dépasser 45s sur mobile
        private const val CALL_TIMEOUT_MS = 90000L
    }

    private val appContext: Context = context.applicationContext

    @Volatile private var api: ILlamaNativeService? = null
    @Volatile private var connectDeferred: CompletableDeferred<ILlamaNativeService>? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val remote = service?.let { ILlamaNativeService.Stub.asInterface(it) }
            if (remote == null) {
                connectDeferred?.completeExceptionally(IllegalStateException("Binder invalide (AIDL)"))
                connectDeferred = null
                return
            }
            api = remote
            connectDeferred?.complete(remote)
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

    private suspend fun getApi(): ILlamaNativeService {
        api?.let { return it }

        // Déjà en cours de connexion ?
        connectDeferred?.let { return withTimeout(CONNECT_TIMEOUT_MS) { it.await() } }

        val deferred = CompletableDeferred<ILlamaNativeService>()
        connectDeferred = deferred

        val intent = Intent(appContext, LlamaNativeService::class.java)

        // Assurer le démarrage du service (certaines ROM sont capricieuses sur le bind direct).
        runCatching { appContext.startService(intent) }
            .onFailure { Log.w(TAG, "startService llama échoué: ${it.message}") }

        // Binder callbacks sont livrés sur le main thread; binder depuis le main évite des cas bizarres.
        val ok = withContext(Dispatchers.Main) {
            appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        if (!ok) {
            connectDeferred = null
            throw IllegalStateException(
                "Impossible de binder le service llama.cpp (:llama_native). " +
                    "Si tu utilises le modèle bundlé, installe l'AAB (pas l'APK). " +
                    "Sinon vérifie que l'appareil est arm64-v8a."
            )
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
            suspend fun callOnce(): String {
                val api = getApi()
                val loaded = api.loadModel(modelPath, threads, contextSize)
                if (!loaded) {
                    val reason = runCatching { api.getLastError() }.getOrNull().orEmpty().ifBlank {
                        "Échec chargement modèle local."
                    }
                    throw IllegalStateException(reason)
                }
                val res = api.generateChat(
                    roles.toTypedArray(),
                    contents.toTypedArray(),
                    maxTokens,
                    temperature,
                    topP,
                    topK,
                    repeatPenalty
                )
                // AIDL String peut être null: normaliser.
                return (res as String?) ?: ""
            }

            try {
                callOnce()
            } catch (e: DeadObjectException) {
                // Le process :llama_native est mort (OOM/SIGSEGV). Rebind + retry une fois.
                Log.w(TAG, "DeadObject (service mort) -> rebind + retry")
                api = null
                try {
                    callOnce()
                } catch (t: Throwable) {
                    throw IllegalStateException(
                        "Service llama.cpp indisponible (crash natif probable). Essayez un modèle plus petit (TinyLlama Q4) ou Groq.",
                        t
                    )
                }
            } catch (e: RemoteException) {
                throw IllegalStateException("Erreur IPC llama.cpp: ${e.message ?: "RemoteException"}", e)
            } catch (e: Throwable) {
                throw IllegalStateException("Erreur llama.cpp: ${e.message ?: e.javaClass.simpleName}", e)
            }
        }
    }
}

