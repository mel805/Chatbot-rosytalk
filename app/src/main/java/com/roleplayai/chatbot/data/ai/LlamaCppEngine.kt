package com.roleplayai.chatbot.data.ai

import android.content.Context
import android.app.ActivityManager
import android.util.Log
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/**
 * Moteur llama.cpp (LOCAL) via JNI.
 *
 * IMPORTANT:
 * - Ce moteur utilise un VRAI modèle GGUF fourni par l'utilisateur (stocké dans /models).
 * - La lib native est compilée via NDK. En CI, les sources llama.cpp sont récupérées
 *   automatiquement (voir workflow) pour builder la lib.
 */
class LlamaCppEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "LlamaCppEngine"

        @Volatile private var nativeLibLoaded: Boolean = false
        private val executor = Executors.newSingleThreadExecutor { r ->
            Thread(r, "llama-cpp-infer").apply { isDaemon = true }
        }

        init {
            try {
                System.loadLibrary("llama-android")
                nativeLibLoaded = true
                Log.i(TAG, "✅ Bibliothèque native llama-android chargée")
            } catch (e: UnsatisfiedLinkError) {
                nativeLibLoaded = false
                Log.w(TAG, "⚠️ Bibliothèque native llama-android indisponible: ${e.message}")
            } catch (e: SecurityException) {
                nativeLibLoaded = false
                Log.w(TAG, "⚠️ Impossible de charger llama-android: ${e.message}")
            }
        }

        // JNI (voir app/src/main/cpp/llama-android.cpp)
        @JvmStatic private external fun loadModel(modelPath: String, nThreads: Int, nCtx: Int): Long
        @JvmStatic private external fun generateChat(
            contextPtr: Long,
            roles: Array<String>,
            contents: Array<String>,
            maxTokens: Int,
            temperature: Float,
            topP: Float,
            topK: Int,
            repeatPenalty: Float
        ): String
        @JvmStatic private external fun cancelGeneration(contextPtr: Long)
        @JvmStatic private external fun freeModel(contextPtr: Long)
        @JvmStatic private external fun isModelLoaded(contextPtr: Long): Boolean
    }
    
    private var modelPath: String? = null
    private var contextPtr: Long = 0L
    private var loadedModelPath: String? = null
    private var loadedNCtx: Int = 0
    private var loadedThreads: Int = 0
    
    fun setModelPath(path: String) {
        modelPath = path
        Log.i(TAG, "📁 Modèle configuré: $path")
    }
    
    fun isAvailable(): Boolean {
        val path = modelPath
        return nativeLibLoaded && path != null && File(path).exists()
    }

    private fun defaultThreads(): Int {
        // Sur mobile, trop de threads peut être contre-productif.
        val cpu = Runtime.getRuntime().availableProcessors()
        return max(1, minOf(4, cpu))
    }

    private fun chooseParamsForModel(modelFile: File): Pair<Int, Int> {
        val cpu = Runtime.getRuntime().availableProcessors()
        val sizeMb = (modelFile.length() / (1024 * 1024)).toInt().coerceAtLeast(1)

        val threads = when {
            sizeMb >= 3500 -> minOf(2, cpu) // ~7B Q4
            sizeMb >= 1500 -> minOf(3, cpu) // ~2-3B
            else -> minOf(4, cpu)          // ~1B
        }.coerceAtLeast(1)

        val nCtx = when {
            sizeMb >= 3500 -> 512
            sizeMb >= 1500 -> 640
            else -> 512
        }

        return threads to nCtx
    }

    private fun ensureLoadedOrThrow() {
        val path = modelPath ?: throw IllegalStateException("Aucun modèle GGUF configuré (Paramètres > llama.cpp)")
        val f = File(path)
        if (!f.exists()) throw IllegalStateException("Modèle GGUF introuvable: $path")
        if (!nativeLibLoaded) throw IllegalStateException("Lib native llama-android indisponible sur cet appareil/build")

        // Empêcher les crashes (OOM) sur modèles trop lourds en amont.
        // Heuristique: refuser si le GGUF dépasse ~35% de la RAM totale (la quantif + KV cache + overhead JNI/ART).
        val sizeBytes = f.length()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }
        val totalMem = mi.totalMem.takeIf { it > 0 } ?: 0L
        if (totalMem > 0) {
            val maxModelBytes = (totalMem * 0.35).toLong()
            if (sizeBytes > maxModelBytes) {
                val sizeMb = (sizeBytes / (1024 * 1024)).toInt()
                val ramMb = (totalMem / (1024 * 1024)).toInt()
                throw IllegalStateException("Ce GGUF (~${sizeMb} MB) est trop lourd pour la RAM de cet appareil (~${ramMb} MB) et peut faire crasher l’application. Utilise TinyLlama/Phi-2 (Q4).")
            }
        }

        if (contextPtr != 0L && isModelLoaded(contextPtr) && loadedModelPath == path) {
            return
        }

        // Charger / recharger le modèle
        if (contextPtr != 0L) {
            try {
                freeModel(contextPtr)
            } catch (e: Throwable) {
                Log.w(TAG, "⚠️ freeModel a échoué (on continue): ${e.message}")
            } finally {
                contextPtr = 0L
            }
        }

        val (threads, nCtx) = chooseParamsForModel(f)
        loadedThreads = threads
        loadedNCtx = nCtx

        Log.i(TAG, "🦙 Chargement GGUF: ${f.name} (~${f.length() / (1024 * 1024)} MB), threads=$threads, n_ctx=$nCtx")
        contextPtr = loadModel(path, threads, nCtx)
        if (contextPtr == 0L || !isModelLoaded(contextPtr)) {
            contextPtr = 0L
            throw IllegalStateException("Échec chargement modèle llama.cpp (vérifie le GGUF et l'espace disque)")
        }

        loadedModelPath = path
    }
    
    /**
     * Génère une réponse locale (llama.cpp) cohérente et immersive.
     */
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>,
        username: String = "Utilisateur",
        userGender: String = "neutre",
        memoryContext: String = "",
        nsfwMode: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        ensureLoadedOrThrow()

        val (roles, contents) = buildChatMessages(
            character = character,
            messages = messages,
            username = username,
            userGender = userGender,
            memoryContext = memoryContext,
            nsfwMode = nsfwMode
        )

        try {
            // IMPORTANT: l'appel natif est bloquant et peu interruptible.
            // On exécute la génération sur un thread dédié avec timeout + annulation best-effort.
            val future = executor.submit<String> {
                generateChat(
                    contextPtr = contextPtr,
                    roles = roles,
                    contents = contents,
                    maxTokens = 120,
                    temperature = 0.9f,
                    topP = 0.9f,
                    topK = 40,
                    repeatPenalty = 1.12f
                )
            }

            val raw = try {
                // Timeout adaptatif (certains GGUF 2-7B sont très lents sur mobile)
                val timeoutSec = when {
                    loadedNCtx >= 768 -> 70L
                    loadedNCtx >= 640 -> 85L
                    else -> 95L
                }
                future.get(timeoutSec, TimeUnit.SECONDS)
            } catch (e: java.util.concurrent.TimeoutException) {
                try {
                    cancelGeneration(contextPtr)
                } catch (_: Throwable) {
                    // ignore
                }
                future.cancel(true)
                throw IllegalStateException(
                    "Le modèle GGUF est trop lent sur cet appareil. Essaie TinyLlama 1.1B (Q4) ou Phi-2 (Q4) et ferme les apps en arrière-plan."
                )
            }

            val cleaned = cleanModelOutput(raw, character.name)
            if (cleaned.isBlank()) {
                throw IllegalStateException("Réponse vide du modèle local")
            }
            return@withContext cleaned
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur génération llama.cpp", e)
            throw e
        }
    }

    private fun buildChatMessages(
        character: Character,
        messages: List<Message>,
        username: String,
        userGender: String,
        memoryContext: String,
        nsfwMode: Boolean
    ): Pair<Array<String>, Array<String>> {
        // Réduire le coût "prefill" (vitesse) tout en gardant un minimum de contexte.
        val recent = messages.takeLast(4)

        val nsfwLine = if (nsfwMode) {
            "Mode adulte: consensuel, progression naturelle, cohérent."
        } else {
            "Contenu tout public."
        }

        val personality = character.personality.trim().take(280)
        val description = character.description.trim().take(380)
        val scenario = character.scenario.trim().take(480)
        val memo = memoryContext.trim().take(280)

        val system = buildString {
            append("Tu es ").append(character.name).append(", un personnage de roleplay (PAS un assistant). ")
            append("Reste fidèle au caractère, immersive, cohérente, et fais avancer la scène. ")
            append("Style: 1-2 paragraphes, concret, sensoriel. ")
            append("Format: *action* (pensée) \"dialogue\". ")
            append("Ne décris pas les actions internes de l'utilisateur. ")
            append("Pose au plus une question utile. ")
            append(nsfwLine).append("\n")
            if (personality.isNotBlank()) append("Personnalité: ").append(personality).append("\n")
            if (description.isNotBlank()) append("Description: ").append(description).append("\n")
            if (scenario.isNotBlank()) append("Scénario: ").append(scenario).append("\n")
            append("Utilisateur: ").append(username).append(" (sexe: ").append(userGender).append(")\n")
            if (memo.isNotBlank()) append("Mémoire: ").append(memo).append("\n")
        }

        val roles = ArrayList<String>(1 + recent.size)
        val contents = ArrayList<String>(1 + recent.size)

        roles.add("system")
        contents.add(system)

        for (m in recent) {
            roles.add(if (m.isUser) "user" else "assistant")
            contents.add(m.content.trim().take(600))
        }

        return roles.toTypedArray() to contents.toTypedArray()
    }

    private fun cleanModelOutput(raw: String, characterName: String): String {
        var out = raw.trim()
        out = out.removePrefix("$characterName:")
        out = out.trim()

        // Couper si le modèle recommence un nouveau speaker
        val lines = out.lines()
        val kept = mutableListOf<String>()
        for (line in lines) {
            val t = line.trim()
            if (t.matches(Regex("^(Utilisateur|$characterName|Assistant|IA)\\s*:.*", RegexOption.IGNORE_CASE))) {
                break
            }
            kept.add(line)
        }
        return kept.joinToString("\n").trim()
    }
    
    fun getAvailableModels(): List<File> {
        val modelsDir = File(context.getExternalFilesDir(null), "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
            return emptyList()
        }
        
        return modelsDir.listFiles { file ->
            file.extension == "gguf"
        }?.toList() ?: emptyList()
    }
    
    fun getModelsDirectory(): File {
        val modelsDir = File(context.getExternalFilesDir(null), "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        return modelsDir
    }
}
