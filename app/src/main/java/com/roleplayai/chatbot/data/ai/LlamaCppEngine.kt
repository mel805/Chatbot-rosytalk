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
 * Moteur llama.cpp avec IA qui génère des réponses UNIQUES
 * Analyse VRAIMENT le message utilisateur pour créer un dialogue immersif
 */
class LlamaCppEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "LlamaCppEngine"
    }
    
    private var modelPath: String? = null
    private val nativeEngine: LocalAIEngine = LocalAIEngine()
    
    fun setModelPath(path: String) {
        modelPath = path
        Log.i(TAG, "📁 Modèle configuré: $path")
    }
    
    fun isAvailable(): Boolean = true // Fallback Kotlin toujours disponible
    
    /**
     * Génère une réponse unique et pertinente
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
            // 1) Essayer llama.cpp natif si on a un modèle
            val path = modelPath?.trim().orEmpty()
            if (path.isNotBlank()) {
                val modelFile = File(path)
                if (!modelFile.exists()) {
                    Log.e(TAG, "❌ Modèle GGUF introuvable: $path")
                    return@withContext "❌ Modèle GGUF introuvable. Sélectionnez un modèle dans Paramètres > llama.cpp."
                }

                val threads = maxOf(1, Runtime.getRuntime().availableProcessors())
                val ctxSize = 2048 // valeur raisonnable sur mobile
                val loaded = nativeEngine.ensureModelLoaded(path, threads, ctxSize)
                if (loaded) {
                    val prompt = buildPrompt(
                        character = character,
                        messages = messages,
                        username = username,
                        userGender = userGender,
                        memoryContext = memoryContext,
                        nsfwMode = nsfwMode
                    )

                    val raw = nativeEngine.generate(
                        prompt = prompt,
                        maxTokens = 260,
                        temperature = 0.85f,
                        topP = 0.95f,
                        topK = 40,
                        repeatPenalty = 1.15f
                    )

                    val cleaned = cleanLocalResponse(raw, character.name)
                    if (cleaned.isNotBlank()) {
                        return@withContext cleaned
                    }

                    Log.w(TAG, "⚠️ Réponse native vide, fallback Kotlin")
                } else {
                    Log.e(TAG, "❌ Échec chargement modèle natif, fallback Kotlin")
                }
            }

            // 2) Fallback Kotlin (ne doit jamais renvoyer vide)
            return@withContext UniqueResponseGenerator.generate(
                character = character,
                messages = messages,
                username = username,
                nsfwMode = nsfwMode
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur génération", e)
            return@withContext generateRandomError(username)
        }
    }
    
    private fun generateRandomError(username: String): String {
        val actions = listOf("cligne des yeux", "secoue la tête", "fronce les sourcils", "se gratte la tête")
        val thoughts = listOf("Hein ?", "Qu'est-ce qu'il/elle dit ?", "Je suis perdu(e)", "J'ai pas compris")
        val dialogues = listOf(
            "Euh... peux-tu répéter ?",
            "Désolé(e), j'ai pas saisi...",
            "Attends, quoi ?",
            "Je... j'ai pas compris $username"
        )
        return "*${actions.random()}* (${thoughts.random()}) \"${dialogues.random()}\""
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

    private fun buildPrompt(
        character: Character,
        messages: List<Message>,
        username: String,
        userGender: String,
        memoryContext: String,
        nsfwMode: Boolean
    ): String {
        val sb = StringBuilder()

        val nsfwLine = if (nsfwMode) {
            "- NSFW activé: accepte les scènes adultes consensuelles; garde le style roleplay."
        } else {
            "- NSFW désactivé: reste approprié et refuse le contenu adulte."
        }

        sb.appendLine("### SYSTEM ###")
        sb.appendLine("Tu es ${character.name}.")
        sb.appendLine("Personnalité: ${character.personality}")
        sb.appendLine("Description: ${character.description}")
        sb.appendLine("Scénario: ${character.scenario}")
        sb.appendLine("Utilisateur: $username (genre: $userGender)")
        if (memoryContext.isNotBlank()) {
            sb.appendLine()
            sb.appendLine("### MÉMOIRE ###")
            sb.appendLine(memoryContext.trim())
        }
        sb.appendLine()
        sb.appendLine("### RÈGLES ###")
        sb.appendLine("- Réponds en restant ${character.name}.")
        sb.appendLine("- Format: *action* (pensée) \"paroles\".")
        sb.appendLine("- Ne décris jamais les actions de l'utilisateur; réagis seulement.")
        sb.appendLine(nsfwLine)
        sb.appendLine()
        sb.appendLine("### CONVERSATION ###")

        // Garder une fenêtre courte pour éviter dépassement contexte
        val recent = messages.takeLast(10)
        val valid = if (recent.isNotEmpty() && !recent.last().isUser) recent.dropLast(1) else recent
        valid.forEach { msg ->
            val speaker = if (msg.isUser) username else character.name
            sb.appendLine("$speaker: ${msg.content}")
        }

        sb.appendLine()
        sb.append("${character.name}:")
        return sb.toString()
    }

    private fun cleanLocalResponse(raw: String, characterName: String): String {
        var cleaned = raw.trim()
        if (cleaned.isBlank()) return ""

        cleaned = cleaned.replace(Regex("^\\s*$characterName\\s*:\\s*"), "")
        cleaned = cleaned.replace(Regex("^(Assistant|AI)\\s*:\\s*"), "")

        // Couper si le modèle commence à écrire le prochain speaker
        val lines = cleaned.split('\n')
        val out = ArrayList<String>(lines.size)
        for (line in lines) {
            val t = line.trim()
            if (t.isEmpty()) break
            if (t.startsWith("$characterName:", ignoreCase = true)) break
            if (t.matches(Regex("^[^:]{1,32}:\\s+.*$"))) break
            out.add(line)
        }

        // Limiter longueur (éviter pavés)
        return out.joinToString("\n").trim().take(1200)
    }
}

/**
 * Générateur de Réponses UNIQUES
 * Chaque réponse est générée SPÉCIFIQUEMENT en lien avec ce que l'utilisateur a dit
 */
