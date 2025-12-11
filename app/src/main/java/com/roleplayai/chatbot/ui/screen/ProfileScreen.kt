package com.roleplayai.chatbot.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roleplayai.chatbot.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit
) {
    val authViewModel: AuthViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Charger les données de l'utilisateur
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            displayName = user.displayName
            username = user.username
            bio = user.bio
            age = user.age
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mon Profil") },
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar avec initiales
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = username.firstOrNull()?.uppercase() 
                            ?: displayName.firstOrNull()?.uppercase() 
                            ?: "?",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Email (non modifiable)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Email, "Email", tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(
                            "Email",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            currentUser?.email ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Messages de succès/erreur
            successMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "✅ $message",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "❌ $error",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Formulaire de profil
            Text(
                "Informations du profil",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = displayName,
                onValueChange = { 
                    displayName = it
                    successMessage = null
                    errorMessage = null
                },
                label = { Text("Nom complet") },
                leadingIcon = { Icon(Icons.Default.Person, "Nom") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )
            
            OutlinedTextField(
                value = username,
                onValueChange = { 
                    username = it
                    successMessage = null
                    errorMessage = null
                },
                label = { Text("Pseudo (utilisé dans les conversations)") },
                leadingIcon = { Icon(Icons.Default.Person, "Pseudo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                supportingText = { 
                    Text("Les personnages vous appelleront par ce nom") 
                }
            )
            
            OutlinedTextField(
                value = age,
                onValueChange = { 
                    age = it
                    successMessage = null
                    errorMessage = null
                },
                label = { Text("Âge (optionnel)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                placeholder = { Text("Ex: 25 ans") }
            )
            
            OutlinedTextField(
                value = bio,
                onValueChange = { 
                    bio = it
                    successMessage = null
                    errorMessage = null
                },
                label = { Text("Bio / Description (optionnel)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                enabled = !isLoading,
                placeholder = { Text("Parlez un peu de vous...") }
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Bouton de sauvegarde
            Button(
                onClick = {
                    scope.launch {
                        if (username.isBlank()) {
                            errorMessage = "Le pseudo ne peut pas être vide"
                            return@launch
                        }
                        
                        isLoading = true
                        successMessage = null
                        errorMessage = null
                        
                        val result = authViewModel.updateUserProfile(
                            displayName = displayName.trim(),
                            username = username.trim(),
                            bio = bio.trim(),
                            age = age.trim()
                        )
                        
                        result.fold(
                            onSuccess = {
                                isLoading = false
                                successMessage = "Profil mis à jour avec succès !"
                            },
                            onFailure = { error ->
                                isLoading = false
                                errorMessage = error.message ?: "Erreur lors de la mise à jour"
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading && username.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Save, "Sauvegarder")
                        Text(
                            text = "Enregistrer les modifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Info sur l'utilisation du pseudo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "💡 Comment ça marche ?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "• Votre pseudo sera utilisé dans les conversations\n" +
                               "• Les personnages vous appelleront par ce nom\n" +
                               "• Vos informations de profil rendent les conversations plus immersives\n" +
                               "• Toutes vos données sont stockées localement",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
