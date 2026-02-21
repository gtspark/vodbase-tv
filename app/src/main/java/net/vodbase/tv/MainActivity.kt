package net.vodbase.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import net.vodbase.tv.data.repository.AuthRepository
import net.vodbase.tv.ui.auth.AuthScreen
import net.vodbase.tv.ui.browse.BrowseScreen
import net.vodbase.tv.ui.detail.DetailScreen
import net.vodbase.tv.ui.home.HomeScreen
import net.vodbase.tv.ui.player.PlayerScreen
import net.vodbase.tv.ui.search.SearchScreen
import net.vodbase.tv.ui.theme.VodBaseTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VodBaseTheme {
                val navController = rememberNavController()
                val isAuthenticated by authRepository.isAuthenticated.collectAsState(initial = false)

                val startDest = if (isAuthenticated) "home" else "auth"

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
                        HomeScreen(
                            onChannelSelected = { channel ->
                                navController.navigate("browse/${channel}")
                            }
                        )
                    }

                    composable(
                        "browse/{channel}",
                        arguments = listOf(navArgument("channel") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val channel = backStackEntry.arguments?.getString("channel") ?: "jerma"
                        BrowseScreen(
                            channel = channel,
                            onVodSelected = { vodId ->
                                navController.navigate("detail/${channel}/${vodId}")
                            },
                            onSearch = {
                                navController.navigate("search/${channel}")
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
                        "search/{channel}",
                        arguments = listOf(navArgument("channel") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val channel = backStackEntry.arguments?.getString("channel") ?: "jerma"
                        SearchScreen(
                            channel = channel,
                            onVodSelected = { vodId ->
                                navController.navigate("detail/${channel}/${vodId}")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
