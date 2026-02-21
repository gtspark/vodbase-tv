package net.vodbase.tv.ui.player

import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.vodbase.tv.data.model.Vod
import net.vodbase.tv.data.repository.ProgressRepository
import net.vodbase.tv.data.repository.VodRepository
import net.vodbase.tv.player.StreamExtractor
import net.vodbase.tv.ui.theme.ChannelThemes
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val streamExtractor: StreamExtractor,
    private val vodRepository: VodRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var vod by mutableStateOf<Vod?>(null)
        private set
    var showOverlay by mutableStateOf(false)
        private set

    private var player: ExoPlayer? = null

    fun loadAndPlay(
        context: android.content.Context,
        channelId: String,
        vodId: String,
        resumeSeconds: Float,
        onPlayerReady: (ExoPlayer) -> Unit
    ) {
        val foundVod = vodRepository.getVodById(channelId, vodId)
        vod = foundVod
        if (foundVod == null) {
            error = "VOD not found"
            isLoading = false
            return
        }

        viewModelScope.launch {
            try {
                isLoading = true
                error = null
                val stream = streamExtractor.extractStream(foundVod.youtubeId)

                val exoPlayer = ExoPlayer.Builder(context).build()
                val mediaItem = MediaItem.fromUri(stream.videoUrl)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()

                if (resumeSeconds > 0) {
                    exoPlayer.seekTo((resumeSeconds * 1000).toLong())
                }

                exoPlayer.playWhenReady = true
                player = exoPlayer
                onPlayerReady(exoPlayer)
                isLoading = false

                // Progress save loop
                startProgressSaving(channelId, foundVod, exoPlayer)
            } catch (e: Exception) {
                error = "Failed to load video: ${e.message}"
                isLoading = false
            }
        }
    }

    private fun startProgressSaving(channelId: String, vod: Vod, player: ExoPlayer) {
        viewModelScope.launch {
            while (true) {
                delay(30_000) // Save every 30s
                if (player.isPlaying) {
                    val currentTime = player.currentPosition / 1000.0
                    val duration = player.duration / 1000.0
                    progressRepository.saveProgress(
                        channelId, vod.id, vod.title, vod.url, currentTime, duration
                    )
                }
            }
        }
    }

    fun toggleOverlay() {
        showOverlay = !showOverlay
        if (showOverlay) {
            viewModelScope.launch {
                delay(5000)
                showOverlay = false
            }
        }
    }

    fun seekBy(player: ExoPlayer?, deltaMs: Long) {
        player?.let {
            val newPos = (it.currentPosition + deltaMs).coerceIn(0, it.duration)
            it.seekTo(newPos)
        }
        showOverlayBriefly()
    }

    fun togglePlayPause(player: ExoPlayer?) {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
        showOverlayBriefly()
    }

    private fun showOverlayBriefly() {
        showOverlay = true
        viewModelScope.launch {
            delay(5000)
            showOverlay = false
        }
    }

    fun release() {
        player?.release()
        player = null
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: String,
    vodId: String,
    resumeTimeSeconds: Float,
    onBack: () -> Unit,
    onNextVod: (String) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val theme = ChannelThemes.forChannelId(channel)
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    LaunchedEffect(vodId) {
        viewModel.loadAndPlay(context, channel, vodId, resumeTimeSeconds) { player ->
            exoPlayer = player
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ->
                        { viewModel.togglePlayPause(exoPlayer); true }
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND ->
                        { viewModel.seekBy(exoPlayer, -10_000); true }
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ->
                        { viewModel.seekBy(exoPlayer, 10_000); true }
                    KeyEvent.KEYCODE_BACK ->
                        { onBack(); true }
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN ->
                        { viewModel.toggleOverlay(); true }
                    else -> false
                }
            }
    ) {
        // Video player
        exoPlayer?.let { player ->
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Loading
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = theme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading VOD...", color = Color.White)
                }
            }
        }

        // Error
        viewModel.error?.let { err ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(err, color = Color(0xFFEF4444), fontSize = 18.sp)
            }
        }

        // Auto-hide overlay
        AnimatedVisibility(
            visible = viewModel.showOverlay,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        viewModel.vod?.title ?: "",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
