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
 * Composant pour afficher les messages avec couleurs immersives
 * - Actions entre *astérisques* : Violet/Orange
 * - Pensées entre (parenthèses) : Bleu/Cyan
 * - Paroles normales : Couleur standard
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
                        color = if (isUser) Color(0xFF1A1A1A) else Color(0xFF2A2A2A),
                        fontWeight = FontWeight.Normal
                    )
                ) {
                    append(normalText)
                }
            }
            
            // Ajouter la correspondance avec style
            when (match) {
                in actionMatches -> {
                    // Actions entre *astérisques* : Violet/Orange avec italique
                    withStyle(
                        style = SpanStyle(
                            color = if (isUser) Color(0xFFFF6B35) else Color(0xFF9C27B0),
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(match.value) // Garde les *astérisques*
                    }
                }
                in thoughtMatches -> {
                    // Pensées entre (parenthèses) : Bleu/Cyan avec italique
                    withStyle(
                        style = SpanStyle(
                            color = if (isUser) Color(0xFF0288D1) else Color(0xFF00BCD4),
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Light
                        )
                    ) {
                        append(match.value) // Garde les (parenthèses)
                    }
                }
            }
            
            currentIndex = match.range.last + 1
        }
        
        // Ajouter le texte restant
        if (currentIndex < message.length) {
            val remainingText = message.substring(currentIndex)
            withStyle(
                style = SpanStyle(
                    color = if (isUser) Color(0xFF1A1A1A) else Color(0xFF2A2A2A),
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
