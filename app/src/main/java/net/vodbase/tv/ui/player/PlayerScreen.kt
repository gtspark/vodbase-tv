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
import androidx.compose.ui.platform.LocalLifecycleOwner
import android.view.WindowManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    // Seek accumulator - shows pending seek target while user is scrubbing
    var pendingSeekMs by mutableStateOf<Long?>(null)
        private set

    private var player: ExoPlayer? = null
    private var overlayJob: Job? = null
    private var seekJob: Job? = null
    private var consecutiveSeeks = 0
    private var lastSeekDirection = 0 // -1 left, +1 right
    private var lastSeekTime = 0L
    private var channelId: String? = null
    private var hasMarkedWatched = false

    @OptIn(UnstableApi::class)
    fun loadAndPlay(
        context: android.content.Context,
        channelId: String,
        vodId: String,
        resumeSeconds: Float,
        onPlayerReady: (ExoPlayer) -> Unit
    ) {
        this.channelId = channelId
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

                // Tuned buffer settings for better seek performance
                val loadControl = DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        15_000,  // min buffer before playback starts (15s)
                        60_000,  // max buffer (1min)
                        1_000,   // playback buffer (1s - start faster after seek)
                        2_000    // rebuffer threshold (2s)
                    )
                    .setBackBuffer(30_000, true) // keep 30s back buffer
                    .build()

                val exoPlayer = ExoPlayer.Builder(context)
                    .setLoadControl(loadControl)
                    .setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    .build()

                val dataSourceFactory = DefaultHttpDataSource.Factory()
                    .setConnectTimeoutMs(15_000)
                    .setReadTimeoutMs(15_000)
                    .setAllowCrossProtocolRedirects(true)

                val sourceType: String
                if (stream.hlsUrl != null) {
                    // HLS: segment-based = fast seeking, auto-adaptive quality
                    val hlsSource = HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(stream.hlsUrl))
                    exoPlayer.setMediaSource(hlsSource)
                    sourceType = "HLS"
                } else if (stream.isAdaptive && stream.audioUrl != null) {
                    // Fallback: progressive adaptive (slow seeking)
                    val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(stream.videoUrl))
                    val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(stream.audioUrl))
                    exoPlayer.setMediaSource(MergingMediaSource(videoSource, audioSource))
                    sourceType = "Progressive+Merge"
                } else {
                    exoPlayer.setMediaItem(MediaItem.fromUri(stream.videoUrl))
                    sourceType = "Progressive"
                }

                android.util.Log.i("VodPlayer", "Source: $sourceType | HLS=${stream.hlsUrl != null} | Res=${stream.resolution}")
                exoPlayer.prepare()

                if (resumeSeconds > 0) {
                    exoPlayer.seekTo((resumeSeconds * 1000).toLong())
                }

                exoPlayer.playWhenReady = true
                exoPlayer.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                        android.util.Log.i("VodPlayer", "isPlaying=$playing pos=${exoPlayer.currentPosition}ms")
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                        val stateName = when(state) {
                            Player.STATE_IDLE -> "IDLE"
                            Player.STATE_BUFFERING -> "BUFFERING"
                            Player.STATE_READY -> "READY"
                            Player.STATE_ENDED -> "ENDED"
                            else -> "UNKNOWN($state)"
                        }
                        android.util.Log.i("VodPlayer", "State=$stateName pos=${exoPlayer.currentPosition}ms buf=${exoPlayer.bufferedPosition}ms")
                    }
                })

                player = exoPlayer
                onPlayerReady(exoPlayer)
                isLoading = false

                startPositionTracking(exoPlayer)
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
                    // Check for completion (>90% watched)
                    checkCompletion(player)
                } catch (_: Exception) { }
            }
        }
    }

    private fun checkCompletion(player: ExoPlayer) {
        if (hasMarkedWatched) return
        val duration = player.duration
        val position = player.currentPosition
        if (duration > 0 && position > 0 && position.toFloat() / duration > 0.9f) {
            hasMarkedWatched = true
            val ch = channelId ?: return
            val v = vod ?: return
            viewModelScope.launch {
                progressRepository.markWatched(ch, v.id, v.title, v.duration)
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

    /**
     * Batched seek with acceleration.
     * Rapid presses accumulate a delta. The actual seek fires 400ms after the last press.
     * Speed ramps: 1-3 presses = 10s, 4-6 = 30s, 7+ = 60s per press.
     */
    fun seekBy(player: ExoPlayer?, direction: Int) {
        player ?: return
        val now = System.currentTimeMillis()

        // Reset acceleration if direction changed or >1.5s since last press
        if (direction != lastSeekDirection || now - lastSeekTime > 1500) {
            consecutiveSeeks = 0
        }
        consecutiveSeeks++
        lastSeekDirection = direction
        lastSeekTime = now

        // Acceleration ramp
        val stepMs = when {
            consecutiveSeeks >= 7 -> 60_000L
            consecutiveSeeks >= 4 -> 30_000L
            else -> 10_000L
        }

        val deltaMs = stepMs * direction
        val currentTarget = pendingSeekMs ?: player.currentPosition
        val newTarget = (currentTarget + deltaMs).coerceIn(0, player.duration.coerceAtLeast(0))
        pendingSeekMs = newTarget

        showOverlayBriefly()

        // Debounce - only execute seek after 400ms of no presses
        seekJob?.cancel()
        seekJob = viewModelScope.launch {
            delay(400)
            android.util.Log.i("VodPlayer", "SEEK from=${player.currentPosition}ms to=${newTarget}ms delta=${newTarget - player.currentPosition}ms")
            player.seekTo(newTarget)
            pendingSeekMs = null
        }
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

    // Keep screen on while player is active
    val activity = context as? android.app.Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Grab focus so we receive key events
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.release() }
    }

    // Pause on home button / app backgrounded
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                exoPlayer?.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
                        { viewModel.seekBy(exoPlayer, -1); true }
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ->
                        { viewModel.seekBy(exoPlayer, 1); true }
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
                    Text("Extracting stream...", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        // Error
        viewModel.error?.let { err ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(err, color = Color(0xFFEF4444), fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Press BACK to return", color = Color(0xFF7A7A9A), fontSize = 16.sp)
                }
            }
        }

        // Pause indicator (center of screen)
        if (!viewModel.isPlaying && !viewModel.isLoading && viewModel.error == null && exoPlayer != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(40.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("II", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Seek indicator (shows target position while scrubbing)
        viewModel.pendingSeekMs?.let { targetMs ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        formatTime(targetMs),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.primary
                    )
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
                        .padding(horizontal = 40.dp, vertical = 20.dp)
                ) {
                    Text(
                        viewModel.vod?.title ?: "",
                        fontSize = 20.sp,
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
                        .padding(horizontal = 40.dp, vertical = 24.dp)
                ) {
                    // Progress bar
                    val displayPos = viewModel.pendingSeekMs ?: viewModel.currentPositionMs
                    val progress = if (viewModel.durationMs > 0) {
                        (displayPos.toFloat() / viewModel.durationMs.toFloat()).coerceIn(0f, 1f)
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Time row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatTime(displayPos),
                            fontSize = 15.sp,
                            color = if (viewModel.pendingSeekMs != null) theme.primary else Color.White
                        )
                        // Play state indicator
                        Text(
                            if (viewModel.pendingSeekMs != null) "Seeking..."
                            else if (viewModel.isPlaying) "Playing" else "Paused",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                viewModel.pendingSeekMs != null -> theme.primary
                                viewModel.isPlaying -> theme.primary
                                else -> Color(0xFFFF9800)
                            }
                        )
                        Text(
                            formatTime(viewModel.durationMs),
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
