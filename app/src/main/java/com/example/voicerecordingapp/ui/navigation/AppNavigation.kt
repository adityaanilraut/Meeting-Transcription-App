package com.example.voicerecordingapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.voicerecordingapp.data.repository.ApiKeyRepository
import com.example.voicerecordingapp.ui.screens.ApiKeySetupScreen
import com.example.voicerecordingapp.ui.screens.DashboardScreen
import com.example.voicerecordingapp.ui.screens.PlaybackScreen
import com.example.voicerecordingapp.ui.screens.RecordingScreen
import com.example.voicerecordingapp.ui.screens.SummaryScreen

@Composable
fun AppNavigation(
    apiKeyRepository: ApiKeyRepository,
    navController: NavHostController = rememberNavController()
) {
    val hasApiKey by apiKeyRepository.hasApiKey.collectAsState(initial = false)
    
    // Navigate to appropriate screen based on API key status
    LaunchedEffect(hasApiKey) {
        if (hasApiKey && navController.currentDestination?.route == Screen.ApiKeySetup.route) {
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.ApiKeySetup.route) { inclusive = true }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = if (hasApiKey) Screen.Dashboard.route else Screen.ApiKeySetup.route
    ) {
        composable(Screen.ApiKeySetup.route) {
            ApiKeySetupScreen(
                onApiKeySaved = {
                    // Navigation will be handled by LaunchedEffect above
                }
            )
        }
        
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToRecording = {
                    navController.navigate(Screen.Recording.route)
                },
                onNavigateToSummary = { meetingId ->
                    navController.navigate(Screen.Summary.createRoute(meetingId))
                },
                onNavigateToPlayback = { meetingId ->
                    navController.navigate(Screen.Playback.createRoute(meetingId))
                }
            )
        }
        
        composable(Screen.Recording.route) {
            RecordingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.Summary.route,
            arguments = listOf(
                navArgument("meetingId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            SummaryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.Playback.route,
            arguments = listOf(
                navArgument("meetingId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getLong("meetingId") ?: -1L
            PlaybackScreen(
                meetingId = meetingId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object ApiKeySetup : Screen("api_key_setup")
    object Dashboard : Screen("dashboard")
    object Recording : Screen("recording")
    object Summary : Screen("summary/{meetingId}") {
        fun createRoute(meetingId: Long) = "summary/$meetingId"
    }
    object Playback : Screen("playback/{meetingId}") {
        fun createRoute(meetingId: Long) = "playback/$meetingId"
    }
}
