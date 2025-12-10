package com.roleplayai.chatbot.ui.screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roleplayai.chatbot.data.ai.GroqAIEngine
import com.roleplayai.chatbot.data.model.ModelConfig
import com.roleplayai.chatbot.data.model.ModelState
import com.roleplayai.chatbot.ui.viewmodel.AuthViewModel
import com.roleplayai.chatbot.ui.viewmodel.ModelViewModel
import com.roleplayai.chatbot.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ModelViewModel,
    onBack: () -> Unit
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
    
    var showModelSelection by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showGroqModels by remember { mutableStateOf(false) }
    var showApiKey by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(groqApiKey) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 🔐 Section Compte (nouvelle)
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
                            Column {
                                Text(
                                    text = if (isAdmin) "👑 Compte Admin" else "Compte",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                currentUser?.let { user ->
                                    Text(
                                        text = user.displayName ?: "Utilisateur",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = user.email ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Button(
                                onClick = {
                                    scope.launch {
                                        authViewModel.signOut()
                                        onBack()
                                    }
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
            
            // Section Modèle IA
            item {
                Text(
                    "Modèle IA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Modèle actuellement sélectionné
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
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
                                        is ModelState.Downloading -> "Téléchargement en cours..."
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
            
            // Actions
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                label = { Text("gsk_...") },
                                placeholder = { Text("Collez votre clé API ici") },
                                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    Row {
                                        IconButton(onClick = { showApiKey = !showApiKey }) {
                                            Icon(
                                                if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                "Afficher/Masquer"
                                            )
                                        }
                                        if (apiKeyInput != groqApiKey) {
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        settingsViewModel.setGroqApiKey(apiKeyInput)
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.Check, "Sauvegarder")
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(Modifier.height(4.dp))
                            TextButton(
                                onClick = { /* TODO: Ouvrir navigateur vers console.groq.com */ }
                            ) {
                                Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Obtenir une clé gratuite sur console.groq.com", style = MaterialTheme.typography.bodySmall)
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
                
                // Mode NSFW
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
                                    if (nsfwMode) "Activé - Contenu adulte autorisé" else "Désactivé - Contenu approprié uniquement",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (nsfwMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = nsfwMode,
                                onCheckedChange = { scope.launch { settingsViewModel.setNsfwMode(it) } },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.error,
                                    checkedTrackColor = MaterialTheme.colorScheme.errorContainer
                                )
                            )
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
                            "Version 1.1.0-beta",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Application de roleplay IA avec modèles locaux",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
    
    // Dialog de sélection de modèle Groq
    if (showGroqModels) {
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
                                        Spacer(Modifier.height(4.dp))
                                        Row {
                                            Text(
                                                "Contexte: ${model.contextLength / 1024}K",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                if (model.nsfwCapable) "NSFW ✓" else "SFW uniquement",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (model.nsfwCapable) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
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
