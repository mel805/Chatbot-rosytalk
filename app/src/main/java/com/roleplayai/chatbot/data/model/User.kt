package com.roleplayai.chatbot.data.model

import kotlinx.serialization.Serializable

/**
 * Modèle d'utilisateur avec profil complet
 */
@Serializable
data class User(
    val email: String,
    val passwordHash: String,
    val pseudo: String,
    val age: Int,
    val gender: String,  // "male", "female", "other"
    val createdAt: Long = System.currentTimeMillis(),
    val isNsfwEnabled: Boolean = false,
    val isAdmin: Boolean = false  // Seul douvdouv21@gmail.com est admin
) {
    companion object {
        const val ADMIN_EMAIL = "douvdouv21@gmail.com"
    }
    /**
     * Vérifie si l'utilisateur est majeur
     */
    fun isAdult(): Boolean = age >= 18
    
    /**
     * Peut activer le mode NSFW (seulement si majeur)
     */
    fun canEnableNsfw(): Boolean = isAdult()
    
    /**
     * Retourne le genre formaté pour les prompts
     */
    fun getGenderForPrompt(): String = when (gender.lowercase()) {
        "male" -> "masculin"
        "female" -> "féminin"
        else -> "non-binaire"
    }
    
    /**
     * Retourne le pronom approprié
     */
    fun getPronoun(): String = when (gender.lowercase()) {
        "male" -> "il"
        "female" -> "elle"
        else -> "iel"
    }
}

/**
 * Sexe/Genre disponibles
 */
enum class Gender(val displayName: String, val value: String) {
    MALE("Homme", "male"),
    FEMALE("Femme", "female"),
    OTHER("Autre", "other")
}
