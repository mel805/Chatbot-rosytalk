package com.roleplayai.chatbot.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roleplayai.chatbot.data.model.ModelConfig
import com.roleplayai.chatbot.data.model.ModelState
import com.roleplayai.chatbot.ui.viewmodel.ModelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ModelViewModel,
    onBack: () -> Unit
) {
    val selectedModel by viewModel.selectedModel.collectAsState()
    val modelState by viewModel.modelState.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val availableRam by viewModel.availableRam.collectAsState()
    
    var showModelSelection by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
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
    
    // Dialog de sélection de modèle
    if (showModelSelection) {
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
                                        // Lancer le téléchargement
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
    if (showDeleteConfirmation) {
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
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
    }
}