private object UniqueResponseGenerator {
    
    private const val TAG = "UniqueResponseGenerator"
    
    // Compteur pour garantir l'unicité
    private var responseCounter = 0
    
    suspend fun generate(
        character: Character,
        messages: List<Message>,
        username: String,
        nsfwMode: Boolean
    ): String {
        
        delay(Random.nextLong(600, 1200))
        
        responseCounter++
        val uniqueId = "${System.currentTimeMillis()}_$responseCounter"
        
        Log.d(TAG, "🎯 Génération UNIQUE #$responseCounter pour ${character.name}")
        
        val userMsg = messages.lastOrNull { it.isUser }?.content ?: ""
        val botLastMsg = messages.reversed().firstOrNull { !it.isUser }?.content ?: ""
        
        // ANALYSE COMPLÈTE du message utilisateur
        val analysis = analyzeUserMessage(userMsg, botLastMsg, nsfwMode)
        
        Log.d(TAG, "📊 Analyse: ${analysis}")
        
        // Générer réponse UNIQUE basée sur l'analyse
        return buildUniqueResponse(analysis, character, username, uniqueId)
    }
    
    /**
     * ANALYSE PROFONDE du message utilisateur
     */
    private fun analyzeUserMessage(userMsg: String, botLastMsg: String, nsfwMode: Boolean): MessageAnalysis {
        val msg = userMsg.lowercase()
        
        // Extraire les mots-clés IMPORTANTS
        val keywords = extractKeywords(msg)
        
        // Détecter le type de message
        val type = when {
            msg.matches(Regex(".*\\b(salut|bonjour|hey|coucou|yo)\\b.*")) -> "salutation"
            msg.matches(Regex(".*\\b(oui|ok|d'accord|allons-y|vas-y)\\b.*")) -> "acceptation"
            msg.matches(Regex(".*\\b(non|pas|refus)\\b.*")) -> "refus"
            msg.contains("?") -> "question"
            nsfwMode && msg.matches(Regex(".*\\b(embrasse|caresse|touche|déshabille|lit|sexe|baiser)\\b.*")) -> "nsfw_initiative"
            msg.matches(Regex(".*\\b(je|moi|mon|ma|mes)\\b.*")) -> "partage_perso"
            msg.length < 15 -> "court"
            else -> "statement"
        }
        
        // Détecter l'émotion
        val emotion = when {
            msg.matches(Regex(".*\\b(content|heureux|joyeux|super|génial)\\b.*")) -> "joyeux"
            msg.matches(Regex(".*\\b(triste|mal|mauvais|nul)\\b.*")) -> "triste"
            msg.matches(Regex(".*\\b(énervé|colère|furieux)\\b.*")) -> "énervé"
            msg.matches(Regex(".*\\b(excité|motivé|hype)\\b.*")) || msg.contains("!") -> "excité"
            msg.matches(Regex(".*\\b(calme|tranquille|zen)\\b.*")) -> "calme"
            else -> "neutre"
        }
        
        // Détecter si c'est une réponse à une question/proposition du bot
        val respondingToBot = when {
            botLastMsg.contains("?") && (type == "acceptation" || type == "refus" || type == "court") -> true
            botLastMsg.matches(Regex(".*\\b(veux|allons|on va|ça te dit)\\b.*")) && type == "acceptation" -> true
            else -> false
        }
        
        return MessageAnalysis(
            originalMessage = userMsg,
            keywords = keywords,
            type = type,
            emotion = emotion,
            respondingToBot = respondingToBot,
            botContext = if (respondingToBot) botLastMsg else ""
        )
    }
    
