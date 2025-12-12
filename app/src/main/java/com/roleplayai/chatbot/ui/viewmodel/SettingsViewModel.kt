package com.roleplayai.chatbot.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roleplayai.chatbot.data.preferences.PreferencesManager
import com.roleplayai.chatbot.data.manager.SharedGroqKeysManager
import com.roleplayai.chatbot.data.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val preferencesManager = PreferencesManager(application)
    private val sharedKeysManager = SharedGroqKeysManager(application)
    private val authManager = AuthManager.getInstance(application)
    
    val groqApiKey = preferencesManager.groqApiKey
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    
    val groqModelId = preferencesManager.groqModelId
        .stateIn(viewModelScope, SharingStarted.Eagerly, "llama-3.1-70b-versatile")
    
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    
        .stateIn(viewModelScope, SharingStarted.Eagerly, "gemini-1.5-flash")
    
    val nsfwMode = preferencesManager.nsfwMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    val useGroqApi = preferencesManager.useGroqApi
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    // AI Engine selection
    val selectedAIEngine = preferencesManager.selectedAIEngine
        .stateIn(viewModelScope, SharingStarted.Eagerly, "GROQ")
    
    val enableAIFallbacks = preferencesManager.enableAIFallbacks
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    
    val llamaCppModelPath = preferencesManager.llamaCppModelPath
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    
    // Clés partagées (temps réel depuis Firebase)
    val sharedGroqKeys: StateFlow<List<String>> = sharedKeysManager
        .getSharedKeysFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    // État de chargement
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // Message de statut
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage
    
    init {
        // Démarrer la synchronisation automatique
        viewModelScope.launch {
            sharedKeysManager.startAutoSync().collect { count ->
                android.util.Log.d("SettingsVM", "🔄 ${count} clés synchronisées")
            }
        }
    }
    
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
            val user = authManager.getCurrentUser()
            
            if (enabled && user != null) {
                // Vérifier si l'utilisateur peut activer le NSFW
                if (user.nsfwBlocked) {
                    // Bloqué par l'admin
                    _statusMessage.value = "🚫 Mode NSFW bloqué par l'administrateur"
                    android.util.Log.w("SettingsVM", "⚠️ Tentative d'activation NSFW refusée: bloqué par admin")
                    return@launch
                } else if (!user.isAdult()) {
                    // Mineur
                    _statusMessage.value = "⚠️ Mode NSFW réservé aux 18+ ans"
                    android.util.Log.w("SettingsVM", "⚠️ Tentative d'activation NSFW refusée: utilisateur mineur (${user.age} ans)")
                    return@launch
                }
            }
            
            preferencesManager.setNsfwMode(enabled)
            
            if (enabled) {
                _statusMessage.value = "🔞 Mode NSFW activé"
            } else {
                _statusMessage.value = "Mode NSFW désactivé"
            }
        }
    }
    
    fun setUseGroqApi(use: Boolean) {
        viewModelScope.launch {
            preferencesManager.setUseGroqApi(use)
        }
    }
    
    fun setSelectedAIEngine(engine: String) {
        viewModelScope.launch {
            preferencesManager.setSelectedAIEngine(engine)
            _statusMessage.value = "✅ Moteur IA changé"
            kotlinx.coroutines.delay(2000)
            _statusMessage.value = null
        }
    }
    
    fun setEnableAIFallbacks(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setEnableAIFallbacks(enabled)
        }
    }
    
    fun setLlamaCppModelPath(path: String) {
        viewModelScope.launch {
            preferencesManager.setLlamaCppModelPath(path)
            _statusMessage.value = "✅ Modèle llama.cpp configuré"
            kotlinx.coroutines.delay(2000)
            _statusMessage.value = null
        }
    }
    
    /**
     * Ajoute une clé partagée (Admin uniquement)
     */
    fun addSharedGroqKey(apiKey: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = sharedKeysManager.addSharedKey(apiKey)
            _isLoading.value = false
            
            _statusMessage.value = if (success) {
                "✅ Clé ajoutée et partagée à tous les utilisateurs"
            } else {
                "❌ Erreur : Clé déjà présente ou invalide"
            }
            
            // Effacer le message après 3 secondes
            kotlinx.coroutines.delay(3000)
            _statusMessage.value = null
        }
    }
    
    /**
     * Supprime une clé partagée (Admin uniquement)
     */
    fun removeSharedGroqKey(apiKey: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = sharedKeysManager.removeSharedKey(apiKey)
            _isLoading.value = false
            
            _statusMessage.value = if (success) {
                "✅ Clé supprimée"
            } else {
                "❌ Erreur lors de la suppression"
            }
            
            kotlinx.coroutines.delay(3000)
            _statusMessage.value = null
        }
    }
    
    /**
     * Synchronise manuellement les clés partagées
     */
    fun syncSharedKeys() {
        viewModelScope.launch {
            _isLoading.value = true
            sharedKeysManager.syncSharedKeysToLocal()
            _isLoading.value = false
            
            _statusMessage.value = "✅ Clés synchronisées"
            kotlinx.coroutines.delay(2000)
            _statusMessage.value = null
        }
    }
}
