package com.roleplayai.chatbot.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun ModelSelectionScreen(
    viewModel: ModelViewModel,
    onModelReady: () -> Unit
) {
    val models by viewModel.availableModels.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val modelState by viewModel.modelState.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val availableStorage by viewModel.availableStorage.collectAsState()
    val availableRam by viewModel.availableRam.collectAsState()
    
    LaunchedEffect(modelState) {
        if (modelState == ModelState.Loaded) {
            onModelReady()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuration du Modèle IA", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // System info card
            SystemInfoCard(
                availableStorage = availableStorage,
                availableRam = availableRam
            )
            
            // Model selection
            Text(
                "Choisissez un modèle IA",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(models) { model ->
                    ModelCard(
                        model = model,
                        isSelected = model == selectedModel,
                        onSelect = { viewModel.selectModel(model) },
                        availableRam = availableRam
                    )
                }
            }
            
            // Download/Load button section
            when (modelState) {
                is ModelState.NotDownloaded -> {
                    Button(
                        onClick = { viewModel.downloadSelectedModel() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedModel != null
                    ) {
                        Icon(Icons.Default.Download, "Download")
                        Spacer(Modifier.width(8.dp))
                        Text("Télécharger le Modèle")
                    }
                }
                is ModelState.Downloading -> {
                    val progress = (modelState as ModelState.Downloading).progress
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = progress / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Téléchargement... ${progress.toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        downloadProgress?.let { prog ->
                            Text(
                                "${formatBytes(prog.bytesDownloaded)} / ${formatBytes(prog.totalBytes)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Vitesse: ${formatBytes(prog.speedBytesPerSecond)}/s",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                is ModelState.Downloaded -> {
                    Button(
                        onClick = { viewModel.loadModel() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, "Load")
                        Spacer(Modifier.width(8.dp))
                        Text("Charger le Modèle")
                    }
                }
                is ModelState.Loading -> {
                    val progress = (modelState as ModelState.Loading).progress
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = progress / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Chargement du modèle... ${progress.toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is ModelState.Loaded -> {
                    Button(
                        onClick = onModelReady,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, "Ready")
                        Spacer(Modifier.width(8.dp))
                        Text("Commencer à Utiliser")
                    }
                }
                is ModelState.Error -> {
                    val error = (modelState as ModelState.Error).message
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Erreur: $error",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.downloadSelectedModel() }
                            ) {
                                Text("Réessayer")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemInfoCard(
    availableStorage: Long,
    availableRam: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Informations Système",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Stockage disponible:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        formatBytes(availableStorage),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text("RAM disponible:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${availableRam} MB",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ModelCard(
    model: ModelConfig,
    isSelected: Boolean,
    onSelect: () -> Unit,
    availableRam: Long
) {
    val isCompatible = model.requiredRam <= availableRam
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isCompatible) Modifier.clickable(onClick = onSelect) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else if (!isCompatible) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    model.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (model.recommended) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Recommandé", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            
            Spacer(Modifier.height(4.dp))
            
            Text(
                model.description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Taille: ${formatBytes(model.size)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "RAM: ${model.requiredRam} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCompatible) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            
            if (!isCompatible) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "⚠️ RAM insuffisante pour ce modèle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
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
