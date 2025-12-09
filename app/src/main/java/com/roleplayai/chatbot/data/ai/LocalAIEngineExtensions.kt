package com.roleplayai.chatbot.data.ai

import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.Message

/**
 * Extensions pour LocalAIEngine - Gestion des questions
 */
fun LocalAIEngine.generateQuestionResponse(
    question: String,
    character: Character,
    messages: List<Message>
): String {
    val validator = ResponseValidator()
    val questionLower = question.lowercase()
    val subject = validator.extractQuestionSubject(question)
    
    return when (subject) {
        "âge" -> {
            when {
                character.personality.contains("timide", ignoreCase = true) -> 
                    "*rougit légèrement* Oh... *joue nerveusement avec ses cheveux* J'ai... euh... je suis assez jeune, tu sais. *détourne le regard* Pourquoi tu me demandes ça ?"
                character.personality.contains("énergique", ignoreCase = true) ->
                    "*sourit largement* J'ai 23 ans ! *geste expressif* L'âge parfait pour profiter de la vie ! *yeux brillants* Et toi ?"
                character.personality.contains("séductrice", ignoreCase = true) ->
                    "*sourire mystérieux* Assez vieille pour savoir ce que je veux... *regard intense* Assez jeune pour encore rêver. *se rapproche* Pourquoi, ça t'intéresse ?"
                else ->
                    "*sourit* J'ai dans la vingtaine. *penche la tête* C'est important pour toi ?"
            }
        }
        
        "nom" -> {
            "*sourit chaleureusement* Je m'appelle ${character.name}. *te regarde* Et toi, comment tu t'appelles ?"
        }
        
        "sentiment" -> {
            when {
                questionLower.contains("va") || questionLower.contains("vas") -> {
                    when {
                        character.personality.contains("timide", ignoreCase = true) ->
                            "*baisse les yeux* Je vais bien, merci... *sourit timidement* C'est gentil de demander. Et toi ?"
                        character.personality.contains("énergique", ignoreCase = true) ->
                            "*saute presque de joie* Je vais super bien ! *sourire radieux* Il fait beau, je suis avec toi, que demander de plus ? *rires* Et toi ?"
                        character.personality.contains("maternelle", ignoreCase = true) ->
                            "*sourire chaleureux* Je vais bien, mon cœur. *te caresse doucement* Mais c'est surtout toi qui m'intéresse. Comment tu te sens ?"
                        else ->
                            "*sourit* Je vais bien, merci de demander. *s'installe confortablement* Et toi, comment ça va ?"
                    }
                }
                questionLower.contains("sens") || questionLower.contains("ressens") -> {
                    "*réfléchit un moment* En ce moment ? *${getCharacterAction(character)}* Je me sens bien... un peu nerveuse peut-être. *sourit* C'est agréable de parler avec toi."
                }
                else -> {
                    "*sourit* Je me sens bien. *te regarde attentivement* Et toi ?"
                }
            }
        }
        
        "aime" -> {
            when {
                questionLower.contains("musique") -> {
                    "*yeux brillants* Oui, j'adore la musique ! *s'anime* J'écoute un peu de tout, mais surtout du rock et de la pop. *sourit* Et toi, tu aimes quel genre ?"
                }
                questionLower.contains("film") || questionLower.contains("ciné") -> {
                    "*sourit* Oui ! J'aime beaucoup les films. *réfléchit* Surtout les drames romantiques et les comédies. *te regarde* Tu as des recommandations ?"
                }
                questionLower.contains("sport") -> {
                    if (character.description.contains("sport", ignoreCase = true)) {
                        "*s'anime* Oui, j'adore le sport ! *geste enthousiaste* C'est ma passion. Et toi, tu pratiques ?"
                    } else {
                        "*sourit* Pas vraiment mon truc... *rires* Je préfère des activités plus calmes. Mais j'admire ceux qui en font !"
                    }
                }
                else -> {
                    "*réfléchit* Il y a beaucoup de choses que j'aime... *${getCharacterAction(character)}* Mais je dirais que ce que je préfère, c'est passer du temps avec des gens comme toi. *sourit* Et toi, qu'est-ce que tu aimes ?"
                }
            }
        }
        
        "fait" -> {
            when {
                character.description.contains("étudiant", ignoreCase = true) ->
                    "*sourit* Je suis étudiante. *yeux pétillants* J'étudie l'art et le design. C'est passionnant ! *montre son enthousiasme* Et toi, tu fais quoi ?"
                character.description.contains("travail", ignoreCase = true) ->
                    "*réfléchit* Je travaille... *${getCharacterAction(character)}* C'est pas toujours facile, mais ça va. *sourit* Et toi ?"
                else ->
                    "*${getCharacterAction(character)}* Oh, un peu de tout... *sourire mystérieux* Je suis assez occupée. Et toi, qu'est-ce que tu fais ?"
            }
        }
        
        "lieu" -> {
            "*sourit* Je vis pas loin d'ici. *regarde autour* C'est un quartier plutôt sympa. *te regarde* Et toi, tu habites dans le coin ?"
        }
        
        "temps" -> {
            "*réfléchit* Maintenant ? *${getCharacterAction(character)}* On est en fin d'après-midi je crois. *sourit* Le temps passe vite quand je parle avec toi."
        }
        
        "raison" -> {
            "*penche la tête* Pourquoi quoi exactement ? *te regarde curieusement* Explique-moi un peu plus ce que tu veux savoir."
        }
        
        "manière" -> {
            when {
                questionLower.contains("comment ça va") || questionLower.contains("comment vas") ->
                    "*sourit* Je vais bien, merci. *${getCharacterAction(character)}* Et toi ?"
                questionLower.contains("comment tu") ->
                    "*réfléchit* Hmm, c'est une bonne question... *${getCharacterAction(character)}* Laisse-moi réfléchir..."
                else ->
                    "*${getCharacterAction(character)}* Je ne suis pas sûre de comprendre ta question. Tu peux reformuler ?"
            }
        }
        
        else -> {
            // Question générique
            "*${getCharacterAction(character)}* C'est une question intéressante... *réfléchit* Laisse-moi y penser. *te regarde* Pourquoi tu me demandes ça ?"
        }
    }
}

private fun getCharacterAction(character: Character): String {
    return when {
        character.personality.contains("timide", ignoreCase = true) -> "joue nerveusement avec ses cheveux"
        character.personality.contains("énergique", ignoreCase = true) -> "geste expressif"
        character.personality.contains("séductrice", ignoreCase = true) -> "regard intense"
        character.personality.contains("maternelle", ignoreCase = true) -> "sourire chaleureux"
        character.personality.contains("mystérieuse", ignoreCase = true) -> "regarde pensivement"
        else -> "sourit"
    }
}
