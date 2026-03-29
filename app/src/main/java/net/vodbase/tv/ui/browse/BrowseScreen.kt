package net.vodbase.tv.ui.browse

import android.view.KeyEvent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import net.vodbase.tv.ui.components.BrowseSkeletonScreen
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.vodbase.tv.data.model.Channel
import net.vodbase.tv.data.model.Vod
import net.vodbase.tv.data.repository.ProgressRepository
import net.vodbase.tv.data.repository.VodRepository
import net.vodbase.tv.ui.theme.AnimationConstants
import net.vodbase.tv.ui.theme.ChannelTheme
import net.vodbase.tv.ui.theme.ChannelThemes
import net.vodbase.tv.ui.theme.DeviceUiProfile
import net.vodbase.tv.ui.theme.LocalAppDimensions
import javax.inject.Inject

data class VodRow(val title: String, val vods: List<Vod>)

data class ContinueWatchingInfo(
    val vod: Vod,
    val currentTime: Double,
    val duration: Double
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val vodRepository: VodRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    var rows by mutableStateOf<List<VodRow>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var watchedIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var totalVods by mutableStateOf(0)
        private set
    var continueWatching by mutableStateOf<ContinueWatchingInfo?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun loadChannel(channelId: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val vods = vodRepository.getVods(channelId)
                totalVods = vods.size
                watchedIds = progressRepository.getWatchedIds(channelId)

                // Check for resume position
                val resumeInfo = progressRepository.getResumeInfo(channelId)
                if (resumeInfo != null) {
                    val matchingVod = vods.find { it.id == resumeInfo.vodId }
                    if (matchingVod != null) {
                        continueWatching = ContinueWatchingInfo(
                            vod = matchingVod,
                            currentTime = resumeInfo.currentTime,
                            duration = resumeInfo.duration
                        )
                    }
                }

                val eras = listOf("Latest", "Peak Era", "Golden Era", "Classic Era")
                val rowList = mutableListOf<VodRow>()

                // Recently Added
                rowList.add(VodRow("Recently Added", vods.take(20)))

                // Series rows - group by series name (already cleaned by autoDetectSeries)
                val seriesGroups = vods
                    .filter { it.series != null }
                    .groupBy { it.series!!.name }
                    .filter { it.value.size >= 3 }
                    .mapValues { (_, v) -> v.sortedBy { it.series!!.part } }
                    .entries
                    .sortedByDescending { it.value.size }
                    .take(10)

                for ((seriesName, seriesVods) in seriesGroups) {
                    // Find Part 1: VOD with no series tag whose title matches or starts with
                    // the series name. Handles cases like:
                    //   Series: "Jerma Streams - Sorcery!" → Part 1: "Jerma Streams - Sorcery! (2nd Playthrough)"
                    //   Series: "Jerma Streams - House Flipper 2" → Part 1: "Jerma Streams - House Flipper 2"
                    val firstPart = seriesVods.minOf { it.series!!.part }
                    val part1Candidates = if (firstPart > 1) {
                        vods.filter { v ->
                            v.series == null && (
                                v.title.trim().equals(seriesName.trim(), ignoreCase = true) ||
                                v.title.trim().startsWith(seriesName.trim(), ignoreCase = true)
                            )
                        }
                    } else emptyList()
                    // Only use Part 1 candidate if we're actually missing Part 1
                    val part1 = part1Candidates.firstOrNull()
                    val allParts = buildList {
                        part1?.let { add(it) }
                        addAll(seriesVods)
                    }

                    val displayName = seriesName
                        .replace(Regex("^\\w+\\s+(Streams|Re-stream|Highlights)\\s*-\\s*"), "")
                        .replace(Regex("^\\w+\\s+Plays\\s+"), "")
                        .ifEmpty { seriesName }
                    rowList.add(VodRow("$displayName (${allParts.size} parts)", allParts))
                }

                // Per era
                for (era in eras) {
                    val eraVods = vods.filter { it.era == era }
                    if (eraVods.isNotEmpty()) {
                        rowList.add(VodRow("$era (${eraVods.size})", eraVods.take(30)))
                    }
                }

                rows = rowList
            } catch (e: Exception) {
                error = "Failed to load VODs: ${e.message}"
            }
            isLoading = false
        }
    }
}

