package com.roleplayai.chatbot.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.roleplayai.chatbot.data.model.Character
import com.roleplayai.chatbot.data.model.CharacterGender
import com.roleplayai.chatbot.ui.components.ImageViewerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterProfileScreen(
    character: Character,
    onBack: () -> Unit,
    onStartChat: () -> Unit,
    isNsfwMode: Boolean = false
) {
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    
    // Utiliser les images appropriées selon le mode NSFW
    val (mainImageUrl, additionalImageUrls) = remember(character, isNsfwMode) {
        if (isNsfwMode && character.nsfwImageUrl.isNotEmpty()) {
            // En mode NSFW : combiner les images SFW + NSFW
            val combinedImages = character.additionalImages + character.nsfwAdditionalImages
            Pair(character.imageUrl, combinedImages)
        } else {
            // Mode normal : seulement les images SFW
            Pair(character.imageUrl, character.additionalImages)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartChat,
                icon = { Icon(Icons.Default.Chat, "Chat") },
                text = { Text("Démarrer conversation") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image principale avec gradient overlay
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    // Image principale optimisée
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(mainImageUrl)
                            .crossfade(400) // Animation fluide
                            .size(1024) // Taille optimale pour image principale
                            .scale(Scale.FIT)
                            .build(),
                        contentDescription = character.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    )
                    
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                    
                    // Nom et genre en bas
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = character.name,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (character.gender) {
                                    CharacterGender.MALE -> Icons.Default.Male
                                    CharacterGender.FEMALE -> Icons.Default.Female
                                    CharacterGender.NON_BINARY -> Icons.Default.Transgender
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = when (character.gender) {
                                    CharacterGender.MALE -> "Masculin"
                                    CharacterGender.FEMALE -> "Féminin"
                                    CharacterGender.NON_BINARY -> "Non-binaire"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            
            // Description générale
            item {
                ProfileSection(
                    title = "📝 Description",
                    icon = Icons.Default.Description
                ) {
                    Text(
                        text = character.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Scénario de départ
            if (character.scenario.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "🎬 Scénario de Départ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Divider(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                thickness = 1.dp
                            )
                            Text(
                                text = character.scenario,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                            )
                        }
                    }
                }
            }
            
            // Description physique
            if (character.physicalDescription.isNotBlank()) {
                item {
                    ProfileSection(
                        title = "👤 Apparence Physique",
                        icon = Icons.Default.Person
                    ) {
                        Text(
                            text = character.physicalDescription,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Personnalité
            item {
                ProfileSection(
                    title = "🎭 Personnalité",
                    icon = Icons.Default.Psychology
                ) {
                    Text(
                        text = character.personality,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Traits de caractère
            if (character.characterTraits.isNotEmpty()) {
                item {
                    ProfileSection(
                        title = "✨ Traits de Caractère",
                        icon = Icons.Default.Star
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            character.characterTraits.forEach { trait ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = trait,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Scénario
            item {
                ProfileSection(
                    title = "📖 Scénario",
                    icon = Icons.Default.Book
                ) {
                    Text(
                        text = character.scenario,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Galerie d'images générées par IA (cliquables pour zoom)
            if (additionalImageUrls.isNotEmpty()) {
                item {
                    ProfileSection(
                        title = if (isNsfwMode) "🔞 Galerie Complète (${additionalImageUrls.size} images)" else "🖼️ Galerie (${additionalImageUrls.size} images)",
                        icon = Icons.Default.PhotoLibrary
                    ) {
                        Text(
                            if (isNsfwMode) "Mode NSFW : Toutes les images SFW + NSFW" else "Cliquez sur une image pour l'agrandir",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(additionalImageUrls) { imageUrl ->
                                Card(
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clickable { selectedImageUrl = imageUrl },
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    // Image optimisée pour chargement rapide
                                    SubcomposeAsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(imageUrl)
                                            .crossfade(300) // Animation fluide
                                            .size(360) // Taille réduite pour miniature (2x pour haute densité)
                                            .scale(Scale.FIT)
                                            .build(),
                                        contentDescription = "Image de ${character.name}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        loading = {
                                            // Placeholder pendant le chargement
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                        },
                                        error = {
                                            // Affichage en cas d'erreur
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.errorContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.BrokenImage,
                                                    contentDescription = "Erreur",
                                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Message d'accueil
            item {
                ProfileSection(
                    title = "💬 Message d'accueil",
                    icon = Icons.Default.Message
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = character.greeting,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
            
            // Espace pour le FAB
            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }
    
    // Dialog pour afficher image en grand
    selectedImageUrl?.let { imageUrl ->
        ImageViewerDialog(
            imageUrl = imageUrl,
            characterName = character.name,
            onDismiss = { selectedImageUrl = null }
        )
    }
}

@Composable
fun ProfileSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}
