package net.vodbase.tv.ui.player

import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
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
    var isPlaying by mutableStateOf(false)
        private set
    var currentPositionMs by mutableStateOf(0L)
        private set
    var durationMs by mutableStateOf(0L)
        private set

    private var player: ExoPlayer? = null
    private var overlayJob: kotlinx.coroutines.Job? = null

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

                if (stream.isAdaptive && stream.audioUrl != null) {
                    val dataSourceFactory = DefaultHttpDataSource.Factory()
                    val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(stream.videoUrl))
                    val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(stream.audioUrl))
                    exoPlayer.setMediaSource(MergingMediaSource(videoSource, audioSource))
                } else {
                    exoPlayer.setMediaItem(MediaItem.fromUri(stream.videoUrl))
                }
                exoPlayer.prepare()

                if (resumeSeconds > 0) {
                    exoPlayer.seekTo((resumeSeconds * 1000).toLong())
                }

                exoPlayer.playWhenReady = true
                exoPlayer.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }
                })

                player = exoPlayer
                onPlayerReady(exoPlayer)
                isLoading = false

                // Position tracking loop (updates UI every 500ms)
                startPositionTracking(exoPlayer)
                // Progress save loop
                startProgressSaving(channelId, foundVod, exoPlayer)
            } catch (e: Exception) {
                error = "Failed to load video: ${e.message}"
                isLoading = false
            }
        }
    }

    private fun startPositionTracking(player: ExoPlayer) {
        viewModelScope.launch {
            while (true) {
                delay(500)
                try {
                    currentPositionMs = player.currentPosition
                    durationMs = player.duration.coerceAtLeast(0)
                } catch (_: Exception) { }
            }
        }
    }

    private fun startProgressSaving(channelId: String, vod: Vod, player: ExoPlayer) {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                try {
                    if (player.isPlaying) {
                        val currentTime = player.currentPosition / 1000.0
                        val duration = player.duration / 1000.0
                        progressRepository.saveProgress(
                            channelId, vod.id, vod.title, vod.url, currentTime, duration
                        )
                    }
                } catch (_: Exception) { }
            }
        }
    }

    fun showOverlayBriefly() {
        showOverlay = true
        overlayJob?.cancel()
        overlayJob = viewModelScope.launch {
            delay(5000)
            showOverlay = false
        }
    }

    fun toggleOverlay() {
        if (showOverlay) {
            overlayJob?.cancel()
            showOverlay = false
        } else {
            showOverlayBriefly()
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

    fun release() {
        player?.release()
        player = null
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
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
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(vodId) {
        viewModel.loadAndPlay(context, channel, vodId, resumeTimeSeconds) { player ->
            exoPlayer = player
        }
    }

    // Grab focus so we receive key events
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
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
                    Text("Extracting stream...", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        // Error
        viewModel.error?.let { err ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(err, color = Color(0xFFEF4444), fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Press BACK to return", color = Color(0xFF7A7A9A), fontSize = 14.sp)
                }
            }
        }

        // Pause indicator (center of screen, brief)
        if (!viewModel.isPlaying && !viewModel.isLoading && viewModel.error == null && exoPlayer != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(36.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("II", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Auto-hide overlay
        AnimatedVisibility(
            visible = viewModel.showOverlay,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top gradient + title
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    Text(
                        viewModel.vod?.title ?: "",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bottom gradient + progress bar + time
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                        .padding(horizontal = 32.dp, vertical = 20.dp)
                ) {
                    // Progress bar
                    val progress = if (viewModel.durationMs > 0) {
                        (viewModel.currentPositionMs.toFloat() / viewModel.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = theme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Time row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatTime(viewModel.currentPositionMs),
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        // Play state indicator
                        Text(
                            if (viewModel.isPlaying) "Playing" else "Paused",
                            fontSize = 14.sp,
                            color = if (viewModel.isPlaying) theme.primary else Color(0xFFFF9800)
                        )
                        Text(
                            formatTime(viewModel.durationMs),
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
