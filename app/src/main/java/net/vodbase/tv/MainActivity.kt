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
import kotlinx.coroutines.flow.MutableSharedFlow
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
import net.vodbase.tv.ui.theme.ChannelThemes
import net.vodbase.tv.ui.theme.VodBaseTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    // SharedFlow for voice search queries - handles duplicate query strings correctly (#11)
    private val pendingSearchQuery = MutableSharedFlow<String>(extraBufferCapacity = 1)

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

                // Collect voice search queries from SharedFlow
                LaunchedEffect(Unit) {
                    pendingSearchQuery.collect { query ->
                        val channel = currentChannel ?: "jerma"
                        navController.navigate("search/$channel?query=${android.net.Uri.encode(query)}")
                    }
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
                                onPlay = { resumeMs ->
                                    navController.navigate("player/${channel}/${vodId}?resumeMs=${resumeMs}")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            "player/{channel}/{vodId}?resumeMs={resumeMs}",
                            arguments = listOf(
                                navArgument("channel") { type = NavType.StringType },
                                navArgument("vodId") { type = NavType.StringType },
                                navArgument("resumeMs") { type = NavType.LongType; defaultValue = 0L }
                            )
                        ) { backStackEntry ->
                            val channel = backStackEntry.arguments?.getString("channel") ?: "jerma"
                            val vodId = backStackEntry.arguments?.getString("vodId") ?: ""
                            val resumeMs = backStackEntry.arguments?.getLong("resumeMs") ?: 0L
                            LaunchedEffect(channel) { currentChannel = channel }
                            PlayerScreen(
                                channel = channel,
                                vodId = vodId,
                                resumeTimeMs = resumeMs,
                                onBack = { navController.popBackStack() },
                                onNextVod = { nextId ->
                                    navController.navigate("player/${channel}/${nextId}?resumeMs=0") {
                                        popUpTo("player/{channel}/{vodId}?resumeMs={resumeMs}") { inclusive = true }
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
                    // Theme follows the currently active channel; falls back to Jerma on home screen
                    val menuTheme = currentChannel?.let { ChannelThemes.forChannelId(it) }
                        ?: ChannelThemes.Jerma
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
                        },
                        theme = menuTheme
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
                pendingSearchQuery.tryEmit(query)
            }
        }
    }
}
