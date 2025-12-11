package com.roleplayai.chatbot.data.auth

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Gestionnaire d'authentification LOCAL (sans Firebase)
 * - Connexion simple avec email
 * - Gestion multi-utilisateurs locale
 * - Admin automatique pour douvdouv21@gmail.com
 * - Préférences par utilisateur
 */
class LocalAuthManager(private val context: Context) {
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()
    
    companion object {
        private const val TAG = "LocalAuthManager"
        private const val ADMIN_EMAIL = "douvdouv21@gmail.com"
        
        private val CURRENT_USER_KEY = stringPreferencesKey("current_user")
        private val USERS_KEY = stringPreferencesKey("all_users")
        
        @Volatile
        private var INSTANCE: LocalAuthManager? = null
        
        fun getInstance(context: Context): LocalAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalAuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    init {
        // Charger l'utilisateur actuel au démarrage
        // On ne peut pas utiliser suspend dans init, on le fera lors du premier accès
    }
    
    /**
     * Charger l'utilisateur actuel depuis le stockage
     */
    suspend fun loadCurrentUser() {
        try {
            val prefs = context.userDataStore.data.first()
            val userJson = prefs[CURRENT_USER_KEY]
            
            if (userJson != null) {
                val user = json.decodeFromString<User>(userJson)
                _currentUser.value = user
                _isAdmin.value = user.email == ADMIN_EMAIL
                Log.i(TAG, "✅ Utilisateur chargé: ${user.email}")
                
                if (_isAdmin.value) {
                    Log.i(TAG, "👑 ADMIN détecté: ${user.email}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur chargement utilisateur", e)
        }
    }
    
    /**
     * Connexion avec email (création automatique si nouveau)
     */
    suspend fun signIn(email: String, displayName: String = "", username: String = "", bio: String = "", age: String = ""): Result<User> {
        return try {
            // Valider l'email
            if (!isValidEmail(email)) {
                return Result.failure(Exception("Email invalide"))
            }
            
            // Vérifier si l'utilisateur existe déjà
            val existingUser = getUserByEmail(email)
            
            // Créer ou mettre à jour l'utilisateur
            val user = if (existingUser != null) {
                // Utilisateur existant - mettre à jour les infos si fournies
                existingUser.copy(
                    displayName = if (displayName.isNotEmpty()) displayName else existingUser.displayName,
                    username = if (username.isNotEmpty()) username else existingUser.username,
                    bio = if (bio.isNotEmpty()) bio else existingUser.bio,
                    age = if (age.isNotEmpty()) age else existingUser.age
                )
            } else {
                // Nouvel utilisateur
                User(
                    email = email,
                    displayName = displayName.ifEmpty { email.split("@")[0] },
                    username = username.ifEmpty { email.split("@")[0] },
                    bio = bio,
                    age = age,
                    isAdmin = email == ADMIN_EMAIL,
                    nsfwEnabled = false,
                    groqApiKey = "",
                    groqModelId = "llama-3.1-8b-instant",
                    createdAt = System.currentTimeMillis()
                )
            }
            
            // Sauvegarder dans les utilisateurs existants
            saveUser(user)
            
            // Définir comme utilisateur actuel
            context.userDataStore.edit { prefs ->
                prefs[CURRENT_USER_KEY] = json.encodeToString(user)
            }
            
            _currentUser.value = user
            _isAdmin.value = user.isAdmin
            
            Log.i(TAG, "✅ Connexion réussie: ${user.email}")
            if (user.isAdmin) {
                Log.i(TAG, "👑 ADMIN connecté: ${user.email}")
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur connexion", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sauvegarder un utilisateur dans la liste
     */
    private suspend fun saveUser(user: User) {
        try {
            val prefs = context.userDataStore.data.first()
            val usersJson = prefs[USERS_KEY] ?: "[]"
            val users = json.decodeFromString<List<User>>(usersJson).toMutableList()
            
            // Supprimer l'ancien si existe
            users.removeAll { it.email == user.email }
            
            // Ajouter le nouveau
            users.add(user.copy(lastLogin = System.currentTimeMillis()))
            
            // Sauvegarder
            context.userDataStore.edit { prefs ->
                prefs[USERS_KEY] = json.encodeToString(users)
            }
            
            Log.d(TAG, "✅ Utilisateur sauvegardé: ${user.email}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur sauvegarde utilisateur", e)
        }
    }
    
    /**
     * Déconnexion
     */
    suspend fun signOut() {
        try {
            context.userDataStore.edit { prefs ->
                prefs.remove(CURRENT_USER_KEY)
            }
            
            _currentUser.value = null
            _isAdmin.value = false
            
            Log.i(TAG, "✅ Déconnexion réussie")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur déconnexion", e)
        }
    }
    
    /**
     * Vérifier si l'utilisateur est admin
     */
    fun isAdmin(): Boolean {
        return _currentUser.value?.isAdmin == true
    }
    
    /**
     * Obtenir les préférences de l'utilisateur actuel
     */
    fun getUserPreferences(): UserPreferences? {
        val user = _currentUser.value ?: return null
        return UserPreferences(
            nsfwEnabled = user.nsfwEnabled,
            groqApiKey = user.groqApiKey,
            groqModelId = user.groqModelId
        )
    }
    
    /**
     * Sauvegarder les préférences de l'utilisateur
     */
    suspend fun saveUserPreferences(preferences: UserPreferences) {
        val user = _currentUser.value ?: return
        
        try {
            val updatedUser = user.copy(
                nsfwEnabled = preferences.nsfwEnabled,
                groqApiKey = preferences.groqApiKey,
                groqModelId = preferences.groqModelId
            )
            
            // Sauvegarder dans la liste des utilisateurs
            saveUser(updatedUser)
            
            // Mettre à jour l'utilisateur actuel
            context.userDataStore.edit { prefs ->
                prefs[CURRENT_USER_KEY] = json.encodeToString(updatedUser)
            }
            
            _currentUser.value = updatedUser
            
            Log.d(TAG, "✅ Préférences sauvegardées pour ${user.email}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur sauvegarde préférences", e)
        }
    }
    
    /**
     * Obtenir tous les utilisateurs (admin uniquement)
     */
    suspend fun getAllUsers(): List<User> {
        return try {
            if (!isAdmin()) return emptyList()
            
            val prefs = context.userDataStore.data.first()
            val usersJson = prefs[USERS_KEY] ?: "[]"
            json.decodeFromString<List<User>>(usersJson)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lecture utilisateurs", e)
            emptyList()
        }
    }
    
    /**
     * Obtenir un utilisateur par email
     */
    private suspend fun getUserByEmail(email: String): User? {
        return try {
            val prefs = context.userDataStore.data.first()
            val usersJson = prefs[USERS_KEY] ?: "[]"
            val users = json.decodeFromString<List<User>>(usersJson)
            users.find { it.email == email }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lecture utilisateur", e)
            null
        }
    }
    
    /**
     * Mettre à jour le profil de l'utilisateur actuel
     */
    suspend fun updateUserProfile(displayName: String, username: String, bio: String, age: String): Result<User> {
        val user = _currentUser.value ?: return Result.failure(Exception("Aucun utilisateur connecté"))
        
        return try {
            val updatedUser = user.copy(
                displayName = displayName,
                username = username,
                bio = bio,
                age = age
            )
            
            // Sauvegarder dans la liste des utilisateurs
            saveUser(updatedUser)
            
            // Mettre à jour l'utilisateur actuel
            context.userDataStore.edit { prefs ->
                prefs[CURRENT_USER_KEY] = json.encodeToString(updatedUser)
            }
            
            _currentUser.value = updatedUser
            
            Log.i(TAG, "✅ Profil mis à jour pour ${user.email}")
            Result.success(updatedUser)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur mise à jour profil", e)
            Result.failure(e)
        }
    }
    
    /**
     * Valider l'email
     */
    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }
}

/**
 * Modèle d'utilisateur local
 */
@Serializable
data class User(
    val email: String,
    val displayName: String,
    val username: String = "",  // Pseudo pour être appelé dans les conversations
    val bio: String = "",  // Biographie/description personnelle
    val age: String = "",  // Âge (optionnel)
    val isAdmin: Boolean = false,
    val nsfwEnabled: Boolean = false,
    val groqApiKey: String = "",
    val groqModelId: String = "llama-3.1-8b-instant",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis()
)

data class UserPreferences(
    val nsfwEnabled: Boolean = false,
    val groqApiKey: String = "",
    val groqModelId: String = "llama-3.1-8b-instant"
)
