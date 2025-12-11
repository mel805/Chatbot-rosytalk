package com.roleplayai.chatbot.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.roleplayai.chatbot.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

/**
 * Gestionnaire d'authentification simple avec SharedPreferences
 * (évite Room/kapt pour simplicité)
 */
class AuthManagerSimple private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "AuthManagerSimple"
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_USERS = "users"
        private const val KEY_CURRENT_EMAIL = "current_email"
        
        @Volatile
        private var INSTANCE: AuthManagerSimple? = null
        
        fun getInstance(context: Context): AuthManagerSimple {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManagerSimple(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    
    // État de l'utilisateur actuel
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()
    
    init {
        // Restaurer la session
        val email = prefs.getString(KEY_CURRENT_EMAIL, null)
        if (email != null) {
            val users = getAllUsers()
            _currentUser.value = users.find { it.email == email }
            _isLoggedIn.value = _currentUser.value != null
        }
    }
    
    /**
     * Restaure la session
     */
    suspend fun restoreSession(): Boolean {
        val email = prefs.getString(KEY_CURRENT_EMAIL, null) ?: return false
        val users = getAllUsers()
        val user = users.find { it.email == email }
        
        return if (user != null) {
            _currentUser.value = user
            _isLoggedIn.value = true
            Log.i(TAG, "✅ Session restaurée: ${user.pseudo}")
            true
        } else {
            logout()
            false
        }
    }
    
    /**
     * Inscription
     */
    suspend fun register(
        email: String,
        password: String,
        pseudo: String,
        age: Int,
        gender: String
    ): AuthResult {
        try {
            // Validation
            if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                return AuthResult.Error("Email invalide")
            }
            
            if (password.length < 6) {
                return AuthResult.Error("Mot de passe trop court (min. 6 caractères)")
            }
            
            if (pseudo.isBlank() || pseudo.length < 2) {
                return AuthResult.Error("Pseudo invalide (min. 2 caractères)")
            }
            
            if (age < 13) {
                return AuthResult.Error("Âge minimum : 13 ans")
            }
            
            // Vérifier si l'email existe
            val users = getAllUsers()
            if (users.any { it.email == email.lowercase() }) {
                return AuthResult.Error("Cet email est déjà utilisé")
            }
            
            // Créer l'utilisateur
            val user = User(
                email = email.lowercase().trim(),
                passwordHash = hashPassword(password),
                pseudo = pseudo.trim(),
                age = age,
                gender = gender,
                isNsfwEnabled = false
            )
            
            // Sauvegarder
            saveUser(user)
            
            // Connexion automatique
            _currentUser.value = user
            _isLoggedIn.value = true
            prefs.edit().putString(KEY_CURRENT_EMAIL, user.email).apply()
            
            Log.i(TAG, "✅ Inscription: ${user.pseudo}")
            return AuthResult.Success(user)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur inscription", e)
            return AuthResult.Error("Erreur: ${e.message}")
        }
    }
    
    /**
     * Connexion
     */
    suspend fun login(email: String, password: String): AuthResult {
        try {
            if (email.isBlank() || password.isBlank()) {
                return AuthResult.Error("Email et mot de passe requis")
            }
            
            val users = getAllUsers()
            val user = users.find { it.email == email.lowercase() }
            
            if (user == null || user.passwordHash != hashPassword(password)) {
                return AuthResult.Error("Email ou mot de passe incorrect")
            }
            
            // Connexion
            _currentUser.value = user
            _isLoggedIn.value = true
            prefs.edit().putString(KEY_CURRENT_EMAIL, user.email).apply()
            
            Log.i(TAG, "✅ Connexion: ${user.pseudo}")
            return AuthResult.Success(user)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur connexion", e)
            return AuthResult.Error("Erreur: ${e.message}")
        }
    }
    
    /**
     * Déconnexion
     */
    fun logout() {
        _currentUser.value = null
        _isLoggedIn.value = false
        prefs.edit().remove(KEY_CURRENT_EMAIL).apply()
        Log.i(TAG, "✅ Déconnexion")
    }
    
    /**
     * Met à jour le profil
     */
    suspend fun updateProfile(
        pseudo: String? = null,
        age: Int? = null,
        gender: String? = null,
        isNsfwEnabled: Boolean? = null
    ): Boolean {
        return try {
            val current = _currentUser.value ?: return false
            
            val updated = current.copy(
                pseudo = pseudo ?: current.pseudo,
                age = age ?: current.age,
                gender = gender ?: current.gender,
                isNsfwEnabled = if (isNsfwEnabled != null && current.isAdult()) {
                    isNsfwEnabled
                } else {
                    current.isNsfwEnabled
                }
            )
            
            saveUser(updated)
            _currentUser.value = updated
            
            Log.i(TAG, "✅ Profil mis à jour")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur màj profil", e)
            false
        }
    }
    
    /**
     * Hash SHA-256
     */
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
    
    /**
     * Sauvegarde un utilisateur
     */
    private fun saveUser(user: User) {
        val users = getAllUsers().toMutableList()
        users.removeAll { it.email == user.email }
        users.add(user)
        
        val json = json.encodeToString(users)
        prefs.edit().putString(KEY_USERS, json).apply()
    }
    
    /**
     * Récupère tous les utilisateurs
     */
    private fun getAllUsers(): List<User> {
        val jsonString = prefs.getString(KEY_USERS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<User>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Obtient l'utilisateur actuel
     */
    fun getCurrentUser(): User? = _currentUser.value
    
    /**
     * Vérifie si connecté
     */
    fun isUserLoggedIn(): Boolean = _isLoggedIn.value
}

/**
 * Résultat d'authentification
 */
sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
