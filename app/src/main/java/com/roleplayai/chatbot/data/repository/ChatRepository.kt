package com.roleplayai.chatbot.data.repository

import com.roleplayai.chatbot.data.model.Chat
import com.roleplayai.chatbot.data.model.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ChatRepository {
    
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()
    
    fun createChat(characterId: String, characterName: String, characterImageUrl: String): Chat {
        val newChat = Chat(
            id = UUID.randomUUID().toString(),
            characterId = characterId,
            characterName = characterName,
            characterImageUrl = characterImageUrl,
            messages = emptyList()
        )
        _chats.value = _chats.value + newChat
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
    }
    
    fun deleteChat(chatId: String) {
        _chats.value = _chats.value.filter { it.id != chatId }
    }
    
    fun clearChatHistory(chatId: String) {
        val chat = getChatById(chatId) ?: return
        val clearedChat = chat.copy(messages = emptyList())
        _chats.value = _chats.value.map {
            if (it.id == chatId) clearedChat else it
        }
    }
}
