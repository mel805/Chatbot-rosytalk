package com.roleplayai.chatbot.data.ai

import android.util.Log
import java.io.File

/**
 * Pont Kotlin -> JNI pour llama.cpp.
 *
 * IMPORTANT:
 * - Les noms des méthodes JNI doivent correspondre à `app/src/main/cpp/jni_interface.cpp`
 *   (Java_com_roleplayai_chatbot_data_ai_LocalAIEngine_*)
 * - La librairie chargée est celle produite par CMake (voir `CMakeLists.txt` racine).
 */
class LocalAIEngine(
    // Conserver constructeur simple (pas de dépendance Android nécessaire)
    @Suppress("unused") private val unused: Any? = null
) {
    companion object {
        private const val TAG = "LocalAIEngine"

        // CMake crée `libroleplay-ai-native.so` (project("roleplay-ai-native"))
        private const val NATIVE_LIB_NAME = "roleplay-ai-native"

        @Volatile
        private var libraryLoaded: Boolean = false

        private val loadLock = Any()
    }

    private external fun nativeLoadModel(modelPath: String, threads: Int, contextSize: Int): Boolean
    private external fun nativeGenerate(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int,
        repeatPenalty: Float
    ): String

    private external fun nativeGenerateChat(
        roles: Array<String>,
        contents: Array<String>,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int,
        repeatPenalty: Float
    ): String
    private external fun nativeUnloadModel()
    private external fun nativeIsLoaded(): Boolean

    init {
        ensureNativeLoaded()
    }

    fun isNativeAvailable(): Boolean = libraryLoaded

    fun isModelLoaded(): Boolean = runCatching { nativeIsLoaded() }.getOrDefault(false)

    fun unloadModel() {
        if (!libraryLoaded) return
        runCatching { nativeUnloadModel() }
            .onFailure { Log.w(TAG, "Erreur unloadModel: ${it.message}") }
    }

    /**
     * Charge le modèle si nécessaire.
     *
     * @return true si le modèle est chargé après l'appel.
     */
    fun ensureModelLoaded(modelPath: String, threads: Int, contextSize: Int): Boolean {
        if (!ensureNativeLoaded()) return false

        val f = File(modelPath)
        if (!f.exists() || !f.isFile) {
            Log.e(TAG, "Modèle introuvable: $modelPath")
            return false
        }

        // Si déjà chargé, on garde (JNI global context unique).
        if (isModelLoaded()) return true

        Log.i(TAG, "Chargement modèle natif: ${f.name} (${f.length() / (1024 * 1024)} MB)")
        val ok = runCatching { nativeLoadModel(modelPath, threads, contextSize) }.getOrDefault(false)
        Log.i(TAG, if (ok) "✅ Modèle chargé" else "❌ Échec chargement modèle")
        return ok
    }

    fun generate(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int,
        repeatPenalty: Float
    ): String {
        if (!libraryLoaded) return ""
        if (!isModelLoaded()) return ""
        return runCatching {
            nativeGenerate(
                prompt = prompt,
                maxTokens = maxTokens,
                temperature = temperature,
                topP = topP,
                topK = topK,
                repeatPenalty = repeatPenalty
            )
        }.getOrDefault("")
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
        if (!libraryLoaded) return ""
        if (!isModelLoaded()) return ""
        if (roles.isEmpty() || roles.size != contents.size) return ""

        return runCatching {
            nativeGenerateChat(
                roles = roles.toTypedArray(),
                contents = contents.toTypedArray(),
                maxTokens = maxTokens,
                temperature = temperature,
                topP = topP,
                topK = topK,
                repeatPenalty = repeatPenalty
            )
        }.getOrDefault("")
    }

    private fun ensureNativeLoaded(): Boolean {
        if (libraryLoaded) return true
        synchronized(loadLock) {
            if (libraryLoaded) return true
            return try {
                System.loadLibrary(NATIVE_LIB_NAME)
                libraryLoaded = true
                Log.i(TAG, "✅ Bibliothèque native chargée: $NATIVE_LIB_NAME")
                true
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "⚠️ Bibliothèque native indisponible: ${e.message}")
                libraryLoaded = false
                false
            }
        }
    }
}

