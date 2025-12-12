package com.roleplayai.chatbot.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Wallpaper
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
import com.roleplayai.chatbot.ui.viewmodel.CharacterViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import coil.size.Scale
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
    val generatingChatId by viewModel.generatingChatId.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val characterViewModel: CharacterViewModel = viewModel()
    val character = remember(characterId) { characterViewModel.getCharacterById(characterId) }
    
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // État pour la sélection de l'image d'arrière-plan
    var showImageSelector by remember { mutableStateOf(false) }
    var selectedBackgroundImage by remember { mutableStateOf<String?>(null) }
    
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
        // Image d'arrière-plan du personnage - COMPLÈTEMENT VISIBLE
        chat?.let { currentChat ->
            AsyncImage(
                model = selectedBackgroundImage ?: currentChat.characterImageUrl,
                contentDescription = "${currentChat.characterName} background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Gradient overlay très léger pour la lisibilité des messages
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
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
                    actions = {
                        // Bouton pour changer l'image d'arrière-plan
                        IconButton(onClick = { showImageSelector = true }) {
                            Icon(
                                Icons.Default.Wallpaper,
                                contentDescription = "Changer l'arrière-plan",
                                tint = Color.White
                            )
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
                    
                    if (isGenerating && generatingChatId != null && generatingChatId == chat!!.id) {
                        item {
                            TypingIndicator(chatName = chat!!.characterName)
                        }
                    }
                }
            }
        }
        } // Fin du Scaffold
    } // Fin du Box avec l'image d'arrière-plan
    
    // Dialog de sélection d'image d'arrière-plan
    if (showImageSelector && character != null) {
        BackgroundImageSelectorDialog(
            character = character,
            currentBackgroundImage = selectedBackgroundImage ?: chat?.characterImageUrl ?: "",
            onImageSelected = { imageUrl ->
                selectedBackgroundImage = imageUrl
                showImageSelector = false
            },
            onDismiss = { showImageSelector = false }
        )
    }
}

@Composable
fun BackgroundImageSelectorDialog(
    character: com.roleplayai.chatbot.data.model.Character,
    currentBackgroundImage: String,
    onImageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val allImages = remember {
        listOf(character.imageUrl) + character.additionalImages
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "Changer l'arrière-plan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${allImages.size} images disponibles",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(400.dp)
            ) {
                items(allImages) { imageUrl ->
                    Card(
                        modifier = Modifier
                            .aspectRatio(0.75f)
                            .clickable { onImageSelected(imageUrl) },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (imageUrl == currentBackgroundImage) 8.dp else 2.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (imageUrl == currentBackgroundImage) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Box {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUrl)
                                    .crossfade(200)
                                    .size(300)
                                    .scale(Scale.FIT)
                                    .build(),
                                contentDescription = character.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            
                            // Indicateur si c'est l'image actuellement sélectionnée
                            if (imageUrl == currentBackgroundImage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Sélectionné",
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                            
                            // Badge "Image principale" pour la première
                            if (imageUrl == character.imageUrl) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        "Principale",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
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
