package net.vodbase.tv.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.vodbase.tv.data.model.Vod
import net.vodbase.tv.data.repository.ProgressRepository
import net.vodbase.tv.data.repository.VodRepository
import net.vodbase.tv.ui.components.ActionButton
import net.vodbase.tv.ui.components.DetailSkeletonScreen
import net.vodbase.tv.ui.components.VodDetailCard
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
    var error by mutableStateOf<String?>(null)
        private set

    fun load(channelId: String, vodId: String) {
        viewModelScope.launch {
            val found = try {
                vodRepository.getVodByIdOrFetch(channelId, vodId)
            } catch (e: Exception) {
                error = "Failed to load: ${e.message}"
                return@launch
            }
            if (found == null) {
                error = "VOD not found"
                return@launch
            }
            vod = found
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
    onPlay: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val theme = ChannelThemes.forChannelId(channel)

    LaunchedEffect(channel, vodId) {
        viewModel.load(channel, vodId)
    }

    val vod = viewModel.vod

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        // Blurred thumbnail background
        if (vod != null) {
            AsyncImage(
                model = vod.thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(25.dp).alpha(0.35f),
                contentScale = ContentScale.Crop
            )
        }

        // Gradient overlay on top of blur
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            theme.primary.copy(alpha = 0.05f),
                            theme.background.copy(alpha = 0.85f)
                        ),
                        radius = 800f
                    )
                )
        )

        // Content
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 24.dp)) {
        if (viewModel.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(viewModel.error ?: "", color = theme.error, fontSize = 16.sp)
            }
        } else if (vod == null) {
            DetailSkeletonScreen(theme)
        } else {
            VodDetailCard(vod = vod, theme = theme) {
                // If resume position exists, make Resume the primary button
                val hasResume = viewModel.resumeTime > 30
                if (hasResume) {
                    val totalSecs = viewModel.resumeTime.toInt()
                    val hrs = totalSecs / 3600
                    val mins = (totalSecs % 3600) / 60
                    val secs = totalSecs % 60
                    val timestamp = if (hrs > 0) "%d:%02d:%02d".format(hrs, mins, secs) else "%d:%02d".format(mins, secs)
                    ActionButton(
                        text = "Resume from $timestamp",
                        onClick = { onPlay((viewModel.resumeTime * 1000).toLong()) },
                        isBright = true,
                        theme = theme,
                        autoFocus = true
                    )
                    ActionButton(
                        text = "Play from Start",
                        onClick = { onPlay(0L) },
                        isBright = false,
                        theme = theme
                    )
                } else {
                    ActionButton(
                        text = "Play",
                        onClick = { onPlay(0L) },
                        isBright = true,
                        theme = theme,
                        autoFocus = true
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
