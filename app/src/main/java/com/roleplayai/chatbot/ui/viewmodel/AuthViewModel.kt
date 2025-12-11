package com.roleplayai.chatbot.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roleplayai.chatbot.data.auth.AuthManager
import com.roleplayai.chatbot.data.auth.AuthResult
import com.roleplayai.chatbot.data.model.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel pour gérer l'authentification
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authManager = AuthManager.getInstance(application)
    
    // États d'authentification
    val currentUser: StateFlow<User?> = authManager.currentUser
    val isLoggedIn: StateFlow<Boolean> = authManager.isLoggedIn
    
    // Tous les utilisateurs sont "admin" (peuvent gérer les clés Groq)
    val isAdmin: StateFlow<Boolean> = isLoggedIn
    
    // États UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage = _successMessage.asStateFlow()
    
    init {
        // Restaurer la session au démarrage
        viewModelScope.launch {
            authManager.restoreSession()
        }
    }
    
    /**
     * Inscription
     */
    fun register(
        email: String,
        password: String,
        confirmPassword: String,
        pseudo: String,
        age: Int,
        gender: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            // Validation
            if (password != confirmPassword) {
                _errorMessage.value = "Les mots de passe ne correspondent pas"
                _isLoading.value = false
                return@launch
            }
            
            // Inscription
            when (val result = authManager.register(email, password, pseudo, age, gender)) {
                is AuthResult.Success -> {
                    _successMessage.value = "Inscription réussie ! Bienvenue ${result.user.pseudo} 👋"
                    // Utilisateur automatiquement connecté
                }
                is AuthResult.Error -> {
                    _errorMessage.value = result.message
                }
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Connexion
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            when (val result = authManager.login(email, password)) {
                is AuthResult.Success -> {
                    _successMessage.value = "Connexion réussie ! Bienvenue ${result.user.pseudo} 👋"
                }
                is AuthResult.Error -> {
                    _errorMessage.value = result.message
                }
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Déconnexion
     */
    fun logout() {
        authManager.logout()
        _successMessage.value = "Déconnecté"
    }
    
    /**
     * Met à jour le profil
     */
    fun updateProfile(
        pseudo: String? = null,
        age: Int? = null,
        gender: String? = null,
        isNsfwEnabled: Boolean? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val success = authManager.updateProfile(pseudo, age, gender, isNsfwEnabled)
            
            if (success) {
                _successMessage.value = "Profil mis à jour ✅"
            } else {
                _errorMessage.value = "Erreur lors de la mise à jour"
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Active/désactive le mode NSFW
     */
    fun toggleNsfw(enabled: Boolean) {
        viewModelScope.launch {
            val user = currentUser.value
            if (user != null) {
                if (enabled && !user.isAdult()) {
                    _errorMessage.value = "⚠️ Mode NSFW réservé aux 18+ ans"
                    return@launch
                }
                
                authManager.updateProfile(isNsfwEnabled = enabled)
                _successMessage.value = if (enabled) {
                    "Mode NSFW activé 🔞"
                } else {
                    "Mode NSFW désactivé"
                }
            }
        }
    }
    
    /**
     * Efface les messages
     */
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
    
    /**
     * Obtient le pseudo de l'utilisateur actuel
     */
    fun getCurrentPseudo(): String {
        return currentUser.value?.pseudo ?: "Utilisateur"
    }
    
    /**
     * Obtient le sexe de l'utilisateur pour les prompts
     */
    fun getUserGenderForPrompt(): String {
        return currentUser.value?.getGenderForPrompt() ?: "neutre"
    }
    
    /**
     * Vérifie si le mode NSFW est activé
     */
    fun isNsfwEnabled(): Boolean {
        return currentUser.value?.isNsfwEnabled == true
    }
}
