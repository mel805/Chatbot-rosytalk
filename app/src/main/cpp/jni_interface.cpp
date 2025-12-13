#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <memory>

// Inclure llama.cpp headers
#include "llama.h"

#define TAG "RolePlayAI-Native"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

// Contexte global pour le modèle
struct ModelContext {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    llama_sampler* sampler = nullptr;
    int n_ctx = 2048;
};

static std::unique_ptr<ModelContext> g_model_ctx;

extern "C" {

/**
 * Charger le modèle LLM
 */
JNIEXPORT jboolean JNICALL
Java_com_roleplayai_chatbot_data_ai_LocalAIEngine_nativeLoadModel(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPath,
        jint threads,
        jint contextSize) {
    
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("===== Chargement du modèle =====");
    LOGI("Chemin: %s", path);
    LOGI("Threads: %d, Context: %d", threads, contextSize);
    
    try {
        // Initialiser llama backend
        llama_backend_init();
        
        // Créer le contexte
        g_model_ctx = std::make_unique<ModelContext>();
        g_model_ctx->n_ctx = contextSize;
        
        // Paramètres du modèle
        llama_model_params model_params = llama_model_default_params();
        model_params.n_gpu_layers = 0; // CPU uniquement sur Android
        
        // Charger le modèle
        LOGI("Chargement du modèle depuis %s...", path);
        g_model_ctx->model = llama_load_model_from_file(path, model_params);
        
        if (g_model_ctx->model == nullptr) {
            LOGE("Échec du chargement du modèle");
            g_model_ctx.reset();
            env->ReleaseStringUTFChars(modelPath, path);
            return JNI_FALSE;
        }
        
        LOGI("Modèle chargé! Création du contexte...");
        
        // Paramètres du contexte
        llama_context_params ctx_params = llama_context_default_params();
        ctx_params.n_ctx = contextSize;
        ctx_params.n_threads = threads;
        ctx_params.n_threads_batch = threads;
        
        // Créer le contexte
        g_model_ctx->ctx = llama_new_context_with_model(g_model_ctx->model, ctx_params);
        
        if (g_model_ctx->ctx == nullptr) {
            LOGE("Échec de la création du contexte");
            llama_free_model(g_model_ctx->model);
            g_model_ctx.reset();
            env->ReleaseStringUTFChars(modelPath, path);
            return JNI_FALSE;
        }
        
        // Créer le sampler
        llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
        g_model_ctx->sampler = llama_sampler_chain_init(sampler_params);
        
        // Ajouter des samplers basiques
        llama_sampler_chain_add(g_model_ctx->sampler, 
                                llama_sampler_init_temp(0.8f));
        llama_sampler_chain_add(g_model_ctx->sampler, 
                                llama_sampler_init_top_k(40));
        llama_sampler_chain_add(g_model_ctx->sampler, 
                                llama_sampler_init_top_p(0.95f, 1));
        llama_sampler_chain_add(g_model_ctx->sampler, 
                                llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
        
        env->ReleaseStringUTFChars(modelPath, path);
        
        LOGI("===== Modèle chargé avec succès! =====");
        return JNI_TRUE;
        
    } catch (const std::exception& e) {
        LOGE("Exception lors du chargement: %s", e.what());
        g_model_ctx.reset();
        env->ReleaseStringUTFChars(modelPath, path);
        return JNI_FALSE;
    }
}

/**
 * Générer une complétion de texte
 */
JNIEXPORT jstring JNICALL
Java_com_roleplayai_chatbot_data_ai_LocalAIEngine_nativeGenerate(
        JNIEnv* env,
        jobject /* this */,
        jstring prompt,
        jint maxTokens,
        jfloat temperature,
        jfloat topP,
        jint topK,
        jfloat repeatPenalty) {
    
    if (!g_model_ctx || !g_model_ctx->model || !g_model_ctx->ctx) {
        LOGE("Modèle non chargé!");
        return env->NewStringUTF("");
    }
    
    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    LOGI("===== Génération de réponse =====");
    LOGD("Prompt (premiers 100 car): %.100s...", promptStr);
    LOGD("Paramètres: max_tokens=%d, temp=%.2f, top_p=%.2f, top_k=%d", 
         maxTokens, temperature, topP, topK);
    
    try {
        // Important: reset KV cache entre générations, sinon on peut finir par générer vide (EOS immédiat)
        llama_kv_cache_clear(g_model_ctx->ctx);

        // Mettre à jour les paramètres du sampler
        llama_sampler_free(g_model_ctx->sampler);
        
        llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
        g_model_ctx->sampler = llama_sampler_chain_init(sampler_params);
        
        llama_sampler_chain_add(g_model_ctx->sampler, 
                                llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(g_model_ctx->sampler, 
                                llama_sampler_init_top_k(topK));
        llama_sampler_chain_add(g_model_ctx->sampler, 
                                llama_sampler_init_top_p(topP, 1));
        llama_sampler_chain_add(g_model_ctx->sampler, 
                                llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
        
        // Tokenizer le prompt avec la nouvelle API
        const llama_vocab* vocab = llama_model_get_vocab(g_model_ctx->model);
        std::vector<llama_token> tokens;
        
        const int n_prompt_tokens = -llama_tokenize(
            vocab,
            promptStr,
            strlen(promptStr),
            nullptr,
            0,
            true,  // add_special
            true   // parse_special
        );
        
        tokens.resize(n_prompt_tokens);
        
        if (llama_tokenize(
                vocab,
                promptStr,
                strlen(promptStr),
                tokens.data(),
                tokens.size(),
                true,
                true) < 0) {
            LOGE("Échec de la tokenization");
            env->ReleaseStringUTFChars(prompt, promptStr);
            return env->NewStringUTF("");
        }
        
        LOGI("Prompt tokenizé: %d tokens", (int)tokens.size());
        
        // Créer le batch
        llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
        
        // Décoder le prompt
        if (llama_decode(g_model_ctx->ctx, batch) != 0) {
            LOGE("Échec du décodage du prompt");
            env->ReleaseStringUTFChars(prompt, promptStr);
            return env->NewStringUTF("");
        }
        
        // Générer les tokens
        std::string response;
        int n_cur = tokens.size();
        int n_gen = 0;
        
        LOGI("Génération en cours...");
        
        while (n_gen < maxTokens) {
            // Sampler le prochain token
            llama_token new_token = llama_sampler_sample(g_model_ctx->sampler, g_model_ctx->ctx, -1);
            
            // Obtenir le vocab
            const llama_vocab* vocab = llama_model_get_vocab(g_model_ctx->model);
            
            // Vérifier fin de génération
            if (llama_vocab_is_eog(vocab, new_token)) {
                LOGI("Token EOS détecté, arrêt de la génération");
                break;
            }
            
            // Décoder le token en texte
            char buf[256];
            int n = llama_token_to_piece(vocab, new_token, buf, sizeof(buf), 0, true);
            if (n > 0) {
                response.append(buf, n);
            }
            
            // Préparer le prochain batch
            batch = llama_batch_get_one(&new_token, 1);
            
            if (llama_decode(g_model_ctx->ctx, batch) != 0) {
                LOGE("Échec du décodage à la position %d", n_cur);
                break;
            }
            
            n_cur++;
            n_gen++;
            
            // Log progression tous les 10 tokens
            if (n_gen % 10 == 0) {
                LOGD("Généré %d/%d tokens", n_gen, maxTokens);
            }
        }
        
        env->ReleaseStringUTFChars(prompt, promptStr);
        
        LOGI("Génération terminée: %d tokens générés", n_gen);
        LOGD("Réponse: %s", response.c_str());
        
        return env->NewStringUTF(response.c_str());
        
    } catch (const std::exception& e) {
        LOGE("Exception lors de la génération: %s", e.what());
        env->ReleaseStringUTFChars(prompt, promptStr);
        return env->NewStringUTF("");
    }
}

/**
 * Décharger le modèle
 */
JNIEXPORT void JNICALL
Java_com_roleplayai_chatbot_data_ai_LocalAIEngine_nativeUnloadModel(
        JNIEnv* env,
        jobject /* this */) {
    
    LOGI("===== Déchargement du modèle =====");
    
    if (g_model_ctx) {
        if (g_model_ctx->sampler) {
            llama_sampler_free(g_model_ctx->sampler);
            g_model_ctx->sampler = nullptr;
        }
        
        if (g_model_ctx->ctx) {
            llama_free(g_model_ctx->ctx);
            g_model_ctx->ctx = nullptr;
        }
        
        if (g_model_ctx->model) {
            llama_free_model(g_model_ctx->model);
            g_model_ctx->model = nullptr;
        }
        
        g_model_ctx.reset();
    }
    
    llama_backend_free();
    
    LOGI("Modèle déchargé");
}

/**
 * Vérifier si le modèle est chargé
 */
JNIEXPORT jboolean JNICALL
Java_com_roleplayai_chatbot_data_ai_LocalAIEngine_nativeIsLoaded(
        JNIEnv* env,
        jobject /* this */) {
    
    return (g_model_ctx && g_model_ctx->model && g_model_ctx->ctx) ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
