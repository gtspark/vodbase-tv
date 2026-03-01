package net.vodbase.tv.ui.player

import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
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
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.vodbase.tv.data.model.Chapter
import net.vodbase.tv.data.model.Vod
import net.vodbase.tv.data.repository.ProgressRepository
import net.vodbase.tv.data.repository.SettingsRepository
import net.vodbase.tv.data.repository.VodRepository
import net.vodbase.tv.player.StreamExtractor
import net.vodbase.tv.ui.theme.ChannelThemes
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val streamExtractor: StreamExtractor,
    private val vodRepository: VodRepository,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository
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
    var pendingSeekMs by mutableStateOf<Long?>(null)
        private set
    var chapters by mutableStateOf<List<Chapter>>(emptyList())
        private set
    // Auto-advance state
    var videoEnded by mutableStateOf(false)
    var upNextVod by mutableStateOf<Vod?>(null)
        private set
    var upNextCountdown by mutableStateOf<Int?>(null)
        private set

    // SharedFlow for next-VOD navigation events (#15 - avoid passing nav callbacks into coroutines)
    private val _nextVodEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val nextVodEvent = _nextVodEvent.asSharedFlow()

    private var countdownJob: Job? = null
    private var player: ExoPlayer? = null
    private var overlayJob: Job? = null
    private var seekJob: Job? = null
    private var loadJob: Job? = null
    private var trackingJob: Job? = null
    private var savingJob: Job? = null
    private var consecutiveSeeks = 0
    private var lastSeekDirection = 0
    private var lastSeekTime = 0L
    var lastLeftTapTime = 0L
    var lastRightTapTime = 0L
    private var channelId: String? = null
    private var currentVodId: String? = null
    private var hasMarkedWatched = false
    private var autoplayEnabled = true
    private var manifestFiles = mutableListOf<java.io.File>()

    @OptIn(UnstableApi::class)
    fun loadAndPlay(
        context: android.content.Context,
        channelId: String,
        vodId: String,
        resumeMs: Long,
        onPlayerReady: (ExoPlayer) -> Unit
    ) {
        // Cancel previous load and release previous player (#7 - prevent ExoPlayer leak)
        loadJob?.cancel()
        releasePlayer()

        this.channelId = channelId
        this.currentVodId = vodId
        hasMarkedWatched = false
        videoEnded = false

        val appContext = context.applicationContext  // #2 - use app context, not Activity

        loadJob = viewModelScope.launch {
            try {
                isLoading = true
                error = null

                // #5 - fetch VODs if cache is cold
                val foundVod = vodRepository.getVodByIdOrFetch(channelId, vodId)
                if (foundVod == null) {
                    error = "VOD not found"
                    isLoading = false
                    return@launch
                }
                vod = foundVod

                // Read settings
                val qualitySetting = settingsRepository.videoQuality.first()
                val speedSetting = settingsRepository.playbackSpeed.first()
                autoplayEnabled = settingsRepository.autoplayNext.first()

                val preferredHeight = when (qualitySetting) {
                    "1080" -> 1080
                    "720" -> 720
                    "480" -> 480
                    "360" -> 360
                    else -> null
                }
                val stream = streamExtractor.extractStream(foundVod.youtubeId, preferredHeight)
                chapters = stream.chapters

                val loadControl = DefaultLoadControl.Builder()
                    .setBufferDurationsMs(15_000, 60_000, 1_000, 2_000)
                    .setBackBuffer(30_000, true)
                    .build()

                val exoPlayer = ExoPlayer.Builder(appContext)
                    .setLoadControl(loadControl)
                    .setSeekParameters(SeekParameters.CLOSEST_SYNC)
                    .build()

                val httpFactory = DefaultHttpDataSource.Factory()
                    .setConnectTimeoutMs(15_000)
                    .setReadTimeoutMs(15_000)
                    .setAllowCrossProtocolRedirects(true)
                val dataSourceFactory = DefaultDataSource.Factory(appContext, httpFactory)

                if (stream.videoDashManifest != null && stream.audioDashManifest != null) {
                    // #3 - unique manifest filenames per VOD to prevent write-during-read
                    val videoManifestFile = java.io.File(appContext.cacheDir, "dash_video_${vodId}.mpd")
                    val audioManifestFile = java.io.File(appContext.cacheDir, "dash_audio_${vodId}.mpd")
                    videoManifestFile.writeText(stream.videoDashManifest)
                    audioManifestFile.writeText(stream.audioDashManifest)
                    manifestFiles.add(videoManifestFile)
                    manifestFiles.add(audioManifestFile)

                    val videoDashSource = DashMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(android.net.Uri.fromFile(videoManifestFile)))
                    val audioDashSource = DashMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(android.net.Uri.fromFile(audioManifestFile)))
                    exoPlayer.setMediaSource(MergingMediaSource(videoDashSource, audioDashSource))
                } else if (stream.hlsUrl != null) {
                    val hlsSource = HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(stream.hlsUrl))
                    exoPlayer.setMediaSource(hlsSource)
                } else if (stream.isAdaptive && stream.audioUrl != null) {
                    val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(stream.videoUrl))
                    val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(stream.audioUrl))
                    exoPlayer.setMediaSource(MergingMediaSource(videoSource, audioSource))
                } else {
                    exoPlayer.setMediaItem(MediaItem.fromUri(stream.videoUrl))
                }

                exoPlayer.prepare()

                if (speedSetting != 1.0f) {
                    exoPlayer.playbackParameters = PlaybackParameters(speedSetting)
                }

                if (resumeMs > 0) {
                    exoPlayer.seekTo(resumeMs)
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
                        if (state == Player.STATE_ENDED) {
                            if (!hasMarkedWatched) {
                                hasMarkedWatched = true
                                val ch = channelId ?: return
                                val v = vod ?: return
                                viewModelScope.launch {
                                    progressRepository.markWatched(ch, v.id, v.title, v.duration)
                                }
                            }
                            videoEnded = true
                        }
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

    // #6 - store Job refs so they can be cancelled on release
    private fun startPositionTracking(player: ExoPlayer) {
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            while (true) {
                delay(500)
                try {
                    currentPositionMs = player.currentPosition
                    durationMs = player.duration.coerceAtLeast(0)
                    checkCompletion(player)
                } catch (_: Exception) { break }
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
        savingJob?.cancel()
        savingJob = viewModelScope.launch {
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
                } catch (_: Exception) { break }
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

    fun seekBy(player: ExoPlayer?, direction: Int) {
        player ?: return
        val now = System.currentTimeMillis()

        if (direction != lastSeekDirection || now - lastSeekTime > 1500) {
            consecutiveSeeks = 0
        }
        consecutiveSeeks++
        lastSeekDirection = direction
        lastSeekTime = now

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

        seekJob?.cancel()
        seekJob = viewModelScope.launch {
            delay(400)
            android.util.Log.i("VodPlayer", "SEEK from=${player.currentPosition}ms to=${newTarget}ms delta=${newTarget - player.currentPosition}ms")
            player.seekTo(newTarget)
            pendingSeekMs = null
        }
    }

    val currentChapter: Chapter? get() {
        val pos = pendingSeekMs ?: currentPositionMs
        return chapters.lastOrNull { it.startTimeMs <= pos }
    }

    fun seekToNextChapter(player: ExoPlayer?) {
        player ?: return
        if (chapters.isEmpty()) return
        seekJob?.cancel()
        pendingSeekMs = null
        val pos = player.currentPosition
        val next = chapters.firstOrNull { it.startTimeMs > pos + 1000 } ?: return
        player.seekTo(next.startTimeMs)
        showOverlayBriefly()
        android.util.Log.i("VodPlayer", "Chapter skip → '${next.title}' @${next.startTimeMs/1000}s")
    }

    fun seekToPrevChapter(player: ExoPlayer?) {
        player ?: return
        if (chapters.isEmpty()) return
        seekJob?.cancel()
        pendingSeekMs = null
        val pos = player.currentPosition
        // If more than 3s into current chapter, go to its start; otherwise go to previous
        val prev = chapters.lastOrNull { it.startTimeMs < pos - 3000 } ?: chapters.first()
        player.seekTo(prev.startTimeMs)
        showOverlayBriefly()
        android.util.Log.i("VodPlayer", "Chapter skip ← '${prev.title}' @${prev.startTimeMs/1000}s")
    }

    fun togglePlayPause(player: ExoPlayer?) {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
        showOverlayBriefly()
    }

    private suspend fun findNextVod(): Vod? {
        val ch = channelId ?: return null
        val currentVod = vod ?: return null

        val series = currentVod.series
        if (series != null) {
            val seriesVods = vodRepository.getSeriesVods(ch, series.name)
            android.util.Log.i("VodPlayer", "Series lookup: '${series.name}' part=${series.part}, found ${seriesVods.size} vods in series")
            val nextPart = seriesVods.find { it.series?.part == series.part + 1 }
            if (nextPart != null) {
                android.util.Log.i("VodPlayer", "Next in series: '${nextPart.title}' (part ${nextPart.series?.part})")
                return nextPart
            }
            android.util.Log.i("VodPlayer", "No part ${series.part + 1} found, falling back to random")
        }

        val allVods = vodRepository.getVods(ch)
        val watchedIds = progressRepository.getWatchedIds(ch)
        val unwatched = allVods.filter { it.id != currentVod.id && !watchedIds.contains(it.id) }
        return if (unwatched.isNotEmpty()) unwatched.random() else allVods.filter { it.id != currentVod.id }.randomOrNull()
    }

    // #15 - emit to SharedFlow instead of calling nav callback from coroutine
    fun startUpNextCountdown(forceAutoplay: Boolean = false) {
        if (!forceAutoplay && !autoplayEnabled) return
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            val next = findNextVod()
            if (next == null) {
                android.util.Log.i("VodPlayer", "Up Next: no next VOD found")
                return@launch
            }
            upNextVod = next
            upNextCountdown = 10
            android.util.Log.i("VodPlayer", "Up Next: '${next.title}' in 10s")
            for (i in 10 downTo 1) {
                upNextCountdown = i
                delay(1000)
            }
            upNextCountdown = 0
            val nextId = upNextVod?.id ?: return@launch
            _nextVodEvent.tryEmit(nextId)
        }
    }

    fun cancelUpNext() {
        countdownJob?.cancel()
        countdownJob = null
        upNextVod = null
        upNextCountdown = null
        videoEnded = false
    }

    fun playUpNextNow() {
        val nextId = upNextVod?.id ?: return
        countdownJob?.cancel()
        countdownJob = null
        upNextVod = null
        upNextCountdown = null
        _nextVodEvent.tryEmit(nextId)
    }

    private fun releasePlayer() {
        trackingJob?.cancel()
        savingJob?.cancel()
        seekJob?.cancel()
        countdownJob?.cancel()
        player?.release()
        player = null
        // Clean up old manifest files
        manifestFiles.forEach { it.delete() }
        manifestFiles.clear()
    }

    fun release() {
        releasePlayer()
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
    resumeTimeMs: Long,
    onBack: () -> Unit,
    onNextVod: (String) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val theme = ChannelThemes.forChannelId(channel)
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(vodId) {
        viewModel.loadAndPlay(context, channel, vodId, resumeTimeMs) { player ->
            exoPlayer = player
        }
    }

    // #15 - collect next-VOD events from SharedFlow (safe navigation)
    LaunchedEffect(Unit) {
        viewModel.nextVodEvent.collect { nextId ->
            onNextVod(nextId)
        }
    }

    // Auto-advance on video end
    LaunchedEffect(viewModel.videoEnded) {
        if (viewModel.videoEnded) {
            viewModel.startUpNextCountdown()
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
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (viewModel.upNextVod != null) {
                            viewModel.playUpNextNow()
                        } else {
                            viewModel.togglePlayPause(exoPlayer)
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> {
                        val now = System.currentTimeMillis()
                        val isRepeat = event.nativeKeyEvent.repeatCount > 0
                        if (!isRepeat && viewModel.chapters.isNotEmpty() && now - viewModel.lastLeftTapTime < 400) {
                            viewModel.seekToPrevChapter(exoPlayer)
                            viewModel.lastLeftTapTime = 0L
                        } else {
                            viewModel.seekBy(exoPlayer, -1)
                            if (!isRepeat) viewModel.lastLeftTapTime = now
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        val now = System.currentTimeMillis()
                        val isRepeat = event.nativeKeyEvent.repeatCount > 0
                        if (!isRepeat && viewModel.chapters.isNotEmpty() && now - viewModel.lastRightTapTime < 400) {
                            viewModel.seekToNextChapter(exoPlayer)
                            viewModel.lastRightTapTime = 0L
                        } else {
                            viewModel.seekBy(exoPlayer, 1)
                            if (!isRepeat) viewModel.lastRightTapTime = now
                        }
                        true
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        if (viewModel.upNextVod != null) {
                            viewModel.cancelUpNext()
                        } else {
                            onBack()
                        }
                        true
                    }
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
                    Text("Loading tape...", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }
        }

        // Error
        viewModel.error?.let { err ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(err, color = theme.error, fontSize = 20.sp)
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

        // Seek indicator with chapter thumbnail preview
        viewModel.pendingSeekMs?.let { targetMs ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Chapter thumbnail preview
                    val nearestChapter = viewModel.chapters.lastOrNull { it.startTimeMs <= targetMs }
                    nearestChapter?.previewUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = nearestChapter.title,
                            modifier = Modifier
                                .width(160.dp)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            nearestChapter.title,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }
                    // Time display
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

                    // Chapter-aware progress bar
                    val barHeight = 4.dp
                    val chapterMarkers = viewModel.chapters
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .clip(RoundedCornerShape(2.dp))
                    ) {
                        val w = size.width
                        val h = size.height
                        // Track
                        drawRect(Color.White.copy(alpha = 0.3f))
                        // Played
                        drawRect(theme.primary, size = Size(w * progress, h))
                        // Chapter ticks
                        if (viewModel.durationMs > 0) {
                            for (ch in chapterMarkers) {
                                val x = (ch.startTimeMs.toFloat() / viewModel.durationMs) * w
                                drawRect(
                                    Color.White.copy(alpha = 0.6f),
                                    topLeft = Offset(x - 1f, 0f),
                                    size = Size(2f, h)
                                )
                            }
                        }
                    }

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

                    // Current chapter name
                    viewModel.currentChapter?.let { chapter ->
                        Text(
                            chapter.title,
                            fontSize = 13.sp,
                            color = theme.primary.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Up Next overlay - shown when video ends and a next VOD is found
        if (viewModel.videoEnded) {
            viewModel.upNextVod?.let { nextVod ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Column(
                        modifier = Modifier
                            .width(320.dp)
                            .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            "Up Next",
                            fontSize = 13.sp,
                            color = theme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        AsyncImage(
                            model = nextVod.thumbnail,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            nextVod.title,
                            fontSize = 14.sp,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        nextVod.series?.let {
                            Text(
                                "Part ${it.part}",
                                fontSize = 12.sp,
                                color = theme.primary.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Playing in ${viewModel.upNextCountdown}s",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                "OK to play now",
                                fontSize = 13.sp,
                                color = theme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
