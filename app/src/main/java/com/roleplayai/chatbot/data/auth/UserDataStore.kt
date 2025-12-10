package com.roleplayai.chatbot.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Singleton pour le DataStore des utilisateurs
 * Évite les erreurs "multiple DataStores active for the same file"
 */
val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "users")
