#include <jni.h>
#include <string>
#include <android/log.h>
#include <vector>
#include <cstring>

#ifdef LLAMA_CPP_AVAILABLE
#include "llama.h"
#endif

#define LOG_TAG "llama-android"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Structure pour stocker le contexte du modèle
struct ModelContext {
#ifdef LLAMA_CPP_AVAILABLE
    llama_model* model;
    llama_context* ctx;
#else
    void* dummy;  // Pour éviter une structure vide
#endif
    std::string model_path;
    bool loaded;
    
    ModelContext() : loaded(false) {
#ifdef LLAMA_CPP_AVAILABLE
        model = nullptr;
        ctx = nullptr;
#else
        dummy = nullptr;
#endif
    }
};

extern "C" {

/**
 * Charge un modèle GGUF
 */
JNIEXPORT jlong JNICALL
Java_com_roleplayai_chatbot_data_ai_LlamaCppEngine_loadModel(
    JNIEnv* env, jclass clazz,
    jstring modelPath, jint nThreads, jint nCtx
) {
    const char* path_cstr = env->GetStringUTFChars(modelPath, nullptr);
    std::string path(path_cstr);
    env->ReleaseStringUTFChars(modelPath, path_cstr);
    
    LOGI("🚀 Chargement modèle: %s", path.c_str());
    
    ModelContext* context = new ModelContext();
    context->model_path = path;
    
#ifdef LLAMA_CPP_AVAILABLE
    // Initialiser llama.cpp
    llama_backend_init();
    
    // Paramètres du modèle
    llama_model_params model_params = llama_model_default_params();
    
    // Charger le modèle
    context->model = llama_model_load_from_file(path.c_str(), model_params);
    if (!context->model) {
        LOGE("❌ Échec chargement modèle: %s", path.c_str());
        delete context;
        return 0;
    }
    
    // Paramètres du contexte
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = nCtx;
    ctx_params.n_threads = nThreads;
    ctx_params.n_threads_batch = nThreads;
    
    // Créer le contexte
    context->ctx = llama_init_from_model(context->model, ctx_params);
    if (!context->ctx) {
        LOGE("❌ Échec création contexte");
        llama_model_free(context->model);
        delete context;
        return 0;
    }
    
    context->loaded = true;
    LOGI("✅ Modèle chargé avec succès");
    
#else
    LOGI("⚠️ llama.cpp non compilé - mode fallback");
    context->loaded = false;
#endif
    
    return reinterpret_cast<jlong>(context);
}

/**
 * Génère du texte
 */
JNIEXPORT jstring JNICALL
Java_com_roleplayai_chatbot_data_ai_LlamaCppEngine_generate(
    JNIEnv* env, jclass clazz,
    jlong contextPtr, jstring prompt,
    jint maxTokens, jfloat temperature, jfloat topP, jint topK, jfloat repeatPenalty
) {
    ModelContext* context = reinterpret_cast<ModelContext*>(contextPtr);
    
    if (!context || !context->loaded) {
        LOGE("❌ Contexte invalide ou modèle non chargé");
        return env->NewStringUTF("*sourit* Le moteur llama.cpp n'est pas encore configuré. Utilisez un autre moteur IA.");
    }
    
    const char* prompt_cstr = env->GetStringUTFChars(prompt, nullptr);
    std::string prompt_str(prompt_cstr);
    env->ReleaseStringUTFChars(prompt, prompt_cstr);
    
    LOGI("📝 Génération avec prompt: %s...", prompt_str.substr(0, 50).c_str());
    
#ifdef LLAMA_CPP_AVAILABLE
    const llama_vocab* vocab = llama_model_get_vocab(context->model);

    // IMPORTANT: éviter l'accumulation du KV cache entre générations
    // (sinon le modèle peut "tourner" très longtemps et ne jamais produire de sortie utile).
    llama_kv_cache_clear(context->ctx);

    // Tokenize le prompt (nouvelle API)
    const int n_prompt_tokens = -llama_tokenize(
        vocab,
        prompt_str.c_str(),
        (int) prompt_str.size(),
        nullptr,
        0,
        true,  // add_special
        true   // parse_special
    );

    if (n_prompt_tokens <= 0) {
        LOGE("❌ Tokenization: n_prompt_tokens=%d", n_prompt_tokens);
        return env->NewStringUTF("Erreur de tokenization");
    }

    std::vector<llama_token> tokens;
    tokens.resize(n_prompt_tokens);

    if (llama_tokenize(
            vocab,
            prompt_str.c_str(),
            (int) prompt_str.size(),
            tokens.data(),
            (int) tokens.size(),
            true,
            true) < 0) {
        LOGE("❌ Échec tokenization");
        return env->NewStringUTF("Erreur de tokenization");
    }

    LOGI("🔢 Tokens prompt: %d", (int) tokens.size());

    // Décoder le prompt
    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
    if (llama_decode(context->ctx, batch) != 0) {
        LOGE("❌ Échec decode prompt");
        return env->NewStringUTF("Erreur de génération");
    }

    // Sampler chain (temp/top-k/top-p + pénalités)
    llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
    llama_sampler* sampler = llama_sampler_chain_init(sampler_params);
    llama_sampler_chain_add(sampler, llama_sampler_init_penalties(
        64,                  // last_n
        repeatPenalty,       // repeat
        0.0f,                // freq
        0.0f                 // present
    ));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(topK));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(topP, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    std::string generated_text;
    int n_gen = 0;

    while (n_gen < maxTokens) {
        llama_token new_token = llama_sampler_sample(sampler, context->ctx, -1);

        if (llama_vocab_is_eog(vocab, new_token)) {
            LOGI("✅ Fin de génération (EOG)");
            break;
        }

        char buf[256];
        int n = llama_token_to_piece(vocab, new_token, buf, sizeof(buf), 0, true);
        if (n > 0) {
            generated_text.append(buf, n);
        }

        batch = llama_batch_get_one(&new_token, 1);
        if (llama_decode(context->ctx, batch) != 0) {
            LOGE("❌ Échec decode token %d", n_gen);
            break;
        }

        n_gen++;
    }

    llama_sampler_free(sampler);
    LOGI("✅ Génération terminée: %d tokens", n_gen);
    return env->NewStringUTF(generated_text.c_str());
    
#else
    LOGI("⚠️ FALLBACK MODE: llama.cpp pas encore compilé");
    std::string generated_text = "*sourit* Bonjour ! Le moteur llama.cpp sera disponible après compilation du NDK avec le modèle GGUF.";
    return env->NewStringUTF(generated_text.c_str());
#endif
}

/**
 * Libère le modèle
 */
JNIEXPORT void JNICALL
Java_com_roleplayai_chatbot_data_ai_LlamaCppEngine_freeModel(
    JNIEnv* env, jclass clazz, jlong contextPtr
) {
    ModelContext* context = reinterpret_cast<ModelContext*>(contextPtr);
    if (!context) return;
    
#ifdef LLAMA_CPP_AVAILABLE
    if (context->ctx) {
        llama_free(context->ctx);
        context->ctx = nullptr;
    }
    if (context->model) {
        llama_model_free(context->model);
        context->model = nullptr;
    }
    llama_backend_free();
#endif
    
    delete context;
    LOGI("✅ Modèle libéré");
}

/**
 * Vérifie si le modèle est chargé
 */
JNIEXPORT jboolean JNICALL
Java_com_roleplayai_chatbot_data_ai_LlamaCppEngine_isModelLoaded(
    JNIEnv* env, jclass clazz, jlong contextPtr
) {
    ModelContext* context = reinterpret_cast<ModelContext*>(contextPtr);
    return (context && context->loaded) ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
