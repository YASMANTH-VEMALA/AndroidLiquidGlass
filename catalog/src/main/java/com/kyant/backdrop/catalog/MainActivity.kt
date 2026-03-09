package com.kyant.backdrop.catalog

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.kyant.backdrop.catalog.notifications.VormexMessagingService
import com.kyant.backdrop.catalog.onboarding.AppRoot

/**
 * Deep link navigation state from push notifications
 */
data class NotificationDeepLink(
    val action: String,
    val userId: String? = null,
    val connectionId: String? = null
)

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    // Deep link state that can be consumed by composables
    var pendingDeepLink by mutableStateOf<NotificationDeepLink?>(null)
        private set
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
            initializeFirebaseMessaging()
        } else {
            Log.d(TAG, "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen with animated exit
        val splashScreen = installSplashScreen()
        
        // Add exit animation
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            // Fade out animation
            splashScreenView.view.animate()
                .alpha(0f)
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(300)
                .withEndAction {
                    splashScreenView.remove()
                }
                .start()
        }
        
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Request notification permission (Android 13+)
        requestNotificationPermission()
        
        // Handle initial intent (e.g., app launched from notification)
        handleIntent(intent)

        setContent {
            val isLightTheme = !isSystemInDarkTheme()

            CompositionLocalProvider(
                LocalIndication provides ripple(color = if (isLightTheme) Color.Black else Color.White)
            ) {
                AppRoot(
                    initialDeepLink = pendingDeepLink,
                    onDeepLinkConsumed = { pendingDeepLink = null }
                )
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        intent ?: return
        
        val action = intent.getStringExtra(VormexMessagingService.EXTRA_ACTION)
        if (action != null) {
            val userId = intent.getStringExtra(VormexMessagingService.EXTRA_USER_ID)
            val connectionId = intent.getStringExtra(VormexMessagingService.EXTRA_CONNECTION_ID)
            
            Log.d(TAG, "Handling deep link: action=$action, userId=$userId, connectionId=$connectionId")
            
            pendingDeepLink = NotificationDeepLink(
                action = action,
                userId = userId,
                connectionId = connectionId
            )
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    initializeFirebaseMessaging()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // TODO: Show UI explaining why notifications are important
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // For Android 12 and below, no runtime permission needed
            initializeFirebaseMessaging()
        }
    }
    
    private fun initializeFirebaseMessaging() {
        // TODO: Initialize Firebase Messaging when google-services.json is added
        // FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        //     if (!task.isSuccessful) {
        //         Log.w(TAG, "Fetching FCM token failed", task.exception)
        //         return@addOnCompleteListener
        //     }
        //     val token = task.result
        //     Log.d(TAG, "FCM Token: $token")
        // }
        Log.d(TAG, "Firebase Messaging initialization skipped (google-services.json not configured)")
    }
}
