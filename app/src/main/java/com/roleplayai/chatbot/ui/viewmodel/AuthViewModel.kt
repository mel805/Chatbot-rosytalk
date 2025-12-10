package com.roleplayai.chatbot.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roleplayai.chatbot.data.auth.LocalAuthManager
import com.roleplayai.chatbot.data.auth.User
import com.roleplayai.chatbot.data.auth.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authManager = LocalAuthManager.getInstance(application)
    
    val currentUser: StateFlow<User?> = authManager.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
    val isAdmin: StateFlow<Boolean> = authManager.isAdmin
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    init {
        // Charger l'utilisateur au démarrage
        viewModelScope.launch {
            authManager.loadCurrentUser()
        }
    }
    
    suspend fun signIn(email: String, displayName: String = "", username: String = "", bio: String = "", age: String = ""): Result<User> {
        return authManager.signIn(email, displayName, username, bio, age)
    }
    
    suspend fun updateUserProfile(displayName: String, username: String, bio: String, age: String): Result<User> {
        return authManager.updateUserProfile(displayName, username, bio, age)
    }
    
    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }
    
    fun isUserAdmin(): Boolean {
        return authManager.isAdmin()
    }
    
    fun getUserPreferences(): UserPreferences? {
        return authManager.getUserPreferences()
    }
    
    fun saveUserPreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            authManager.saveUserPreferences(preferences)
        }
    }
    
    suspend fun getAllUsers(): List<User> {
        return authManager.getAllUsers()
    }
}
