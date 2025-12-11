package com.roleplayai.chatbot.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roleplayai.chatbot.data.auth.AuthManager
import com.roleplayai.chatbot.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pour la gestion des utilisateurs (ADMIN UNIQUEMENT)
 */
class AdminViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authManager = AuthManager.getInstance(application)
    
    // Liste des utilisateurs
    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()
    
    // États UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage = _successMessage.asStateFlow()
    
    /**
     * Charge tous les utilisateurs
     */
    fun loadAllUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val users = authManager.getAllUsersForAdmin()
                _allUsers.value = users.sortedByDescending { it.createdAt }
                
                android.util.Log.d("AdminVM", "✅ ${users.size} utilisateurs chargés")
            } catch (e: Exception) {
                _errorMessage.value = "Erreur: ${e.message}"
                android.util.Log.e("AdminVM", "❌ Erreur chargement utilisateurs", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Met à jour un utilisateur
     */
    fun updateUser(
        targetEmail: String,
        isNsfwEnabled: Boolean,
        isAdmin: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                val success = authManager.updateUserAsAdmin(
                    targetEmail = targetEmail,
                    isNsfwEnabled = isNsfwEnabled,
                    isAdmin = isAdmin
                )
                
                if (success) {
                    _successMessage.value = "✅ Utilisateur mis à jour"
                    loadAllUsers() // Recharger la liste
                } else {
                    _errorMessage.value = "❌ Échec de la mise à jour"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erreur: ${e.message}"
                android.util.Log.e("AdminVM", "❌ Erreur màj utilisateur", e)
            } finally {
                _isLoading.value = false
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
}
