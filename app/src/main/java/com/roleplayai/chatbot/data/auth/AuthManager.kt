package com.roleplayai.chatbot.data.auth

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Gestionnaire d'authentification Firebase
 * - Connexion Google
 * - Gestion des utilisateurs
 * - Système admin pour douvdouv21@gmail.com
 */
class AuthManager(private val context: Context) {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()
    
    companion object {
        private const val TAG = "AuthManager"
        private const val ADMIN_EMAIL = "douvdouv21@gmail.com"
        
        // Pour obtenir ce token, allez dans Firebase Console → Project Settings → Web API Key
        private const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
    }
    
    init {
        // Observer les changements d'authentification
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            _isAdmin.value = user?.email == ADMIN_EMAIL
            
            if (user != null) {
                Log.i(TAG, "Utilisateur connecté: ${user.email}")
                if (_isAdmin.value) {
                    Log.i(TAG, "👑 ADMIN détecté: ${user.email}")
                }
                // Créer/Mettre à jour le profil Firestore
                updateUserProfile(user)
            }
        }
        
        // Initialiser avec l'utilisateur actuel
        _currentUser.value = auth.currentUser
        _isAdmin.value = auth.currentUser?.email == ADMIN_EMAIL
    }
    
    /**
     * Obtenir le client Google Sign-In
     */
    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        
        return GoogleSignIn.getClient(context, gso)
    }
    
    /**
     * Se connecter avec un token Google
     */
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Utilisateur null après connexion")
            
            Log.i(TAG, "✅ Connexion réussie: ${user.email}")
            
            // Créer le profil Firestore
            updateUserProfile(user)
            
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur connexion Google", e)
            Result.failure(e)
        }
    }
    
    /**
     * Créer/Mettre à jour le profil utilisateur dans Firestore
     */
    private fun updateUserProfile(user: FirebaseUser) {
        val userDoc = firestore.collection("users").document(user.uid)
        
        val userData = hashMapOf(
            "email" to user.email,
            "displayName" to user.displayName,
            "photoURL" to user.photoUrl?.toString(),
            "isAdmin" to (user.email == ADMIN_EMAIL),
            "lastLogin" to com.google.firebase.Timestamp.now()
        )
        
        // Utiliser set avec merge pour ne pas écraser les données existantes
        userDoc.set(userData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "✅ Profil Firestore mis à jour")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Erreur mise à jour Firestore", e)
            }
    }
    
    /**
     * Déconnexion
     */
    suspend fun signOut() {
        try {
            auth.signOut()
            getGoogleSignInClient().signOut().await()
            Log.i(TAG, "✅ Déconnexion réussie")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur déconnexion", e)
        }
    }
    
    /**
     * Vérifier si l'utilisateur est admin
     */
    fun isAdmin(): Boolean {
        return auth.currentUser?.email == ADMIN_EMAIL
    }
    
    /**
     * Obtenir les préférences utilisateur depuis Firestore
     */
    suspend fun getUserPreferences(): UserPreferences? {
        val user = auth.currentUser ?: return null
        
        return try {
            val doc = firestore.collection("users")
                .document(user.uid)
                .get()
                .await()
            
            UserPreferences(
                nsfwEnabled = doc.getBoolean("nsfwEnabled") ?: false,
                groqApiKey = doc.getString("groqApiKey") ?: "",
                groqModelId = doc.getString("groqModelId") ?: "llama-3.1-8b-instant"
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lecture préférences", e)
            null
        }
    }
    
    /**
     * Sauvegarder les préférences utilisateur
     */
    suspend fun saveUserPreferences(preferences: UserPreferences) {
        val user = auth.currentUser ?: return
        
        try {
            firestore.collection("users")
                .document(user.uid)
                .update(
                    mapOf(
                        "nsfwEnabled" to preferences.nsfwEnabled,
                        "groqApiKey" to preferences.groqApiKey,
                        "groqModelId" to preferences.groqModelId
                    )
                )
                .await()
            
            Log.d(TAG, "✅ Préférences sauvegardées")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur sauvegarde préférences", e)
        }
    }
}

data class UserPreferences(
    val nsfwEnabled: Boolean = false,
    val groqApiKey: String = "",
    val groqModelId: String = "llama-3.1-8b-instant"
)
