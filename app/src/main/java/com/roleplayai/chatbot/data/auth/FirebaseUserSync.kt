package com.roleplayai.chatbot.data.auth

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.roleplayai.chatbot.data.model.User
import kotlinx.coroutines.tasks.await

/**
 * Synchronisation des utilisateurs avec Firebase Firestore
 * Permet à l'admin de voir TOUS les comptes créés sur n'importe quel appareil
 */
class FirebaseUserSync(private val context: Context) {
    
    companion object {
        private const val TAG = "FirebaseUserSync"
        private const val COLLECTION_USERS = "users"
    }
    
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * Synchronise un utilisateur vers Firebase
     */
    suspend fun syncUserToFirebase(user: User): Boolean {
        return try {
            val userMap = mapOf(
                "email" to user.email,
                "pseudo" to user.pseudo,
                "age" to user.age,
                "gender" to user.gender,
                "createdAt" to user.createdAt,
                "isNsfwEnabled" to user.isNsfwEnabled,
                "isAdmin" to user.isAdmin
            )
            
            firestore.collection(COLLECTION_USERS)
                .document(user.email) // Email comme ID unique
                .set(userMap)
                .await()
            
            Log.i(TAG, "✅ Utilisateur ${user.pseudo} synchronisé vers Firebase")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur sync Firebase: ${e.message}")
            false
        }
    }
    
    /**
     * Récupère TOUS les utilisateurs depuis Firebase
     * Accessible uniquement par l'admin
     */
    suspend fun getAllUsersFromFirebase(): List<User> {
        return try {
            val snapshot = firestore.collection(COLLECTION_USERS)
                .get()
                .await()
            
            val users = snapshot.documents.mapNotNull { doc ->
                try {
                    User(
                        email = doc.getString("email") ?: return@mapNotNull null,
                        passwordHash = "", // Pas stocké dans Firebase pour sécurité
                        pseudo = doc.getString("pseudo") ?: "Unknown",
                        age = (doc.getLong("age") ?: 18).toInt(),
                        gender = doc.getString("gender") ?: "other",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        isNsfwEnabled = doc.getBoolean("isNsfwEnabled") ?: false,
                        isAdmin = doc.getBoolean("isAdmin") ?: false
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erreur parsing user: ${e.message}")
                    null
                }
            }
            
            Log.i(TAG, "✅ ${users.size} utilisateurs récupérés depuis Firebase")
            users
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur récupération Firebase: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Met à jour un utilisateur dans Firebase
     */
    suspend fun updateUserInFirebase(email: String, updates: Map<String, Any>): Boolean {
        return try {
            firestore.collection(COLLECTION_USERS)
                .document(email)
                .update(updates)
                .await()
            
            Log.i(TAG, "✅ Utilisateur $email mis à jour dans Firebase")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur màj Firebase: ${e.message}")
            false
        }
    }
}
