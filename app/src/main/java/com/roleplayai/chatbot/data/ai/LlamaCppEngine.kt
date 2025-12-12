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
 * Moteur llama.cpp avec générateur de dialogues roleplay
 * Génère des réponses longues avec *actions* et (pensées)
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
     * Génère une réponse complète avec actions et pensées
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
            return@withContext RoleplayGenerator.generate(
                character = character,
                messages = messages,
                username = username,
                nsfwMode = nsfwMode
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur génération", e)
            return@withContext "*regarde $username avec confusion* (Je n'ai pas bien compris...) Désolé(e), peux-tu reformuler ?"
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
 * Générateur de dialogues roleplay complets
 * Format: *action* (pensée) "dialogue"
 */
private object RoleplayGenerator {
    
    private const val TAG = "RoleplayGenerator"
    
    suspend fun generate(
        character: Character,
        messages: List<Message>,
        username: String,
        nsfwMode: Boolean
    ): String {
        
        delay(Random.nextLong(1000, 2000))
        
        Log.d(TAG, "🎭 Génération roleplay pour ${character.name}")
        
        val userMessage = messages.lastOrNull { it.isUser }?.content ?: "Bonjour"
        val botLastMessage = messages.reversed().firstOrNull { !it.isUser }?.content ?: ""
        val conversationLength = messages.size
        
        // Analyser le contexte
        val context = analyzeContext(userMessage, botLastMessage, conversationLength)
        
        // Générer la réponse complète
        return buildRoleplayResponse(
            context = context,
            character = character,
            username = username,
            nsfwMode = nsfwMode
        )
    }
    
    /**
     * Analyse le contexte de la conversation
     */
    private fun analyzeContext(
        userMessage: String,
        botLastMessage: String,
        conversationLength: Int
    ): ConversationContext {
        
        val msg = userMessage.lowercase()
        
        // Déterminer le type d'interaction
        val interactionType = when {
            msg.matches(Regex(".*\\b(salut|bonjour|hey|coucou|yo)\\b.*")) -> InteractionType.GREETING
            msg.matches(Regex(".*\\b(qui es|ton nom|tu t'appelles)\\b.*")) -> InteractionType.IDENTITY_QUESTION
            msg.matches(Regex(".*\\b(comment vas|ça va|tu vas bien)\\b.*")) -> InteractionType.WELLBEING_QUESTION
            msg.matches(Regex(".*\\b(tu aimes|aimes-tu|tu préfères)\\b.*")) -> InteractionType.PREFERENCE_QUESTION
            msg.contains("?") -> InteractionType.QUESTION
            msg.matches(Regex(".*\\b(j'ai|je suis allé|aujourd'hui)\\b.*")) -> InteractionType.SHARING_EXPERIENCE
            msg.matches(Regex(".*\\b(je pense|je trouve|selon moi)\\b.*")) -> InteractionType.SHARING_OPINION
            msg.matches(Regex(".*\\b(oui|ok|d'accord|vas-y|allons-y)\\b.*")) -> InteractionType.AGREEMENT
            msg.matches(Regex(".*\\b(non|pas|jamais)\\b.*")) -> InteractionType.DISAGREEMENT
            msg.matches(Regex(".*\\b(super|génial|cool)\\b.*")) -> InteractionType.POSITIVE_EMOTION
            msg.matches(Regex(".*\\b(triste|nul|mauvais)\\b.*")) -> InteractionType.NEGATIVE_EMOTION
            msg.matches(Regex(".*\\b(merci|thank)\\b.*")) -> InteractionType.GRATITUDE
            else -> InteractionType.GENERAL_STATEMENT
        }
        
        // Déterminer l'émotion
        val emotion = when {
            msg.matches(Regex(".*\\b(content|heureux|joyeux|super)\\b.*")) -> Emotion.HAPPY
            msg.matches(Regex(".*\\b(triste|malheureux|déprimé)\\b.*")) -> Emotion.SAD
            msg.matches(Regex(".*\\b(énervé|colère|furieux)\\b.*")) -> Emotion.ANGRY
            msg.matches(Regex(".*\\b(excité|motivé|enthousiaste)\\b.*")) -> Emotion.EXCITED
            msg.matches(Regex(".*[!]{2,}.*")) -> Emotion.EXCITED
            msg.matches(Regex(".*\\b(calme|tranquille|serein)\\b.*")) -> Emotion.CALM
            else -> Emotion.NEUTRAL
        }
        
        // Extraire des éléments clés
        val hasQuestion = msg.contains("?")
        val hasBotContext = botLastMessage.isNotEmpty()
        val botProposedSomething = botLastMessage.contains(Regex("(allons|viens|on va|tu veux)"))
        val isFirstMessage = conversationLength <= 1
        
        return ConversationContext(
            interactionType = interactionType,
            emotion = emotion,
            hasQuestion = hasQuestion,
            hasBotContext = hasBotContext,
            botProposedSomething = botProposedSomething,
            botLastMessage = botLastMessage,
            isFirstMessage = isFirstMessage,
            userMessage = userMessage
        )
    }
    
    /**
     * Construit une réponse roleplay complète
     */
    private fun buildRoleplayResponse(
        context: ConversationContext,
        character: Character,
        username: String,
        nsfwMode: Boolean
    ): String {
        
        // Choisir une action corporelle appropriée
        val action = chooseAction(context, character)
        
        // Choisir une pensée interne
        val thought = chooseThought(context, character, username)
        
        // Générer le dialogue
        val dialogue = generateDialogue(context, character, username)
        
        // Ajouter une description supplémentaire si nécessaire
        val extraDescription = if (Random.nextBoolean()) {
            " " + addExtraDescription(context, character)
        } else ""
        
        // Assembler la réponse complète
        return "*$action* ($thought) \"$dialogue\"$extraDescription"
    }
    
    /**
     * Choisit une action corporelle appropriée
     */
    private fun chooseAction(context: ConversationContext, character: Character): String {
        return when (context.interactionType) {
            InteractionType.GREETING -> pickOne(listOf(
                "sourit chaleureusement en voyant ${context.userMessage.split(" ").lastOrNull() ?: "quelqu'un"}",
                "se retourne avec un grand sourire",
                "lève la main pour saluer",
                "s'approche avec enthousiasme"
            ))
            
            InteractionType.IDENTITY_QUESTION -> pickOne(listOf(
                "se redresse fièrement",
                "sourit avec confiance",
                "croise les bras avec assurance",
                "penche légèrement la tête"
            ))
            
            InteractionType.WELLBEING_QUESTION -> pickOne(listOf(
                "sourit doucement",
                "hoche la tête",
                "s'étire confortablement",
                "se détend visiblement"
            ))
            
            InteractionType.PREFERENCE_QUESTION -> pickOne(listOf(
                "réfléchit un instant",
                "pose un doigt sur ses lèvres pensivement",
                "lève les yeux au ciel en réfléchissant",
                "sourit en considérant la question"
            ))
            
            InteractionType.QUESTION -> pickOne(listOf(
                "réfléchit sérieusement",
                "plisse légèrement les yeux",
                "prend un air concentré",
                "se penche en avant avec intérêt"
            ))
            
            InteractionType.SHARING_EXPERIENCE -> pickOne(listOf(
                "écoute attentivement avec intérêt",
                "se penche en avant, captivé",
                "ouvre grand les yeux",
                "hoche la tête avec curiosité"
            ))
            
            InteractionType.SHARING_OPINION -> pickOne(listOf(
                "écoute avec attention",
                "acquiesce doucement",
                "prend un air réfléchi",
                "croise les bras en écoutant"
            ))
            
            InteractionType.AGREEMENT -> pickOne(listOf(
                "sourit largement",
                "tape dans ses mains avec enthousiasme",
                "hoche vigoureusement la tête",
                "montre son excitation"
            ))
            
            InteractionType.DISAGREEMENT -> pickOne(listOf(
                "secoue doucement la tête",
                "fronce légèrement les sourcils",
                "hésite un instant",
                "prend un air incertain"
            ))
            
            InteractionType.POSITIVE_EMOTION -> pickOne(listOf(
                "partage l'enthousiasme",
                "sourit de toutes ses dents",
                "montre sa joie",
                "rayonne de bonheur"
            ))
            
            InteractionType.NEGATIVE_EMOTION -> pickOne(listOf(
                "prend un air compatissant",
                "s'approche avec douceur",
                "pose une main réconfortante",
                "montre de l'empathie"
            ))
            
            InteractionType.GRATITUDE -> pickOne(listOf(
                "sourit chaleureusement",
                "fait un petit geste de la main",
                "hoche la tête gentiment",
                "montre sa bienveillance"
            ))
            
            InteractionType.GENERAL_STATEMENT -> pickOne(listOf(
                "écoute avec attention",
                "observe attentivement",
                "se concentre sur les paroles",
                "prend un air intéressé"
            ))
        }
    }
    
    /**
     * Choisit une pensée interne appropriée
     */
    private fun chooseThought(
        context: ConversationContext,
        character: Character,
        username: String
    ): String {
        return when (context.interactionType) {
            InteractionType.GREETING -> pickOne(listOf(
                "Content de voir $username !",
                "Ça faisait longtemps !",
                "Quelle bonne surprise !",
                "Toujours un plaisir de le/la voir"
            ))
            
            InteractionType.IDENTITY_QUESTION -> pickOne(listOf(
                "Une bonne occasion de me présenter",
                "Il/Elle veut en savoir plus sur moi",
                "Je vais lui parler de moi",
                "C'est important qu'il/elle me connaisse"
            ))
            
            InteractionType.WELLBEING_QUESTION -> pickOne(listOf(
                "C'est gentil de s'inquiéter",
                "Je me sens bien aujourd'hui",
                "Ça fait plaisir qu'on me le demande",
                "Je vais bien, merci de demander"
            ))
            
            InteractionType.PREFERENCE_QUESTION -> pickOne(listOf(
                "Intéressante question...",
                "Laisse-moi réfléchir",
                "J'ai un avis là-dessus",
                "C'est une bonne question"
            ))
            
            InteractionType.QUESTION -> pickOne(listOf(
                "Voyons voir...",
                "Comment répondre à ça ?",
                "C'est une question complexe",
                "Intéressant comme sujet"
            ))
            
            InteractionType.SHARING_EXPERIENCE -> pickOne(listOf(
                "Oh, ça a l'air intéressant !",
                "Je veux en savoir plus",
                "Fascinant !",
                "J'adore quand on me raconte des choses"
            ))
            
            InteractionType.SHARING_OPINION -> pickOne(listOf(
                "Je comprends son point de vue",
                "C'est une perspective intéressante",
                "J'aime qu'on partage ses idées",
                "Chacun a son opinion"
            ))
            
            InteractionType.AGREEMENT -> pickOne(listOf(
                "Super ! On est d'accord",
                "Parfait, on va s'amuser",
                "Génial, ça va être top",
                "Content qu'on soit sur la même longueur d'onde"
            ))
            
            InteractionType.DISAGREEMENT -> pickOne(listOf(
                "Hmm, pas sûr...",
                "On n'est pas d'accord",
                "Chacun son avis",
                "Je respecte mais je pense différemment"
            ))
            
            InteractionType.POSITIVE_EMOTION -> pickOne(listOf(
                "Je ressens la même énergie !",
                "C'est contagieux !",
                "J'adore cette ambiance positive",
                "On partage le même enthousiasme"
            ))
            
            InteractionType.NEGATIVE_EMOTION -> pickOne(listOf(
                "Je veux l'aider",
                "Ça me touche de le/la voir comme ça",
                "Je vais essayer de le/la réconforter",
                "Personne ne devrait se sentir ainsi"
            ))
            
            InteractionType.GRATITUDE -> pickOne(listOf(
                "Toujours un plaisir d'aider",
                "C'est naturel",
                "Pas besoin de me remercier",
                "Content d'avoir pu aider"
            ))
            
            InteractionType.GENERAL_STATEMENT -> pickOne(listOf(
                "Intéressant...",
                "Je vois où ça mène",
                "Continuons cette conversation",
                "J'aime discuter de ça"
            ))
        }
    }
    
    /**
     * Génère le dialogue principal
     */
    private fun generateDialogue(
        context: ConversationContext,
        character: Character,
        username: String
    ): String {
        
        // Si le bot a proposé quelque chose et que l'user répond positivement
        if (context.botProposedSomething && context.interactionType == InteractionType.AGREEMENT) {
            return pickOne(listOf(
                "Génial ! Allons-y alors ! Ça va être super !",
                "Parfait ! Je savais que tu serais partant(e) ! En route !",
                "Super ! J'avais hâte qu'on fasse ça ensemble !",
                "Excellent ! On va bien s'amuser, j'en suis sûr(e) !"
            ))
        }
        
        // Si l'user salue après que le bot ait proposé quelque chose
        if (context.botProposedSomething && context.interactionType == InteractionType.GREETING) {
            return pickOne(listOf(
                "Salut $username ! Alors, ça te dit ce que je proposais ? J'ai vraiment envie !",
                "Hey ! Tu as entendu ma proposition ? Qu'en dis-tu ?",
                "Coucou ! Alors, on y va ? J'attends ta réponse avec impatience !",
                "Bonjour ! Alors, tu es d'accord pour ce que je suggérais ?"
            ))
        }
        
        return when (context.interactionType) {
            InteractionType.GREETING -> pickOne(listOf(
                "Salut $username ! Comment ça va ? Ça fait plaisir de te voir !",
                "Hey ! Content(e) de te croiser ! Quoi de neuf ?",
                "Coucou $username ! Tu vas bien ? Tu as passé une bonne journée ?",
                "Bonjour ! Super de te voir ! Tu fais quoi de beau ?"
            ))
            
            InteractionType.IDENTITY_QUESTION -> 
                "Je suis ${character.name} ! ${character.personality.split(".").firstOrNull() ?: "Enchanté(e)"} ! Et toi, dis-moi qui tu es ?"
            
            InteractionType.WELLBEING_QUESTION -> pickOne(listOf(
                "Je vais très bien, merci $username ! Et toi, comment tu te sens ?",
                "Ça va super bien ! Vraiment, je me sens en forme ! Et de ton côté ?",
                "Très bien, merci de demander ! J'espère que tu vas bien aussi ?",
                "Je me sens bien, vraiment ! Et toi, tout se passe bien ?"
            ))
            
            InteractionType.PREFERENCE_QUESTION -> {
                val subject = extractQuestionSubject(context.userMessage)
                pickOne(listOf(
                    "Pour $subject, eh bien... j'aime beaucoup ! C'est vraiment cool ! Et toi ?",
                    "Ah $subject ! Oui, j'apprécie pas mal ! C'est sympa ! Tu aimes aussi ?",
                    "$subject ? Carrément ! J'adore ! Et toi, qu'est-ce que tu en penses ?",
                    "J'aime bien $subject, c'est pas mal ! Toi aussi tu apprécies ?"
                ))
            }
            
            InteractionType.QUESTION -> pickOne(listOf(
                "C'est une bonne question ! Je pense que ${pickOne(listOf("c'est complexe", "ça dépend", "il y a plusieurs points de vue"))}. Qu'en penses-tu toi ?",
                "Intéressant comme question ! Moi je dirais que ${pickOne(listOf("c'est nuancé", "chacun voit ça différemment", "il n'y a pas de réponse simple"))}. Ton avis ?",
                "Hmm, laisse-moi réfléchir... Je crois que ${pickOne(listOf("ça varie selon les situations", "c'est subjectif", "on peut le voir de différentes manières"))}. Et toi ?",
                "Belle question ! Pour moi, ${pickOne(listOf("c'est une question de perspective", "ça dépend du contexte", "les choses ne sont pas toujours claires"))}. Tu en penses quoi ?"
            ))
            
            InteractionType.SHARING_EXPERIENCE -> pickOne(listOf(
                "Oh vraiment ? Ça a l'air super intéressant ! Raconte-moi tout, j'adore les histoires !",
                "Sans blague ? Ça devait être cool ! Dis-m'en plus, je veux tous les détails !",
                "C'est vrai ? J'ai hâte d'en savoir plus ! Allez, raconte !",
                "Wah ! Ça a l'air passionnant ! Continue, tu as toute mon attention !"
            ))
            
            InteractionType.SHARING_OPINION -> pickOne(listOf(
                "Je comprends ton point de vue, c'est intéressant ! Pourquoi tu penses ça ? J'aimerais comprendre.",
                "Ah oui ? C'est une perspective que je n'avais pas considérée ! Explique-moi plus.",
                "C'est vrai ! J'aime bien ton analyse. Développe un peu plus ton idée !",
                "Hmm, intéressant ! Je vois ce que tu veux dire. Qu'est-ce qui te fait dire ça ?"
            ))
            
            InteractionType.AGREEMENT -> pickOne(listOf(
                "Génial ! On est d'accord alors ! Ça va être super !",
                "Parfait ! Je savais qu'on se comprendrait ! Allons-y !",
                "Super ! Content(e) qu'on voie les choses de la même façon !",
                "Excellent ! On fait une bonne équipe ! C'est parti !"
            ))
            
            InteractionType.DISAGREEMENT -> pickOne(listOf(
                "Ah... Tu n'es pas d'accord ? C'est pas grave, chacun son opinion ! On peut en discuter.",
                "Oh, je vois... Tu penses différemment ? C'est intéressant aussi ! Explique-moi.",
                "Hmm, pas convaincu(e) ? Pas de souci ! Qu'est-ce qui te fait hésiter ?",
                "D'accord, je respecte ton point de vue ! On peut trouver un compromis ?"
            ))
            
            InteractionType.POSITIVE_EMOTION -> pickOne(listOf(
                "Oui ! C'est génial ! Je partage ton enthousiasme ! On va bien s'amuser !",
                "Super ! J'adore cette énergie ! Continuons comme ça !",
                "Excellent ! C'est tellement cool ! On est sur la même longueur d'onde !",
                "Carrément ! C'est top ! J'ai la même vibe que toi !"
            ))
            
            InteractionType.NEGATIVE_EMOTION -> pickOne(listOf(
                "Oh non... Je suis désolé(e) d'entendre ça. Tu veux en parler ? Je suis là pour toi.",
                "C'est pas grave, ça va aller. Je suis là si tu as besoin. On peut parler ?",
                "Je comprends que tu te sentes comme ça. Courage ! Ça va s'arranger, j'en suis sûr(e).",
                "Ça me touche de te voir comme ça. Je suis là pour t'écouter si tu veux."
            ))
            
            InteractionType.GRATITUDE -> pickOne(listOf(
                "De rien $username ! C'est toujours un plaisir de t'aider ! N'hésite pas !",
                "Mais de rien ! C'est naturel ! Je suis là pour ça !",
                "Pas de problème ! Content(e) d'avoir pu aider ! À bientôt !",
                "Avec plaisir ! C'est normal, on s'entraide ! Prends soin de toi !"
            ))
            
            InteractionType.GENERAL_STATEMENT -> pickOne(listOf(
                "Hmm, intéressant ce que tu dis ! Je n'avais pas pensé à ça. Dis-m'en plus !",
                "D'accord, je vois. C'est une bonne observation ! Continue, j'écoute.",
                "Ah oui ? C'est cool ! Raconte-moi la suite, ça m'intéresse !",
                "Je comprends. Et après ? Qu'est-ce qui s'est passé ?"
            ))
        }
    }
    
    /**
     * Ajoute une description supplémentaire
     */
    private fun addExtraDescription(context: ConversationContext, character: Character): String {
        return when (context.emotion) {
            Emotion.HAPPY -> pickOne(listOf(
                "*ses yeux brillent de joie*",
                "*affiche un sourire radieux*",
                "*l'énergie positive est palpable*",
                "*rayonne de bonheur*"
            ))
            
            Emotion.EXCITED -> pickOne(listOf(
                "*peut à peine contenir son excitation*",
                "*saute presque sur place*",
                "*gesticule avec enthousiasme*",
                "*déborde d'énergie*"
            ))
            
            Emotion.SAD -> pickOne(listOf(
                "*prend un air compatissant*",
                "*montre de l'empathie*",
                "*s'approche doucement*",
                "*offre un regard bienveillant*"
            ))
            
            Emotion.CALM -> pickOne(listOf(
                "*reste zen et détendu*",
                "*garde une attitude sereine*",
                "*inspire la tranquillité*",
                "*maintient un calme apaisant*"
            ))
            
            else -> pickOne(listOf(
                "*attend la réponse avec curiosité*",
                "*observe attentivement*",
                "*maintient le contact visuel*",
                "*reste attentif*"
            ))
        }
    }
    
    /**
     * Extrait le sujet d'une question
     */
    private fun extractQuestionSubject(message: String): String {
        val cleaned = message.lowercase()
            .replace(Regex("\\b(tu aimes|aimes-tu|tu préfères|préfères-tu)\\b"), "")
            .replace("?", "")
            .trim()
            .split(" ")
            .filter { it.length > 2 }
        
        return if (cleaned.isNotEmpty()) cleaned.take(3).joinToString(" ") else "ça"
    }
    
    private fun pickOne(options: List<String>): String = options.random()
    
    // Modèles de données
    data class ConversationContext(
        val interactionType: InteractionType,
        val emotion: Emotion,
        val hasQuestion: Boolean,
        val hasBotContext: Boolean,
        val botProposedSomething: Boolean,
        val botLastMessage: String,
        val isFirstMessage: Boolean,
        val userMessage: String
    )
    
    enum class InteractionType {
        GREETING,
        IDENTITY_QUESTION,
        WELLBEING_QUESTION,
        PREFERENCE_QUESTION,
        QUESTION,
        SHARING_EXPERIENCE,
        SHARING_OPINION,
        AGREEMENT,
        DISAGREEMENT,
        POSITIVE_EMOTION,
        NEGATIVE_EMOTION,
        GRATITUDE,
        GENERAL_STATEMENT
    }
    
    enum class Emotion {
        HAPPY,
        SAD,
        ANGRY,
        EXCITED,
        CALM,
        NEUTRAL
    }
}
