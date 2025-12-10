package com.roleplayai.chatbot.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseUser
import com.roleplayai.chatbot.data.auth.AuthManager
import com.roleplayai.chatbot.data.auth.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authManager = AuthManager(application)
    
    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
    val isAdmin: StateFlow<Boolean> = authManager.isAdmin
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        return authManager.getGoogleSignInClient()
    }
    
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return authManager.signInWithGoogle(idToken)
    }
    
    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }
    
    fun isUserAdmin(): Boolean {
        return authManager.isAdmin()
    }
    
    suspend fun getUserPreferences(): UserPreferences? {
        return authManager.getUserPreferences()
    }
    
    fun saveUserPreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            authManager.saveUserPreferences(preferences)
        }
    }
}
