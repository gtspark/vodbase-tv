package net.vodbase.tv

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.vodbase.tv.data.repository.AuthRepository
import net.vodbase.tv.ui.auth.AuthScreen
import net.vodbase.tv.ui.browse.BrowseScreen
import net.vodbase.tv.ui.detail.DetailScreen
import net.vodbase.tv.ui.home.HomeScreen
import net.vodbase.tv.ui.menu.MenuOverlay
import net.vodbase.tv.ui.player.PlayerScreen
import net.vodbase.tv.ui.search.SearchScreen
import net.vodbase.tv.ui.shuffle.ShuffleScreen
import net.vodbase.tv.ui.theme.VodBaseTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    // Shared state for voice search query delivered via onNewIntent
    private var pendingSearchQuery by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle voice search query delivered at launch (cold start)
        handleSearchIntent(intent)

        setContent {
            VodBaseTheme {
                val navController = rememberNavController()
                val isAuthenticated by authRepository.isAuthenticated.collectAsState(initial = false)
                val userEmail by authRepository.userEmail.collectAsState(initial = null)

                val startDest = if (isAuthenticated) "home" else "auth"

                var showMenu by remember { mutableStateOf(false) }
                var currentChannel by remember { mutableStateOf<String?>(null) }

                // Handle pending voice search query - navigate to search when one arrives
                LaunchedEffect(pendingSearchQuery) {
                    val query = pendingSearchQuery ?: return@LaunchedEffect
                    pendingSearchQuery = null
                    val channel = currentChannel ?: "jerma"
                    navController.navigate("search/$channel?query=${android.net.Uri.encode(query)}")
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onPreviewKeyEvent { event ->
                            if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_MENU
                            ) {
                                showMenu = !showMenu
                                true
                            } else {
                                false
                            }
                        }
                ) {
                    NavHost(navController = navController, startDestination = startDest) {
                        composable("auth") {
                            AuthScreen(
                                onAuthenticated = {
                                    navController.navigate("home") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                },
                                onSkip = {
                                    navController.navigate("home") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            LaunchedEffect(Unit) { currentChannel = null }
                            HomeScreen(
                                onChannelSelected = { channel ->
                                    currentChannel = channel
                                    navController.navigate("browse/${channel}")
                                }
                            )
                        }

                        composable(
                            "browse/{channel}",
                            arguments = listOf(navArgument("channel") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val channel = backStackEntry.arguments?.getString("channel") ?: "jerma"
                            LaunchedEffect(channel) { currentChannel = channel }
                            BrowseScreen(
                                channel = channel,
                                onVodSelected = { vodId ->
                                    navController.navigate("detail/${channel}/${vodId}")
                                },
                                onSearch = {
                                    navController.navigate("search/${channel}")
                                },
                                onShuffle = {
                                    navController.navigate("shuffle/${channel}")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            "detail/{channel}/{vodId}",
                            arguments = listOf(
                                navArgument("channel") { type = NavType.StringType },
                                navArgument("vodId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val channel = backStackEntry.arguments?.getString("channel") ?: "jerma"
                            val vodId = backStackEntry.arguments?.getString("vodId") ?: ""
                            LaunchedEffect(channel) { currentChannel = channel }
                            DetailScreen(
                                channel = channel,
                                vodId = vodId,
                                onPlay = { resumeTime ->
                                    navController.navigate("player/${channel}/${vodId}?resume=${resumeTime}")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            "player/{channel}/{vodId}?resume={resume}",
                            arguments = listOf(
                                navArgument("channel") { type = NavType.StringType },
                                navArgument("vodId") { type = NavType.StringType },
                                navArgument("resume") { type = NavType.FloatType; defaultValue = 0f }
                            )
                        ) { backStackEntry ->
                            val channel = backStackEntry.arguments?.getString("channel") ?: "jerma"
                            val vodId = backStackEntry.arguments?.getString("vodId") ?: ""
                            val resume = backStackEntry.arguments?.getFloat("resume") ?: 0f
                            LaunchedEffect(channel) { currentChannel = channel }
                            PlayerScreen(
                                channel = channel,
                                vodId = vodId,
                                resumeTimeSeconds = resume,
                                onBack = { navController.popBackStack() },
                                onNextVod = { nextId ->
                                    navController.navigate("player/${channel}/${nextId}?resume=0") {
                                        popUpTo("player/{channel}/{vodId}?resume={resume}") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(
                            "search/{channel}?query={query}",
                            arguments = listOf(
                                navArgument("channel") { type = NavType.StringType },
                                navArgument("query") { type = NavType.StringType; defaultValue = "" }
                            )
                        ) { backStackEntry ->
                            val channel = backStackEntry.arguments?.getString("channel") ?: "jerma"
                            val query = backStackEntry.arguments?.getString("query") ?: ""
                            LaunchedEffect(channel) { currentChannel = channel }
                            SearchScreen(
                                channel = channel,
                                initialQuery = query,
                                onVodSelected = { vodId ->
                                    navController.navigate("detail/${channel}/${vodId}")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            "shuffle/{channel}",
                            arguments = listOf(navArgument("channel") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val channel = backStackEntry.arguments?.getString("channel") ?: "jerma"
                            LaunchedEffect(channel) { currentChannel = channel }
                            ShuffleScreen(
                                channel = channel,
                                onPlay = { vodId ->
                                    navController.navigate("detail/${channel}/${vodId}")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Menu overlay drawn on top of NavHost
                    MenuOverlay(
                        isVisible = showMenu,
                        onDismiss = { showMenu = false },
                        userEmail = userEmail,
                        onSignOut = {
                            lifecycleScope.launch {
                                authRepository.logout()
                                navController.navigate("auth") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        onSignIn = {
                            navController.navigate("auth")
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSearchIntent(intent)
    }

    private fun handleSearchIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEARCH) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            if (!query.isNullOrBlank()) {
                pendingSearchQuery = query
            }
        }
    }
}
