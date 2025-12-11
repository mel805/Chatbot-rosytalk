package com.roleplayai.chatbot.data.repository

import android.content.Context
import com.roleplayai.chatbot.data.model.Chat
import com.roleplayai.chatbot.data.model.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ChatRepository(context: Context? = null) {
    
    private val chatDataStore: ChatDataStore? = context?.let { ChatDataStore(it) }
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    init {
        // Charger les conversations sauvegardées au démarrage
        context?.let {
            scope.launch {
                try {
                    val savedChats = chatDataStore?.loadChats() ?: emptyList()
                    _chats.value = savedChats
                    android.util.Log.i("ChatRepository", "✅ ${savedChats.size} conversations chargées")
                } catch (e: Exception) {
                    android.util.Log.e("ChatRepository", "❌ Erreur chargement conversations", e)
                }
            }
        }
    }
    
    /**
     * Sauvegarder les conversations dans le DataStore
     */
    private suspend fun saveChats() {
        try {
            chatDataStore?.saveChats(_chats.value)
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "❌ Erreur sauvegarde conversations", e)
        }
    }
    
    fun createChat(characterId: String, characterName: String, characterImageUrl: String): Chat {
        val newChat = Chat(
            id = UUID.randomUUID().toString(),
            characterId = characterId,
            characterName = characterName,
            characterImageUrl = characterImageUrl,
            messages = emptyList()
        )
        _chats.value = _chats.value + newChat
        
        // Sauvegarder
        scope.launch {
            saveChats()
        }
        
        return newChat
    }
    
    fun getChatById(chatId: String): Chat? {
        return _chats.value.find { it.id == chatId }
    }
    
    fun getChatsByCharacter(characterId: String): List<Chat> {
        return _chats.value.filter { it.characterId == characterId }
    }
    
    fun addMessage(chatId: String, content: String, isUser: Boolean) {
        val chat = getChatById(chatId) ?: return
        val newMessage = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            content = content,
            isUser = isUser
        )
        
        val updatedChat = chat.copy(
            messages = chat.messages + newMessage,
            lastMessageTime = System.currentTimeMillis()
        )
        
        _chats.value = _chats.value.map {
            if (it.id == chatId) updatedChat else it
        }
        
        // Sauvegarder
        scope.launch {
            saveChats()
        }
    }
    
    fun deleteChat(chatId: String) {
        _chats.value = _chats.value.filter { it.id != chatId }
        
        // Sauvegarder
        scope.launch {
            saveChats()
        }
    }
    
    fun clearChatHistory(chatId: String) {
        val chat = getChatById(chatId) ?: return
        val clearedChat = chat.copy(messages = emptyList())
        _chats.value = _chats.value.map {
            if (it.id == chatId) clearedChat else it
        }
        
        // Sauvegarder
        scope.launch {
            saveChats()
        }
    }
}
