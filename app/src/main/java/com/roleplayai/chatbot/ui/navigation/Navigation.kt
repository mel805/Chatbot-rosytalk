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
import com.roleplayai.chatbot.ui.screen.CharacterListScreen
import com.roleplayai.chatbot.ui.screen.CharacterProfileScreen
import com.roleplayai.chatbot.ui.screen.ChatScreen
import com.roleplayai.chatbot.ui.screen.LoginScreen
import com.roleplayai.chatbot.ui.screen.ModelSelectionScreen
import com.roleplayai.chatbot.ui.screen.ProfileScreen
import com.roleplayai.chatbot.ui.screen.SettingsScreen
import com.roleplayai.chatbot.ui.screen.SplashScreen
import com.roleplayai.chatbot.ui.viewmodel.AuthViewModel
import com.roleplayai.chatbot.ui.viewmodel.CharacterViewModel
import com.roleplayai.chatbot.ui.viewmodel.ChatViewModel
import com.roleplayai.chatbot.ui.viewmodel.ModelViewModel
import kotlinx.coroutines.delay

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object ModelSelection : Screen("model_selection")
    object CharacterList : Screen("character_list")
    object Settings : Screen("settings")
    object Profile : Screen("user_profile")
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
    modelViewModel: ModelViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
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
                    // Connecté → Vérifier modèle
                    val isFirstLaunch = modelViewModel.isFirstLaunch()
                    val isModelDownloaded = modelViewModel.isModelDownloaded()
                    
                    if (isFirstLaunch || !isModelDownloaded) {
                        // Premier lancement ou modèle pas téléchargé : aller vers sélection
                        navController.navigate(Screen.ModelSelection.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        // Lancement normal : aller directement vers la liste
                        navController.navigate(Screen.CharacterList.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            }
        }
        
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // Après connexion, naviguer vers sélection de modèle ou liste
                    navController.navigate(Screen.ModelSelection.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.ModelSelection.route) {
            ModelSelectionScreen(
                viewModel = modelViewModel,
                onModelReady = {
                    navController.navigate(Screen.CharacterList.route) {
                        popUpTo(Screen.ModelSelection.route) { inclusive = true }
                    }
                }
            )
            
            // Marquer le premier lancement comme complété quand le modèle est prêt
            LaunchedEffect(modelViewModel.modelState.collectAsState().value) {
                if (modelViewModel.modelState.value == com.roleplayai.chatbot.data.model.ModelState.Loaded) {
                    modelViewModel.setFirstLaunchCompleted()
                }
            }
        }
        
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
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(Screen.CharacterProfile.route) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getString("characterId") ?: return@composable
            val character = characterViewModel.getCharacterById(characterId) ?: return@composable
            
            CharacterProfileScreen(
                character = character,
                onBack = { navController.popBackStack() },
                onStartChat = {
                    navController.navigate(Screen.Chat.createRoute(characterId)) {
                        popUpTo(Screen.CharacterList.route)
                    }
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = modelViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Chat.route) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getString("characterId") ?: return@composable
            
            // Initialiser le moteur local avec le modèle téléchargé
            LaunchedEffect(Unit) {
                val modelPath = modelViewModel.getModelPath()
                if (modelPath != null) {
                    // Modèle trouvé, l'initialiser
                    chatViewModel.initializeLocalAI(modelPath)
                } else {
                    // Pas de modèle, essayer de charger le modèle sélectionné
                    val selectedModel = modelViewModel.selectedModel.value
                    if (selectedModel != null) {
                        val path = modelViewModel.modelDownloader.getModelPath(selectedModel)
                        if (path != null) {
                            chatViewModel.initializeLocalAI(path)
                        }
                    }
                }
            }
            
            ChatScreen(
                viewModel = chatViewModel,
                characterId = characterId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
