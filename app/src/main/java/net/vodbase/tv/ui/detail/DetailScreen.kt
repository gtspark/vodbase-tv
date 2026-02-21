package net.vodbase.tv.ui.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import net.vodbase.tv.ui.theme.ChannelTheme
import net.vodbase.tv.ui.theme.ChannelThemes
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val vodRepository: VodRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    var vod by mutableStateOf<Vod?>(null)
        private set
    var resumeTime by mutableStateOf(0.0)
        private set
    var isWatched by mutableStateOf(false)
        private set

    fun load(channelId: String, vodId: String) {
        vod = vodRepository.getVodById(channelId, vodId)
        viewModelScope.launch {
            val resume = progressRepository.getResumePosition(channelId)
            if (resume?.first == vodId) {
                resumeTime = resume.second
            }
            isWatched = progressRepository.isWatched(channelId, vodId)
        }
    }

    fun markWatched(channelId: String) {
        val v = vod ?: return
        isWatched = true
        viewModelScope.launch {
            progressRepository.markWatched(channelId, v.id, v.title, v.duration)
        }
    }
}

@Composable
fun DetailScreen(
    channel: String,
    vodId: String,
    onPlay: (Float) -> Unit,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val theme = ChannelThemes.forChannelId(channel)

    LaunchedEffect(channel, vodId) {
        viewModel.load(channel, vodId)
    }

    val vod = viewModel.vod

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
            .padding(horizontal = 40.dp, vertical = 24.dp)
    ) {
        if (vod == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading...", color = theme.onSurface.copy(alpha = 0.5f), fontSize = 16.sp)
            }
        } else {
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
                        .weight(0.8f)
                        .aspectRatio(16f / 9f)
                        .clip(theme.shape),
                    contentScale = ContentScale.Crop
                )

                // Metadata + buttons
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                    // Action buttons - vertical stack
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            text = "Play",
                            onClick = { onPlay(0f) },
                            isBright = true,
                            theme = theme
                        )

                        if (viewModel.resumeTime > 30) {
                            val mins = (viewModel.resumeTime / 60).toInt()
                            val secs = (viewModel.resumeTime % 60).toInt()
                            ActionButton(
                                text = "Resume from ${mins}:${"%02d".format(secs)}",
                                onClick = { onPlay(viewModel.resumeTime.toFloat()) },
                                isBright = false,
                                theme = theme
                            )
                        }

                        ActionButton(
                            text = if (viewModel.isWatched) "Watched" else "Mark Watched",
                            onClick = { viewModel.markWatched(channel) },
                            isBright = false,
                            theme = theme,
                            enabled = !viewModel.isWatched
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    isBright: Boolean,
    theme: ChannelTheme,
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "btnBorder"
    )

    val bgColor = when {
        isFocused && isBright -> theme.primary
        isFocused -> theme.primary.copy(alpha = 0.25f)
        isBright -> theme.primary.copy(alpha = 0.8f)
        else -> theme.surface
    }
    val textColor = when {
        isFocused && isBright -> theme.background
        isFocused -> Color.White
        isBright -> theme.background
        else -> theme.onSurface.copy(alpha = 0.7f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(theme.shape)
            .background(bgColor)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = theme.focusRing.copy(alpha = borderAlpha),
                shape = theme.shape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = if (!enabled) textColor.copy(alpha = 0.5f) else textColor
        )
    }
}

@Composable
fun MetaBadge(text: String, theme: ChannelTheme) {
    Box(
        modifier = Modifier
            .background(theme.surface, theme.shape)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 11.sp, color = theme.onSurface.copy(alpha = 0.6f))
    }
}