@Composable
fun BrowseScreen(
    channel: String,
    onVodSelected: (String) -> Unit,
    onSearch: () -> Unit,
    onShuffle: () -> Unit,
    onBack: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel()
) {
    val theme = ChannelThemes.forChannelId(channel)
    val dims = LocalAppDimensions.current

    LaunchedEffect(channel) {
        viewModel.loadChannel(channel)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        theme.primary.copy(alpha = 0.04f),
                        theme.background,
                        theme.background
                    )
                )
            )
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_BACK -> { onBack(); true }
                    KeyEvent.KEYCODE_SEARCH, KeyEvent.KEYCODE_MENU -> { onSearch(); true }
                    else -> false
                }
            }
    ) {
        if (viewModel.isLoading) {
            BrowseSkeletonScreen(theme)
        } else if (viewModel.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        viewModel.error ?: "Unknown error",
                        color = theme.error,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Press SELECT to retry",
                        color = theme.onSurface.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = dims.rowSpacing),
                verticalArrangement = Arrangement.spacedBy(dims.rowSpacing)
            ) {
                // Header
                item(key = "header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = dims.screenHPad,
                                vertical = if (dims.profile == DeviceUiProfile.THOR_BOTTOM) 0.dp else 4.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            if (dims.profile == DeviceUiProfile.THOR_BOTTOM) 6.dp else 10.dp
                        )
                    ) {
                        Text(
                            theme.channelName,
                            fontSize = dims.headerFontSp.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primary
                        )
                        if (viewModel.totalVods > 0) {
                            Text(
                                "${viewModel.totalVods} VODs",
                                fontSize = if (dims.profile == DeviceUiProfile.THOR_BOTTOM) 10.sp else 13.sp,
                                color = theme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        // Shuffle button
                        val shuffleInteraction = remember { MutableInteractionSource() }
                        val shuffleFocused by shuffleInteraction.collectIsFocusedAsState()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (shuffleFocused) theme.primary
                                    else theme.primary.copy(alpha = 0.15f)
                                )
                                .clickable(
                                    interactionSource = shuffleInteraction,
                                    indication = null
                                ) { onShuffle() }
                                .padding(
                                    horizontal = if (dims.profile == DeviceUiProfile.THOR_BOTTOM) 8.dp else 14.dp,
                                    vertical = if (dims.profile == DeviceUiProfile.THOR_BOTTOM) 4.dp else 6.dp
                                )
                        ) {
                            Text(
                                "Shuffle",
                                fontSize = if (dims.profile == DeviceUiProfile.THOR_BOTTOM) 10.sp else 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (shuffleFocused) theme.background else theme.primary
                            )
                        }
                    }
                }

                // Continue Watching hero card
                viewModel.continueWatching?.let { cw ->
                    item(key = "continue_watching") {
                        ContinueWatchingHero(
                            info = cw,
                            theme = theme,
                            onResume = { onVodSelected(cw.vod.id) }
                        )
                    }
                }

                // VOD rows
                items(viewModel.rows.size, key = { viewModel.rows[it].title }) { index ->
                    val row = viewModel.rows[index]
                    Column() {
                        Text(
                            row.title,
                            modifier = Modifier.padding(
                                start = dims.screenHPad,
                                bottom = if (dims.profile == DeviceUiProfile.THOR_BOTTOM) 4.dp else 8.dp
                            ),
                            fontSize = dims.rowLabelFontSp.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.onSurface.copy(alpha = 0.9f)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = dims.screenHPad),
                            horizontalArrangement = Arrangement.spacedBy(
                                if (dims.profile == DeviceUiProfile.THOR_BOTTOM) 8.dp else 12.dp
                            )
                        ) {
                            items(row.vods, key = { it.id }) { vod ->
                                VodCard(
                                    vod = vod,
                                    theme = theme,
                                    isWatched = viewModel.watchedIds.contains(vod.id),
                                    onClick = { onVodSelected(vod.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingHero(
    info: ContinueWatchingInfo,
    theme: ChannelTheme,
    onResume: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = AnimationConstants.FOCUS_DURATION_MS),
        label = "heroBorder"
    )
    val scale by animateFloatAsState(
        targetValue = if (isFocused) AnimationConstants.HERO_SCALE_FOCUSED else AnimationConstants.CARD_SCALE_UNFOCUSED,
        animationSpec = tween(durationMillis = AnimationConstants.FOCUS_DURATION_MS, easing = AnimationConstants.FOCUS_EASING),
        label = "heroScale"
    )
    val titleColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else theme.onSurface,
        animationSpec = tween(durationMillis = AnimationConstants.COLOR_DURATION_MS),
        label = "heroTitleColor"
    )

    val dims = LocalAppDimensions.current
    val isThor = dims.profile == DeviceUiProfile.THOR_BOTTOM
    val progressFraction = if (info.duration > 0) (info.currentTime / info.duration).toFloat().coerceIn(0f, 1f) else 0f
    val mins = (info.currentTime / 60).toInt()
    val secs = (info.currentTime % 60).toInt()
    val totalMins = (info.duration / 60).toInt()

    Column(modifier = Modifier.padding(horizontal = dims.screenHPad)) {
        Text(
            "Continue Watching",
            fontSize = dims.rowLabelFontSp.sp,
            fontWeight = FontWeight.SemiBold,
            color = theme.primary,
            modifier = Modifier.padding(bottom = if (isThor) 4.dp else 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isThor) Modifier else Modifier.height(dims.heroHeight))
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(theme.shape)
                .background(theme.surface)
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) theme.focusRing.copy(alpha = borderAlpha)
                    else theme.primary.copy(alpha = 0.15f),
                    shape = theme.shape
                )
                .onFocusChanged { isFocused = it.isFocused }
                .clickable { onResume() }
        ) {
            if (isThor) {
                // Thor: compact horizontal layout — thumbnail left, title + resume right
                Row(
                    modifier = Modifier.fillMaxWidth().padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small thumbnail with progress bar
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .aspectRatio(16f / 9f)
                            .clip(theme.shape)
                    ) {
                        AsyncImage(
                            model = info.vod.thumbnail,
                            contentDescription = info.vod.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressFraction)
                                    .background(theme.primary)
                            )
                        }
                    }

                    // Title + time — fills remaining space
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            info.vod.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 13.sp
                        )
                        Text(
                            "${mins}:${"%02d".format(secs)} / ${totalMins} min",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = theme.primary
                        )
                    }

                    // Resume badge
                    Crossfade(
                        targetState = isFocused,
                        animationSpec = tween(durationMillis = AnimationConstants.COLOR_DURATION_MS),
                        label = "heroBadgeCrossfade"
                    ) { focused ->
                        Box(
                            modifier = Modifier
                                .background(theme.primary.copy(alpha = if (focused) 0.3f else 0.15f), theme.shape)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                if (focused) "▶" else info.vod.era,
                                fontSize = 10.sp,
                                fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
                                color = if (focused) theme.primary else theme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                // TV / Handheld: side-by-side Row layout
                Row(modifier = Modifier.fillMaxSize()) {
                    // Large thumbnail
                    Box(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                        AsyncImage(
                            model = info.vod.thumbnail,
                            contentDescription = info.vod.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Progress bar overlay at bottom of thumbnail
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressFraction)
                                    .background(theme.primary)
                            )
                        }
                    }

                    // Info panel
                    Column(
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxHeight()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            info.vod.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Resume time
                        Text(
                            "${mins}:${"%02d".format(secs)} / ${totalMins} min",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = theme.primary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Meta badges
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Crossfade(
                                targetState = isFocused,
                                animationSpec = tween(durationMillis = AnimationConstants.COLOR_DURATION_MS),
                                label = "heroBadgeCrossfade"
                            ) { focused ->
                                Box(
                                    modifier = Modifier
                                        .background(theme.primary.copy(alpha = 0.15f), theme.shape)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        if (focused) "Resume" else info.vod.era,
                                        fontSize = 11.sp,
                                        fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
                                        color = if (focused) theme.primary else theme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            if (!isFocused) {
                                info.vod.gameContent?.let {
                                    Box(
                                        modifier = Modifier
                                            .background(theme.surface, theme.shape)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(it, fontSize = 11.sp, color = theme.onSurface.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
