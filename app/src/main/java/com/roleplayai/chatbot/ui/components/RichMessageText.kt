package com.roleplayai.chatbot.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Composant pour afficher les messages avec couleurs immersives DISTINCTES
 * PERSONNAGE:
 * - 🟣 Actions *astérisques* : VIOLET vif
 * - 🔵 Pensées (parenthèses) : BLEU CLAIR
 * - ⚫ Paroles : BLANC (sur fond coloré)
 * 
 * UTILISATEUR:
 * - 🟠 Actions *astérisques* : ORANGE vif
 * - 🔵 Pensées (parenthèses) : BLEU FONCÉ
 * - ⚫ Paroles : BLANC (sur fond coloré)
 */
@Composable
fun RichMessageText(
    message: String,
    isUser: Boolean
) {
    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        val actionRegex = Regex("""\*([^*]+)\*""")
        val thoughtRegex = Regex("""\(([^)]+)\)""")
        
        // Liste de toutes les correspondances (actions et pensées)
        val actionMatches = actionRegex.findAll(message).toList()
        val thoughtMatches = thoughtRegex.findAll(message).toList()
        val allMatches = (actionMatches + thoughtMatches).sortedBy { it.range.first }
        
        for (match in allMatches) {
            // Ajouter le texte avant la correspondance (paroles normales)
            if (currentIndex < match.range.first) {
                val normalText = message.substring(currentIndex, match.range.first)
                withStyle(
                    style = SpanStyle(
                        color = Color.White, // Paroles en blanc pour contraste
                        fontWeight = FontWeight.Normal
                    )
                ) {
                    append(normalText)
                }
            }
            
            // Ajouter la correspondance avec style
            when (match) {
                in actionMatches -> {
                    // 🟣🟠 Actions entre *astérisques* : VIOLET vif (perso) / ORANGE vif (user)
                    withStyle(
                        style = SpanStyle(
                            color = if (isUser) Color(0xFFFF8C00) else Color(0xFFBA55D3), // Orange vif / Violet orchidée
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(match.value) // Garde les *astérisques*
                    }
                }
                in thoughtMatches -> {
                    // 🔵 Pensées entre (parenthèses) : BLEU distinct
                    withStyle(
                        style = SpanStyle(
                            color = if (isUser) Color(0xFF1E90FF) else Color(0xFF87CEEB), // Bleu dodger / Bleu ciel
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(match.value) // Garde les (parenthèses)
                    }
                }
            }
            
            currentIndex = match.range.last + 1
        }
        
        // Ajouter le texte restant (paroles)
        if (currentIndex < message.length) {
            val remainingText = message.substring(currentIndex)
            withStyle(
                style = SpanStyle(
                    color = Color.White, // Paroles en blanc
                    fontWeight = FontWeight.Normal
                )
            ) {
                append(remainingText)
            }
        }
    }
    
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge
    )
}
