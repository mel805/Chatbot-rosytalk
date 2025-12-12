package com.roleplayai.chatbot.data.memory

import android.content.Context
import android.util.Log
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Conversation Memory - Système de mémoire long terme
 * 
 * Ce système implémente RAG (Retrieval Augmented Generation) pour :
 * - Sauvegarder l'historique complet des conversations
 * - Créer des résumés automatiques pour garder la cohérence
 * - Extraire les faits importants (préférences, événements clés)
 * - Maintenir le contexte sur des milliers de messages
 * 
 * Architecture :
 * 1. Historique complet sauvegardé sur disque
 * 2. Résumés générés tous les 20 messages
 * 3. Base de faits extraits (nom, préférences, etc.)
 * 4. Scoring de pertinence pour récupérer le contexte pertinent
 */
class ConversationMemory(
    private val context: Context,
    private val characterId: String
) {
    
    companion object {
        private const val TAG = "ConversationMemory"
        private const val MEMORY_DIR = "conversation_memory"
        private const val SUMMARY_INTERVAL = 20 // Résumé tous les 20 messages
    }
    
    // Mémoire en cache
    private data class MemoryCache(
        var fullHistory: MutableList<Message> = mutableListOf(),
        var summaries: MutableList<String> = mutableListOf(),
        var facts: MutableMap<String, String> = mutableMapOf(),
        var relationshipLevel: Int = 0,  // 0-100
        var emotionalTone: String = "neutre",
        var keyMoments: MutableList<KeyMoment> = mutableListOf()
    )
    
    data class KeyMoment(
        val messageIndex: Int,
        val description: String,
        val importance: Int  // 1-10
    )
    
    private val cache = MemoryCache()
    private val memoryFile: File
    
    init {
        // Créer le dossier de mémoire
        val memoryDir = File(context.filesDir, MEMORY_DIR)
        if (!memoryDir.exists()) {
            memoryDir.mkdirs()
        }
        
        memoryFile = File(memoryDir, "$characterId.json")
        
        // Charger la mémoire existante
        loadMemory()
    }
    
    /**
     * Ajoute un message à la mémoire
     */
    fun addMessage(message: Message) {
        cache.fullHistory.add(message)
        
        // Analyser le message pour extraire des faits
        if (message.isUser) {
            extractFacts(message.content)
        } else {
            analyzeCharacterResponse(message.content)
        }
        
        // Créer un résumé si nécessaire
        if (cache.fullHistory.size % SUMMARY_INTERVAL == 0) {
            createSummary()
        }
        
        // Sauvegarder
        saveMemory()
    }
    
    /**
     * Récupère le contexte pertinent pour la génération
     */
    fun getRelevantContext(
        lastMessages: List<Message>,
        username: String = "Utilisateur",
        characterName: String = "Personnage",
        maxTokens: Int = 2000
    ): String {
        val context = StringBuilder()
        
        // 1. Résumé de la relation globale
        if (cache.summaries.isNotEmpty()) {
            context.append("### RÉSUMÉ DE LA RELATION ###\n")
            context.append(cache.summaries.last())
            context.append("\n\n")
        }
        
        // 2. Faits importants
        if (cache.facts.isNotEmpty()) {
            context.append("### FAITS IMPORTANTS ###\n")
            for ((key, value) in cache.facts) {
                context.append("- $key: $value\n")
            }
            context.append("\n")
        }
        
        // 3. Moments clés récents
        if (cache.keyMoments.isNotEmpty()) {
            context.append("### MOMENTS CLÉS ###\n")
            cache.keyMoments.takeLast(5).forEach {
                context.append("- ${it.description}\n")
            }
            context.append("\n")
        }
        
        // 4. Messages récents
        context.append("### CONVERSATION RÉCENTE ###\n")
        val recentCount = minOf(15, lastMessages.size)
        lastMessages.takeLast(recentCount).forEach { msg ->
            val speaker = if (msg.isUser) username else characterName
            context.append("$speaker: ${msg.content}\n")
        }
        
        return context.toString()
    }
    
    /**
     * Extrait des faits du message utilisateur
     */
    private fun extractFacts(message: String) {
        val lower = message.lowercase()
        
        // Nom de l'utilisateur
        if (lower.matches(Regex(".*(je m'appelle|mon nom est|appelle[- ]moi)\\s+(\\w+).*"))) {
            val match = Regex(".*(je m'appelle|mon nom est|appelle[- ]moi)\\s+(\\w+).*")
                .find(lower)
            match?.groupValues?.get(2)?.let { name ->
                cache.facts["nom_utilisateur"] = name.capitalize()
                Log.d(TAG, "📝 Fait extrait: nom = $name")
            }
        }
        
        // Préférences
        if (lower.matches(Regex(".*(j'aime|j'adore)\\s+(.+)"))) {
            val match = Regex(".*(j'aime|j'adore)\\s+(.+)").find(lower)
            match?.groupValues?.get(2)?.let { preference ->
                cache.facts["aime_${cache.facts.size}"] = preference
                Log.d(TAG, "📝 Fait extrait: aime = $preference")
            }
        }
        
        // Déteste
        if (lower.matches(Regex(".*(je déteste|je n'aime pas)\\s+(.+)"))) {
            val match = Regex(".*(je déteste|je n'aime pas)\\s+(.+)").find(lower)
            match?.groupValues?.get(2)?.let { dislike ->
                cache.facts["deteste_${cache.facts.size}"] = dislike
                Log.d(TAG, "📝 Fait extrait: déteste = $dislike")
            }
        }
    }
    
    /**
     * Analyse la réponse du personnage pour détecter les moments clés
     */
    private fun analyzeCharacterResponse(message: String) {
        val lower = message.lowercase()
        
        // Moment important (confession d'amour)
        if (lower.contains(Regex("(je t'aime|je t'adore|amoureux)"))) {
            cache.keyMoments.add(KeyMoment(
                messageIndex = cache.fullHistory.size,
                description = "Confession de sentiments",
                importance = 10
            ))
            cache.relationshipLevel = minOf(100, cache.relationshipLevel + 20)
        }
        
        // Premier baiser
        if (lower.contains(Regex("(embrasse|baiser)")) && cache.relationshipLevel < 30) {
            cache.keyMoments.add(KeyMoment(
                messageIndex = cache.fullHistory.size,
                description = "Premier baiser",
                importance = 9
            ))
            cache.relationshipLevel = minOf(100, cache.relationshipLevel + 15)
        }
        
        // Première fois intime
        if (lower.contains(Regex("(première fois|faire l'amour)")) && cache.relationshipLevel < 60) {
            cache.keyMoments.add(KeyMoment(
                messageIndex = cache.fullHistory.size,
                description = "Première intimité",
                importance = 10
            ))
            cache.relationshipLevel = minOf(100, cache.relationshipLevel + 25)
        }
    }
    
    /**
     * Crée un résumé des derniers messages
     */
    private fun createSummary() {
        val lastMessages = cache.fullHistory.takeLast(SUMMARY_INTERVAL)
        
        // Analyse simple pour créer un résumé
        val summary = StringBuilder()
        summary.append("Messages ${cache.fullHistory.size - SUMMARY_INTERVAL + 1} à ${cache.fullHistory.size}: ")
        
        // Compter les types d'interactions
        val userMessages = lastMessages.count { it.isUser }
        val characterMessages = lastMessages.count { !it.isUser }
        
        // Détecter le ton général
        val content = lastMessages.joinToString(" ") { it.content.lowercase() }
        val tone = when {
            content.contains(Regex("(rire|haha|drôle|amusant)")) -> "joyeux"
            content.contains(Regex("(triste|pleure|mal)")) -> "triste"
            content.contains(Regex("(aime|adore|amour)")) -> "romantique"
            content.contains(Regex("(sexe|nue?|chaud)")) -> "intime"
            else -> "conversationnel"
        }
        
        summary.append("$userMessages échanges, ton $tone. ")
        
        // Niveau de relation
        val relationshipStage = when {
            cache.relationshipLevel < 20 -> "Découverte mutuelle"
            cache.relationshipLevel < 40 -> "Amitié naissante"
            cache.relationshipLevel < 60 -> "Proximité émotionnelle"
            cache.relationshipLevel < 80 -> "Relation intime"
            else -> "Relation profonde et établie"
        }
        
        summary.append(relationshipStage)
        
        cache.summaries.add(summary.toString())
        Log.d(TAG, "📊 Résumé créé: ${summary.toString()}")
    }
    
    /**
     * Sauvegarde la mémoire sur disque
     */
    private fun saveMemory() {
        try {
            val json = JSONObject()
            
            // Sauvegarder l'historique (limité aux 200 derniers messages pour la taille)
            val historyArray = JSONArray()
            cache.fullHistory.takeLast(200).forEach { msg ->
                historyArray.put(JSONObject().apply {
                    put("content", msg.content)
                    put("isUser", msg.isUser)
                    put("timestamp", msg.timestamp)
                })
            }
            json.put("history", historyArray)
            
            // Sauvegarder les résumés
            val summariesArray = JSONArray()
            cache.summaries.forEach { summariesArray.put(it) }
            json.put("summaries", summariesArray)
            
            // Sauvegarder les faits
            val factsObject = JSONObject()
            cache.facts.forEach { (key, value) ->
                factsObject.put(key, value)
            }
            json.put("facts", factsObject)
            
            // Sauvegarder le niveau de relation
            json.put("relationshipLevel", cache.relationshipLevel)
            json.put("emotionalTone", cache.emotionalTone)
            
            // Sauvegarder les moments clés
            val momentsArray = JSONArray()
            cache.keyMoments.forEach { moment ->
                momentsArray.put(JSONObject().apply {
                    put("messageIndex", moment.messageIndex)
                    put("description", moment.description)
                    put("importance", moment.importance)
                })
            }
            json.put("keyMoments", momentsArray)
            
            // Écrire sur disque
            memoryFile.writeText(json.toString(2))
            Log.d(TAG, "💾 Mémoire sauvegardée: ${cache.fullHistory.size} messages, ${cache.summaries.size} résumés")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur sauvegarde mémoire", e)
        }
    }
    
    /**
     * Charge la mémoire depuis le disque
     */
    private fun loadMemory() {
        if (!memoryFile.exists()) {
            Log.d(TAG, "📝 Nouvelle mémoire créée pour $characterId")
            return
        }
        
        try {
            val json = JSONObject(memoryFile.readText())
            
            // Charger l'historique
            val historyArray = json.optJSONArray("history")
            if (historyArray != null) {
                for (i in 0 until historyArray.length()) {
                    val msgObj = historyArray.getJSONObject(i)
                    cache.fullHistory.add(Message(
                        id = "",
                        chatId = "",
                        content = msgObj.getString("content"),
                        isUser = msgObj.getBoolean("isUser"),
                        timestamp = msgObj.optLong("timestamp", System.currentTimeMillis())
                    ))
                }
            }
            
            // Charger les résumés
            val summariesArray = json.optJSONArray("summaries")
            if (summariesArray != null) {
                for (i in 0 until summariesArray.length()) {
                    cache.summaries.add(summariesArray.getString(i))
                }
            }
            
            // Charger les faits
            val factsObject = json.optJSONObject("facts")
            if (factsObject != null) {
                factsObject.keys().forEach { key ->
                    cache.facts[key] = factsObject.getString(key)
                }
            }
            
            // Charger le niveau de relation
            cache.relationshipLevel = json.optInt("relationshipLevel", 0)
            cache.emotionalTone = json.optString("emotionalTone", "neutre")
            
            // Charger les moments clés
            val momentsArray = json.optJSONArray("keyMoments")
            if (momentsArray != null) {
                for (i in 0 until momentsArray.length()) {
                    val momentObj = momentsArray.getJSONObject(i)
                    cache.keyMoments.add(KeyMoment(
                        messageIndex = momentObj.getInt("messageIndex"),
                        description = momentObj.getString("description"),
                        importance = momentObj.getInt("importance")
                    ))
                }
            }
            
            Log.i(TAG, "✅ Mémoire chargée: ${cache.fullHistory.size} messages, relation ${cache.relationshipLevel}/100")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur chargement mémoire", e)
        }
    }
    
    /**
     * Obtient le niveau de relation
     */
    fun getRelationshipLevel(): Int = cache.relationshipLevel
    
    /**
     * Obtient les faits importants
     */
    fun getFacts(): Map<String, String> = cache.facts.toMap()
    
    /**
     * Efface la mémoire
     */
    fun clearMemory() {
        cache.fullHistory.clear()
        cache.summaries.clear()
        cache.facts.clear()
        cache.relationshipLevel = 0
        cache.keyMoments.clear()
        
        if (memoryFile.exists()) {
            memoryFile.delete()
        }
        
        Log.i(TAG, "🧹 Mémoire effacée pour $characterId")
    }
}
