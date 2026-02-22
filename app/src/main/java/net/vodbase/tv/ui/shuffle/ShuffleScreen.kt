package net.vodbase.tv.ui.shuffle

import android.view.KeyEvent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.vodbase.tv.data.model.Vod
import net.vodbase.tv.data.repository.ProgressRepository
import net.vodbase.tv.data.repository.VodRepository
import net.vodbase.tv.ui.components.ActionButton
import net.vodbase.tv.ui.components.VodDetailCard
import net.vodbase.tv.ui.theme.ChannelThemes
import javax.inject.Inject

@HiltViewModel
class ShuffleViewModel @Inject constructor(
    private val vodRepository: VodRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    var currentVod by mutableStateOf<Vod?>(null)
        private set

    /** Builds the candidate pool: unwatched VODs, falling back to all VODs if all are watched. */
    private suspend fun buildPool(channelId: String): List<Vod> {
        val vods = vodRepository.getVods(channelId)
        val watchedIds = progressRepository.getWatchedIds(channelId)
        val unwatched = vods.filter { it.id !in watchedIds }
        return if (unwatched.isNotEmpty()) unwatched else vods
    }

    fun loadRandom(channelId: String) {
        viewModelScope.launch {
            currentVod = buildPool(channelId).randomOrNull()
        }
    }

    fun loadNext(channelId: String) {
        viewModelScope.launch {
            val pool = buildPool(channelId)
            val excluded = currentVod?.id
            val candidates = pool.filter { it.id != excluded }
            currentVod = if (candidates.isNotEmpty()) candidates.randomOrNull() else pool.randomOrNull()
        }
    }

    fun getSeriesPart1(channelId: String): Vod? {
        val series = currentVod?.series ?: return null
        return vodRepository.getSeriesPart1(channelId, series.name)
    }
}

@Composable
fun ShuffleScreen(
    channel: String,
    onPlay: (vodId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: ShuffleViewModel = hiltViewModel()
) {
    val theme = ChannelThemes.forChannelId(channel)

    LaunchedEffect(channel) {
        viewModel.loadRandom(channel)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        theme.primary.copy(alpha = 0.05f),
                        theme.background
                    ),
                    radius = 800f
                )
            )
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                    event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK
                ) {
                    onBack()
                    true
                } else {
                    false
                }
            }
            .padding(horizontal = 40.dp, vertical = 24.dp)
    ) {
        Crossfade(
            targetState = viewModel.currentVod,
            animationSpec = tween(durationMillis = 350),
            label = "shuffleCrossfade"
        ) { vod ->
            if (vod == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Finding something good...",
                        color = theme.onSurface.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                }
            } else {
                VodDetailCard(vod = vod, theme = theme, label = "Shuffle Pick") {
                    ActionButton(
                        text = "Play",
                        onClick = { onPlay(vod.id) },
                        isBright = true,
                        theme = theme,
                        autoFocus = true
                    )

                    ActionButton(
                        text = "Nah, Next",
                        onClick = { viewModel.loadNext(channel) },
                        isBright = false,
                        theme = theme
                    )

                    // Only show "Start from Part 1" if this is a series and not already at part 1
                    if (vod.series != null && vod.series.part > 1) {
                        ActionButton(
                            text = "Start from Part 1",
                            onClick = {
                                val part1 = viewModel.getSeriesPart1(channel)
                                if (part1 != null) onPlay(part1.id)
                            },
                            isBright = false,
                            theme = theme
                        )
                    }
                }
            }
        }
    }
}
