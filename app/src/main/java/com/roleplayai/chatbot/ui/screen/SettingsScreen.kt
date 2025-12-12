package com.roleplayai.chatbot.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roleplayai.chatbot.data.ai.GroqAIEngine
import com.roleplayai.chatbot.data.ai.AIOrchestrator
import com.roleplayai.chatbot.data.model.ModelConfig
import com.roleplayai.chatbot.data.model.ModelState
import com.roleplayai.chatbot.ui.viewmodel.AuthViewModel
import com.roleplayai.chatbot.ui.viewmodel.ModelViewModel
import com.roleplayai.chatbot.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ModelViewModel,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToAdminUsers: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val settingsViewModel: SettingsViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val scope = rememberCoroutineScope()
    
    val currentUser by authViewModel.currentUser.collectAsState()
    val isAdmin by authViewModel.isAdmin.collectAsState()
    
    val selectedModel by viewModel.selectedModel.collectAsState()
    val modelState by viewModel.modelState.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val availableRam by viewModel.availableRam.collectAsState()
    
    // Groq settings
    val useGroqApi by settingsViewModel.useGroqApi.collectAsState()
    val groqApiKey by settingsViewModel.groqApiKey.collectAsState()
    val groqModelId by settingsViewModel.groqModelId.collectAsState()
    val nsfwMode by settingsViewModel.nsfwMode.collectAsState()
    
    // AI Engine settings
    val selectedAIEngine by settingsViewModel.selectedAIEngine.collectAsState()
    val enableAIFallbacks by settingsViewModel.enableAIFallbacks.collectAsState()
    val llamaCppModelPath by settingsViewModel.llamaCppModelPath.collectAsState()
    
    // Clés partagées
    val sharedGroqKeys by settingsViewModel.sharedGroqKeys.collectAsState()
    val isLoading by settingsViewModel.isLoading.collectAsState()
    val statusMessage by settingsViewModel.statusMessage.collectAsState()
    
    var showModelSelection by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showGroqModels by remember { mutableStateOf(false) }
    var showAddKeyDialog by remember { mutableStateOf(false) }
    var newApiKeyInput by remember { mutableStateOf("") }
    var keyToDelete by remember { mutableStateOf<String?>(null) }
    var showAIEngineSelection by remember { mutableStateOf(false) }
    var showLlamaCppModelSelection by remember { mutableStateOf(false) }
    
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
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Paramètres",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (isAdmin) {
                    Text(
                        text = "👑 Mode Administrateur",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Compte
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAdmin) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAdmin) "👑 Compte Admin" else "Compte",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                currentUser?.let { user ->
                                    Text(
                                        text = user.pseudo,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Button(
                                onClick = {
                                    authViewModel.logout()
                                    onLogout() // Naviguer vers la page de connexion
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.ExitToApp, "Déconnexion", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Déconnexion")
                            }
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        
                        // Bouton vers le profil
                        OutlinedButton(
                            onClick = onNavigateToProfile,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Person, "Profil", modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Modifier mon profil")
                        }
                        
                        if (isAdmin) {
                            Divider()
                            Text(
                                text = "✅ Vous avez le contrôle total de l'application",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // === SECTION MOTEUR IA (TOUS) ===
            item {
                Text(
                    "🤖 Moteur d'Intelligence Artificielle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Choisissez le moteur IA pour générer les réponses des personnages.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Sélection du moteur IA
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAIEngineSelection = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Moteur actuel",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            val engineEnum = try {
                                AIOrchestrator.AIEngine.valueOf(selectedAIEngine)
                            } catch (e: Exception) {
                                AIOrchestrator.AIEngine.GROQ
                            }
                            Text(
                                engineEnum.getDisplayName(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                engineEnum.getDescription(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
            
            // Fallbacks automatiques
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Fallbacks automatiques",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (enableAIFallbacks) "Si le moteur principal échoue, essayer d'autres moteurs" else "Utiliser uniquement le moteur sélectionné",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableAIFallbacks,
                            onCheckedChange = { scope.launch { settingsViewModel.setEnableAIFallbacks(it) } }
                        )
                    }
                }
            }
            
            // Configuration llama.cpp si sélectionné
            if (selectedAIEngine == "LLAMA_CPP") {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLlamaCppModelSelection = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Modèle llama.cpp",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (llamaCppModelPath.isNotBlank()) {
                                        File(llamaCppModelPath).name
                                    } else {
                                        "Aucun modèle sélectionné"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (llamaCppModelPath.isNotBlank()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }
            }
            
            // Mode NSFW (accessible à TOUS)
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Préférences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Mode NSFW (18+)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                when {
                                    currentUser?.nsfwBlocked == true -> "🚫 Bloqué par l'administrateur"
                                    currentUser?.isAdult() == false -> "⚠️ Réservé aux 18+ ans"
                                    nsfwMode -> "Activé - Contenu adulte autorisé"
                                    else -> "Désactivé - Contenu approprié uniquement"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    currentUser?.nsfwBlocked == true -> MaterialTheme.colorScheme.error
                                    nsfwMode -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        Switch(
                            checked = nsfwMode,
                            onCheckedChange = { scope.launch { settingsViewModel.setNsfwMode(it) } },
                            enabled = currentUser?.canEnableNsfw() == true,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.error,
                                checkedTrackColor = MaterialTheme.colorScheme.errorContainer
                            )
                        )
                    }
                }
            }
            
            // === SECTION ADMIN SEULEMENT ===
            if (isAdmin) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "⚙️ Configuration Administrateur",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Bouton Gestion des Utilisateurs
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        ListItem(
                            headlineContent = { Text("👥 Gestion des Utilisateurs") },
                            supportingContent = { Text("Voir et gérer tous les comptes") },
                            leadingContent = {
                                Icon(Icons.Default.People, null)
                            },
                            trailingContent = {
                                Icon(Icons.Default.ChevronRight, null)
                            },
                            modifier = Modifier.clickable { onNavigateToAdminUsers() }
                        )
                    }
                }
                
                // Modèle IA Local (admin)
                item {
                    Text(
                        "Modèle IA Local",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Modèle actuel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        selectedModel?.name ?: "Aucun",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        when (modelState) {
                                            is ModelState.NotDownloaded -> "Non téléchargé"
                                            is ModelState.Downloading -> "Téléchargement..."
                                            is ModelState.Downloaded -> "Téléchargé"
                                            is ModelState.Loading -> "Chargement..."
                                            is ModelState.Loaded -> "Chargé et prêt"
                                            is ModelState.Error -> "Erreur"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when (modelState) {
                                            is ModelState.Loaded -> MaterialTheme.colorScheme.primary
                                            is ModelState.Error -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                
                                Icon(
                                    when (modelState) {
                                        is ModelState.Loaded -> Icons.Default.CheckCircle
                                        is ModelState.Error -> Icons.Default.Error
                                        is ModelState.Downloading, is ModelState.Loading -> Icons.Default.CloudDownload
                                        else -> Icons.Default.Cloud
                                    },
                                    contentDescription = null,
                                    tint = when (modelState) {
                                        is ModelState.Loaded -> MaterialTheme.colorScheme.primary
                                        is ModelState.Error -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
                
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showModelSelection = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ChangeCircle, "Changer")
                                Spacer(Modifier.width(16.dp))
                                Text("Changer de modèle", style = MaterialTheme.typography.bodyLarge)
                            }
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }
                
                if (modelState == ModelState.Downloaded || modelState == ModelState.Loaded) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDeleteConfirmation = true },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Delete,
                                        "Supprimer",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        "Supprimer le modèle",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                // Section Groq API (admin)
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "🚀 Groq API (Admin)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Groq API offre des réponses ultra-rapides (1-2s) et parfaitement cohérentes, gratuitement !",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Utiliser Groq API",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (useGroqApi) "Activé - Modèles locaux désactivés" else "Désactivé - Utilise modèles locaux",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (useGroqApi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = useGroqApi,
                                onCheckedChange = { scope.launch { settingsViewModel.setUseGroqApi(it) } }
                            )
                        }
                    }
                }
                
                if (useGroqApi) {
                    // Section Clés API Groq (Multi-clés partagées)
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "🔑 Clés API Groq Partagées",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "${sharedGroqKeys.size} clé(s) • Rotation automatique",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = { showAddKeyDialog = true },
                                        enabled = !isLoading
                                    ) {
                                        Icon(Icons.Default.Add, "Ajouter", modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Ajouter")
                                    }
                                }
                                
                                if (statusMessage != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        statusMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (statusMessage!!.startsWith("✅")) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        }
                                    )
                                }
                                
                                if (sharedGroqKeys.isEmpty()) {
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "Aucune clé configurée. Ajoutez une clé pour activer Groq API.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    TextButton(
                                        onClick = { /* Ouvrir navigateur */ }
                                    ) {
                                        Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Obtenir une clé gratuite", style = MaterialTheme.typography.bodySmall)
                                    }
                                } else {
                                    Spacer(Modifier.height(12.dp))
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        sharedGroqKeys.forEachIndexed { index, key ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            "Clé ${index + 1}",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(Modifier.height(2.dp))
                                                        Text(
                                                            "${key.take(20)}...",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { keyToDelete = key },
                                                        enabled = !isLoading
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            "Supprimer",
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "ℹ️ Ces clés sont partagées avec tous les utilisateurs et tournent automatiquement en cas de limite.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    // Modèle Groq
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showGroqModels = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Modèle Groq",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    val currentModel = GroqAIEngine.AVAILABLE_MODELS.find { it.id == groqModelId }
                                    Text(
                                        currentModel?.name ?: groqModelId,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        currentModel?.description ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, null)
                            }
                        }
                    }
                }
            }
            
            // Section À propos
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "À propos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "RolePlay AI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Version 1.4.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Application de roleplay IA avec mémoire et personnalisation",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
    
    // Dialog de sélection de modèle Groq
    if (showGroqModels && isAdmin) {
        AlertDialog(
            onDismissRequest = { showGroqModels = false },
            title = { Text("Sélectionner un modèle Groq") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(GroqAIEngine.AVAILABLE_MODELS) { model ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        settingsViewModel.setGroqModelId(model.id)
                                    }
                                    showGroqModels = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (model.id == groqModelId) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                model.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (model.recommended) {
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "⭐ Recommandé",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            model.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (model.id == groqModelId) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            "Sélectionné",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGroqModels = false }) {
                    Text("Fermer")
                }
            }
        )
    }
    
    // Dialog de sélection de modèle local (admin)
    if (showModelSelection && isAdmin) {
        AlertDialog(
            onDismissRequest = { showModelSelection = false },
            title = { Text("Changer de modèle") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableModels) { model ->
                        val isCompatible = model.requiredRam <= availableRam
                        val isDownloaded = viewModel.isModelDownloaded(model)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (isCompatible) Modifier.clickable {
                                    viewModel.selectModel(model)
                                    if (!isDownloaded) {
                                        viewModel.downloadSelectedModel()
                                    }
                                    showModelSelection = false
                                } else Modifier),
                            colors = CardDefaults.cardColors(
                                containerColor = if (model == selectedModel) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else if (!isCompatible) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            model.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "${formatBytes(model.size)} • RAM: ${model.requiredRam} MB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isCompatible) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.error
                                            }
                                        )
                                    }
                                    if (isDownloaded) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            "Téléchargé",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelSelection = false }) {
                    Text("Fermer")
                }
            }
        )
    }
    
    // Dialog de confirmation de suppression
    if (showDeleteConfirmation && isAdmin) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Supprimer le modèle ?") },
            text = {
                Text("Cette action supprimera le modèle téléchargé. Vous devrez le retélécharger pour l'utiliser à nouveau.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedModel?.let { viewModel.deleteModel(it) }
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Annuler")
                }
            }
        )
    }
    
    // Dialog pour ajouter une clé Groq
    if (showAddKeyDialog && isAdmin) {
        AlertDialog(
            onDismissRequest = { 
                showAddKeyDialog = false
                newApiKeyInput = ""
            },
            icon = { Icon(Icons.Default.Add, "Ajouter") },
            title = { Text("Ajouter une clé API Groq") },
            text = {
                Column {
                    Text(
                        "Cette clé sera partagée avec tous les utilisateurs et fera partie de la rotation automatique.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newApiKeyInput,
                        onValueChange = { newApiKeyInput = it },
                        label = { Text("Clé API") },
                        placeholder = { Text("gsk_...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { /* Ouvrir console.groq.com */ }
                    ) {
                        Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Obtenir une clé gratuite", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newApiKeyInput.isNotBlank()) {
                            settingsViewModel.addSharedGroqKey(newApiKeyInput)
                            showAddKeyDialog = false
                            newApiKeyInput = ""
                        }
                    },
                    enabled = newApiKeyInput.isNotBlank() && !isLoading
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showAddKeyDialog = false
                        newApiKeyInput = ""
                    }
                ) {
                    Text("Annuler")
                }
            }
        )
    }
    
    // Dialog de confirmation de suppression de clé
    if (keyToDelete != null && isAdmin) {
        AlertDialog(
            onDismissRequest = { keyToDelete = null },
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Supprimer cette clé ?") },
            text = {
                Column {
                    Text("Cette clé ne sera plus accessible aux utilisateurs.")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${keyToDelete!!.take(20)}...",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.removeSharedGroqKey(keyToDelete!!)
                        keyToDelete = null
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { keyToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
    
    // Dialog de sélection du moteur IA
    if (showAIEngineSelection) {
        AlertDialog(
            onDismissRequest = { showAIEngineSelection = false },
            title = { Text("Choisir le moteur IA") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AIOrchestrator.AIEngine.values().toList()) { engine ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        settingsViewModel.setSelectedAIEngine(engine.name)
                                    }
                                    showAIEngineSelection = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (engine.name == selectedAIEngine) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                engine.getDisplayName(),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            if (engine.isLocal()) {
                                                Text(
                                                    "📱 Local",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            } else {
                                                Text(
                                                    "☁️ Cloud",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            engine.getDescription(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (engine.name == selectedAIEngine) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            "Sélectionné",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAIEngineSelection = false }) {
                    Text("Fermer")
                }
            }
        )
    }
    
    // Dialog de sélection du modèle llama.cpp
    if (showLlamaCppModelSelection) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val modelsDir = File(context.getExternalFilesDir(null), "models")
        val availableModels = remember {
            if (modelsDir.exists()) {
                modelsDir.listFiles { file -> file.extension == "gguf" }?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        }
        
        AlertDialog(
            onDismissRequest = { showLlamaCppModelSelection = false },
            title = { Text("Sélectionner un modèle GGUF") },
            text = {
                Column {
                    if (availableModels.isEmpty()) {
                        Text(
                            "Aucun modèle trouvé dans:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            modelsDir.absolutePath,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Téléchargez un modèle .gguf (Phi-3, Gemma, TinyLlama) et placez-le dans ce dossier.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableModels) { modelFile ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                settingsViewModel.setLlamaCppModelPath(modelFile.absolutePath)
                                            }
                                            showLlamaCppModelSelection = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (modelFile.absolutePath == llamaCppModelPath) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                modelFile.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "${modelFile.length() / (1024 * 1024)} MB",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (modelFile.absolutePath == llamaCppModelPath) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                "Sélectionné",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLlamaCppModelSelection = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
    }
}
