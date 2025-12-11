package com.roleplayai.chatbot.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.roleplayai.chatbot.data.model.Chat
import com.roleplayai.chatbot.ui.viewmodel.ChatViewModel
import com.roleplayai.chatbot.ui.viewmodel.CharacterViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    onChatClick: (String, String) -> Unit, // chatId, characterId
    onCharacterProfileClick: (String) -> Unit,
    onNewConversation: (String) -> Unit
) {
    val chatViewModel: ChatViewModel = viewModel()
    val characterViewModel: CharacterViewModel = viewModel()
    
    val chats by chatViewModel.allChats.collectAsState()
    val sortedChats = remember(chats) {
        chats.sortedByDescending { it.lastMessageTime }
    }
    
    var showOptionsDialog by remember { mutableStateOf<Chat?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Conversations",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${sortedChats.size} conversation(s) en cours",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        
        // Liste des conversations
        if (sortedChats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Text(
                        "Aucune conversation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Commencez une conversation depuis l'onglet Explorer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sortedChats, key = { it.id }) { chat ->
                    ChatCard(
                        chat = chat,
                        onClick = { onChatClick(chat.id, chat.characterId) },
                        onOptionsClick = { showOptionsDialog = chat }
                    )
                }
            }
        }
    }
    
    // Dialog des options
    showOptionsDialog?.let { chat ->
        AlertDialog(
            onDismissRequest = { showOptionsDialog = null },
            title = { Text(chat.characterName) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Continuer la conversation
                    TextButton(
                        onClick = {
                            onChatClick(chat.id, chat.characterId)
                            showOptionsDialog = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ChatBubble, null, Modifier.padding(end = 8.dp))
                        Text("Continuer la conversation")
                    }
                    
                    Divider()
                    
                    // Nouvelle conversation
                    TextButton(
                        onClick = {
                            onNewConversation(chat.characterId)
                            showOptionsDialog = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddCircle, null, Modifier.padding(end = 8.dp))
                        Text("Nouvelle conversation")
                    }
                    
                    Divider()
                    
                    // Voir le profil
                    TextButton(
                        onClick = {
                            onCharacterProfileClick(chat.characterId)
                            showOptionsDialog = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Person, null, Modifier.padding(end = 8.dp))
                        Text("Voir le profil")
                    }
                    
                    Divider()
                    
                    // Supprimer la conversation
                    TextButton(
                        onClick = {
                            chatViewModel.deleteChat(chat.id)
                            showOptionsDialog = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.padding(end = 8.dp))
                        Text("Supprimer la conversation")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOptionsDialog = null }) {
                    Text("Fermer")
                }
            }
        )
    }
}

@Composable
fun ChatCard(
    chat: Chat,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar du personnage
            AsyncImage(
                model = chat.characterImageUrl,
                contentDescription = chat.characterName,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            // Informations de la conversation
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = chat.characterName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (chat.messages.isNotEmpty()) {
                    val lastMessage = chat.messages.last()
                    Text(
                        text = if (lastMessage.isUser) "Vous: ${lastMessage.content}" 
                              else lastMessage.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "Aucun message",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Timestamp
                Text(
                    text = formatTimestamp(chat.lastMessageTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Badge du nombre de messages
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${chat.messages.size}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            // Bouton options
            IconButton(onClick = onOptionsClick) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "À l'instant"
        diff < 3600_000 -> "${diff / 60_000} min"
        diff < 86400_000 -> "${diff / 3600_000} h"
        diff < 604800_000 -> "${diff / 86400_000} j"
        else -> {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
            sdf.format(Date(timestamp))
        }
    }
}
