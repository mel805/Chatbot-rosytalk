package com.roleplayai.chatbot.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roleplayai.chatbot.data.ai.AIEngine
import com.roleplayai.chatbot.data.ai.LocalAIEngine
import com.roleplayai.chatbot.data.model.Chat
import com.roleplayai.chatbot.data.model.InferenceConfig
import com.roleplayai.chatbot.data.model.Message
import com.roleplayai.chatbot.data.repository.CharacterRepository
import com.roleplayai.chatbot.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val chatRepository = ChatRepository()
    private val characterRepository = CharacterRepository()
    private val aiEngine = AIEngine(application)
    private var localAIEngine: LocalAIEngine? = null
    private var useLocalEngine = false
    
    private val _currentChat = MutableStateFlow<Chat?>(null)
    val currentChat: StateFlow<Chat?> = _currentChat.asStateFlow()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    val allChats = chatRepository.chats
    
    fun createOrGetChat(characterId: String): Chat {
        // Check if a chat already exists for this character
        val existingChat = chatRepository.getChatsByCharacter(characterId).firstOrNull()
        if (existingChat != null) {
            _currentChat.value = existingChat
            return existingChat
        }
        
        // Create new chat
        val character = characterRepository.getCharacterById(characterId)
            ?: throw IllegalArgumentException("Character not found")
        
        val newChat = chatRepository.createChat(
            characterId = character.id,
            characterName = character.name,
            characterImageUrl = character.imageUrl
        )
        
        // Add greeting message
        chatRepository.addMessage(
            chatId = newChat.id,
            content = character.greeting,
            isUser = false
        )
        
        _currentChat.value = chatRepository.getChatById(newChat.id)
        return _currentChat.value!!
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
                
                // UTILISER UNIQUEMENT LE MOTEUR LOCAL (modèles téléchargés)
                val response = if (localAIEngine != null) {
                    localAIEngine!!.generateResponse(character, updatedChat.messages)
                } else {
                    // Si pas de modèle chargé, erreur claire
                    throw IllegalStateException("Aucun modèle IA n'est chargé. Veuillez télécharger un modèle dans les paramètres.")
                }
                
                // Add AI response
                chatRepository.addMessage(
                    chatId = chat.id,
                    content = response,
                    isUser = false
                )
                
                // Update current chat
                _currentChat.value = chatRepository.getChatById(chat.id)
                
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
    
    fun setAIEndpoint(endpoint: String) {
        aiEngine.setAPIEndpoint(endpoint)
    }
    
    fun setAIKey(key: String) {
        aiEngine.setAPIKey(key)
    }
    
    fun setUseLocalAPI(use: Boolean, endpoint: String = "http://localhost:8080/v1/chat/completions") {
        aiEngine.setUseLocalAPI(use, endpoint)
    }
    
    fun initializeLocalAI(modelPath: String, config: InferenceConfig = InferenceConfig()) {
        viewModelScope.launch {
            try {
                localAIEngine = LocalAIEngine(
                    context = getApplication(),
                    modelPath = modelPath,
                    config = config
                )
                
                val loaded = localAIEngine?.loadModel() ?: false
                if (loaded) {
                    useLocalEngine = true
                }
            } catch (e: Exception) {
                _error.value = "Erreur d'initialisation de l'IA locale: ${e.message}"
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        localAIEngine?.unloadModel()
    }
}
