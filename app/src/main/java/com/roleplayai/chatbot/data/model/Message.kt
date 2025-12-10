package com.roleplayai.chatbot.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val chatId: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class Chat(
    val id: String,
    val characterId: String,
    val characterName: String,
    val characterImageUrl: String,
    val messages: List<Message> = emptyList(),
    val lastMessageTime: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
