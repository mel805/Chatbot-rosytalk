package com.roleplayai.chatbot.data.ai;

/**
 * AIDL IPC interface for llama.cpp running in a separate process.
 */
interface ILlamaNativeService {
    boolean isLoaded();

    boolean loadModel(String modelPath, int threads, int contextSize);

    /**
     * Dernière erreur côté process natif (chargement/génération), vide si aucune.
     */
    String getLastError();

    String generateChat(in String[] roles,
                        in String[] contents,
                        int maxTokens,
                        float temperature,
                        float topP,
                        int topK,
                        float repeatPenalty);
}

