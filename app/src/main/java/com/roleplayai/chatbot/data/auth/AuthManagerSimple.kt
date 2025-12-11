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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
    private val firebaseSync = FirebaseUserSync(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // État de l'utilisateur actuel
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()
    
    init {
        // Migration automatique : mettre douvdouv21@gmail.com en admin si existe
        migrateAdminAccount()
        
        // Restaurer la session
        val email = prefs.getString(KEY_CURRENT_EMAIL, null)
        if (email != null) {
            val users = getAllUsers()
            _currentUser.value = users.find { it.email == email }
            _isLoggedIn.value = _currentUser.value != null
        }
    }
    
    /**
     * Migration : Met douvdouv21@gmail.com en admin s'il existe
     */
    private fun migrateAdminAccount() {
        try {
            val users = getAllUsers().toMutableList()
            val adminUser = users.find { it.email == User.ADMIN_EMAIL }
            
            if (adminUser != null && !adminUser.isAdmin) {
                Log.i(TAG, "🔄 Migration : Promotion de ${adminUser.email} en admin")
                val updated = adminUser.copy(isAdmin = true)
                users.removeAll { it.email == User.ADMIN_EMAIL }
                users.add(updated)
                
                val json = json.encodeToString(users)
                prefs.edit().putString(KEY_USERS, json).apply()
                
                // Si c'est l'utilisateur actuel, mettre à jour
                if (_currentUser.value?.email == User.ADMIN_EMAIL) {
                    _currentUser.value = updated
                }
                
                Log.i(TAG, "✅ ${adminUser.email} est maintenant admin")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur migration admin", e)
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
            
            // Créer l'utilisateur (vérifier si admin)
            val isAdmin = email.lowercase().trim() == User.ADMIN_EMAIL
            val user = User(
                email = email.lowercase().trim(),
                passwordHash = hashPassword(password),
                pseudo = pseudo.trim(),
                age = age,
                gender = gender,
                isNsfwEnabled = false,
                isAdmin = isAdmin
            )
            
            if (isAdmin) {
                Log.i(TAG, "👑 Création compte ADMIN: $email")
            }
            
            // Sauvegarder localement
            saveUser(user)
            
            // Synchroniser vers Firebase
            scope.launch {
                firebaseSync.syncUserToFirebase(user)
            }
            
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
     * Obtient tous les utilisateurs (ADMIN UNIQUEMENT)
     * Inclut les utilisateurs de Firebase (tous les appareils)
     */
    suspend fun getAllUsersForAdmin(): List<User> {
        val current = _currentUser.value
        if (current?.isAdmin != true) {
            Log.w(TAG, "⚠️ Accès refusé: non-admin")
            return emptyList()
        }
        
        // Récupérer les users Firebase (tous les appareils)
        val firebaseUsers = firebaseSync.getAllUsersFromFirebase()
        
        // Récupérer les users locaux
        val localUsers = getAllUsers()
        
        // Fusionner (Firebase prioritaire)
        val allEmails = (firebaseUsers.map { it.email } + localUsers.map { it.email }).toSet()
        val mergedUsers = allEmails.mapNotNull { email ->
            firebaseUsers.find { it.email == email } ?: localUsers.find { it.email == email }
        }
        
        Log.i(TAG, "✅ Total: ${mergedUsers.size} users (${firebaseUsers.size} Firebase + ${localUsers.size} local)")
        return mergedUsers.sortedByDescending { it.createdAt }
    }
    
    /**
     * Met à jour un utilisateur (ADMIN UNIQUEMENT)
     */
    suspend fun updateUserAsAdmin(targetEmail: String, isNsfwEnabled: Boolean? = null, isAdmin: Boolean? = null): Boolean {
        return try {
            val current = _currentUser.value
            if (current?.isAdmin != true) {
                Log.w(TAG, "⚠️ Modification refusée: non-admin")
                return false
            }
            
            val users = getAllUsers()
            val targetUser = users.find { it.email == targetEmail } ?: return false
            
            // Ne peut pas se retirer l'admin soi-même
            if (targetEmail == current.email && isAdmin == false) {
                Log.w(TAG, "⚠️ Impossible de se retirer l'admin")
                return false
            }
            
            val updated = targetUser.copy(
                isNsfwEnabled = isNsfwEnabled ?: targetUser.isNsfwEnabled,
                isAdmin = isAdmin ?: targetUser.isAdmin
            )
            
            saveUser(updated)
            
            // Synchroniser vers Firebase
            scope.launch {
                firebaseSync.updateUserInFirebase(
                    targetEmail,
                    mapOf(
                        "isNsfwEnabled" to updated.isNsfwEnabled,
                        "isAdmin" to updated.isAdmin
                    )
                )
            }
            
            Log.i(TAG, "✅ Utilisateur ${targetUser.pseudo} mis à jour par admin")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur màj utilisateur", e)
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
