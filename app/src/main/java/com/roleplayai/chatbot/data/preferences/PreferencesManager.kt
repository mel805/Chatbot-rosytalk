package com.roleplayai.chatbot.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    
    companion object {
        private val SELECTED_MODEL_ID = stringPreferencesKey("selected_model_id")
        private val MODEL_DOWNLOADED = booleanPreferencesKey("model_downloaded")
        private val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        private val SELECTED_MODEL_NAME = stringPreferencesKey("selected_model_name")
        private val MODEL_PATH = stringPreferencesKey("model_path")
        
        // Groq API settings
        private val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        private val GROQ_MODEL_ID = stringPreferencesKey("groq_model_id")
        private val NSFW_MODE_ENABLED = booleanPreferencesKey("nsfw_mode_enabled")
        private val USE_GROQ_API = booleanPreferencesKey("use_groq_api")
    }
    
    val selectedModelId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_MODEL_ID]
    }
    
    val isModelDownloaded: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MODEL_DOWNLOADED] ?: false
    }
    
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FIRST_LAUNCH] ?: true
    }
    
    val selectedModelName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_MODEL_NAME]
    }
    
    val modelPath: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[MODEL_PATH]
    }
    
    suspend fun setSelectedModel(modelId: String, modelName: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_MODEL_ID] = modelId
            preferences[SELECTED_MODEL_NAME] = modelName
        }
    }
    
    suspend fun setModelDownloaded(downloaded: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MODEL_DOWNLOADED] = downloaded
        }
    }
    
    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH] = false
        }
    }
    
    suspend fun setModelPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[MODEL_PATH] = path
        }
    }
    
    suspend fun clearModelData() {
        context.dataStore.edit { preferences ->
            preferences.remove(SELECTED_MODEL_ID)
            preferences.remove(MODEL_DOWNLOADED)
            preferences.remove(SELECTED_MODEL_NAME)
            preferences.remove(MODEL_PATH)
        }
    }
    
    // Groq API settings
    suspend fun setGroqApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[GROQ_API_KEY] = apiKey
        }
    }
    
    val groqApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[GROQ_API_KEY] ?: ""
    }
    
    suspend fun setGroqModelId(modelId: String) {
        context.dataStore.edit { preferences ->
            preferences[GROQ_MODEL_ID] = modelId
        }
    }
    
    val groqModelId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[GROQ_MODEL_ID] ?: "llama-3.3-70b-versatile"
    }
    
    suspend fun setNsfwMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NSFW_MODE_ENABLED] = enabled
        }
    }
    
    val nsfwMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NSFW_MODE_ENABLED] ?: false
    }
    
    suspend fun setUseGroqApi(use: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_GROQ_API] = use
        }
    }
    
    val useGroqApi: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_GROQ_API] ?: false
    }
}
