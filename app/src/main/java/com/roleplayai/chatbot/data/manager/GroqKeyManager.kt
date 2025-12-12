package com.roleplayai.chatbot.data.manager

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Gestionnaire de clés API Groq avec rotation automatique
 * 
 * Permet d'ajouter plusieurs clés API Groq qui tourneront automatiquement
 * quand l'une atteint sa limite de requêtes
 * 
 * Fonctionnalités :
 * - Support de plusieurs clés API
 * - Rotation automatique en cas d'erreur 429 (rate limit)
 * - Détection et blacklist temporaire des clés épuisées
 * - Réinitialisation automatique toutes les 24h
 */
class GroqKeyManager(private val context: Context) {
    
    companion object {
        private const val TAG = "GroqKeyManager"
        private const val PREFS_NAME = "groq_keys_prefs"
        private const val KEY_API_KEYS = "api_keys"
        private const val KEY_CURRENT_INDEX = "current_index"
        private const val KEY_BLACKLIST = "blacklist"
        private const val KEY_LAST_RESET = "last_reset"
        
        private const val RESET_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 heures
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    
    // Liste des clés API
    private var apiKeys: MutableList<String> = mutableListOf()
    
    // Index de la clé actuelle
    private var currentIndex = 0
    
    // Clés blacklistées temporairement (rate limit)
    private val blacklistedKeys = mutableSetOf<String>()
    
    init {
        loadKeys()
        checkAndResetBlacklist()
    }
    
    /**
     * Charge les clés depuis les préférences
     */
    private fun loadKeys() {
        val keysString = prefs.getString(KEY_API_KEYS, "") ?: ""
        apiKeys = if (keysString.isNotEmpty()) {
            keysString.split(",").toMutableList()
        } else {
            mutableListOf()
        }
        
        currentIndex = prefs.getInt(KEY_CURRENT_INDEX, 0)
        
        val blacklistString = prefs.getString(KEY_BLACKLIST, "") ?: ""
        if (blacklistString.isNotEmpty()) {
            blacklistedKeys.addAll(blacklistString.split(","))
        }
        
        Log.d(TAG, "📊 Chargé: ${apiKeys.size} clés, Index: $currentIndex, Blacklist: ${blacklistedKeys.size}")
    }
    
    /**
     * Vérifie si la blacklist doit être réinitialisée (toutes les 24h)
     */
    private fun checkAndResetBlacklist() {
        val lastReset = prefs.getLong(KEY_LAST_RESET, 0)
        val now = System.currentTimeMillis()
        
        if (now - lastReset > RESET_INTERVAL_MS) {
            Log.d(TAG, "🔄 Réinitialisation blacklist (24h écoulées)")
            blacklistedKeys.clear()
            prefs.edit()
                .putString(KEY_BLACKLIST, "")
                .putLong(KEY_LAST_RESET, now)
                .apply()
        }
    }
    
    /**
     * Ajoute une clé API
     */
    suspend fun addKey(apiKey: String) = mutex.withLock {
        if (apiKey.isBlank()) {
            Log.w(TAG, "⚠️ Tentative d'ajout d'une clé vide")
            return@withLock
        }
        
        if (!apiKeys.contains(apiKey)) {
            apiKeys.add(apiKey)
            saveKeys()
            Log.d(TAG, "✅ Clé ajoutée (Total: ${apiKeys.size})")
        } else {
            Log.d(TAG, "⚠️ Clé déjà présente")
        }
    }
    
    /**
     * Supprime une clé API
     */
    suspend fun removeKey(apiKey: String) = mutex.withLock {
        if (apiKeys.remove(apiKey)) {
            saveKeys()
            // Réajuster l'index si nécessaire
            if (currentIndex >= apiKeys.size) {
                currentIndex = 0
            }
            Log.d(TAG, "✅ Clé supprimée (Total: ${apiKeys.size})")
        }
    }
    
    /**
     * Définit les clés (remplace toutes les clés existantes)
     */
    suspend fun setKeys(keys: List<String>) = mutex.withLock {
        apiKeys.clear()
        apiKeys.addAll(keys.filter { it.isNotBlank() })
        currentIndex = 0
        blacklistedKeys.clear()
        saveKeys()
        Log.d(TAG, "✅ ${apiKeys.size} clés définies")
    }
    
    /**
     * Obtient la clé actuelle (non blacklistée)
     */
    suspend fun getCurrentKey(): String? = mutex.withLock {
        if (apiKeys.isEmpty()) {
            Log.w(TAG, "⚠️ Aucune clé API disponible")
            return@withLock null
        }
        
        // Chercher une clé non blacklistée
        val availableKeys = apiKeys.filter { !blacklistedKeys.contains(it) }
        
        if (availableKeys.isEmpty()) {
            Log.w(TAG, "⚠️ Toutes les clés sont blacklistées")
            return@withLock null
        }
        
        // Si la clé actuelle est blacklistée, passer à la suivante
        var attempts = 0
        while (blacklistedKeys.contains(apiKeys[currentIndex]) && attempts < apiKeys.size) {
            rotateToNextKey()
            attempts++
        }
        
        val key = apiKeys[currentIndex]
        Log.d(TAG, "🔑 Clé actuelle: Index $currentIndex/${apiKeys.size}")
        return@withLock key
    }
    
    /**
     * Marque la clé actuelle comme ayant atteint sa limite (rate limit)
     * et passe automatiquement à la suivante
     */
    suspend fun markCurrentKeyAsRateLimited() = mutex.withLock {
        if (apiKeys.isEmpty()) return@withLock
        
        val key = apiKeys[currentIndex]
        blacklistedKeys.add(key)
        Log.w(TAG, "⚠️ Clé ${currentIndex + 1}/${apiKeys.size} rate limitée, rotation...")
        
        saveBlacklist()
        rotateToNextKey()
    }
    
    /**
     * Passe à la clé suivante
     */
    private fun rotateToNextKey() {
        if (apiKeys.isEmpty()) return
        
        currentIndex = (currentIndex + 1) % apiKeys.size
        prefs.edit().putInt(KEY_CURRENT_INDEX, currentIndex).apply()
        
        Log.d(TAG, "🔄 Rotation vers clé ${currentIndex + 1}/${apiKeys.size}")
    }

    /**
     * Passe à la clé suivante SANS blacklister la clé actuelle.
     * Utile pour ignorer une clé invalide (401/403) ou une erreur ponctuelle.
     */
    suspend fun rotateToNextKeyWithoutBlacklist() = mutex.withLock {
        if (apiKeys.isEmpty()) return@withLock
        rotateToNextKey()
    }
    
    /**
     * Obtient toutes les clés
     */
    suspend fun getAllKeys(): List<String> = mutex.withLock {
        return@withLock apiKeys.toList()
    }
    
    /**
     * Obtient le nombre de clés disponibles (non blacklistées)
     */
    suspend fun getAvailableKeysCount(): Int = mutex.withLock {
        return@withLock apiKeys.count { !blacklistedKeys.contains(it) }
    }
    
    /**
     * Obtient le nombre total de clés
     */
    suspend fun getTotalKeysCount(): Int = mutex.withLock {
        return@withLock apiKeys.size
    }
    
    /**
     * Réinitialise manuellement la blacklist
     */
    suspend fun resetBlacklist() = mutex.withLock {
        blacklistedKeys.clear()
        prefs.edit()
            .putString(KEY_BLACKLIST, "")
            .putLong(KEY_LAST_RESET, System.currentTimeMillis())
            .apply()
        Log.d(TAG, "🔄 Blacklist réinitialisée manuellement")
    }
    
    /**
     * Sauvegarde les clés
     */
    private fun saveKeys() {
        prefs.edit()
            .putString(KEY_API_KEYS, apiKeys.joinToString(","))
            .putInt(KEY_CURRENT_INDEX, currentIndex)
            .apply()
    }
    
    /**
     * Sauvegarde la blacklist
     */
    private fun saveBlacklist() {
        prefs.edit()
            .putString(KEY_BLACKLIST, blacklistedKeys.joinToString(","))
            .apply()
    }
}
