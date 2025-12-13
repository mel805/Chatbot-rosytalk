package com.roleplayai.chatbot.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roleplayai.chatbot.ui.viewmodel.ModelViewModel
import androidx.compose.foundation.layout.Box

sealed class NavigationItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Explorer : NavigationItem("explorer", Icons.Default.Explore, "Explorer")
    object Chats : NavigationItem("chats", Icons.Default.Chat, "Conversations")
    object Settings : NavigationItem("settings", Icons.Default.Settings, "Paramètres")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onCharacterClick: (String) -> Unit,
    onCharacterProfileClick: (String) -> Unit,
    onChatClick: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAdminUsers: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val modelViewModel: ModelViewModel = viewModel()
    var selectedTab by remember { mutableStateOf(NavigationItem.Explorer.route) }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                val items = listOf(
                    NavigationItem.Explorer,
                    NavigationItem.Chats,
                    NavigationItem.Settings
                )
                
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedTab == item.route,
                        onClick = { selectedTab = item.route },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                NavigationItem.Explorer.route -> {
                    ExplorerScreen(
                        onCharacterClick = onCharacterProfileClick // Aller vers le profil au lieu du chat
                    )
                }
                NavigationItem.Chats.route -> {
                    ChatsScreen(
                        onChatClick = { chatId, characterId ->
                            // Naviguer vers le chat existant
                            onChatClick(characterId)
                        },
                        onCharacterProfileClick = onCharacterProfileClick,
                        onNewConversation = { characterId ->
                            // Créer une nouvelle conversation
                            onCharacterClick(characterId)
                        }
                    )
                }
                NavigationItem.Settings.route -> {
                    SettingsScreen(
                        viewModel = modelViewModel,
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToAdminUsers = onNavigateToAdminUsers,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}
