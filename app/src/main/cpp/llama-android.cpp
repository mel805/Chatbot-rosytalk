#include <jni.h>
#include <string>
#include <android/log.h>
#include <vector>

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
    llama_backend_init(false);
    
    // Paramètres du modèle
    llama_model_params model_params = llama_model_default_params();
    
    // Charger le modèle
    context->model = llama_load_model_from_file(path.c_str(), model_params);
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
    context->ctx = llama_new_context_with_model(context->model, ctx_params);
    if (!context->ctx) {
        LOGE("❌ Échec création contexte");
        llama_free_model(context->model);
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
    // Tokenize le prompt
    std::vector<llama_token> tokens_list;
    tokens_list.resize(prompt_str.size() + 1);
    int n_tokens = llama_tokenize(
        context->model,
        prompt_str.c_str(),
        prompt_str.size(),
        tokens_list.data(),
        tokens_list.size(),
        true,  // add_bos
        false  // special
    );
    tokens_list.resize(n_tokens);
    
    LOGI("🔢 Tokens: %d", n_tokens);
    
    // Évaluer le prompt
    llama_batch batch = llama_batch_init(tokens_list.size(), 0, 1);
    for (size_t i = 0; i < tokens_list.size(); i++) {
        llama_batch_add(batch, tokens_list[i], i, {0}, false);
    }
    batch.logits[batch.n_tokens - 1] = true;  // Calculer logits pour le dernier token
    
    if (llama_decode(context->ctx, batch) != 0) {
        LOGE("❌ Échec decode");
        llama_batch_free(batch);
        return env->NewStringUTF("Erreur de génération");
    }
    
    // Générer la réponse token par token
    std::string generated_text;
    int n_cur = batch.n_tokens;
    int n_gen = 0;
    
    while (n_gen < maxTokens) {
        // Obtenir les logits
        float* logits = llama_get_logits_ith(context->ctx, batch.n_tokens - 1);
        
        // Sample le prochain token (sampling simple pour commencer)
        int n_vocab = llama_n_vocab(context->model);
        std::vector<llama_token_data> candidates;
        candidates.reserve(n_vocab);
        
        for (llama_token token_id = 0; token_id < n_vocab; token_id++) {
            candidates.push_back({token_id, logits[token_id], 0.0f});
        }
        
        llama_token_data_array candidates_p = {
            candidates.data(),
            candidates.size(),
            false
        };
        
        // Sample avec temperature
        llama_sample_top_k(context->ctx, &candidates_p, topK, 1);
        llama_sample_top_p(context->ctx, &candidates_p, topP, 1);
        llama_sample_temp(context->ctx, &candidates_p, temperature);
        llama_token new_token_id = llama_sample_token(context->ctx, &candidates_p);
        
        // Vérifier fin de génération
        if (new_token_id == llama_token_eos(context->model)) {
            LOGI("✅ Fin de génération (EOS)");
            break;
        }
        
        // Convertir en texte
        char buf[256];
        int n = llama_token_to_piece(context->model, new_token_id, buf, sizeof(buf));
        if (n > 0) {
            generated_text.append(buf, n);
        }
        
        // Préparer le prochain batch
        llama_batch_clear(batch);
        llama_batch_add(batch, new_token_id, n_cur, {0}, true);
        n_cur++;
        n_gen++;
        
        // Évaluer le nouveau token
        if (llama_decode(context->ctx, batch) != 0) {
            LOGE("❌ Échec decode token %d", n_gen);
            break;
        }
    }
    
    llama_batch_free(batch);
    
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
        llama_free_model(context->model);
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
