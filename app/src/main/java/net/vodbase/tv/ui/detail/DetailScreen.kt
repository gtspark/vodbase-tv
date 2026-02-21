package net.vodbase.tv.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            Text("Loading...", color = theme.onSurface)
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Thumbnail
                AsyncImage(
                    model = vod.thumbnail,
                    contentDescription = vod.title,
                    modifier = Modifier
                        .width(560.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                // Metadata
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        vod.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Meta row
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MetaBadge(vod.duration, theme)
                        MetaBadge(vod.era, theme)
                        vod.gameContent?.let { MetaBadge(it, theme) }
                    }

                    Text(
                        vod.date,
                        fontSize = 14.sp,
                        color = theme.onSurface.copy(alpha = 0.5f)
                    )

                    // Series info
                    vod.series?.let { series ->
                        Text(
                            "${series.name} - Part ${series.part}",
                            fontSize = 14.sp,
                            color = theme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onPlay(0f) },
                            colors = ButtonDefaults.colors(
                                containerColor = theme.primary,
                                contentColor = theme.background
                            )
                        ) {
                            Text("Play", fontWeight = FontWeight.Bold)
                        }

                        if (viewModel.resumeTime > 30) {
                            val mins = (viewModel.resumeTime / 60).toInt()
                            val secs = (viewModel.resumeTime % 60).toInt()
                            Button(
                                onClick = { onPlay(viewModel.resumeTime.toFloat()) },
                                colors = ButtonDefaults.colors(
                                    containerColor = theme.surface,
                                    contentColor = theme.onSurface
                                )
                            ) {
                                Text("Resume from ${mins}:${"%02d".format(secs)}")
                            }
                        }

                        Button(
                            onClick = { viewModel.markWatched(channel) },
                            enabled = !viewModel.isWatched,
                            colors = ButtonDefaults.colors(
                                containerColor = if (viewModel.isWatched) Color(0xFF1A5E1A) else theme.surface,
                                contentColor = if (viewModel.isWatched) Color(0xFF90EE90) else theme.onSurface
                            )
                        ) {
                            Text(if (viewModel.isWatched) "Watched" else "Mark Watched")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetaBadge(text: String, theme: net.vodbase.tv.ui.theme.ChannelTheme) {
    Box(
        modifier = Modifier
            .background(
                theme.surface,
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 12.sp, color = theme.onSurface.copy(alpha = 0.7f))
    }
}
