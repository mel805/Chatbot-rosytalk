package com.roleplayai.chatbot.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roleplayai.chatbot.data.ai.AIEngine
import com.roleplayai.chatbot.data.ai.LocalAIEngine
import com.roleplayai.chatbot.data.ai.GroqAIEngine
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
    
    private val chatRepository = ChatRepository()
    private val characterRepository = CharacterRepository()
    private val preferencesManager = PreferencesManager(application)
    private val aiEngine = AIEngine(application)
    private var localAIEngine: LocalAIEngine? = null
    private var groqAIEngine: GroqAIEngine? = null
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
                
                // Vérifier si Groq API est activée
                val useGroq = preferencesManager.useGroqApi.first()
                
                val response = if (useGroq) {
                    // Tenter Groq d'abord avec fallback automatique
                    try {
                        // TOUJOURS réinitialiser pour prendre en compte les changements de modèle
                        initializeGroqEngine()
                        
                        val groqResponse = groqAIEngine?.generateResponse(character, updatedChat.messages)
                            ?: throw Exception("Groq API non configurée")
                        
                        // Vérifier si erreur de limite Groq
                        if (groqResponse.contains("rate limit", ignoreCase = true) ||
                            groqResponse.contains("limite", ignoreCase = true) ||
                            groqResponse.contains("quota", ignoreCase = true) ||
                            groqResponse.contains("Erreur", ignoreCase = true)) {
                            throw Exception("Limite Groq atteinte")
                        }
                        
                        groqResponse
                    } catch (e: Exception) {
                        // Basculement automatique vers IA locale
                        android.util.Log.w("ChatViewModel", "⚠️ Groq indisponible (${e.message}), basculement vers IA locale")
                        
                        // Essayer LocalAI
                        try {
                            // S'assurer que LocalAI est initialisé
                            if (localAIEngine == null) {
                                android.util.Log.w("ChatViewModel", "💡 Initialisation IA locale pour fallback...")
                                // Créer LocalAI avec fallback intelligent
                                val nsfwMode = preferencesManager.nsfwMode.first()
                                localAIEngine = LocalAIEngine(
                                    context = getApplication(),
                                    modelPath = "",  // Pas de modèle = fallback intelligent
                                    config = InferenceConfig(contextLength = 2048),
                                    nsfwMode = nsfwMode
                                )
                            }
                            
                            val localResponse = localAIEngine!!.generateResponse(character, updatedChat.messages)
                            
                            // Ajouter un message d'info
                            "⚠️ Groq indisponible (limite atteinte). Utilisation de l'IA locale.\n\n$localResponse"
                        } catch (localError: Exception) {
                            android.util.Log.e("ChatViewModel", "❌ Erreur IA locale aussi", localError)
                            "Désolé, Groq a atteint ses limites et l'IA locale n'est pas disponible.\n\n💡 Astuce : Téléchargez un modèle local dans Paramètres > Modèle IA pour continuer à discuter même quand Groq est indisponible !"
                        }
                    }
                } else {
                    // Groq désactivé, utiliser LocalAI
                    try {
                        // S'assurer que LocalAI est initialisé
                        if (localAIEngine == null) {
                            android.util.Log.i("ChatViewModel", "💡 Initialisation IA locale (Groq désactivé)...")
                            val nsfwMode = preferencesManager.nsfwMode.first()
                            localAIEngine = LocalAIEngine(
                                context = getApplication(),
                                modelPath = "",
                                config = InferenceConfig(contextLength = 2048),
                                nsfwMode = nsfwMode
                            )
                        }
                        
                        // Générer avec LocalAI
                        localAIEngine!!.generateResponse(character, updatedChat.messages)
                    } catch (e: Exception) {
                        android.util.Log.e("ChatViewModel", "❌ Erreur LocalAI (Groq désactivé)", e)
                        "Erreur de l'IA locale.\n\n💡 Astuce : Téléchargez un modèle local dans Paramètres > Modèle IA pour de meilleures réponses, ou activez Groq API pour des réponses ultra-rapides !"
                    }
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
    
    suspend fun initializeLocalAI(modelPath: String) {
        try {
            val nsfwMode = preferencesManager.nsfwMode.first()
            
            localAIEngine = LocalAIEngine(
                context = getApplication(),
                modelPath = modelPath,
                config = InferenceConfig(contextLength = 2048),
                nsfwMode = nsfwMode
            )
            
            val loaded = localAIEngine?.loadModel() ?: false
            if (loaded) {
                useLocalEngine = true
                android.util.Log.i("ChatViewModel", "✅ IA locale initialisée et prête")
            } else {
                android.util.Log.i("ChatViewModel", "💡 IA locale en mode fallback (pas de modèle chargé)")
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "❌ Erreur initialisation IA locale", e)
            _error.value = "Erreur d'initialisation de l'IA locale: ${e.message}"
        }
    }
    
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
    
    override fun onCleared() {
        super.onCleared()
        localAIEngine?.unloadModel()
    }
}
