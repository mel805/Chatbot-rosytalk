package com.roleplayai.chatbot.data.model

data class Character(
    val id: String,
    val name: String,
    val description: String,
    val personality: String,
    val scenario: String,
    val imageUrl: String,
    val category: CharacterCategory,
    val themes: List<CharacterTheme>,
    val greeting: String,
    val exampleDialogues: List<String> = emptyList(),
    val gender: CharacterGender = CharacterGender.FEMALE,
    
    // Description courte pour la liste (ex: "Amie de ta fille")
    val shortDescription: String = "",
    
    // Nouvelles informations pour la page de profil
    val physicalDescription: String = "",  // Description physique détaillée
    val characterTraits: List<String> = emptyList(), // Traits de caractère
    val additionalImages: List<String> = emptyList(), // URLs d'images supplémentaires
    
    // Images NSFW (utilisées uniquement si le mode NSFW est activé)
    val nsfwImageUrl: String = "",  // Image principale NSFW
    val nsfwAdditionalImages: List<String> = emptyList() // Images additionnelles NSFW
)

enum class CharacterGender {
    MALE,       // Masculin
    FEMALE,     // Féminin
    NON_BINARY  // Non-binaire
}

enum class CharacterCategory {
    ANIME,
    FANTASY,
    REAL,
    CELEBRITY,
    HISTORICAL
}

enum class CharacterTheme {
    FAMILY_MOM,
    FAMILY_SISTER,
    FAMILY_DAUGHTER,
    FRIEND_FEMALE,
    FRIEND_MALE,
    NEIGHBOR,
    MILF,
    GIRLFRIEND,
    BOYFRIEND,
    TEACHER,
    STUDENT,
    COWORKER,
    BOSS,
    STRANGER,
    CHILDHOOD_FRIEND
}

fun CharacterTheme.toDisplayName(): String {
    return when (this) {
        CharacterTheme.FAMILY_MOM -> "Maman"
        CharacterTheme.FAMILY_SISTER -> "Sœur"
        CharacterTheme.FAMILY_DAUGHTER -> "Fille"
        CharacterTheme.FRIEND_FEMALE -> "Amie"
        CharacterTheme.FRIEND_MALE -> "Ami"
        CharacterTheme.NEIGHBOR -> "Voisin(e)"
        CharacterTheme.MILF -> "MILF"
        CharacterTheme.GIRLFRIEND -> "Petite amie"
        CharacterTheme.BOYFRIEND -> "Petit ami"
        CharacterTheme.TEACHER -> "Professeur"
        CharacterTheme.STUDENT -> "Étudiant(e)"
        CharacterTheme.COWORKER -> "Collègue"
        CharacterTheme.BOSS -> "Patron(ne)"
        CharacterTheme.STRANGER -> "Inconnu(e)"
        CharacterTheme.CHILDHOOD_FRIEND -> "Ami d'enfance"
    }
}

fun CharacterCategory.toDisplayName(): String {
    return when (this) {
        CharacterCategory.ANIME -> "Anime/Manga"
        CharacterCategory.FANTASY -> "Fantasy"
        CharacterCategory.REAL -> "Réel"
        CharacterCategory.CELEBRITY -> "Célébrité"
        CharacterCategory.HISTORICAL -> "Historique"
    }
}
