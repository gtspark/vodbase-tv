package net.vodbase.tv.ui.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
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
            .background(theme.background)
            .padding(48.dp)
    ) {
        if (vod == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading...", color = theme.onSurface.copy(alpha = 0.5f), fontSize = 18.sp)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(40.dp)
            ) {
                // Thumbnail
                AsyncImage(
                    model = vod.thumbnail,
                    contentDescription = vod.title,
                    modifier = Modifier
                        .width(560.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                // Metadata
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        vod.title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Meta row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetaBadge(vod.duration, theme)
                        MetaBadge(vod.era, theme)
                        vod.gameContent?.let { MetaBadge(it, theme) }
                    }

                    Text(
                        vod.date,
                        fontSize = 15.sp,
                        color = theme.onSurface.copy(alpha = 0.4f)
                    )

                    // Series info
                    vod.series?.let { series ->
                        Text(
                            "${series.name} - Part ${series.part}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = theme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ThemedButton(
                            text = "Play",
                            onClick = { onPlay(0f) },
                            theme = theme,
                            isPrimary = true
                        )

                        if (viewModel.resumeTime > 30) {
                            val mins = (viewModel.resumeTime / 60).toInt()
                            val secs = (viewModel.resumeTime % 60).toInt()
                            ThemedButton(
                                text = "Resume from ${mins}:${"%02d".format(secs)}",
                                onClick = { onPlay(viewModel.resumeTime.toFloat()) },
                                theme = theme,
                                isPrimary = false
                            )
                        }

                        ThemedButton(
                            text = if (viewModel.isWatched) "Watched" else "Mark Watched",
                            onClick = { viewModel.markWatched(channel) },
                            theme = theme,
                            isPrimary = false,
                            enabled = !viewModel.isWatched
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThemedButton(
    text: String,
    onClick: () -> Unit,
    theme: ChannelTheme,
    isPrimary: Boolean,
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "btnScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .scale(scale),
        colors = ButtonDefaults.colors(
            containerColor = if (isPrimary) theme.primary else theme.surface,
            contentColor = if (isPrimary) theme.background else theme.onSurface,
            focusedContainerColor = if (isPrimary) theme.primary else theme.focusRing.copy(alpha = 0.2f),
            focusedContentColor = if (isPrimary) theme.background else Color.White
        )
    ) {
        Text(
            text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
    }
}

@Composable
fun MetaBadge(text: String, theme: ChannelTheme) {
    Box(
        modifier = Modifier
            .background(
                theme.surface,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, fontSize = 13.sp, color = theme.onSurface.copy(alpha = 0.7f))
    }
}
