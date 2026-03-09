package com.kyant.backdrop.catalog.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kyant.backdrop.catalog.NotificationDeepLink
import com.kyant.backdrop.catalog.data.OnboardingPreferences

sealed class Route(val path: String) {
    data object Onboarding : Route("onboarding")
    data object ProfileSetup : Route("profile_setup")
    data object Home : Route("home")
}

@Composable
fun AppRoot(
    initialDeepLink: NotificationDeepLink? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    
    // Track deep link state
    var pendingDeepLink by remember { mutableStateOf(initialDeepLink) }
    
    // Update pending deep link when new one arrives
    LaunchedEffect(initialDeepLink) {
        if (initialDeepLink != null) {
            pendingDeepLink = initialDeepLink
        }
    }
    
    // Load preference from DataStore for initial route
    val hasSeenOnboarding by OnboardingPreferences.hasSeenOnboarding(context)
        .collectAsState(initial = false)
    
    // Determine start destination based on onboarding state
    val startDestination = if (hasSeenOnboarding) Route.Home.path else Route.Onboarding.path
    
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        // Onboarding screen (welcome slides)
        composable(Route.Onboarding.path) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Route.ProfileSetup.path) {
                        popUpTo(Route.Onboarding.path) { inclusive = true }
                    }
                }
            )
        }
        
        // Profile setup wizard (college, goals, interests)
        composable(Route.ProfileSetup.path) {
            ProfileSetupWizard(
                onComplete = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.ProfileSetup.path) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.ProfileSetup.path) { inclusive = true }
                    }
                }
            )
        }
        
        // Home screen
        composable(Route.Home.path) {
            HomeScreen(
                deepLink = pendingDeepLink,
                onDeepLinkConsumed = {
                    pendingDeepLink = null
                    onDeepLinkConsumed()
                }
            )
        }
    }
}
