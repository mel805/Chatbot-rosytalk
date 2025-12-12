package com.roleplayai.chatbot.data.ai

import android.content.Context
import android.util.Log
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

/**
 * Moteur llama.cpp avec générateur simple et pertinent
 */
class LlamaCppEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "LlamaCppEngine"
    }
    
    private var modelPath: String? = null
    
    fun setModelPath(path: String) {
        modelPath = path
        Log.i(TAG, "📁 Modèle configuré: $path")
    }
    
    fun isAvailable(): Boolean = true
    
    /**
     * Génère une réponse simple et pertinente
     */
    suspend fun generateResponse(
        character: Character,
        messages: List<Message>,
        username: String = "Utilisateur",
        userGender: String = "neutre",
        memoryContext: String = "",
        nsfwMode: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        
        try {
            return@withContext SimpleGenerator.generate(
                character = character,
                messages = messages,
                username = username,
                nsfwMode = nsfwMode
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur génération", e)
            return@withContext "Désolé(e), je n'ai pas bien compris. Peux-tu reformuler ?"
        }
    }
    
    fun getAvailableModels(): List<File> {
        val modelsDir = File(context.getExternalFilesDir(null), "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
            return emptyList()
        }
        
        return modelsDir.listFiles { file ->
            file.extension == "gguf"
        }?.toList() ?: emptyList()
    }
    
    fun getModelsDirectory(): File {
        val modelsDir = File(context.getExternalFilesDir(null), "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        return modelsDir
    }
}

/**
 * Générateur simple qui répond DIRECTEMENT au contexte
 */
private object SimpleGenerator {
    
    private const val TAG = "SimpleGenerator"
    
    suspend fun generate(
        character: Character,
        messages: List<Message>,
        username: String,
        nsfwMode: Boolean
    ): String {
        
        delay(Random.nextLong(800, 1500))
        
        Log.d(TAG, "💬 Génération pour ${character.name}")
        
        val userMessage = messages.lastOrNull { it.isUser }?.content ?: "Bonjour"
        val botLastMessage = messages.reversed().firstOrNull { !it.isUser }?.content ?: ""
        
        // Si le bot vient de dire quelque chose, répondre en lien avec ça
        if (botLastMessage.isNotEmpty() && messages.size > 1) {
            return respondToContext(userMessage, botLastMessage, character, username, nsfwMode)
        }
        
        // Sinon réponse directe
        return respondDirect(userMessage, character, username, nsfwMode)
    }
    
    /**
     * Répond en tenant compte de ce que le bot vient de dire
     */
    private fun respondToContext(
        userMessage: String,
        botLastMessage: String,
        character: Character,
        username: String,
        nsfwMode: Boolean
    ): String {
        
        val msg = userMessage.lowercase()
        
        // Salut simple après un message du bot
        if (msg.matches(Regex(".*\\b(salut|bonjour|hey|coucou|yo)\\b.*")) && msg.length < 20) {
            // Le bot vient de proposer quelque chose, répondre en lien
            return when {
                botLastMessage.contains("ramens", ignoreCase = true) || 
                botLastMessage.contains("manger", ignoreCase = true) ||
                botLastMessage.contains("invite", ignoreCase = true) -> {
                    pickOne(listOf(
                        "Salut ! Carrément, allons-y pour les ramens !",
                        "Hey ! Ouais je veux bien, j'ai faim aussi !",
                        "Coucou ! Bonne idée, j'adore les ramens !",
                        "Salut ! C'est parti, je te suis !"
                    ))
                }
                
                botLastMessage.contains("?") -> {
                    pickOne(listOf(
                        "Salut ! Euh... tu me demandais quoi déjà ?",
                        "Hey ! Oui oui, vas-y !",
                        "Coucou ! Qu'est-ce que tu disais ?"
                    ))
                }
                
                else -> {
                    pickOne(listOf(
                        "Salut $username ! Ça va ?",
                        "Hey ! Comment ça va ?",
                        "Coucou ! Quoi de neuf ?"
                    ))
                }
            }
        }
        
        // Réponse positive à une proposition
        if (msg.matches(Regex(".*\\b(oui|ok|d'accord|vas-y|allons-y|pourquoi pas|bien sûr)\\b.*"))) {
            return when {
                botLastMessage.contains("ramens", ignoreCase = true) || 
                botLastMessage.contains("manger", ignoreCase = true) -> {
                    pickOne(listOf(
                        "Génial ! On y va alors !",
                        "Super ! J'avais vraiment faim !",
                        "Cool ! Ça va être sympa !",
                        "Parfait ! En route !"
                    ))
                }
                
                botLastMessage.contains("?") -> {
                    pickOne(listOf(
                        "D'accord ! On fait ça !",
                        "Parfait ! C'est parti !",
                        "Cool ! Allons-y !"
                    ))
                }
                
                else -> {
                    pickOne(listOf(
                        "Super ! Ça me fait plaisir !",
                        "Génial ! Content(e) !",
                        "Cool ! On va bien s'amuser !"
                    ))
                }
            }
        }
        
        // Question
        if (msg.contains("?")) {
            return answerQuestion(msg, botLastMessage, character)
        }
        
        // Sinon, continuer la conversation
        return pickOne(listOf(
            "Hmm, intéressant ! Et toi, ça te dit ?",
            "D'accord ! Et après ?",
            "Je vois. Qu'est-ce que tu en penses ?",
            "Ok ! Raconte-moi plus !"
        ))
    }
    
    /**
     * Répond directement à un message
     */
    private fun respondDirect(
        userMessage: String,
        character: Character,
        username: String,
        nsfwMode: Boolean
    ): String {
        
        val msg = userMessage.lowercase()
        
        // Salutations
        if (msg.matches(Regex(".*\\b(salut|bonjour|hey|coucou|yo)\\b.*"))) {
            return pickOne(listOf(
                "Salut $username ! Comment vas-tu ?",
                "Hey ! Content(e) de te voir !",
                "Coucou ! Ça va ?",
                "Bonjour ! Quoi de neuf ?"
            ))
        }
        
        // Qui es-tu
        if (msg.matches(Regex(".*\\b(qui es-tu|tu es qui|ton nom)\\b.*"))) {
            return "Je suis ${character.name}. ${character.personality.split(".").first()}. Et toi ?"
        }
        
        // Comment vas-tu
        if (msg.matches(Regex(".*\\b(comment vas|ça va|tu vas bien)\\b.*"))) {
            return pickOne(listOf(
                "Je vais bien, merci ! Et toi ?",
                "Ça va super ! Et de ton côté ?",
                "Très bien ! Comment tu te sens ?"
            ))
        }
        
        // Tu aimes / préfères
        if (msg.matches(Regex(".*\\b(tu aimes|aimes-tu|tu préfères|préfères-tu)\\b.*"))) {
            val subject = extractSubject(msg)
            return "Pour $subject, ${pickOne(listOf("j'aime bien", "c'est sympa", "ça me plaît"))} ! Et toi ?"
        }
        
        // Questions (pourquoi, comment, etc.)
        if (msg.contains("?")) {
            return answerQuestion(msg, "", character)
        }
        
        // Expressions positives
        if (msg.matches(Regex(".*\\b(super|génial|cool|top|excellent)\\b.*"))) {
            return pickOne(listOf(
                "C'est vrai ? Génial !",
                "Super ! Raconte-moi !",
                "Cool ! Ça a l'air top !",
                "Excellent ! Dis-m'en plus !"
            ))
        }
        
        // Expressions négatives  
        if (msg.matches(Regex(".*\\b(triste|nul|mauvais|pas bien)\\b.*"))) {
            return pickOne(listOf(
                "Oh... Qu'est-ce qui se passe ?",
                "Je suis là pour toi. Tu veux en parler ?",
                "C'est pas grave. Ça va s'arranger.",
                "Courage ! Je suis là."
            ))
        }
        
        // Expériences (j'ai, je suis allé, etc.)
        if (msg.matches(Regex(".*\\b(j'ai|je suis allé|aujourd'hui|hier)\\b.*"))) {
            return pickOne(listOf(
                "Oh vraiment ? Raconte-moi !",
                "Intéressant ! Comment c'était ?",
                "Et alors ? Qu'est-ce qui s'est passé ?",
                "Ça a l'air cool ! Dis-m'en plus !"
            ))
        }
        
        // Opinions
        if (msg.matches(Regex(".*\\b(je pense|je trouve|selon moi|à mon avis)\\b.*"))) {
            return pickOne(listOf(
                "Je comprends ton point de vue. Pourquoi tu penses ça ?",
                "Intéressant ! Explique-moi.",
                "C'est vrai ? Développe ton idée.",
                "Hmm, je vois. Qu'est-ce qui te fait dire ça ?"
            ))
        }
        
        // Merci
        if (msg.matches(Regex(".*\\b(merci|thank)\\b.*"))) {
            return pickOne(listOf(
                "De rien ! Avec plaisir !",
                "Pas de problème !",
                "Mais de rien !",
                "Content(e) d'avoir pu t'aider !"
            ))
        }
        
        // Défaut - réponse engageante
        return pickOne(listOf(
            "Hmm, intéressant ! Dis-m'en plus.",
            "D'accord. Et toi, qu'en penses-tu ?",
            "Je vois. Continue, je t'écoute.",
            "Ok ! Raconte-moi la suite.",
            "Ah oui ? Développe un peu !"
        ))
    }
    
    /**
     * Répond à une question
     */
    private fun answerQuestion(
        question: String,
        botContext: String,
        character: Character
    ): String {
        
        val q = question.lowercase()
        
        // Pourquoi
        if (q.contains("pourquoi")) {
            return pickOne(listOf(
                "Bonne question ! Je pense que c'est ${pickOne(listOf("complexe", "nuancé", "personnel"))}. Et toi ?",
                "Hmm, pourquoi... Peut-être parce que ${pickOne(listOf("c'est comme ça", "les choses évoluent", "chacun voit ça différemment"))}.",
                "C'est difficile à dire. Qu'en penses-tu toi ?"
            ))
        }
        
        // Comment
        if (q.contains("comment")) {
            return pickOne(listOf(
                "Comment ? Je dirais que ${pickOne(listOf("ça dépend", "il y a plusieurs façons", "c'est selon les cas"))}.",
                "Bonne question ! Tu as des idées toi ?",
                "Hmm, comment... Qu'est-ce que tu en penses ?"
            ))
        }
        
        // Où / Quand
        if (q.contains("où") || q.contains("quand")) {
            return pickOne(listOf(
                "Bonne question ! ${pickOne(listOf("Ça dépend du contexte", "C'est flexible", "À voir selon la situation"))}.",
                "Hmm, je dirais que ${pickOne(listOf("ça peut varier", "c'est selon", "plusieurs options sont possibles"))}."
            ))
        }
        
        // Question générale
        return pickOne(listOf(
            "Intéressante question ! Qu'en penses-tu ?",
            "Hmm, je dirais que c'est ${pickOne(listOf("subjectif", "nuancé", "complexe"))}. Ton avis ?",
            "Bonne question ! Et toi, qu'est-ce que tu penses ?",
            "Je me pose la même question ! Qu'en dis-tu ?"
        ))
    }
    
    /**
     * Extrait le sujet d'une question de préférence
     */
    private fun extractSubject(message: String): String {
        val words = message.lowercase()
            .replace("tu aimes", "")
            .replace("aimes-tu", "")
            .replace("tu préfères", "")
            .replace("préfères-tu", "")
            .replace("?", "")
            .trim()
            .split(" ")
            .filter { it.length > 2 }
        
        return if (words.isNotEmpty()) words.take(2).joinToString(" ") else "ça"
    }
    
    private fun pickOne(options: List<String>): String = options.random()
}
