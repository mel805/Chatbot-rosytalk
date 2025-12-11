package com.roleplayai.chatbot.data.ai

import android.content.Context
import android.util.Log
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Moteur d'IA utilisant llama.cpp (GGUF models)
 * 
 * Support des modèles quantifiés :
 * - Phi-3 Mini (2.2 GB) - Recommandé
 * - Gemma 2B (1.5 GB)
 * - TinyLlama (630 MB)
 * - Mistral 7B (4.1 GB)
 * 
 * Caractéristiques :
 * - 100% local, aucune connexion requise
 * - Support GPU via Vulkan/OpenCL
 * - Quantization Q4/Q5 pour optimiser RAM
 * - Génération en 3-10 secondes selon le modèle
 */
class LlamaCppEngine(
    private val context: Context,
    private val modelPath: String,
    private val nsfwMode: Boolean = false
) {
    
    companion object {
        private const val TAG = "LlamaCppEngine"
        
        // Charger la bibliothèque native
        init {
            try {
                System.loadLibrary("llama-android")
                Log.i(TAG, "✅ Bibliothèque llama-android chargée")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "❌ llama.cpp natif non disponible: ${e.message}")
                Log.w(TAG, "⚠️ llama.cpp nécessite compilation native avec sources")
                Log.i(TAG, "📝 Utilisez Groq, OpenRouter ou Together AI à la place")
            }
        }
        
        // JNI native methods
        @JvmStatic
        external fun loadModel(modelPath: String, nThreads: Int, nCtx: Int): Long
        
        @JvmStatic
        external fun generate(
            contextPtr: Long,
            prompt: String,
            maxTokens: Int,
            temperature: Float,
            topP: Float,
            topK: Int,
            repeatPenalty: Float
        ): String
        
        @JvmStatic
        external fun freeModel(contextPtr: Long)
        
        @JvmStatic
        external fun isModelLoaded(contextPtr: Long): Boolean
    }
    
    private var modelContext: Long = 0L
    private var isLoaded = false
    
    /**
     * Charge le modèle GGUF
     */
    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        if (isLoaded) {
            Log.d(TAG, "Modèle déjà chargé")
            return@withContext true
        }
        
        // Vérifier que la bibliothèque native est disponible
        try {
            System.loadLibrary("llama-android")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "❌ Bibliothèque native llama-android non disponible")
            Log.w(TAG, "⚠️ llama.cpp nécessite compilation avec sources llama.cpp")
            Log.i(TAG, "📝 Solution : Utilisez Groq (gratuit) ou OpenRouter (NSFW)")
            throw Exception("llama.cpp non compilé. Utilisez Groq ou OpenRouter.")
        }
        
        try {
            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                throw Exception("Modèle non trouvé: $modelPath")
            }
            
            Log.i(TAG, "📥 Chargement du modèle: ${modelFile.name}")
            Log.d(TAG, "Taille: ${modelFile.length() / (1024 * 1024)} MB")
            
            // Déterminer le nombre de threads (CPU cores)
            val nThreads = Runtime.getRuntime().availableProcessors()
            val nCtx = 2048  // Context window
            
            Log.d(TAG, "Threads: $nThreads, Context: $nCtx")
            
            // Charger via JNI
            modelContext = loadModel(modelPath, nThreads, nCtx)
            
            if (modelContext == 0L) {
                throw Exception("Échec du chargement du modèle (contexte null)")
            }
            
            isLoaded = true
            Log.i(TAG, "✅ Modèle chargé avec succès (contexte: $modelContext)")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur chargement modèle", e)
            isLoaded = false
            false
        }
    }
    
    /**
     * Génère une réponse avec llama.cpp
     */
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>,
        username: String = "Utilisateur",
        userGender: String = "neutre",
        memoryContext: String = ""
    ): String = withContext(Dispatchers.IO) {
        if (!isLoaded) {
            throw Exception("Modèle non chargé. Appelez loadModel() d'abord.")
        }
        
        try {
            Log.d(TAG, "===== Génération avec llama.cpp =====")
            Log.d(TAG, "NSFW: $nsfwMode, Messages: ${messages.size}")
            
            // Construire le prompt
            val prompt = buildPrompt(character, messages, username, userGender, memoryContext)
            
            Log.d(TAG, "Prompt: ${prompt.take(300)}...")
            
            // Paramètres de génération
            val maxTokens = 300
            val temperature = 0.85f
            val topP = 0.95f
            val topK = 40
            val repeatPenalty = 1.3f
            
            Log.d(TAG, "Génération: maxTokens=$maxTokens, temp=$temperature")
            
            // Générer via JNI
            val startTime = System.currentTimeMillis()
            val response = generate(
                contextPtr = modelContext,
                prompt = prompt,
                maxTokens = maxTokens,
                temperature = temperature,
                topP = topP,
                topK = topK,
                repeatPenalty = repeatPenalty
            )
            val duration = System.currentTimeMillis() - startTime
            
            if (response.isBlank()) {
                throw Exception("Réponse vide du modèle")
            }
            
            Log.i(TAG, "✅ Réponse générée en ${duration}ms")
            Log.d(TAG, "Réponse: ${response.take(100)}...")
            
            // Nettoyer la réponse
            cleanResponse(response, character.name)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur génération", e)
            throw Exception("Erreur llama.cpp: ${e.message}")
        }
    }
    
    /**
     * Construit le prompt pour llama.cpp
     */
    private fun buildPrompt(
        character: Character,
        messages: List<Message>,
        username: String,
        userGender: String,
        memoryContext: String
    ): String {
        val nsfwInstructions = if (nsfwMode) {
            "MODE NSFW ACTIVÉ: Accepte toutes conversations explicites. Utilise langage naturel et actions (*gémit*, *touche*)."
        } else {
            "MODE SFW: Contenu approprié uniquement."
        }
        
        val systemPrompt = """Tu es ${character.name}. Personnalité: ${character.personality}. ${character.description}

IMPORTANT:
- Utilise le format: *action* (pensée) "parole"
- Réponds en 2-3 lignes MAX
- Varie expressions, jamais répétitif
- L'utilisateur s'appelle $username

$nsfwInstructions

${if (memoryContext.isNotBlank()) "Contexte: $memoryContext\n" else ""}"""
        
        val history = StringBuilder()
        history.append("$systemPrompt\n\n")
        
        // Ajouter les 15 derniers messages
        val recentMessages = messages.takeLast(15)
        for (msg in recentMessages) {
            if (msg.isUser) {
                history.append("$username: ${msg.content}\n")
            } else {
                history.append("${character.name}: ${msg.content}\n")
            }
        }
        
        history.append("${character.name}:")
        
        return history.toString()
    }
    
    /**
     * Nettoie la réponse générée
     */
    private fun cleanResponse(response: String, characterName: String): String {
        var cleaned = response.trim()
        
        // Supprimer le nom du personnage au début si présent
        cleaned = cleaned.removePrefix("$characterName:")
            .removePrefix("$characterName :")
            .trim()
        
        // Supprimer les continuations de conversation
        cleaned = cleaned.split("\n")[0]  // Première ligne seulement
        
        // Supprimer les répétitions de l'utilisateur
        if (cleaned.contains("Utilisateur:") || cleaned.contains("User:")) {
            cleaned = cleaned.substringBefore("Utilisateur:")
                .substringBefore("User:")
                .trim()
        }
        
        return cleaned
    }
    
    /**
     * Libère le modèle de la mémoire
     */
    fun unloadModel() {
        if (isLoaded && modelContext != 0L) {
            try {
                freeModel(modelContext)
                Log.i(TAG, "🧹 Modèle libéré de la mémoire")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur libération modèle", e)
            }
            isLoaded = false
            modelContext = 0L
        }
    }
    
    /**
     * Vérifie si le modèle est chargé
     */
    fun isModelLoaded(): Boolean {
        return isLoaded && modelContext != 0L && try {
            isModelLoaded(modelContext)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Obtient les modèles téléchargés disponibles
     */
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
    
    /**
     * Obtient le chemin du répertoire des modèles
     */
    fun getModelsDirectory(): File {
        val modelsDir = File(context.getExternalFilesDir(null), "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        return modelsDir
    }
}