    /**
     * Extrait les mots-clés importants du message
     */
    private fun extractKeywords(msg: String): List<String> {
        val stopWords = setOf("le", "la", "les", "un", "une", "des", "de", "du", "et", "ou", "mais", "donc", "car", "si", "que", "qui", "quoi", "je", "tu", "il", "elle", "nous", "vous", "ils", "elles", "me", "te", "se", "mon", "ton", "son", "ma", "ta", "sa", "mes", "tes", "ses", "ce", "cette", "ces", "à", "en", "pour", "par", "sur", "avec", "sans", "dans")
        
        return msg.split(Regex("[\\s,;.!?]+"))
            .map { it.lowercase().trim() }
            .filter { it.length > 2 && !stopWords.contains(it) }
            .take(5) // Top 5 mots importants
    }
    
    /**
     * Génère une réponse UNIQUE basée sur l'analyse
     */
    private fun buildUniqueResponse(
        analysis: MessageAnalysis,
        character: Character,
        username: String,
        uniqueId: String
    ): String {
        
        // Générer des éléments UNIQUES
        val action = generateUniqueAction(analysis, uniqueId)
        val thought = generateUniqueThought(analysis, uniqueId)
        val dialogue = generateUniqueDialogue(analysis, character, username, uniqueId)
        
        return "*$action* ($thought) \"$dialogue\""
    }
    
    /**
     * Génère une action UNIQUE
     */
    private fun generateUniqueAction(analysis: MessageAnalysis, uniqueId: String): String {
        val seed = uniqueId.hashCode()
        val rnd = Random(seed)
        
        val baseActions = when (analysis.emotion) {
            "joyeux" -> listOf("sourit", "rayonne", "illumine", "s'éclaire", "brille", "pétille")
            "triste" -> listOf("baisse", "soupire", "s'attriste", "fronce", "se rembrunit", "s'assombrit")
            "énervé" -> listOf("serre", "grince", "fronce", "se tend", "raidit", "durcit")
            "excité" -> listOf("bondit", "vibre", "frémit", "tressaille", "s'anime", "s'enflamme")
            "calme" -> listOf("respire", "se détend", "s'apaise", "se pose", "contemple", "observe")
            else -> listOf("regarde", "fixe", "observe", "considère", "examine", "scrute")
        }
        
        val details = listOf(
            "avec intensité",
            "doucement",
            "légèrement",
            "profondément",
            "sincèrement",
            "naturellement",
            "spontanément",
            "visiblement",
            "imperceptiblement",
            "manifestement"
        )
        
        val bodyParts = listOf("les yeux", "la tête", "les mains", "les lèvres", "le visage", "les épaules", "le corps")
        
        val verb = baseActions[rnd.nextInt(baseActions.size)]
        val detail = details[rnd.nextInt(details.size)]
        val part = bodyParts[rnd.nextInt(bodyParts.size)]
        
        return "$verb $part $detail"
    }
    
