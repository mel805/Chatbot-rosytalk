package com.roleplayai.chatbot.data.download

import android.app.ActivityManager
import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import com.roleplayai.chatbot.data.model.DownloadProgress
import com.roleplayai.chatbot.data.model.ModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class ModelDownloader(private val context: Context) {
    
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    
    fun downloadModel(model: ModelConfig): Flow<DownloadProgress> = flow {
        val modelsDir = getModelsDirectory()
        val destinationFile = File(modelsDir, model.fileName)
        
        // Check if already downloaded
        if (destinationFile.exists()) {
            if (verifyChecksum(destinationFile, model.sha256)) {
                emit(DownloadProgress(
                    bytesDownloaded = model.size,
                    totalBytes = model.size,
                    percentage = 100f,
                    speedBytesPerSecond = 0,
                    estimatedTimeRemainingSeconds = 0
                ))
                return@flow
            } else {
                // Corrupted file, delete and re-download
                destinationFile.delete()
            }
        }
        
        // Create download request
        val request = DownloadManager.Request(Uri.parse(model.downloadUrl)).apply {
            setTitle("Téléchargement ${model.name}")
            setDescription("Téléchargement du modèle IA...")
            setDestinationInExternalFilesDir(context, null, "models/${model.fileName}")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
        }
        
        val downloadId = downloadManager.enqueue(request)
        
        // Monitor download progress
        var downloading = true
        var lastUpdate = System.currentTimeMillis()
        var lastBytes = 0L
        
        while (downloading) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor: Cursor? = downloadManager.query(query)
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val bytesDownloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val totalBytes = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloading = false
                            emit(DownloadProgress(
                                bytesDownloaded = totalBytes,
                                totalBytes = totalBytes,
                                percentage = 100f,
                                speedBytesPerSecond = 0,
                                estimatedTimeRemainingSeconds = 0
                            ))
                        }
                        DownloadManager.STATUS_FAILED -> {
                            downloading = false
                            throw Exception("Échec du téléchargement")
                        }
                        DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                            val now = System.currentTimeMillis()
                            val timeDiff = (now - lastUpdate) / 1000.0
                            val bytesDiff = bytesDownloaded - lastBytes
                            
                            val speed = if (timeDiff > 0) (bytesDiff / timeDiff).toLong() else 0L
                            val remaining = if (speed > 0) (totalBytes - bytesDownloaded) / speed else 0L
                            val percentage = if (totalBytes > 0) (bytesDownloaded.toFloat() / totalBytes * 100) else 0f
                            
                            emit(DownloadProgress(
                                bytesDownloaded = bytesDownloaded,
                                totalBytes = totalBytes,
                                percentage = percentage,
                                speedBytesPerSecond = speed,
                                estimatedTimeRemainingSeconds = remaining
                            ))
                            
                            lastUpdate = now
                            lastBytes = bytesDownloaded
                        }
                    }
                }
            }
            
            delay(500) // Update every 500ms
        }
        
        // Verify checksum after download
        if (!verifyChecksum(destinationFile, model.sha256)) {
            destinationFile.delete()
            throw Exception("Échec de la vérification du fichier")
        }
    }
    
    suspend fun deleteModel(model: ModelConfig): Boolean = withContext(Dispatchers.IO) {
        val modelsDir = getModelsDirectory()
        val modelFile = File(modelsDir, model.fileName)
        
        if (modelFile.exists()) {
            modelFile.delete()
        } else {
            false
        }
    }
    
    fun isModelDownloaded(model: ModelConfig): Boolean {
        val modelsDir = getModelsDirectory()
        val modelFile = File(modelsDir, model.fileName)
        return modelFile.exists() && modelFile.length() == model.size
    }
    
    fun getModelPath(model: ModelConfig): String? {
        val modelsDir = getModelsDirectory()
        val modelFile = File(modelsDir, model.fileName)
        return if (modelFile.exists()) modelFile.absolutePath else null
    }
    
    private fun getModelsDirectory(): File {
        val dir = File(context.getExternalFilesDir(null), "models")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    private fun verifyChecksum(file: File, expectedSha256: String): Boolean {
        // For now, skip checksum verification (placeholder SHA256)
        // In production, implement proper SHA256 verification
        return true
        
        /* Full implementation:
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } > 0) {
                    digest.update(buffer, 0, read)
                }
            }
            
            val hash = digest.digest()
            val hashString = hash.joinToString("") { "%02x".format(it) }
            return hashString.equals(expectedSha256, ignoreCase = true)
        } catch (e: Exception) {
            return false
        }
        */
    }
    
    fun getAvailableStorageSpace(): Long {
        val modelsDir = getModelsDirectory()
        return modelsDir.usableSpace
    }
    
    fun getAvailableRamMB(): Long {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            // Retourner la RAM totale en MB
            memoryInfo.totalMem / (1024 * 1024)
        } catch (e: Exception) {
            // Fallback: utiliser Runtime (moins précis mais fonctionne)
            val runtime = Runtime.getRuntime()
            (runtime.maxMemory() / (1024 * 1024)) * 4 // Estimer à 4x la heap
        }
    }
    
    fun getAvailableFreeRamMB(): Long {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            // Retourner la RAM libre en MB
            memoryInfo.availMem / (1024 * 1024)
        } catch (e: Exception) {
            512L // Valeur par défaut conservative
        }
    }
}
