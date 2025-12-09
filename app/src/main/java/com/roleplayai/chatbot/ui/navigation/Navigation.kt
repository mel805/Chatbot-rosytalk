package com.roleplayai.chatbot.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.roleplayai.chatbot.ui.screen.CharacterListScreen
import com.roleplayai.chatbot.ui.screen.ChatScreen
import com.roleplayai.chatbot.ui.screen.SplashScreen
import com.roleplayai.chatbot.ui.viewmodel.CharacterViewModel
import com.roleplayai.chatbot.ui.viewmodel.ChatViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object CharacterList : Screen("character_list")
    object Chat : Screen("chat/{characterId}") {
        fun createRoute(characterId: String) = "chat/$characterId"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    characterViewModel: CharacterViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onLoadingComplete = {
                    navController.navigate(Screen.CharacterList.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.CharacterList.route) {
            CharacterListScreen(
                viewModel = characterViewModel,
                onCharacterSelected = { characterId ->
                    navController.navigate(Screen.Chat.createRoute(characterId))
                }
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
