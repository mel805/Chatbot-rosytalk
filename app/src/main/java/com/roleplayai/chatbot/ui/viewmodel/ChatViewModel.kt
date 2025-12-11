package com.roleplayai.chatbot.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roleplayai.chatbot.data.ai.AIEngine
import com.roleplayai.chatbot.data.ai.LocalAIEngine
import com.roleplayai.chatbot.data.ai.GroqAIEngine
import com.roleplayai.chatbot.data.ai.HuggingFaceAIEngine
import com.roleplayai.chatbot.data.auth.LocalAuthManager
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
    private val authManager = LocalAuthManager.getInstance(application)
    private val aiEngine = AIEngine(application)
    private var localAIEngine: LocalAIEngine? = null
    private var groqAIEngine: GroqAIEngine? = null
    private var huggingFaceEngine: HuggingFaceAIEngine? = null
    private var useLocalEngine = false
    
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
                
                // Obtenir le pseudo de l'utilisateur
                val username = authManager.currentUser.value?.username?.takeIf { it.isNotBlank() }
                    ?: authManager.currentUser.value?.displayName
                    ?: "Utilisateur"
                
                // CASCADE INTELLIGENTE D'IA : Groq → HuggingFace → LocalAI
                // Groq = Principal (ultra-rapide, excellente qualité)
                // HuggingFace = Fallback 1 (gratuit, bonne qualité, un peu plus lent)
                // LocalAI = Fallback 2 (template intelligent, toujours disponible)
                
                val useGroq = preferencesManager.useGroqApi.first()
                
                val response = if (useGroq) {
                    // STRATÉGIE 1 : Tenter Groq d'abord
                    android.util.Log.i("ChatViewModel", "🚀 Tentative avec Groq API...")
                    tryGroqWithFallback(character, updatedChat.messages, username)
                } else {
                    // STRATÉGIE 2 : Groq désactivé, utiliser directement les fallbacks
                    android.util.Log.i("ChatViewModel", "💡 Groq désactivé, utilisation des IA alternatives...")
                    tryFallbackEngines(character, updatedChat.messages, username)
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
    
    /**
     * STRATÉGIE 1 : Tenter Groq avec fallback automatique vers HuggingFace puis LocalAI
     */
    private suspend fun tryGroqWithFallback(
        character: com.roleplayai.chatbot.data.model.Character,
        messages: List<Message>,
        username: String
    ): String {
        return try {
            // ÉTAPE 1 : Tenter Groq
            android.util.Log.d("ChatViewModel", "1️⃣ Tentative Groq API...")
            initializeGroqEngine()
            
            val groqResponse = groqAIEngine?.generateResponse(character, messages, username)
                ?: throw Exception("Groq API non configurée")
            
            // Vérifier si erreur de limite Groq
            if (groqResponse.contains("rate limit", ignoreCase = true) ||
                groqResponse.contains("limite", ignoreCase = true) ||
                groqResponse.contains("quota", ignoreCase = true) ||
                groqResponse.startsWith("Erreur", ignoreCase = true)) {
                throw Exception("Limite Groq atteinte")
            }
            
            android.util.Log.i("ChatViewModel", "✅ Réponse générée avec Groq")
            groqResponse
            
        } catch (e: Exception) {
            // ÉTAPE 2 : Groq a échoué, tenter HuggingFace
            android.util.Log.w("ChatViewModel", "⚠️ Groq indisponible (${e.message}), tentative HuggingFace...")
            
            try {
                tryHuggingFace(character, messages, username)
            } catch (hfError: Exception) {
                // ÉTAPE 3 : HuggingFace a échoué, utiliser LocalAI
                android.util.Log.w("ChatViewModel", "⚠️ HuggingFace indisponible (${hfError.message}), utilisation LocalAI...")
                tryLocalAI(character, messages, username)
            }
        }
    }
    
    /**
     * STRATÉGIE 2 : Utiliser directement les fallbacks (Groq désactivé)
     */
    private suspend fun tryFallbackEngines(
        character: com.roleplayai.chatbot.data.model.Character,
        messages: List<Message>,
        username: String
    ): String {
        return try {
            // ÉTAPE 1 : Tenter HuggingFace d'abord
            android.util.Log.d("ChatViewModel", "1️⃣ Tentative HuggingFace API...")
            tryHuggingFace(character, messages, username)
            
        } catch (e: Exception) {
            // ÉTAPE 2 : HuggingFace a échoué, utiliser LocalAI
            android.util.Log.w("ChatViewModel", "⚠️ HuggingFace indisponible (${e.message}), utilisation LocalAI...")
            tryLocalAI(character, messages, username)
        }
    }
    
    /**
     * Tenter de générer avec HuggingFace Inference API (GRATUIT)
     * Essaie d'abord le modèle rapide Phi-3, puis Mistral si échec
     */
    private suspend fun tryHuggingFace(
        character: com.roleplayai.chatbot.data.model.Character,
        messages: List<Message>,
        username: String
    ): String {
        val nsfwMode = preferencesManager.nsfwMode.first()
        
        // STRATÉGIE 1 : Essayer Phi-3 Mini (plus rapide)
        try {
            android.util.Log.d("ChatViewModel", "🤗 Tentative avec Phi-3 Mini (rapide)...")
            val phiEngine = HuggingFaceAIEngine(
                apiKey = "",
                model = "microsoft/Phi-3-mini-4k-instruct",  // Plus rapide
                nsfwMode = nsfwMode
            )
            val response = phiEngine.generateResponse(character, messages, username, maxRetries = 1)
            android.util.Log.i("ChatViewModel", "✅ Réponse générée avec Phi-3 Mini")
            return response
        } catch (e: Exception) {
            android.util.Log.w("ChatViewModel", "⚠️ Phi-3 indisponible, essai Mistral...")
        }
        
        // STRATÉGIE 2 : Essayer Mistral 7B (plus puissant mais plus lent)
        try {
            android.util.Log.d("ChatViewModel", "🤗 Tentative avec Mistral 7B...")
            if (huggingFaceEngine == null) {
                huggingFaceEngine = HuggingFaceAIEngine(
                    apiKey = "",
                    model = "mistralai/Mistral-7B-Instruct-v0.2",
                    nsfwMode = nsfwMode
                )
            }
            val response = huggingFaceEngine!!.generateResponse(character, messages, username, maxRetries = 2)
            android.util.Log.i("ChatViewModel", "✅ Réponse générée avec Mistral 7B")
            return response
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "❌ HuggingFace complètement indisponible")
            throw e
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
        
        return try {
            if (localAIEngine == null) {
                android.util.Log.d("ChatViewModel", "🧠 Initialisation LocalAI Engine...")
                val modelPath = preferencesManager.modelPath.first() ?: ""
                localAIEngine = LocalAIEngine(
                    context = getApplication(),
                    modelPath = modelPath,
                    config = InferenceConfig(contextLength = 2048),
                    nsfwMode = nsfwMode
                )
                if (modelPath.isNotEmpty()) {
                    localAIEngine!!.loadModel()
                }
            }
            
            val response = localAIEngine!!.generateResponse(character, messages, username)
            android.util.Log.i("ChatViewModel", "✅ Réponse générée avec LocalAI (fallback intelligent)")
            response
            
        } catch (e: Exception) {
            // Fallback absolu de sécurité (ne peut jamais échouer)
            android.util.Log.e("ChatViewModel", "❌ Erreur LocalAI, utilisation fallback absolu", e)
            "*sourit* (Hmm...) Désolé(e), j'ai eu un petit problème technique. Peux-tu répéter ?\n\n💡 Astuce : Pour de meilleures réponses, activez Groq API dans les Paramètres !"
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Nettoyer tous les moteurs d'IA
        localAIEngine?.unloadModel()
        groqAIEngine = null
        huggingFaceEngine = null
        android.util.Log.d("ChatViewModel", "🧹 Moteurs d'IA nettoyés")
    }
}
