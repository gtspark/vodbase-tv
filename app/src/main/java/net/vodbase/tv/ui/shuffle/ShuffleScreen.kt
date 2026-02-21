package net.vodbase.tv.ui.shuffle

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.vodbase.tv.data.model.Vod
import net.vodbase.tv.data.repository.ProgressRepository
import net.vodbase.tv.data.repository.VodRepository
import net.vodbase.tv.ui.detail.ActionButton
import net.vodbase.tv.ui.detail.MetaBadge
import net.vodbase.tv.ui.theme.ChannelTheme
import net.vodbase.tv.ui.theme.ChannelThemes
import javax.inject.Inject

@HiltViewModel
class ShuffleViewModel @Inject constructor(
    private val vodRepository: VodRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    var currentVod by mutableStateOf<Vod?>(null)
        private set

    fun loadRandom(channelId: String) {
        viewModelScope.launch {
            val vods = vodRepository.getVods(channelId)
            val watchedIds = progressRepository.getWatchedIds(channelId)
            val unwatched = vods.filter { it.id !in watchedIds }
            // Prefer unwatched; fall back to full list if all watched
            val pool = if (unwatched.isNotEmpty()) unwatched else vods
            currentVod = pool.randomOrNull()
        }
    }

    fun loadNext(channelId: String) {
        viewModelScope.launch {
            val vods = vodRepository.getVods(channelId)
            val watchedIds = progressRepository.getWatchedIds(channelId)
            val unwatched = vods.filter { it.id !in watchedIds }
            val pool = if (unwatched.isNotEmpty()) unwatched else vods
            val excluded = currentVod?.id
            val candidates = pool.filter { it.id != excluded }
            currentVod = if (candidates.isNotEmpty()) candidates.randomOrNull() else pool.randomOrNull()
        }
    }

    fun getSeriesPart1(channelId: String): Vod? {
        val series = currentVod?.series ?: return null
        return vodRepository.getSeriesVods(channelId, series.name).firstOrNull()
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
                if (event.type == KeyEventType.KeyDown &&
                    event.key == Key.Back
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
                ShuffleContent(
                    vod = vod,
                    channel = channel,
                    theme = theme,
                    onPlay = onPlay,
                    onNext = { viewModel.loadNext(channel) },
                    onPart1 = {
                        val part1 = viewModel.getSeriesPart1(channel)
                        if (part1 != null) onPlay(part1.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun ShuffleContent(
    vod: Vod,
    channel: String,
    theme: ChannelTheme,
    onPlay: (vodId: String) -> Unit,
    onNext: () -> Unit,
    onPart1: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        AsyncImage(
            model = vod.thumbnail,
            contentDescription = vod.title,
            modifier = Modifier
                .weight(1.4f)
                .aspectRatio(16f / 9f)
                .clip(theme.shape),
            contentScale = ContentScale.Crop
        )

        // Metadata + buttons
        Column(
            modifier = Modifier.weight(0.6f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Shuffle label
            Text(
                "Shuffle Pick",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.primary.copy(alpha = 0.8f),
                letterSpacing = 1.sp
            )

            Text(
                vod.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = theme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )

            // Meta row
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetaBadge(vod.duration, theme)
                MetaBadge(vod.era, theme)
                vod.gameContent?.let { MetaBadge(it, theme) }
            }

            Text(
                vod.date,
                fontSize = 12.sp,
                color = theme.onSurface.copy(alpha = 0.4f)
            )

            vod.series?.let { series ->
                Text(
                    "${series.name} - Part ${series.part}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = theme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(
                    text = "Play",
                    onClick = { onPlay(vod.id) },
                    isBright = true,
                    theme = theme
                )

                ActionButton(
                    text = "Nah, Next",
                    onClick = onNext,
                    isBright = false,
                    theme = theme
                )

                // Only show "Start from Part 1" if this is a series and we're not already at part 1
                if (vod.series != null && vod.series.part > 1) {
                    ActionButton(
                        text = "Start from Part 1",
                        onClick = onPart1,
                        isBright = false,
                        theme = theme
                    )
                }
            }
        }
    }
}
