package com.roleplayai.chatbot.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roleplayai.chatbot.data.ai.GroqAIEngine
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
                
                // Récupérer TOUTES les clés Groq pour la rotation
                val allGroqKeys = groqKeyManager.getAllKeys()
                val groqApiKey = allGroqKeys.joinToString(",") // Jointure pour AIOrchestrator
                
                val groqModelId = preferencesManager.groqModelId.first()
                val nsfwMode = preferencesManager.nsfwMode.first()
                val llamaCppModelPath = preferencesManager.llamaCppModelPath.first()
                
                android.util.Log.i("ChatViewModel", "🤖 Moteur sélectionné: $selectedEngine")
                android.util.Log.d("ChatViewModel", "Fallbacks: $enableFallbacks, NSFW: $nsfwMode")
                android.util.Log.d("ChatViewModel", "🔑 Clés Groq disponibles: ${allGroqKeys.size}")
                
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
                val msg = e.message ?: e.localizedMessage ?: e.javaClass.simpleName
                _error.value = "Erreur lors de la génération de la réponse: $msg"
                android.util.Log.e("ChatViewModel", "❌ Erreur génération réponse", e)
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
    
    
    
    override fun onCleared() {
        super.onCleared()
        // Nettoyer tous les moteurs d'IA
        groqAIEngine = null
        conversationMemories.clear()
        android.util.Log.d("ChatViewModel", "🧹 Moteurs d'IA nettoyés")
    }
}
