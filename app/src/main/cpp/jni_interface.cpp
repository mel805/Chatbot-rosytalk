#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <memory>
#include <cstring>

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

static std::string jstring_to_std(JNIEnv* env, jstring js) {
    if (!js) return {};
    const char* c = env->GetStringUTFChars(js, nullptr);
    std::string s = c ? c : "";
    if (c) env->ReleaseStringUTFChars(js, c);
    return s;
}

static std::string apply_chat_template(
        const std::vector<std::string>& roles,
        const std::vector<std::string>& contents) {
    // Fallback simple si template indisponible
    if (!g_model_ctx || !g_model_ctx->model) {
        return "";
    }

#if defined(LLAMA_API_VERSION) || 1
    std::vector<llama_chat_message> chat;
    chat.reserve(roles.size());
    for (size_t i = 0; i < roles.size(); i++) {
        llama_chat_message m;
        m.role = roles[i].c_str();
        m.content = contents[i].c_str();
        chat.push_back(m);
    }

    const char* tmpl = nullptr;
    // Beaucoup de modèles GGUF contiennent un chat template.
    // Si la fonction n'existe pas dans cette version, la compilation échouera,
    // mais le workflow clone une version récente de llama.cpp où elle est présente.
    tmpl = llama_model_chat_template(g_model_ctx->model, nullptr);
    if (tmpl == nullptr) {
        // Modèle sans template -> fallback brut
        std::string fallback;
        for (size_t i = 0; i < roles.size(); i++) {
            fallback += roles[i];
            fallback += ": ";
            fallback += contents[i];
            fallback += "\n";
        }
        fallback += "assistant:";
        return fallback;
    }

    const int32_t n_required = llama_chat_apply_template(
            tmpl,
            chat.data(),
            chat.size(),
            true,
            nullptr,
            0);
    if (n_required <= 0) {
        // Fallback "role: content"
        std::string fallback;
        for (size_t i = 0; i < roles.size(); i++) {
            fallback += roles[i];
            fallback += ": ";
            fallback += contents[i];
            fallback += "\n";
        }
        fallback += "assistant:";
        return fallback;
    }

    std::string out;
    out.resize((size_t)n_required + 1);
    const int32_t n_written = llama_chat_apply_template(
            tmpl,
            chat.data(),
            chat.size(),
            true,
            out.data(),
            (int32_t)out.size());
    if (n_written > 0 && (size_t)n_written < out.size()) {
        out.resize((size_t)n_written);
    }
    if (!out.empty() && out.back() == '\0') out.pop_back();
    return out;
#else
    (void) roles;
    (void) contents;
    return "";
#endif
}

