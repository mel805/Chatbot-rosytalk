package com.roleplayai.chatbot.data.ai

import android.content.Context
import android.util.Log
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.InferenceConfig
import com.roleplayai.chatbot.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LocalAIEngine - Wrapper pour llama.cpp
 * 
 * Cette classe sera connectée à llama.cpp via JNI pour l'inférence locale.
 * Pour l'instant, elle simule le comportement et utilise des réponses de fallback.
 * 
 * TODO: Implémenter l'interface JNI avec llama.cpp
 */
class LocalAIEngine(
    private val context: Context,
    private val modelPath: String,
    private val config: InferenceConfig = InferenceConfig()
) {
    
    private val promptOptimizer = PromptOptimizer()
    private var isModelLoaded = false
    private var contextSize = config.contextLength
    
    // Native methods (to be implemented with JNI)
    private external fun nativeLoadModel(modelPath: String, threads: Int, contextSize: Int): Boolean
    private external fun nativeGenerate(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int,
        repeatPenalty: Float
    ): String
    private external fun nativeUnloadModel()
    private external fun nativeIsLoaded(): Boolean
    
    companion object {
        init {
            try {
                // System.loadLibrary("llama")
                // System.loadLibrary("roleplay-ai-native")
                Log.d("LocalAIEngine", "Native libraries would be loaded here")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("LocalAIEngine", "Failed to load native libraries", e)
            }
        }
    }
    
    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            // TODO: Replace with actual JNI call
            // isModelLoaded = nativeLoadModel(modelPath, config.threads, config.contextLength)
            
            // Simulate loading
            Log.d("LocalAIEngine", "Loading model from: $modelPath")
            kotlinx.coroutines.delay(1000) // Simulate loading time
            isModelLoaded = true
            
            Log.d("LocalAIEngine", "Model loaded successfully")
            true
        } catch (e: Exception) {
            Log.e("LocalAIEngine", "Failed to load model", e)
            false
        }
    }
    
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>
    ): String = withContext(Dispatchers.IO) {
        if (!isModelLoaded) {
            Log.w("LocalAIEngine", "Model not loaded, using fallback")
            return@withContext getFallbackResponse(character)
        }
        
        try {
            // Build optimized prompt
            val prompt = promptOptimizer.buildEnhancedPrompt(
                character = character,
                messages = messages,
                maxContextLength = contextSize
            )
            
            val optimizedPrompt = promptOptimizer.optimizeForModel(prompt, contextSize)
            
            Log.d("LocalAIEngine", "Generating response...")
            
            // TODO: Replace with actual JNI call
            /*
            val response = nativeGenerate(
                prompt = optimizedPrompt,
                maxTokens = config.maxTokens,
                temperature = config.temperature,
                topP = config.topP,
                topK = config.topK,
                repeatPenalty = config.repeatPenalty
            )
            */
            
            // Simulate generation with enhanced fallback
            val response = generateEnhancedFallback(character, messages)
            
            // Post-process response
            val cleaned = cleanResponse(response)
            val enhanced = promptOptimizer.enhanceResponseCoherence(character, messages, cleaned)
            
            Log.d("LocalAIEngine", "Response generated: ${enhanced.take(50)}...")
            enhanced
            
        } catch (e: Exception) {
            Log.e("LocalAIEngine", "Error generating response", e)
            getFallbackResponse(character)
        }
    }
    
    fun unloadModel() {
        if (isModelLoaded) {
            try {
                // nativeUnloadModel()
                isModelLoaded = false
                Log.d("LocalAIEngine", "Model unloaded")
            } catch (e: Exception) {
                Log.e("LocalAIEngine", "Error unloading model", e)
            }
        }
    }
    
    private fun generateEnhancedFallback(character: Character, messages: List<Message>): String {
        // Enhanced fallback responses based on character and context
        val lastMessage = messages.lastOrNull { it.isUser }?.content?.lowercase() ?: ""
        
        return when {
            lastMessage.contains("bonjour") || lastMessage.contains("salut") || lastMessage.contains("hey") -> {
                when (character.personality.lowercase()) {
                    in listOf("timide", "douce") -> "*rougit légèrement* Bonjour... *sourit timidement* Comment vas-tu aujourd'hui?"
                    in listOf("énergique", "joyeuse") -> "*court vers toi avec un grand sourire* Salut! Je suis tellement contente de te voir! *yeux brillants*"
                    in listOf("séductrice", "confiante") -> "*sourire charmeur* Bonjour... *te regarde intensément* Tu viens me tenir compagnie?"
                    else -> "Bonjour! *sourit* Comment puis-je t'aider aujourd'hui?"
                }
            }
            
            lastMessage.contains("comment") && (lastMessage.contains("vas") || lastMessage.contains("va")) -> {
                when (character.personality.lowercase()) {
                    in listOf("timide", "douce") -> "*baisse les yeux un instant* Je vais bien, merci... *joue nerveusement avec ses cheveux* Et toi?"
                    in listOf("énergique", "joyeuse") -> "*saute de joie* Je vais super bien! *te prend la main* Et devine quoi, il s'est passé quelque chose d'incroyable aujourd'hui!"
                    in listOf("séductrice", "confiante") -> "*se penche vers toi* Je vais bien, mais je serais encore mieux... *sourit mystérieusement* si tu restais un peu avec moi."
                    else -> "Je vais bien, merci! *sourit* C'est gentil de demander. Et toi, comment te sens-tu?"
                }
            }
            
            lastMessage.contains("quoi de neuf") || lastMessage.contains("nouveauté") -> {
                "*réfléchit un instant* Hmm, eh bien... *${getCharacterAction(character)}* J'ai passé une journée plutôt intéressante. Je pensais justement à toi. *sourire sincère* Tu veux que je te raconte?"
            }
            
            lastMessage.contains("aime") || lastMessage.contains("passion") -> {
                when {
                    character.description.contains("art", ignoreCase = true) -> 
                        "*yeux brillants* Oh, tu t'intéresses à mes passions? *sourit* J'adore l'art, tu sais. Il y a quelque chose de magique à créer quelque chose de beau de ses propres mains. *se rapproche* Tu dessines ou peins?"
                    character.description.contains("sport", ignoreCase = true) ->
                        "*s'anime* Le sport, c'est ma vie! *geste enthousiaste* L'adrénaline, le dépassement de soi... Il n'y a rien de tel! *te tape amicalement l'épaule* Tu fais du sport toi aussi?"
                    else ->
                        "*réfléchit* Tu sais, j'ai beaucoup de centres d'intérêt... *sourire* Mais ce que j'aime par-dessus tout, c'est passer du temps avec des personnes qui comptent pour moi. *te regarde* Comme maintenant."
                }
            }
            
            lastMessage.contains("merci") -> {
                when (character.personality.lowercase()) {
                    in listOf("timide", "douce") -> "*rougit* Oh, ce n'est rien... *sourit doucement* Je suis heureuse de pouvoir t'aider."
                    in listOf("énergique", "joyeuse") -> "*te serre dans ses bras* De rien! *sourire radieux* Tu sais que je ferais n'importe quoi pour toi!"
                    in listOf("maternelle", "bienveillante") -> "*caresse tendrement tes cheveux* Voyons, pas de merci entre nous. *sourire chaleureux* C'est naturel de prendre soin des personnes qu'on apprécie."
                    else -> "*sourit* Je t'en prie, c'est avec plaisir. N'hésite pas si tu as besoin d'autre chose."
                }
            }
            
            lastMessage.contains("triste") || lastMessage.contains("mal") || lastMessage.contains("problème") -> {
                "*expression inquiète* Oh non... *${getComfortAction(character)}* Qu'est-ce qui ne va pas? *voix douce* Tu veux m'en parler? Je suis là pour toi, tu sais."
            }
            
            lastMessage.contains("?") -> {
                "*réfléchit à ta question* C'est une bonne question... *${getCharacterAction(character)}* Laisse-moi y penser. ${getThoughtfulResponse(character)}"
            }
            
            messages.size > 10 -> {
                // Long conversation context
                "*sourit en repensant à notre conversation* Tu sais, j'apprécie vraiment nos échanges. *${getCharacterAction(character)}* Il y a quelque chose de spécial dans la façon dont on peut parler de tout ensemble."
            }
            
            else -> {
                // Generic contextual response
                "*${getCharacterAction(character)}* ${getContextualResponse(character, lastMessage)}"
            }
        }
    }
    
    private fun getCharacterAction(character: Character): String {
        return when {
            character.personality.contains("timide", ignoreCase = true) -> "joue nerveusement avec ses cheveux"
            character.personality.contains("énergique", ignoreCase = true) -> "geste expressif"
            character.personality.contains("séductrice", ignoreCase = true) -> "regard intense"
            character.personality.contains("maternelle", ignoreCase = true) -> "sourire chaleureux"
            character.personality.contains("mystérieuse", ignoreCase = true) -> "regarde pensivement"
            else -> "sourit"
        }
    }
    
    private fun getComfortAction(character: Character): String {
        return when {
            character.personality.contains("maternelle", ignoreCase = true) -> "te prend dans ses bras"
            character.personality.contains("timide", ignoreCase = true) -> "pose doucement sa main sur ton épaule"
            character.personality.contains("ami", ignoreCase = true) -> "te tape amicalement le dos"
            else -> "s'approche de toi"
        }
    }
    
    private fun getThoughtfulResponse(character: Character): String {
        val responses = listOf(
            "Je pense que...",
            "D'après mon expérience...",
            "Hmm, je dirais que...",
            "Si tu veux mon avis...",
            "Laisse-moi te dire ce que j'en pense..."
        )
        return responses.random()
    }
    
    private fun getContextualResponse(character: Character, lastMessage: String): String {
        return when {
            lastMessage.length < 20 -> "Je t'écoute... *${getCharacterAction(character)}* Continue, ça m'intéresse."
            lastMessage.contains("!") -> "Tu sembles enthousiaste! *sourit* J'aime ton énergie."
            else -> "Je comprends ce que tu veux dire. *${getCharacterAction(character)}* C'est intéressant."
        }
    }
    
    private fun getFallbackResponse(character: Character): String {
        val fallbacks = listOf(
            "*${getCharacterAction(character)}* Je réfléchis à ce que tu viens de dire...",
            "C'est intéressant... *${getCharacterAction(character)}* Dis-m'en plus.",
            "*${getCharacterAction(character)}* Hmm, et toi, qu'en penses-tu?",
            "Je t'écoute attentivement. *${getCharacterAction(character)}* Continue...",
            "*${getCharacterAction(character)}* Tu sais, j'apprécie vraiment qu'on puisse parler comme ça."
        )
        return fallbacks.random()
    }
    
    private fun cleanResponse(response: String): String {
        return response
            .trim()
            .replace(Regex("^(Assistant:|AI:|User:)", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}
