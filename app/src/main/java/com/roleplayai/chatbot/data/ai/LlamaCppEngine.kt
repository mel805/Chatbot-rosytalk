package com.roleplayai.chatbot.data.ai

import android.content.Context
import android.util.Log
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max

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
        @JvmStatic private external fun generate(
            contextPtr: Long,
            prompt: String,
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

    private fun ensureLoadedOrThrow() {
        val path = modelPath ?: throw IllegalStateException("Aucun modèle GGUF configuré (Paramètres > llama.cpp)")
        val f = File(path)
        if (!f.exists()) throw IllegalStateException("Modèle GGUF introuvable: $path")
        if (!nativeLibLoaded) throw IllegalStateException("Lib native llama-android indisponible sur cet appareil/build")

        if (contextPtr != 0L && isModelLoaded(contextPtr)) {
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

        val threads = defaultThreads()
        val nCtx = 2048
        contextPtr = loadModel(path, threads, nCtx)
        if (contextPtr == 0L || !isModelLoaded(contextPtr)) {
            contextPtr = 0L
            throw IllegalStateException("Échec chargement modèle llama.cpp (vérifie le GGUF et l'espace disque)")
        }
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

        val prompt = buildPrompt(
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
                generate(
                    contextPtr = contextPtr,
                    prompt = prompt,
                    maxTokens = 96,
                    temperature = 0.85f,
                    topP = 0.92f,
                    topK = 40,
                    repeatPenalty = 1.15f
                )
            }

            val raw = try {
                future.get(45, TimeUnit.SECONDS)
            } catch (e: java.util.concurrent.TimeoutException) {
                try {
                    cancelGeneration(contextPtr)
                } catch (_: Throwable) {
                    // ignore
                }
                future.cancel(true)
                throw IllegalStateException(
                    "Le modèle local met trop de temps à répondre. Essaie un GGUF plus léger (TinyLlama/Phi-2)."
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

    private fun buildPrompt(
        character: Character,
        messages: List<Message>,
        username: String,
        userGender: String,
        memoryContext: String,
        nsfwMode: Boolean
    ): String {
        val recent = messages.takeLast(16)

        val nsfwLine = if (nsfwMode) {
            "- Mode adulte: reste consensuel, progression naturelle, cohérent avec le tempérament."
        } else {
            "- Contenu tout public."
        }

        val sb = StringBuilder()
        sb.append(
            """
            ### SYSTEM
            Tu es ${character.name} (personnage de roleplay), pas un assistant.
            - Personnalité: ${character.personality}
            - Description: ${character.description}
            - Scénario: ${character.scenario}
            $nsfwLine
            - Règles: ne décris QUE tes actions (pas celles de l'utilisateur). Reste fidèle au caractère/temperament.
            - Initiative: réagis + fais avancer la scène (propose une action ou un angle), pose au plus une question utile.
            - Style: 1-3 paragraphes, immersif, concret, pas de métadonnées.
            - Format: *action* (pensée) "dialogue"
            Utilisateur: $username (sexe: $userGender)
            """.trimIndent()
        )

        if (memoryContext.isNotBlank()) {
            sb.append("\n\n### MEMOIRE\n")
            sb.append(memoryContext.trim())
        }

        sb.append("\n\n### CONVERSATION\n")
        for (m in recent) {
            val speaker = if (m.isUser) username else character.name
            sb.append(speaker).append(": ").append(m.content.trim()).append("\n")
        }

        sb.append("\n### REPONSE\n")
        sb.append(character.name).append(":")
        return sb.toString()
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
