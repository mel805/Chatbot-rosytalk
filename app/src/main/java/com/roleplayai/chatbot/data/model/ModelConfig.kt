package com.roleplayai.chatbot.data.model

data class ModelConfig(
    val id: String,
    val name: String,
    val description: String,
    val size: Long, // Size in bytes
    val downloadUrl: String,
    val sha256: String,
    val fileName: String,
    val requiredRam: Long, // RAM in MB
    val quantization: String, // Q4_K_M, Q5_K_M, Q8_0, etc.
    val contextLength: Int = 4096,
    val recommended: Boolean = false
)

enum class ModelType {
    TINY_LLAMA_1B,
    PHI_2,
    GEMMA_2B,
    MISTRAL_7B,
    CUSTOM
}

data class InferenceConfig(
    val temperature: Float = 0.7f,   // Plus déterministe pour cohérence
    val topP: Float = 0.9f,
    val topK: Int = 30,              // Réduit pour plus de cohérence
    val repeatPenalty: Float = 1.3f, // Anti-répétition renforcé
    val contextLength: Int = 1024,   // Réduit pour mobile (économise RAM)
    val maxTokens: Int = 60,         // Très court pour rapidité mobile
    val threads: Int = 4,
    val batchSize: Int = 512
)

sealed class ModelState {
    object NotDownloaded : ModelState()
    data class Downloading(val progress: Float) : ModelState()
    object Downloaded : ModelState()
    data class Loading(val progress: Float) : ModelState()
    object Loaded : ModelState()
    data class Error(val message: String) : ModelState()
}

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percentage: Float,
    val speedBytesPerSecond: Long,
    val estimatedTimeRemainingSeconds: Long
)
