plugins {
    id("com.android.asset-pack")
}

assetPack {
    // Nom utilisé par Play Asset Delivery (et par l'app pour localiser les assets).
    packName = "llama_models"

    // Pour le Play Store, tu pourras changer en "fast-follow" ou "on-demand".
    dynamicDelivery {
        deliveryType = "install-time"
    }
}

/**
 * Télécharge un modèle GGUF "bundlé" dans l'asset-pack, sans le committer.
 *
 * Usage:
 * -PbundledModel=tinyllama -PdownloadBundledModel=true
 *
 * Le fichier final attendu par l'app:
 * llama_models/src/main/assets/models/model.gguf
 */
val bundledModel: String? = (findProperty("bundledModel") as String?)?.trim()?.ifBlank { null }
val downloadBundledModel: Boolean =
    ((findProperty("downloadBundledModel") as String?)?.trim()?.lowercase() == "true")

val outFile = layout.projectDirectory.file("src/main/assets/models/model.gguf").asFile

val tinyLlamaUrl =
    "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.q4_k_m.gguf?download=true"

fun expectedUrlFor(model: String): String = when (model.lowercase()) {
    "tinyllama" -> tinyLlamaUrl
    else -> error("Modèle bundlé inconnu: $model")
}

tasks.register("prepareBundledModel") {
    group = "llm"
    description = "Prépare le modèle GGUF bundlé (download optionnel) pour l'asset-pack."

    doLast {
        if (bundledModel == null) {
            logger.lifecycle("No bundledModel specified; skipping model preparation.")
            return@doLast
        }

        // Si le fichier existe déjà, on le garde (utile pour dev local).
        if (outFile.exists() && outFile.length() > 50L * 1024 * 1024) {
            logger.lifecycle("Bundled model already present: ${outFile.absolutePath} (${outFile.length() / (1024 * 1024)} MB)")
            return@doLast
        }

        if (!downloadBundledModel) {
            logger.lifecycle(
                "Bundled model missing but downloadBundledModel=false. Place the GGUF at: ${outFile.absolutePath}"
            )
            return@doLast
        }

        val url = expectedUrlFor(bundledModel)
        outFile.parentFile.mkdirs()
        logger.lifecycle("Downloading bundled model '$bundledModel' -> ${outFile.absolutePath}")
        logger.lifecycle("URL: $url")

        // Téléchargement stream -> fichier
        val tmp = File(outFile.parentFile, outFile.name + ".partial")
        tmp.delete()

        java.net.URL(url).openStream().use { input ->
            tmp.outputStream().use { output ->
                val buf = ByteArray(1024 * 1024)
                var read: Int
                var total: Long = 0
                while (true) {
                    read = input.read(buf)
                    if (read <= 0) break
                    output.write(buf, 0, read)
                    total += read.toLong()
                    if (total % (64L * 1024 * 1024) == 0L) {
                        logger.lifecycle("... downloaded ${total / (1024 * 1024)} MB")
                    }
                }
            }
        }

        if (tmp.length() < 50L * 1024 * 1024) {
            tmp.delete()
            error("Downloaded file too small; download likely failed.")
        }

        if (outFile.exists()) outFile.delete()
        tmp.renameTo(outFile)
        logger.lifecycle("✅ Bundled model ready: ${outFile.absolutePath} (${outFile.length() / (1024 * 1024)} MB)")
    }
}

