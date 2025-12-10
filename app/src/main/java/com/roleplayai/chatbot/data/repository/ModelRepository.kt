package com.roleplayai.chatbot.data.repository

import com.roleplayai.chatbot.data.model.ModelConfig
import com.roleplayai.chatbot.data.model.ModelType

class ModelRepository {
    
    fun getAvailableModels(): List<ModelConfig> {
        return listOf(
            // TinyLlama - Recommandé pour faible RAM
            ModelConfig(
                id = "tinyllama-1.1b-q4",
                name = "TinyLlama 1.1B (Rapide)",
                description = "Modèle léger, idéal pour tous les appareils. Réponses rapides.",
                size = 637L * 1024 * 1024, // 637 MB
                downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
                sha256 = "3a0f01a9b0f1e1c8e8b1e1c8e8b1e1c8e8b1e1c8e8b1e1c8e8b1e1c8e8b1e1c8", // Placeholder
                fileName = "tinyllama-1.1b-q4.gguf",
                requiredRam = 1024, // 1 GB
                quantization = "Q4_K_M",
                recommended = true
            ),
            
            // Phi-2 - Bon équilibre
            ModelConfig(
                id = "phi-2-q4",
                name = "Phi-2 2.7B (Équilibré)",
                description = "Microsoft Phi-2, excellent équilibre qualité/performance.",
                size = 1600L * 1024 * 1024, // 1.6 GB
                downloadUrl = "https://huggingface.co/TheBloke/phi-2-GGUF/resolve/main/phi-2.Q4_K_M.gguf",
                sha256 = "4b0f02a9b0f2e2c9e9b2e2c9e9b2e2c9e9b2e2c9e9b2e2c9e9b2e2c9e9b2e2c9",
                fileName = "phi-2-q4.gguf",
                requiredRam = 2048, // 2 GB
                quantization = "Q4_K_M",
                recommended = false
            ),
            
            // Gemma 2B - Haute qualité
            ModelConfig(
                id = "gemma-2b-q5",
                name = "Gemma 2B (Qualité)",
                description = "Google Gemma, haute qualité pour appareils puissants.",
                size = 1700L * 1024 * 1024, // 1.7 GB
                downloadUrl = "https://huggingface.co/lmstudio-community/gemma-2b-it-GGUF/resolve/main/gemma-2b-it-q5_k_m.gguf",
                sha256 = "5c0f03a9b0f3e3c0e0b3e3c0e0b3e3c0e0b3e3c0e0b3e3c0e0b3e3c0e0b3e3c0",
                fileName = "gemma-2b-q5.gguf",
                requiredRam = 3072, // 3 GB
                quantization = "Q5_K_M",
                recommended = false
            ),
            
            // Mistral 7B - Pour appareils haut de gamme
            ModelConfig(
                id = "mistral-7b-q4",
                name = "Mistral 7B (Avancé)",
                description = "Modèle le plus puissant, requiert 4+ GB RAM. Qualité maximale.",
                size = 4100L * 1024 * 1024, // 4.1 GB
                downloadUrl = "https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF/resolve/main/mistral-7b-instruct-v0.2.Q4_K_M.gguf",
                sha256 = "6d0f04a9b0f4e4d1e1b4e4d1e1b4e4d1e1b4e4d1e1b4e4d1e1b4e4d1e1b4e4d1",
                fileName = "mistral-7b-q4.gguf",
                requiredRam = 4096, // 4 GB
                quantization = "Q4_K_M",
                recommended = false
            )
        )
    }
    
    fun getRecommendedModel(availableRamMB: Long): ModelConfig {
        return when {
            availableRamMB < 2048 -> getAvailableModels()[0] // TinyLlama
            availableRamMB < 3072 -> getAvailableModels()[1] // Phi-2
            availableRamMB < 4096 -> getAvailableModels()[2] // Gemma
            else -> getAvailableModels()[3] // Mistral
        }
    }
    
    fun getModelById(id: String): ModelConfig? {
        return getAvailableModels().find { it.id == id }
    }
}
