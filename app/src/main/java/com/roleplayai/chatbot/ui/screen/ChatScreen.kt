package com.roleplayai.chatbot.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.roleplayai.chatbot.data.model.Chat
import com.roleplayai.chatbot.data.model.Message
import com.roleplayai.chatbot.ui.components.RichMessageText
import com.roleplayai.chatbot.ui.theme.MessageAIBubble
import com.roleplayai.chatbot.ui.theme.MessageUserBubble
import com.roleplayai.chatbot.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    characterId: String,
    onBack: () -> Unit
) {
    val chat by viewModel.currentChat.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Create or get chat
    LaunchedEffect(characterId) {
        viewModel.createOrGetChat(characterId)
    }
    
    // Scroll to bottom when new messages arrive
    LaunchedEffect(chat?.messages?.size) {
        chat?.messages?.let {
            if (it.isNotEmpty()) {
                scope.launch {
                    listState.animateScrollToItem(it.size - 1)
                }
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Image d'arrière-plan du personnage
        chat?.let { currentChat ->
            AsyncImage(
                model = currentChat.characterImageUrl,
                contentDescription = "${currentChat.characterName} background",
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.3f), // Transparence pour ne pas gêner la lecture
                contentScale = ContentScale.Crop
            )
            
            // Gradient overlay léger pour améliorer la lisibilité
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
        }
        
        // Contenu de la conversation par-dessus
        Scaffold(
            containerColor = Color.Transparent, // Fond transparent pour voir l'image
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            chat?.let {
                                AsyncImage(
                                    model = it.characterImageUrl,
                                    contentDescription = it.characterName,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    it.characterName,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Écrivez votre message...") },
                        enabled = !isGenerating,
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )
                    
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        enabled = !isGenerating && messageText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (messageText.isNotBlank() && !isGenerating)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Send,
                            "Send",
                            tint = if (messageText.isNotBlank() && !isGenerating)
                                Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Error message
            error?.let {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                }
            }
            
            // Messages
            if (chat == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(chat!!.messages) { message ->
                        MessageBubble(message = message, chatName = chat!!.characterName)
                    }
                    
                    if (isGenerating) {
                        item {
                            TypingIndicator(chatName = chat!!.characterName)
                        }
                    }
                }
            }
        }
        } // Fin du Scaffold
    } // Fin du Box avec l'image d'arrière-plan
}

@Composable
fun MessageBubble(message: Message, chatName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    chatName.first().toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Surface(
            shape = RoundedCornerShape(
                topStart = if (message.isUser) 16.dp else 4.dp,
                topEnd = if (message.isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = if (message.isUser) MessageUserBubble else MessageAIBubble,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            // Utiliser RichMessageText pour affichage immersif avec couleurs
            Column(modifier = Modifier.padding(12.dp)) {
                RichMessageText(
                    message = message.content,
                    isUser = message.isUser
                )
            }
        }
    }
}

@Composable
fun TypingIndicator(chatName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                chatName.first().toString(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MessageAIBubble
        ) {
            Row(
                modifier = Modifier.padding(16.dp, 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.6f))
                    )
                }
            }
        }
    }
}
