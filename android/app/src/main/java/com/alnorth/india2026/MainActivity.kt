package com.alnorth.india2026

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alnorth.india2026.api.ApiClient
import com.alnorth.india2026.model.SubmissionResult
import com.alnorth.india2026.repository.GitHubRepository
import com.alnorth.india2026.repository.UpdateInfo
import com.alnorth.india2026.ui.composables.BranchFooter
import com.alnorth.india2026.ui.screens.CrashScreen
import com.alnorth.india2026.ui.screens.DayListScreen
import com.alnorth.india2026.ui.screens.EditDayScreen
import com.alnorth.india2026.ui.screens.PullRequestListScreen
import com.alnorth.india2026.ui.screens.ResultScreen
import com.alnorth.india2026.ui.screens.ShareTargetScreen
import com.alnorth.india2026.ui.theme.India2026Theme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Install custom crash handler
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(applicationContext))

        // Extract shared image URIs from intent
        val sharedImageUris = extractSharedImages(intent)

        enableEdgeToEdge()
        setContent {
            India2026Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent(sharedImageUris = sharedImageUris)
                }
            }
        }
    }

    private fun extractSharedImages(intent: Intent): List<Uri> {
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM)?.let { uri ->
                    listOf(uri as Uri)
                } ?: emptyList()
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.mapNotNull {
                    it as? Uri
                } ?: emptyList()
            }
            else -> emptyList()
        }
    }
}

@Composable
fun MainContent(sharedImageUris: List<Uri> = emptyList()) {
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
        India2026App(sharedImageUris = sharedImageUris)
    }
}

@Composable
fun India2026App(sharedImageUris: List<Uri> = emptyList()) {
    val navController = rememberNavController()
    var submissionResult: SubmissionResult? = null
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    val scope = rememberCoroutineScope()

    // Track shared URIs that need to be passed to EditDayScreen
    var pendingSharedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Determine start destination based on whether we have shared images
    val startDestination = if (sharedImageUris.isNotEmpty()) "share_target" else "day_list"

    // Check for updates on app start
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val repository = GitHubRepository(ApiClient.gitHubApi)
                val result = repository.checkForUpdate(BuildConfig.COMMIT_SHA)
                result.onSuccess { info ->
                    updateInfo = info
                }
            } catch (e: Exception) {
                Log.d("India2026App", "Failed to check for updates: ${e.message}")
            }
        }
    }

    Scaffold(
        bottomBar = {
            BranchFooter(updateInfo = updateInfo)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        // Share Target Screen - shows day picker when app receives shared images
        composable("share_target") {
            ShareTargetScreen(
                sharedImageUris = sharedImageUris,
                onDaySelected = { slug, uris ->
                    pendingSharedUris = uris
                    navController.navigate("edit_day/$slug") {
                        popUpTo("share_target") { inclusive = true }
                    }
                },
                onCancel = {
                    // Close the activity when user cancels sharing
                    (navController.context as? ComponentActivity)?.finish()
                }
            )
        }

        // Day List Screen
        composable("day_list") {
            DayListScreen(
                onDaySelected = { slug ->
                    navController.navigate("edit_day/$slug")
                },
                onViewPullRequests = {
                    navController.navigate("pull_requests")
                }
            )
        }

        // Edit Day Screen
        composable(
            route = "edit_day/{slug}?branchName={branchName}",
            arguments = listOf(
                navArgument("slug") { type = NavType.StringType },
                navArgument("branchName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("slug") ?: return@composable
            val branchName = backStackEntry.arguments?.getString("branchName")
            // Consume pending shared URIs
            val initialSharedUris = remember { pendingSharedUris.also { pendingSharedUris = emptyList() } }
            EditDayScreen(
                slug = slug,
                branchName = branchName,
                initialSharedPhotos = initialSharedUris,
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
                    },
                    onEditDay = { slug, branchName ->
                        navController.navigate("edit_day/$slug?branchName=$branchName")
                    }
                )
            }
        }

        // Pull Request List Screen
        composable("pull_requests") {
            PullRequestListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPullRequestSelected = { result ->
                    submissionResult = result
                    navController.navigate("result") {
                        popUpTo("pull_requests") { inclusive = false }
                    }
                },
                onEditDay = { slug, branchName ->
                    navController.navigate("edit_day/$slug?branchName=$branchName")
                }
            )
        }
        }
    }
}
