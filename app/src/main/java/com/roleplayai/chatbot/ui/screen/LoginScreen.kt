package com.roleplayai.chatbot.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roleplayai.chatbot.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val authViewModel: AuthViewModel = viewModel()
    val scope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo et titre
            Icon(
                Icons.Default.Login,
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(32.dp))
            
            Text(
                text = "RolePlay AI",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Conversations Immersives avec IA",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(Modifier.height(64.dp))
            
            // Message d'erreur
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Champ Email
            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    errorMessage = null
                },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, "Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !isLoading
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Champ Nom (optionnel)
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Nom complet (optionnel)") },
                leadingIcon = { Icon(Icons.Default.Person, "Nom") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Champ Pseudo
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Pseudo (pour les conversations)") },
                leadingIcon = { Icon(Icons.Default.Person, "Pseudo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                placeholder = { Text("Comment voulez-vous être appelé ?") }
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Bouton Connexion
            Button(
                onClick = {
                    scope.launch {
                        if (email.isBlank()) {
                            errorMessage = "Veuillez entrer votre email"
                            return@launch
                        }
                        
                        isLoading = true
                        errorMessage = null
                        
                        val result = authViewModel.signIn(
                            email = email.trim(),
                            displayName = displayName.trim(),
                            username = username.trim()
                        )
                        
                        result.fold(
                            onSuccess = {
                                isLoading = false
                                onLoginSuccess()
                            },
                            onFailure = { error ->
                                isLoading = false
                                errorMessage = error.message ?: "Erreur de connexion"
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
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
                        Icon(Icons.Default.Login, "Connexion")
                        Text(
                            text = "Se connecter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ℹ️ Informations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "• Connexion simple avec votre email\n" +
                               "• Choisissez votre pseudo pour les conversations\n" +
                               "• Vos conversations sont sauvegardées\n" +
                               "• Les personnages vous appelleront par votre pseudo\n" +
                               "• Vous pouvez activer le mode NSFW\n" +
                               "• Conversations privées et sécurisées\n" +
                               "• Aucune donnée envoyée sur Internet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(Modifier.weight(1f))
            
            // Note admin
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Text(
                    text = "👑 Admin : douvdouv21@gmail.com a le contrôle total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
