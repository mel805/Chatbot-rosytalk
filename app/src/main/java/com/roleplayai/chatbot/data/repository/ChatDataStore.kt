package com.roleplayai.chatbot.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.roleplayai.chatbot.data.model.Chat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * DataStore pour persister les conversations
 */
private val Context.chatDataStore by preferencesDataStore(name = "chats")

class ChatDataStore(private val context: Context) {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    companion object {
        private val CHATS_KEY = stringPreferencesKey("all_chats")
    }
    
    /**
     * Sauvegarder toutes les conversations
     */
    suspend fun saveChats(chats: List<Chat>) {
        context.chatDataStore.edit { prefs ->
            prefs[CHATS_KEY] = json.encodeToString(chats)
        }
    }
    
    /**
     * Charger toutes les conversations
     */
    suspend fun loadChats(): List<Chat> {
        return try {
            val prefs = context.chatDataStore.data.first()
            val chatsJson = prefs[CHATS_KEY] ?: "[]"
            json.decodeFromString<List<Chat>>(chatsJson)
        } catch (e: Exception) {
            android.util.Log.e("ChatDataStore", "❌ Erreur chargement chats", e)
            emptyList()
        }
    }
    
    /**
     * Observer les changements de conversations
     */
    fun observeChats(): Flow<List<Chat>> {
        return context.chatDataStore.data.map { prefs ->
            try {
                val chatsJson = prefs[CHATS_KEY] ?: "[]"
                json.decodeFromString<List<Chat>>(chatsJson)
            } catch (e: Exception) {
                android.util.Log.e("ChatDataStore", "❌ Erreur observation chats", e)
                emptyList()
            }
        }
    }
    
    /**
     * Effacer toutes les conversations
     */
    suspend fun clearAllChats() {
        context.chatDataStore.edit { prefs ->
            prefs.remove(CHATS_KEY)
        }
    }
}
