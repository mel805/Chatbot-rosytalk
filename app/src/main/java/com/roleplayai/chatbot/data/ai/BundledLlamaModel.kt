package com.roleplayai.chatbot.data.ai

import android.content.Context
import android.util.Log
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import java.io.File
import java.io.FileOutputStream

/**
 * Résout un modèle GGUF "intégré" (sans téléchargement UI) via :
 * - Play Asset Delivery asset pack (AAB / Play Store)
 * - ou, en debug/dev, via assets/ (si tu mets un petit modèle dedans pour test)
 *
 * Convention: le fichier s'appelle `models/model.gguf`.
 *
 * Note: mettre un vrai LLM dans l'APK n'accélère pas l'inférence. Ça enlève juste l'étape
 * de téléchargement dans l'app et prépare la distribution Play Store.
 */
internal object BundledLlamaModel {
    private const val TAG = "BundledLlamaModel"

    // Doit matcher le module asset-pack (settings.gradle + llama_models/build.gradle.kts)
    private const val PACK_NAME = "llama_models"

    // Chemin relatif dans l'asset pack / assets
    private const val RELATIVE_ASSET_PATH = "models/model.gguf"

    /**
     * Retourne un chemin fichier local (dans filesDir) si un modèle "bundlé" est trouvé,
     * sinon null.
     *
     * Le modèle est copié une seule fois dans `filesDir/llm/model.gguf`.
     */
    fun resolveOrNull(context: Context): String? {
        val outDir = File(context.filesDir, "llm").apply { mkdirs() }
        val outFile = File(outDir, "model.gguf")
        if (outFile.exists() && outFile.length() > 0) {
            return outFile.absolutePath
        }

        // 1) Essayer via Play Asset Delivery (asset pack)
        runCatching {
            val mgr = AssetPackManagerFactory.getInstance(context)
            val loc = mgr.getPackLocation(PACK_NAME) ?: return@runCatching
            val assetsPath = loc.assetsPath() ?: return@runCatching
            val src = File(assetsPath, RELATIVE_ASSET_PATH)
            if (!src.exists() || !src.isFile) return@runCatching

            Log.i(TAG, "📦 Modèle trouvé via asset pack: ${src.absolutePath}")
            copyFile(src, outFile)
        }.onFailure {
            Log.w(TAG, "Asset pack non disponible: ${it.message}")
        }

        if (outFile.exists() && outFile.length() > 0) {
            return outFile.absolutePath
        }

        // 2) Essayer via assets/ (utile en dev local, si tu mets un modèle petit)
        runCatching {
            context.assets.open(RELATIVE_ASSET_PATH).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output, bufferSize = 1024 * 1024)
                }
            }
            Log.i(TAG, "📦 Modèle extrait depuis assets -> ${outFile.absolutePath}")
        }.onFailure {
            // normal si aucun fichier en assets
            outFile.delete()
        }

        return outFile.takeIf { it.exists() && it.length() > 0 }?.absolutePath
    }

    private fun copyFile(src: File, dst: File) {
        src.inputStream().use { input ->
            FileOutputStream(dst).use { output ->
                input.copyTo(output, bufferSize = 1024 * 1024)
            }
        }
    }
}

