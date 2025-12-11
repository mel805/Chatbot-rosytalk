package com.roleplayai.chatbot.data.manager

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Gestionnaire de clés API Groq PARTAGÉES entre tous les utilisateurs
 * 
 * VERSION SIMPLIFIÉE sans Firebase :
 * - Les clés sont gérées via GroqKeyManager (local)
 * - L'admin peut ajouter/supprimer des clés via l'UI
 * - Les clés sont synchronisées automatiquement dans l'app
 * 
 * Architecture :
 * - SharedPreferences pour stocker les clés
 * - Intégration directe avec GroqKeyManager
 * - Flow pour la réactivité UI
 */
class SharedGroqKeysManager(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "SharedGroqKeys"
    }
    
    private val groqKeyManager = GroqKeyManager(context)
    private val _keysFlow = MutableStateFlow<List<String>>(emptyList())
    
    init {
        // Charger les clés au démarrage
        CoroutineScope(Dispatchers.IO).launch {
            val keys = groqKeyManager.getAllKeys()
            _keysFlow.value = keys
            Log.d(TAG, "✅ ${keys.size} clés chargées")
        }
    }
    
    /**
     * Récupère les clés partagées en temps réel
     */
    fun getSharedKeysFlow(): Flow<List<String>> {
        return _keysFlow
    }
    
    /**
     * Récupère les clés partagées
     */
    suspend fun getSharedKeys(): List<String> {
        return groqKeyManager.getAllKeys()
    }
    
    /**
     * Ajoute une clé partagée (Admin uniquement)
     */
    suspend fun addSharedKey(apiKey: String): Boolean {
        return try {
            if (apiKey.isBlank()) {
                Log.w(TAG, "⚠️ Clé vide")
                return false
            }
            
            groqKeyManager.addKey(apiKey)
            
            // Mettre à jour le Flow
            _keysFlow.value = groqKeyManager.getAllKeys()
            
            Log.d(TAG, "✅ Clé ajoutée (Total: ${_keysFlow.value.size})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur ajout clé", e)
            false
        }
    }
    
    /**
     * Supprime une clé partagée (Admin uniquement)
     */
    suspend fun removeSharedKey(apiKey: String): Boolean {
        return try {
            groqKeyManager.removeKey(apiKey)
            
            // Mettre à jour le Flow
            _keysFlow.value = groqKeyManager.getAllKeys()
            
            Log.d(TAG, "✅ Clé supprimée (Total: ${_keysFlow.value.size})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur suppression clé", e)
            false
        }
    }
    
    /**
     * Synchronise les clés partagées avec le GroqKeyManager local
     */
    suspend fun syncSharedKeysToLocal() {
        try {
            val keys = groqKeyManager.getAllKeys()
            _keysFlow.value = keys
            Log.d(TAG, "✅ ${keys.size} clés synchronisées")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur synchronisation", e)
        }
    }
    
    /**
     * Initialise l'écoute en temps réel et synchronise automatiquement
     */
    suspend fun startAutoSync(): Flow<Int> {
        // Synchroniser immédiatement
        syncSharedKeysToLocal()
        
        // Retourner un flow qui émet le nombre de clés
        return _keysFlow.map { keys -> keys.size }
    }
}