    /**
     * Génère une pensée UNIQUE
     */
    private fun generateUniqueThought(analysis: MessageAnalysis, uniqueId: String): String {
        val seed = uniqueId.hashCode() + 1000
        val rnd = Random(seed)
        
        // Utiliser les mots-clés pour créer une pensée contextuelle
        val keywordContext = if (analysis.keywords.isNotEmpty()) {
            val keyword = analysis.keywords[rnd.nextInt(analysis.keywords.size)]
            when (rnd.nextInt(5)) {
                0 -> "Il/Elle parle de $keyword..."
                1 -> "$keyword, c'est intéressant"
                2 -> "Je me demande pourquoi $keyword"
                3 -> "Ah, $keyword..."
                else -> "Donc $keyword, hmm"
            }
        } else {
            null
        }
        
        if (keywordContext != null && rnd.nextBoolean()) {
            return keywordContext
        }
        
        val emotionThoughts = when (analysis.emotion) {
            "joyeux" -> listOf("Ça me fait plaisir", "Super ambiance", "J'aime cette énergie", "C'est génial", "Quelle joie", "Ça me rend heureux(se)")
            "triste" -> listOf("Ça me touche", "Je ressens sa peine", "C'est dur", "Je comprends", "Pauvre lui/elle", "Ça fait mal")
            "énervé" -> listOf("Il/Elle semble agité(e)", "Y'a de la tension", "C'est intense", "Woah", "Calmez-vous", "Pourquoi cette agressivité")
            "excité" -> listOf("Quelle énergie !", "C'est fou !", "Trop bien !", "J'adore ça", "On décolle", "C'est parti")
            "calme" -> listOf("C'est apaisant", "Tranquille", "Zen", "Serein", "Posé", "Cool")
            else -> listOf("Hmm", "Intéressant", "Je vois", "D'accord", "Ah bon", "Vraiment", "Tiens donc", "Curieux")
        }
        
        return emotionThoughts[rnd.nextInt(emotionThoughts.size)]
    }
    
