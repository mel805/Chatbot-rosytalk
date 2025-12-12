package com.roleplayai.chatbot.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.roleplayai.chatbot.ui.screen.*
import com.roleplayai.chatbot.ui.viewmodel.AuthViewModel
import com.roleplayai.chatbot.ui.viewmodel.CharacterViewModel
import com.roleplayai.chatbot.ui.viewmodel.ChatViewModel
import com.roleplayai.chatbot.ui.viewmodel.ModelViewModel
import com.roleplayai.chatbot.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Main : Screen("main")
    object CharacterList : Screen("character_list")
    object Settings : Screen("settings")
    object Profile : Screen("user_profile")
    object AdminUsers : Screen("admin_users")
    object Chat : Screen("chat/{characterId}") {
        fun createRoute(characterId: String) = "chat/$characterId"
    }
    object CharacterProfile : Screen("profile/{characterId}") {
        fun createRoute(characterId: String) = "profile/$characterId"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    characterViewModel: CharacterViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onLoadingComplete = {
                    // Navigation gérée dans le composable avec LaunchedEffect
                }
            )
            
            // 🔐 Vérifier l'authentification et premier lancement
            LaunchedEffect(Unit) {
                delay(2000) // Attendre la fin du splash
                
                // Vérifier si utilisateur connecté
                if (currentUser == null) {
                    // Pas connecté → Écran de connexion
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                } else {
                    // ✅ Plus de téléchargement obligatoire au premier lancement :
                    // l'utilisateur peut choisir/télécharger un GGUF depuis les Paramètres (llama.cpp).
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }
        }
        
        composable(Screen.Login.route) {
            AuthScreen(
                onAuthSuccess = {
                    // Après connexion, aller directement à l'écran principal.
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                viewModel = authViewModel
            )
        }
        
        composable(Screen.Main.route) {
            MainScreen(
                onCharacterClick = { characterId ->
                    navController.navigate(Screen.Chat.createRoute(characterId))
                },
                onCharacterProfileClick = { characterId ->
                    navController.navigate(Screen.CharacterProfile.createRoute(characterId))
                },
                onChatClick = { characterId ->
                    // Naviguer vers le chat du personnage
                    navController.navigate(Screen.Chat.createRoute(characterId))
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToAdminUsers = {
                    navController.navigate(Screen.AdminUsers.route)
                },
                onLogout = {
                    // Déconnexion : retour à l'écran de connexion
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Garder CharacterList pour compatibilité (optionnel)
        composable(Screen.CharacterList.route) {
            CharacterListScreen(
                viewModel = characterViewModel,
                onCharacterSelected = { characterId ->
                    navController.navigate(Screen.Chat.createRoute(characterId))
                },
                onCharacterProfileClick = { characterId ->
                    navController.navigate(Screen.CharacterProfile.createRoute(characterId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Main.route)
                }
            )
        }
        
        composable(Screen.CharacterProfile.route) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getString("characterId") ?: return@composable
            val character = characterViewModel.getCharacterById(characterId) ?: return@composable
            val isNsfwMode by settingsViewModel.nsfwMode.collectAsState()
            
            // Vérifier s'il y a une conversation existante
            val hasExistingChat = chatViewModel.hasExistingChat(characterId)
            
            CharacterProfileScreen(
                character = character,
                onBack = { navController.popBackStack() },
                onStartNewChat = {
                    // Créer une NOUVELLE conversation (supprime l'ancienne)
                    chatViewModel.createNewChat(characterId)
                    navController.navigate(Screen.Chat.createRoute(characterId)) {
                        popUpTo(Screen.Main.route)
                    }
                },
                onContinueChat = if (hasExistingChat) {
                    {
                        // Continuer la conversation existante
                        val existingChat = chatViewModel.getExistingChat(characterId)
                        if (existingChat != null) {
                            chatViewModel.selectChat(existingChat.id)
                            navController.navigate(Screen.Chat.createRoute(characterId)) {
                                popUpTo(Screen.Main.route)
                            }
                        }
                    }
                } else null,
                hasExistingChat = hasExistingChat,
                isNsfwMode = isNsfwMode
            )
        }
        
        composable(Screen.Profile.route) {
            UserProfileScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.AdminUsers.route) {
            AdminUsersScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Chat.route) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getString("characterId") ?: return@composable
            
            ChatScreen(
                viewModel = chatViewModel,
                characterId = characterId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
