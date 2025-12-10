package com.roleplayai.chatbot.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roleplayai.chatbot.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val preferencesManager = PreferencesManager(application)
    
    val groqApiKey = preferencesManager.groqApiKey
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    
    val groqModelId = preferencesManager.groqModelId
        .stateIn(viewModelScope, SharingStarted.Eagerly, "llama-3.1-70b-versatile")
    
    val nsfwMode = preferencesManager.nsfwMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    val useGroqApi = preferencesManager.useGroqApi
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    val koboldEndpoint = preferencesManager.koboldEndpoint
        .stateIn(viewModelScope, SharingStarted.Eagerly, "http://localhost:5001")
    
    val useKoboldApi = preferencesManager.useKoboldApi
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    val textGenEndpoint = preferencesManager.textGenEndpoint
        .stateIn(viewModelScope, SharingStarted.Eagerly, "http://localhost:5000")
    
    val useTextGenApi = preferencesManager.useTextGenApi
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    fun setGroqApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesManager.setGroqApiKey(apiKey)
        }
    }
    
    fun setGroqModelId(modelId: String) {
        viewModelScope.launch {
            preferencesManager.setGroqModelId(modelId)
        }
    }
    
    fun setNsfwMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNsfwMode(enabled)
        }
    }
    
    fun setUseGroqApi(use: Boolean) {
        viewModelScope.launch {
            preferencesManager.setUseGroqApi(use)
        }
    }
    
    fun setKoboldEndpoint(endpoint: String) {
        viewModelScope.launch {
            preferencesManager.setKoboldEndpoint(endpoint)
        }
    }
    
    fun setUseKoboldApi(use: Boolean) {
        viewModelScope.launch {
            preferencesManager.setUseKoboldApi(use)
        }
    }
    
    fun setTextGenEndpoint(endpoint: String) {
        viewModelScope.launch {
            preferencesManager.setTextGenEndpoint(endpoint)
        }
    }
    
    fun setUseTextGenApi(use: Boolean) {
        viewModelScope.launch {
            preferencesManager.setUseTextGenApi(use)
        }
    }
}