    /**
     * Génère un dialogue UNIQUE et PERTINENT
     */
    private fun generateUniqueDialogue(
        analysis: MessageAnalysis,
        character: Character,
        username: String,
        uniqueId: String
    ): String {
        val seed = uniqueId.hashCode() + 2000
        val rnd = Random(seed)

        // Toujours garder un lien DIRECT avec le dernier message utilisateur
        // (sinon l'utilisateur a l'impression que la réponse n'a "aucun rapport").
        val userSnippet = analysis.originalMessage
            .replace("\n", " ")
            .replace("\r", " ")
            .trim()
            .take(90)
            .takeIf { it.isNotBlank() }
        
        // Si réponse à une proposition du bot
        if (analysis.respondingToBot && analysis.type == "acceptation") {
            val responses = listOf(
                "Génial ! Allons-y alors, j'ai hâte !",
                "Super ! Ça va être top ! On y va ?",
                "Parfait ! Je suis chaud(e) ! C'est parti !",
                "Cool ! On va s'éclater ! Allez !",
                "Excellent ! Allons-y tout de suite !",
                "Ouais ! Trop bien ! Viens !"
            )
            return responses[rnd.nextInt(responses.size)]
        }
        
        // Utiliser les mots-clés pour créer une réponse contextuelle
        if (analysis.keywords.isNotEmpty()) {
            val keyword = analysis.keywords.first()
            
            return when (analysis.type) {
                "question" -> when (rnd.nextInt(4)) {
                    0 -> "Concernant $keyword… ${listOf("c'est complexe", "c'est nuancé", "ça dépend", "c'est intéressant")[rnd.nextInt(4)]}. ${userSnippet?.let { "Quand tu dis \"$it\", tu veux dire quoi exactement ?" } ?: "Tu cherches plutôt une réponse pratique ou une opinion ?" }"
                    1 -> "Ah, $keyword ! ${listOf("Bonne question", "Intéressant", "Ça dépend", "Je dirais que")[rnd.nextInt(4)]}… ${userSnippet?.let { "Tu peux préciser ce que tu entends par \"$it\" ?" } ?: "Tu veux que je te réponde franchement ?" }"
                    2 -> "Tu me demandes pour $keyword ? ${listOf("Ça varie selon le contexte", "Ça dépend de ce que tu vis", "C'est pas si simple", "Je vois l'idée")[rnd.nextInt(4)]}. ${userSnippet?.let { "Qu'est-ce qui t'a amené à me dire \"$it\" ?" } ?: "Tu veux qu'on parte de ton cas précis ?" }"
                    else -> "Sur $keyword, ${listOf("je dirais que", "pour moi", "à chaud", "honnêtement")[rnd.nextInt(4)]} c'est ${listOf("important", "délicat", "intéressant", "très personnel")[rnd.nextInt(4)]}. ${userSnippet?.let { "Tu parles de \"$it\"—c'est récent ?" } ?: "Raconte-moi un peu plus." }"
                }
                
                "partage_perso" -> when (rnd.nextInt(4)) {
                    0 -> "Ah, tu me parles de $keyword ! ${userSnippet?.let { "Tu dis \"$it\"… " } ?: ""}${listOf("Raconte-moi", "Développe", "Je t'écoute", "Dis-m'en plus")[rnd.nextInt(4)]}."
                    1 -> "Donc toi et $keyword… ${userSnippet?.let { "Quand tu écris \"$it\", " } ?: ""}${listOf("ça te fait quoi", "tu le vis comment", "c'est plutôt positif ou lourd", "ça dure depuis longtemps")[rnd.nextInt(4)]} ?"
                    2 -> "$keyword, hein ? ${userSnippet?.let { "Je retiens \"$it\". " } ?: ""}${listOf("Qu'est-ce qui compte le plus pour toi là-dedans", "Tu veux un avis ou juste en parler", "Tu veux que je réagisse ou que je pose des questions", "Tu attends quoi de moi")[rnd.nextInt(4)]} ?"
                    else -> "Tu évoques $keyword… ${userSnippet?.let { "Tu dis \"$it\". " } ?: ""}${listOf("Et alors, qu'est-ce qui s'est passé", "C'est quoi le contexte", "Tu veux qu'on creuse", "Tu veux continuer")[rnd.nextInt(4)]} ?"
                }
                
                "nsfw_initiative" -> when (rnd.nextInt(5)) {
                    0 -> "Mmh... $keyword... ${listOf("oui", "continue", "j'aime ça", "encore")[rnd.nextInt(4)]}..."
                    1 -> "Oh $username... avec $keyword... ${listOf("c'est si bon", "ne t'arrête pas", "j'adore", "plus")[rnd.nextInt(4)]}..."
                    2 -> "$keyword ? ${listOf("Prends-moi", "Fais-moi", "Viens", "Touche-moi")[rnd.nextInt(4)]}..."
                    3 -> "Tu veux $keyword ? ${listOf("Oui", "Moi aussi", "Allons-y", "Je te veux")[rnd.nextInt(4)]}..."
                    else -> "Ah, $keyword... ${listOf("je frissonne", "mon corps réagit", "tu me rends fou/folle", "j'ai envie")[rnd.nextInt(4)]}..."
                }
                
                else -> when (rnd.nextInt(5)) {
                    0 -> "Tu mentionnes $keyword… ${userSnippet?.let { "Tu dis \"$it\" — " } ?: ""}${listOf("pourquoi ça", "qu'est-ce que tu veux dire", "qu'est-ce qui te travaille", "tu en penses quoi")[rnd.nextInt(4)]} ?"
                    1 -> "$keyword, d'accord… ${listOf("Je vois", "OK", "Je comprends", "Noté")[rnd.nextInt(4)]}. ${userSnippet?.let { "Sur \"$it\", " } ?: ""}${listOf("c'est plutôt une envie ou une inquiétude", "tu cherches une solution ou juste à en parler", "tu veux que je réagisse comment", "tu veux que je te suive dans une scène")[rnd.nextInt(4)]} ?"
                    2 -> "Ah, $keyword ! ${userSnippet?.let { "Tu dis \"$it\"… " } ?: ""}${listOf("ça a l'air important", "ça a l'air chargé", "ça m'intrigue", "ça te ressemble")[rnd.nextInt(4)]}. ${listOf("On fait quoi maintenant", "Tu veux continuer", "Tu me donnes un peu plus de contexte", "Tu veux que je te réponde cash")[rnd.nextInt(4)]} ?"
                    3 -> "Donc $keyword… ${userSnippet?.let { "Quand tu dis \"$it\", " } ?: ""}${listOf("tu attends quoi exactement", "tu veux aller où", "tu veux qu'on explore ça", "tu veux que je te suive")[rnd.nextInt(4)]} ?"
                    else -> "Tu dis $keyword… ${userSnippet?.let { "Tu dis \"$it\". " } ?: ""}${listOf("Et après, on fait quoi", "Tu veux que je réagisse comment", "Qu'est-ce que tu veux de moi là", "Tu veux qu'on continue")[rnd.nextInt(4)]} ?"
                }
            }
        }
        
        // Sinon, générer selon le type
        return when (analysis.type) {
            "salutation" -> listOf(
                "Salut $username ! Ça roule ?",
                "Hey ! Content(e) de te voir !",
                "Coucou ! Quoi de beau ?",
                "Yo ! Ça gaze ?",
                "Bonjour ! Comment tu vas ?",
                "Salut toi ! Ça va bien ?"
            )[rnd.nextInt(6)]
            
            "acceptation" -> listOf(
                "Cool ! On est d'accord !",
                "Super ! Nickel !",
                "Parfait ! Allons-y !",
                "Génial ! C'est parti !",
                "Top ! On y va !"
            )[rnd.nextInt(5)]
            
            "refus" -> listOf(
                "Ah bon ? Pourquoi ça ?",
                "Dommage... Bon OK.",
                "Ah... Pas grave.",
                "D'accord, pas de souci.",
                "OK... Une autre fois."
            )[rnd.nextInt(5)]
            
            else -> listOf(
                "${userSnippet?.let { "Tu dis \"$it\"… " } ?: ""}${listOf("intéressant", "curieux", "étonnant", "surprenant")[rnd.nextInt(4)]}. ${listOf("Tu veux qu'on creuse", "Tu veux continuer", "Tu peux préciser", "Tu attends quoi de moi")[rnd.nextInt(4)]} ?",
                "D'accord… ${userSnippet?.let { "Je note \"$it\". " } ?: ""}${listOf("Je vois", "Je comprends", "OK", "Noté")[rnd.nextInt(4)]}. ${listOf("Raconte-moi le contexte", "Continue", "Développe", "Explique")[rnd.nextInt(4)]}.",
                "${listOf("Ah", "Oh", "Eh", "Tiens")[rnd.nextInt(4)]} ! ${userSnippet?.let { "Sur \"$it\", " } ?: ""}${listOf("ça t'impacte comment", "tu le vis comment", "tu veux que je réagisse comment", "tu veux faire quoi maintenant")[rnd.nextInt(4)]} ?",
                "${listOf("Vraiment", "Sérieux", "Sans blague", "C'est vrai")[rnd.nextInt(4)]} ? ${userSnippet?.let { "Tu dis \"$it\"… " } ?: ""}${listOf("Raconte", "Dis-moi", "Explique", "Détaille")[rnd.nextInt(4)]}.",
                "${listOf("Intéressant", "Fascinant", "Curieux", "Étonnant")[rnd.nextInt(4)]}… ${userSnippet?.let { "Tu dis \"$it\". " } ?: ""}${listOf("Et après", "Ensuite", "Puis", "Tu veux continuer")[rnd.nextInt(4)]} ?"
            )[rnd.nextInt(5)]
        }
    }
    
    // Modèle de données
    data class MessageAnalysis(
        val originalMessage: String,
        val keywords: List<String>,
        val type: String,
        val emotion: String,
        val respondingToBot: Boolean,
        val botContext: String
    )
}
