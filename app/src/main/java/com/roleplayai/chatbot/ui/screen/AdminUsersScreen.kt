package com.roleplayai.chatbot.ui.screen

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roleplayai.chatbot.data.model.User
import com.roleplayai.chatbot.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Écran de gestion des utilisateurs (ADMIN UNIQUEMENT)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    onBack: () -> Unit,
    adminViewModel: AdminViewModel = viewModel()
) {
    val users by adminViewModel.allUsers.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()
    val errorMessage by adminViewModel.errorMessage.collectAsState()
    val successMessage by adminViewModel.successMessage.collectAsState()
    
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        adminViewModel.loadAllUsers()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👥 Gestion des Utilisateurs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages
            errorMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Error, null)
                        Text(msg, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            
            successMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Text(msg, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            
            // En-tête
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "👑 Panneau Administrateur",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${users.size} utilisateur(s) enregistré(s)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Liste des utilisateurs
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(users) { user ->
                        UserCard(
                            user = user,
                            onEdit = {
                                selectedUser = user
                                showEditDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Dialog d'édition
    if (showEditDialog && selectedUser != null) {
        EditUserDialog(
            user = selectedUser!!,
            onDismiss = {
                showEditDialog = false
                selectedUser = null
                adminViewModel.clearMessages()
            },
            onSave = { nsfwEnabled, isAdmin ->
                adminViewModel.updateUser(
                    selectedUser!!.email,
                    nsfwEnabled,
                    isAdmin
                )
                showEditDialog = false
                selectedUser = null
            }
        )
    }
}

@Composable
private fun UserCard(
    user: User,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (user.isAdmin) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // En-tête
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (user.isAdmin) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Admin",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = user.pseudo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Modifier")
                }
            }
            
            Divider()
            
            // Infos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(
                    icon = Icons.Default.Cake,
                    label = "${user.age} ans"
                )
                InfoChip(
                    icon = Icons.Default.Wc,
                    label = when (user.gender) {
                        "male" -> "Homme"
                        "female" -> "Femme"
                        else -> "Autre"
                    }
                )
                if (user.isNsfwEnabled) {
                    InfoChip(
                        icon = Icons.Default.Warning,
                        label = "NSFW",
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                }
                if (user.isAdmin) {
                    InfoChip(
                        icon = Icons.Default.Star,
                        label = "Admin",
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
            
            Text(
                text = "Créé le ${SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(Date(user.createdAt))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun EditUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (nsfwEnabled: Boolean, isAdmin: Boolean) -> Unit
) {
    var nsfwEnabled by remember { mutableStateOf(user.isNsfwEnabled) }
    var isAdmin by remember { mutableStateOf(user.isAdmin) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, null) },
        title = { Text("Modifier ${user.pseudo}") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Divider()
                
                // NSFW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🔞 Mode NSFW", fontWeight = FontWeight.Bold)
                        if (!user.isAdult()) {
                            Text(
                                text = "⚠️ Mineur (${user.age} ans)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Switch(
                        checked = nsfwEnabled,
                        onCheckedChange = { nsfwEnabled = it },
                        enabled = user.isAdult()
                    )
                }
                
                // Admin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("👑 Administrateur", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Accès gestion utilisateurs",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = isAdmin,
                        onCheckedChange = { isAdmin = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(nsfwEnabled, isAdmin) }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
