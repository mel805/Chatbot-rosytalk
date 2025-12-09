package com.roleplayai.chatbot.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.roleplayai.chatbot.ui.screen.CharacterListScreen
import com.roleplayai.chatbot.ui.screen.ChatScreen
import com.roleplayai.chatbot.ui.screen.ModelSelectionScreen
import com.roleplayai.chatbot.ui.screen.SettingsScreen
import com.roleplayai.chatbot.ui.screen.SplashScreen
import com.roleplayai.chatbot.ui.viewmodel.CharacterViewModel
import com.roleplayai.chatbot.ui.viewmodel.ChatViewModel
import com.roleplayai.chatbot.ui.viewmodel.ModelViewModel
import kotlinx.coroutines.delay

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object ModelSelection : Screen("model_selection")
    object CharacterList : Screen("character_list")
    object Settings : Screen("settings")
    object Chat : Screen("chat/{characterId}") {
        fun createRoute(characterId: String) = "chat/$characterId"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    characterViewModel: CharacterViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    modelViewModel: ModelViewModel = viewModel()
) {
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
            
            // Vérifier si c'est le premier lancement
            LaunchedEffect(Unit) {
                delay(2000) // Attendre la fin du splash
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
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = modelViewModel,
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
