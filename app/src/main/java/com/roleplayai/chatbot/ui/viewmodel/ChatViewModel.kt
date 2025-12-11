package com.roleplayai.chatbot.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roleplayai.chatbot.data.ai.GroqAIEngine
import com.roleplayai.chatbot.data.ai.TogetherAIEngine
import com.roleplayai.chatbot.data.ai.SmartLocalAI
import com.roleplayai.chatbot.data.ai.AIOrchestrator
import com.roleplayai.chatbot.data.memory.ConversationMemory
import com.roleplayai.chatbot.data.manager.GroqKeyManager
import com.roleplayai.chatbot.data.auth.AuthManager
import com.roleplayai.chatbot.data.model.Chat
import com.roleplayai.chatbot.data.model.InferenceConfig
import com.roleplayai.chatbot.data.model.Message
import com.roleplayai.chatbot.data.preferences.PreferencesManager
import com.roleplayai.chatbot.data.repository.CharacterRepository
import com.roleplayai.chatbot.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val chatRepository = ChatRepository(application)
    private val characterRepository = CharacterRepository()
    private val preferencesManager = PreferencesManager(application)
    private val authManager = AuthManager.getInstance(application)
    
    // AI Orchestrator - Gère tous les moteurs d'IA
    private val aiOrchestrator = AIOrchestrator(application)
    
    // Moteurs d'IA (legacy, pour compatibilité)
    private var groqAIEngine: GroqAIEngine? = null
    private var togetherAIEngine: TogetherAIEngine? = null
    private val smartLocalAIs = mutableMapOf<String, SmartLocalAI>()
    
    // Gestionnaire de clés Groq avec rotation
    private val groqKeyManager = GroqKeyManager(application)
    
    // Mémoire conversationnelle long terme
    private val conversationMemories = mutableMapOf<String, ConversationMemory>()
    
    private val _currentChat = MutableStateFlow<Chat?>(null)
    val currentChat: StateFlow<Chat?> = _currentChat.asStateFlow()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    val allChats = chatRepository.chats
    
    // Vérifier si une conversation existe pour ce personnage
    fun hasExistingChat(characterId: String): Boolean {
        return chatRepository.getChatsByCharacter(characterId).isNotEmpty()
    }
    
    // Obtenir le chat existant (pour le continuer)
    fun getExistingChat(characterId: String): Chat? {
        return chatRepository.getChatsByCharacter(characterId).firstOrNull()
    }
    
    // Créer un NOUVEAU chat (supprime l'ancien si existe)
    fun createNewChat(characterId: String): Chat {
        // Supprimer l'ancien chat s'il existe
        val existingChat = chatRepository.getChatsByCharacter(characterId).firstOrNull()
        if (existingChat != null) {
            chatRepository.deleteChat(existingChat.id)
        }
        
        // Créer nouveau chat
        val character = characterRepository.getCharacterById(characterId)
            ?: throw IllegalArgumentException("Character not found")
        
        val newChat = chatRepository.createChat(
            characterId = character.id,
            characterName = character.name,
            characterImageUrl = character.imageUrl
        )
        
        // Ajouter le message de salutation
        chatRepository.addMessage(
            chatId = newChat.id,
            content = character.greeting,
            isUser = false
        )
        
        _currentChat.value = chatRepository.getChatById(newChat.id)
        return _currentChat.value!!
    }
    
    // Ancienne fonction pour compatibilité (cherche ou crée)
    fun createOrGetChat(characterId: String): Chat {
        // Check if a chat already exists for this character
        val existingChat = chatRepository.getChatsByCharacter(characterId).firstOrNull()
        if (existingChat != null) {
            _currentChat.value = existingChat
            return existingChat
        }
        
        // Create new chat
        return createNewChat(characterId)
    }
    
    fun selectChat(chatId: String) {
        _currentChat.value = chatRepository.getChatById(chatId)
    }
    
    fun sendMessage(content: String) {
        val chat = _currentChat.value ?: return
        if (content.isBlank() || _isGenerating.value) return
        
        viewModelScope.launch {
            try {
                // Add user message
                chatRepository.addMessage(
                    chatId = chat.id,
                    content = content.trim(),
                    isUser = true
                )
                
                // Update current chat
                _currentChat.value = chatRepository.getChatById(chat.id)
                
                // Generate AI response
                _isGenerating.value = true
                _error.value = null
                
                val character = characterRepository.getCharacterById(chat.characterId)
                    ?: throw IllegalArgumentException("Character not found")
                
                val updatedChat = chatRepository.getChatById(chat.id)!!
                
                // Obtenir ou créer la mémoire conversationnelle pour ce personnage
                val memory = conversationMemories.getOrPut(chat.characterId) {
                    ConversationMemory(getApplication(), chat.characterId)
                }
                
                // Ajouter le message utilisateur à la mémoire long terme
                updatedChat.messages.lastOrNull { it.isUser }?.let { userMsg ->
                    memory.addMessage(userMsg)
                    android.util.Log.d("ChatViewModel", "🧠 Mémoire: Niveau ${memory.getRelationshipLevel()}/100, ${memory.getFacts().size} faits enregistrés")
                }
                
                // Récupérer le contexte de mémoire pour enrichir les prompts IA
                val memoryContext = memory.getRelevantContext(updatedChat.messages)
                android.util.Log.d("ChatViewModel", "🧠 Contexte mémoire : ${memoryContext.take(100)}...")
                
                // Obtenir le pseudo et le sexe de l'utilisateur
                val currentUser = authManager.getCurrentUser()
                
                // Logs détaillés pour debug
                if (currentUser == null) {
                    android.util.Log.w("ChatViewModel", "⚠️ ATTENTION: currentUser est NULL - utilisateur non connecté ?")
                } else {
                    android.util.Log.d("ChatViewModel", "✅ Utilisateur connecté: ${currentUser.email}")
                    android.util.Log.d("ChatViewModel", "✅ Pseudo: '${currentUser.pseudo}'")
                    if (currentUser.pseudo.isBlank()) {
                        android.util.Log.e("ChatViewModel", "❌ ERREUR: Le pseudo est VIDE pour ${currentUser.email}")
                    }
                }
                
                val username = currentUser?.pseudo?.takeIf { it.isNotBlank() } ?: "Utilisateur"
                val userGender = currentUser?.getGenderForPrompt() ?: "neutre"
                
                android.util.Log.d("ChatViewModel", "👤 Utilisateur final pour IA: '$username' ($userGender)")
                
                // Avertissement si on utilise le fallback
                if (username == "Utilisateur") {
                    android.util.Log.w("ChatViewModel", "⚠️ Utilisation du nom par défaut 'Utilisateur' - le pseudo n'a pas pu être récupéré")
                }
                
                // NOUVELLE ARCHITECTURE : AI Orchestrator
                // Gère automatiquement la cascade des moteurs selon la configuration
                
                val selectedEngine = preferencesManager.selectedAIEngine.first()
                val enableFallbacks = preferencesManager.enableAIFallbacks.first()
                val groqApiKey = groqKeyManager.getCurrentKey()
                val groqModelId = preferencesManager.groqModelId.first()
                val nsfwMode = preferencesManager.nsfwMode.first()
                val llamaCppModelPath = preferencesManager.llamaCppModelPath.first()
                
                android.util.Log.i("ChatViewModel", "🤖 Moteur sélectionné: $selectedEngine")
                android.util.Log.d("ChatViewModel", "Fallbacks: $enableFallbacks, NSFW: $nsfwMode")
                
                // Convertir le string en enum
                val engineEnum = try {
                    AIOrchestrator.AIEngine.valueOf(selectedEngine)
                } catch (e: Exception) {
                    android.util.Log.w("ChatViewModel", "Moteur invalide: $selectedEngine, fallback vers GROQ")
                    AIOrchestrator.AIEngine.GROQ
                }
                
                // Configuration de génération
                val generationConfig = AIOrchestrator.GenerationConfig(
                    primaryEngine = engineEnum,
                    enableFallbacks = enableFallbacks,
                    nsfwMode = nsfwMode,
                    groqApiKey = groqApiKey,
                    groqModelId = groqModelId,
                    llamaCppModelPath = llamaCppModelPath
                )
                
                // Générer avec l'orchestrateur
                val result = aiOrchestrator.generateResponse(
                    character = character,
                    messages = updatedChat.messages,
                    username = username,
                    userGender = userGender,
                    memoryContext = memoryContext,
                    config = generationConfig
                )
                
                android.util.Log.i("ChatViewModel", "✅ Réponse générée par ${result.usedEngine.name} en ${result.generationTimeMs}ms")
                if (result.hadFallback) {
                    android.util.Log.w("ChatViewModel", "⚠️ Fallback utilisé (moteur principal indisponible)")
                }
                
                val response = result.response
                
                // Add AI response
                chatRepository.addMessage(
                    chatId = chat.id,
                    content = response,
                    isUser = false
                )
                
                // Update current chat
                _currentChat.value = chatRepository.getChatById(chat.id)
                
                // Ajouter la réponse IA à la mémoire
                chatRepository.getChatById(chat.id)?.messages?.lastOrNull { !it.isUser }?.let { aiMsg ->
                    memory.addMessage(aiMsg)
                }
                
            } catch (e: Exception) {
                _error.value = "Erreur lors de la génération de la réponse: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    fun deleteChat(chatId: String) {
        chatRepository.deleteChat(chatId)
        if (_currentChat.value?.id == chatId) {
            _currentChat.value = null
        }
    }
    
    fun clearChatHistory(chatId: String) {
        chatRepository.clearChatHistory(chatId)
        _currentChat.value = chatRepository.getChatById(chatId)
    }
    
    fun clearError() {
        _error.value = null
    }
    
    // Méthodes AIEngine et LocalAI supprimées
    
    private suspend fun initializeGroqEngine() {
        try {
            val apiKey = preferencesManager.groqApiKey.first()
            val modelId = preferencesManager.groqModelId.first()
            val nsfwMode = preferencesManager.nsfwMode.first()
            
            android.util.Log.d("ChatViewModel", "===== Initialisation Groq Engine =====")
            android.util.Log.d("ChatViewModel", "Modèle sélectionné: $modelId")
            android.util.Log.d("ChatViewModel", "NSFW mode: $nsfwMode")
            android.util.Log.d("ChatViewModel", "Clé API présente: ${apiKey.isNotBlank()}")
            
            if (apiKey.isBlank()) {
                _error.value = "Clé API Groq manquante. Configurez-la dans Paramètres."
                return
            }
            
            // TOUJOURS recréer l'engine pour prendre en compte les nouveaux paramètres
            groqAIEngine = GroqAIEngine(
                apiKey = apiKey,
                model = modelId,
                nsfwMode = nsfwMode
            )
            
            android.util.Log.i("ChatViewModel", "✅ Groq Engine initialisé avec modèle: $modelId")
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "❌ Erreur initialisation Groq", e)
            _error.value = "Erreur d'initialisation de Groq: ${e.message}"
        }
    }
    
    /**
     * STRATÉGIE 1 : Tenter Groq avec rotation automatique de clés
     */
    private suspend fun tryGroqWithFallback(
        character: com.roleplayai.chatbot.data.model.Character,
        messages: List<Message>,
        username: String,
        userGender: String,
        memoryContext: String
    ): String {
        // Récupérer la clé actuelle du gestionnaire
        val apiKey = groqKeyManager.getCurrentKey()
        
        if (apiKey == null) {
            android.util.Log.w("ChatViewModel", "⚠️ Aucune clé Groq disponible, fallback Together AI...")
            return tryFallbackEngines(character, messages, username, userGender, memoryContext)
        }
        
        return try {
            val modelId = preferencesManager.groqModelId.first()
            val nsfwMode = preferencesManager.nsfwMode.first()
            
            // Réinitialiser le moteur avec la clé actuelle
            groqAIEngine = GroqAIEngine(
                apiKey = apiKey,
                model = modelId.takeIf { it.isNotBlank() } ?: "llama-3.1-70b-versatile",
                nsfwMode = nsfwMode
            )
            
            val response = groqAIEngine!!.generateResponse(character, messages, username, userGender, memoryContext)
            android.util.Log.i("ChatViewModel", "✅ Réponse Groq (${groqKeyManager.getAvailableKeysCount()}/${groqKeyManager.getTotalKeysCount()} clés dispo)")
            response
            
        } catch (e: Exception) {
            // Vérifier si c'est un rate limit (429)
            if (e.message?.contains("429") == true || e.message?.contains("rate") == true) {
                android.util.Log.w("ChatViewModel", "⚠️ Clé Groq rate limitée, rotation...")
                groqKeyManager.markCurrentKeyAsRateLimited()
                
                // Réessayer avec la clé suivante si disponible
                val nextKey = groqKeyManager.getCurrentKey()
                if (nextKey != null) {
                    android.util.Log.d("ChatViewModel", "🔄 Réessai avec clé suivante...")
                    return tryGroqWithFallback(character, messages, username, userGender, memoryContext)
                }
            }
            
            // Fallback vers Together AI
            android.util.Log.w("ChatViewModel", "⚠️ Groq indisponible (${e.message}), fallback Together AI...")
            tryFallbackEngines(character, messages, username, userGender, memoryContext)
        }
    }
    
    /**
     * STRATÉGIE 2 : Utiliser directement les fallbacks (Groq désactivé)
     * CASCADE : Together AI → SmartLocalAI (toujours disponible)
     */
    private suspend fun tryFallbackEngines(
        character: com.roleplayai.chatbot.data.model.Character,
        messages: List<Message>,
        username: String,
        userGender: String,
        memoryContext: String
    ): String {
        // ÉTAPE 1 : Tenter Together AI (API gratuite rapide)
        try {
            android.util.Log.d("ChatViewModel", "1️⃣ Tentative Together AI...")
            return tryTogetherAI(character, messages, username, userGender, memoryContext)
        } catch (e: Exception) {
            android.util.Log.w("ChatViewModel", "⚠️ Together AI indisponible (${e.message})")
        }
        
        // ÉTAPE 2 : SmartLocalAI (ne peut jamais échouer)
        android.util.Log.d("ChatViewModel", "2️⃣ Utilisation SmartLocalAI...")
        return trySmartLocalAI(character, messages, username, userGender)
    }
    
    /**
     * Tenter de générer avec Together AI (API GRATUITE rapide)
     */
    private suspend fun tryTogetherAI(
        character: com.roleplayai.chatbot.data.model.Character,
        messages: List<Message>,
        username: String,
        userGender: String,
        memoryContext: String
    ): String {
        val nsfwMode = preferencesManager.nsfwMode.first()
        
        if (togetherAIEngine == null) {
            android.util.Log.d("ChatViewModel", "🤝 Initialisation Together AI Engine...")
            togetherAIEngine = TogetherAIEngine(
                apiKey = "",  // Gratuit sans clé
                model = "mistralai/Mistral-7B-Instruct-v0.2",
                nsfwMode = nsfwMode
            )
        }
        
        val response = togetherAIEngine!!.generateResponse(character, messages, username, userGender, memoryContext, maxRetries = 2)
        android.util.Log.i("ChatViewModel", "✅ Réponse générée avec Together AI")
        return response
    }
    
    /**
     * Utilise SmartLocalAI (IA locale avec mémoire - NE PEUT JAMAIS ÉCHOUER)
     */
    private suspend fun trySmartLocalAI(
        character: com.roleplayai.chatbot.data.model.Character,
        messages: List<Message>,
        username: String,
        userGender: String
    ): String {
        val nsfwMode = preferencesManager.nsfwMode.first()
        
        try {
            android.util.Log.d("ChatViewModel", "🧠 Génération avec SmartLocalAI...")
            
            // Obtenir ou créer SmartLocalAI pour ce personnage
            val smartAI = smartLocalAIs.getOrPut(character.id) {
                SmartLocalAI(
                    context = getApplication(),
                    character = character,
                    characterId = character.id,
                    nsfwMode = nsfwMode
                )
            }
            
            // Extraire le dernier message utilisateur
            val userMessage = messages.lastOrNull { it.isUser }?.content ?: ""
            val response = smartAI.generateResponse(userMessage, messages, username)
            android.util.Log.i("ChatViewModel", "✅ Réponse SmartLocalAI (avec mémoire)")
            return response
            
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "❌ Erreur SmartLocalAI", e)
            // Fallback absolu
            return "*sourit* Désolé(e), j'ai eu un petit bug. Tu peux répéter ?"
        }
    }
    
    /**
     * Tenter de générer avec LocalAI (llama.cpp ou templates intelligents)
     * NE PEUT JAMAIS ÉCHOUER - dernier fallback absolu
     */
    private suspend fun tryLocalAI(
        character: com.roleplayai.chatbot.data.model.Character,
        messages: List<Message>,
        username: String
    ): String {
        val nsfwMode = preferencesManager.nsfwMode.first()
        
        // Cette fonction a été supprimée - utilisation uniquement d'APIs externes
        throw Exception("LocalAI supprimé - utilisez Groq, Together AI ou HuggingFace")
    }
    
    override fun onCleared() {
        super.onCleared()
        // Nettoyer tous les moteurs d'IA
        groqAIEngine = null
        togetherAIEngine = null
        smartLocalAIs.clear()
        conversationMemories.clear()
        android.util.Log.d("ChatViewModel", "🧹 Moteurs d'IA nettoyés")
    }
}
