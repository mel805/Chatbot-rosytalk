#include <jni.h>
#include <string>
#include <android/log.h>
#include <vector>
#include <cstring>
#include <atomic>

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
    int n_threads;
    int n_ctx;
    std::atomic<bool> cancel_requested;
    
    ModelContext() : loaded(false), n_threads(0), n_ctx(0), cancel_requested(false) {
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
    context->n_threads = nThreads;
    context->n_ctx = nCtx;
    
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
    context->cancel_requested.store(false);
    
    // IMPORTANT: éviter l'accumulation d'état entre générations.
    // On nettoie la mémoire (KV cache) de la séquence 0 au lieu de recréer le contexte
    // (bien plus rapide, et évite les timeouts même avec TinyLlama).
    llama_memory_t mem = llama_get_memory(context->ctx);
    (void) llama_memory_seq_rm(mem, 0, -1, -1);

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
        if (context->cancel_requested.load()) {
            LOGI("🛑 Annulation demandée (cancel_generation)");
            break;
        }
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
 * Génère du texte à partir de messages (system/user/assistant) en appliquant le chat template du GGUF.
 * Cela améliore fortement la cohérence/immersion car le format correspond à l'entraînement du modèle.
 */
JNIEXPORT jstring JNICALL
Java_com_roleplayai_chatbot_data_ai_LlamaCppEngine_generateChat(
    JNIEnv* env, jclass clazz,
    jlong contextPtr, jobjectArray rolesArr, jobjectArray contentsArr,
    jint maxTokens, jfloat temperature, jfloat topP, jint topK, jfloat repeatPenalty
) {
    ModelContext* context = reinterpret_cast<ModelContext*>(contextPtr);

    if (!context || !context->loaded) {
        LOGE("❌ Contexte invalide ou modèle non chargé (generateChat)");
        return env->NewStringUTF("Le moteur llama.cpp n'est pas configuré.");
    }

    const jsize n_roles = rolesArr ? env->GetArrayLength(rolesArr) : 0;
    const jsize n_contents = contentsArr ? env->GetArrayLength(contentsArr) : 0;
    if (n_roles <= 0 || n_roles != n_contents) {
        LOGE("❌ generateChat: tableaux invalides roles=%d contents=%d", (int) n_roles, (int) n_contents);
        return env->NewStringUTF("Erreur: messages invalides");
    }

#ifdef LLAMA_CPP_AVAILABLE
    const llama_vocab* vocab = llama_model_get_vocab(context->model);
    context->cancel_requested.store(false);

    // Nettoyer la mémoire de la séquence 0 (on repart d'un prompt propre pour éviter les dérives).
    llama_memory_t mem = llama_get_memory(context->ctx);
    (void) llama_memory_seq_rm(mem, 0, -1, -1);

    // Copier les strings Java -> C++ (stables) et construire le tableau de chat messages.
    std::vector<std::string> roles;
    std::vector<std::string> contents;
    roles.reserve(n_roles);
    contents.reserve(n_roles);

    std::vector<llama_chat_message> chat;
    chat.reserve(n_roles);

    for (jsize i = 0; i < n_roles; i++) {
        jstring jrole = (jstring) env->GetObjectArrayElement(rolesArr, i);
        jstring jcontent = (jstring) env->GetObjectArrayElement(contentsArr, i);

        const char* role_c = jrole ? env->GetStringUTFChars(jrole, nullptr) : nullptr;
        const char* content_c = jcontent ? env->GetStringUTFChars(jcontent, nullptr) : nullptr;

        roles.emplace_back(role_c ? role_c : "");
        contents.emplace_back(content_c ? content_c : "");

        if (jrole && role_c) env->ReleaseStringUTFChars(jrole, role_c);
        if (jcontent && content_c) env->ReleaseStringUTFChars(jcontent, content_c);
        if (jrole) env->DeleteLocalRef(jrole);
        if (jcontent) env->DeleteLocalRef(jcontent);

        llama_chat_message msg;
        msg.role = roles.back().c_str();
        msg.content = contents.back().c_str();
        chat.push_back(msg);
    }

    // Appliquer le template chat.
    // ATTENTION: certains GGUF n'embarquent pas de chat template -> tmpl peut être nullptr.
    // Or llama_chat_apply_template() requiert un template non-null (sinon crash possible).
    const char * tmpl = llama_model_chat_template(context->model, nullptr);
    if (tmpl == nullptr) {
        // Fallback: choisir un template builtin (préférer chatml si dispo).
        const char * builtins[32] = {0};
        const int32_t n = llama_chat_builtin_templates(builtins, 32);
        const char * pick = nullptr;
        for (int i = 0; i < n; i++) {
            if (builtins[i] && std::strcmp(builtins[i], "chatml") == 0) {
                pick = builtins[i];
                break;
            }
        }
        tmpl = pick ? pick : (n > 0 ? builtins[0] : "chatml");
        LOGI("ℹ️ Chat template absent du GGUF, fallback='%s'", tmpl);
    }
    int32_t n_prompt_chars = llama_chat_apply_template(
        tmpl,
        chat.data(),
        chat.size(),
        true,   // add_assistant
        nullptr,
        0
    );

    if (n_prompt_chars <= 0) {
        LOGE("❌ llama_chat_apply_template: n_prompt_chars=%d", (int) n_prompt_chars);
        return env->NewStringUTF("Erreur: template chat indisponible");
    }

    std::string prompt_str;
    prompt_str.resize((size_t) n_prompt_chars);
    const int32_t n_written = llama_chat_apply_template(
        tmpl,
        chat.data(),
        chat.size(),
        true,
        prompt_str.data(),
        (int32_t) prompt_str.size()
    );

    if (n_written <= 0) {
        LOGE("❌ llama_chat_apply_template (2): n_written=%d", (int) n_written);
        return env->NewStringUTF("Erreur: template chat indisponible");
    }

    // Tokenize le prompt
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
        LOGE("❌ Tokenization (template): n_prompt_tokens=%d", n_prompt_tokens);
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
        LOGE("❌ Échec tokenization (template)");
        return env->NewStringUTF("Erreur de tokenization");
    }

    LOGI("🔢 Tokens prompt (template): %d", (int) tokens.size());

    // Décoder le prompt
    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
    if (llama_decode(context->ctx, batch) != 0) {
        LOGE("❌ Échec decode prompt (template)");
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
        if (context->cancel_requested.load()) {
            LOGI("🛑 Annulation demandée (cancel_generation)");
            break;
        }

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
    LOGI("✅ Génération terminée (template): %d tokens", n_gen);
    return env->NewStringUTF(generated_text.c_str());

#else
    (void) n_roles;
    (void) n_contents;
    LOGI("⚠️ FALLBACK MODE: llama.cpp pas encore compilé (generateChat)");
    return env->NewStringUTF("Le moteur llama.cpp sera disponible après compilation.");
#endif
}

/**
 * Demande l'annulation d'une génération en cours.
 */
JNIEXPORT void JNICALL
Java_com_roleplayai_chatbot_data_ai_LlamaCppEngine_cancelGeneration(
    JNIEnv* env, jclass clazz, jlong contextPtr
) {
#ifdef LLAMA_CPP_AVAILABLE
    ModelContext* context = reinterpret_cast<ModelContext*>(contextPtr);
    if (!context) return;
    context->cancel_requested.store(true);
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
