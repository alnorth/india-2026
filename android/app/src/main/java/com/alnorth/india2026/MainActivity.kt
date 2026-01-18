package com.alnorth.india2026

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alnorth.india2026.model.SubmissionResult
import com.alnorth.india2026.ui.screens.CrashScreen
import com.alnorth.india2026.ui.screens.DayListScreen
import com.alnorth.india2026.ui.screens.EditDayScreen
import com.alnorth.india2026.ui.screens.ResultScreen
import com.alnorth.india2026.ui.theme.India2026Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Install custom crash handler
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(applicationContext))

        enableEdgeToEdge()
        setContent {
            India2026Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Temporary minimal UI for testing
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.foundation.layout.Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                        ) {
                            androidx.compose.material3.Text(
                                "India 2026 App",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            androidx.compose.material3.Text(
                                "Minimal test version",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            androidx.compose.material3.Text(
                                "If you see this, the app is working!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainContent() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("crash_data", Context.MODE_PRIVATE) }
    var crashMessage by remember { mutableStateOf(prefs.getString("crash_message", null)) }

    if (crashMessage != null) {
        CrashScreen(
            crashMessage = crashMessage!!,
            onDismiss = {
                prefs.edit().clear().apply()
                crashMessage = null
            }
        )
    } else {
        India2026App()
    }
}

@Composable
fun India2026App() {
    val navController = rememberNavController()
    var submissionResult: SubmissionResult? = null

    NavHost(
        navController = navController,
        startDestination = "day_list"
    ) {
        // Day List Screen
        composable("day_list") {
            DayListScreen(
                onDaySelected = { slug ->
                    navController.navigate("edit_day/$slug")
                }
            )
        }

        // Edit Day Screen
        composable(
            route = "edit_day/{slug}",
            arguments = listOf(
                navArgument("slug") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("slug") ?: return@composable
            EditDayScreen(
                slug = slug,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToResult = { result ->
                    submissionResult = result
                    navController.navigate("result") {
                        popUpTo("day_list") { inclusive = false }
                    }
                }
            )
        }

        // Result Screen
        composable("result") {
            submissionResult?.let { result ->
                ResultScreen(
                    result = result,
                    onCreateAnother = {
                        navController.navigate("day_list") {
                            popUpTo("day_list") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