static std::string generate_from_prompt(
        const std::string& promptStr,
        int maxTokens,
        float temperature,
        float topP,
        int topK,
        float repeatPenalty) {
    if (!g_model_ctx || !g_model_ctx->model || !g_model_ctx->ctx) {
        return "";
    }

    // Reset mémoire/KV cache entre générations (API récente)
    llama_memory_t mem = llama_get_memory(g_model_ctx->ctx);
    if (mem) {
        llama_memory_clear(mem, true);
    }

    // Sampler
    if (g_model_ctx->sampler) {
        llama_sampler_free(g_model_ctx->sampler);
        g_model_ctx->sampler = nullptr;
    }
    llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
    g_model_ctx->sampler = llama_sampler_chain_init(sampler_params);

    llama_sampler_chain_add(g_model_ctx->sampler, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(g_model_ctx->sampler, llama_sampler_init_top_k(topK));
    llama_sampler_chain_add(g_model_ctx->sampler, llama_sampler_init_top_p(topP, 1));
    if (repeatPenalty > 1.0f) {
        // Anti-répétition (approx Groq frequency_penalty)
        llama_sampler_chain_add(g_model_ctx->sampler, llama_sampler_init_penalties(64, repeatPenalty, 0.0f, 0.0f));
    }
    llama_sampler_chain_add(g_model_ctx->sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    const llama_vocab* vocab = llama_model_get_vocab(g_model_ctx->model);
    std::vector<llama_token> tokens;

    const int n_prompt_tokens = -llama_tokenize(
            vocab,
            promptStr.c_str(),
            (int)promptStr.size(),
            nullptr,
            0,
            true,
            true);
    if (n_prompt_tokens <= 0) {
        return "";
    }
    tokens.resize(n_prompt_tokens);
    if (llama_tokenize(
                vocab,
                promptStr.c_str(),
                (int)promptStr.size(),
                tokens.data(),
                tokens.size(),
                true,
                true) < 0) {
        return "";
    }

    // Troncature si prompt trop long (conserver la fin, plus pertinente)
    const int n_ctx = g_model_ctx->n_ctx;
    int max_prompt = n_ctx - maxTokens - 8;
    if (max_prompt < 256) max_prompt = 256;
    if ((int)tokens.size() > max_prompt) {
        tokens.erase(tokens.begin(), tokens.end() - max_prompt);
    }

    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
    if (llama_decode(g_model_ctx->ctx, batch) != 0) {
        return "";
    }

    std::string response;
    int n_gen = 0;
    while (n_gen < maxTokens) {
        llama_token new_token = llama_sampler_sample(g_model_ctx->sampler, g_model_ctx->ctx, -1);
        if (llama_vocab_is_eog(vocab, new_token)) {
            break;
        }

        char buf[256];
        int n = llama_token_to_piece(vocab, new_token, buf, sizeof(buf), 0, true);
        if (n > 0) response.append(buf, n);

        batch = llama_batch_get_one(&new_token, 1);
        if (llama_decode(g_model_ctx->ctx, batch) != 0) {
            break;
        }
        n_gen++;
    }

    return response;
}

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
    
    const std::string promptStr = jstring_to_std(env, prompt);
    LOGI("===== Génération de réponse =====");
    LOGD("Prompt (premiers 100 car): %.100s...", promptStr.c_str());
    LOGD("Paramètres: max_tokens=%d, temp=%.2f, top_p=%.2f, top_k=%d", 
         maxTokens, temperature, topP, topK);
    
    try {
        const std::string response = generate_from_prompt(
                promptStr,
                maxTokens,
                temperature,
                topP,
                topK,
                repeatPenalty);
        LOGD("Réponse: %s", response.c_str());
        return env->NewStringUTF(response.c_str());
        
    } catch (const std::exception& e) {
        LOGE("Exception lors de la génération: %s", e.what());
        return env->NewStringUTF("");
    }
}

/**
 * Générer une réponse en mode chat (applique le template GGUF).
 */
JNIEXPORT jstring JNICALL
Java_com_roleplayai_chatbot_data_ai_LocalAIEngine_nativeGenerateChat(
        JNIEnv* env,
        jobject /* this */,
        jobjectArray roles,
        jobjectArray contents,
        jint maxTokens,
        jfloat temperature,
        jfloat topP,
        jint topK,
        jfloat repeatPenalty) {
    if (!g_model_ctx || !g_model_ctx->model || !g_model_ctx->ctx) {
        LOGE("Modèle non chargé!");
        return env->NewStringUTF("");
    }

    const jsize n_roles = roles ? env->GetArrayLength(roles) : 0;
    const jsize n_contents = contents ? env->GetArrayLength(contents) : 0;
    if (n_roles <= 0 || n_roles != n_contents) {
        LOGE("Roles/contents invalides");
        return env->NewStringUTF("");
    }

    std::vector<std::string> r;
    std::vector<std::string> c;
    r.reserve((size_t)n_roles);
    c.reserve((size_t)n_roles);

    for (jsize i = 0; i < n_roles; i++) {
        auto jr = (jstring) env->GetObjectArrayElement(roles, i);
        auto jc = (jstring) env->GetObjectArrayElement(contents, i);
        r.push_back(jstring_to_std(env, jr));
        c.push_back(jstring_to_std(env, jc));
        env->DeleteLocalRef(jr);
        env->DeleteLocalRef(jc);
    }

    std::string prompt = apply_chat_template(r, c);
    if (prompt.empty()) {
        LOGE("Prompt chat vide (template indisponible?)");
        return env->NewStringUTF("");
    }

    LOGI("===== Génération chat (template GGUF) =====");
    LOGD("Prompt chat (premiers 120 car): %.120s...", prompt.c_str());

    const std::string response = generate_from_prompt(
            prompt,
            maxTokens,
            temperature,
            topP,
            topK,
            repeatPenalty);

    return env->NewStringUTF(response.c_str());
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
