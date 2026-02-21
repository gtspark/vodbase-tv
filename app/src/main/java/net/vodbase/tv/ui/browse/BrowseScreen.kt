package net.vodbase.tv.ui.browse

import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.vodbase.tv.data.model.Channel
import net.vodbase.tv.data.model.Vod
import net.vodbase.tv.data.repository.ProgressRepository
import net.vodbase.tv.data.repository.VodRepository
import net.vodbase.tv.ui.theme.ChannelTheme
import net.vodbase.tv.ui.theme.ChannelThemes
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
    onShuffle: () -> Unit = {},
    onBack: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel()
) {
    val theme = ChannelThemes.forChannelId(channel)

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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = theme.primary)
            }
        } else if (viewModel.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        viewModel.error ?: "Unknown error",
                        color = Color(0xFFEF4444),
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
            TvLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            theme.channelName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primary
                        )
                        if (viewModel.totalVods > 0) {
                            Text(
                                "${viewModel.totalVods} VODs",
                                fontSize = 13.sp,
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
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "Shuffle",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (shuffleFocused) theme.background else theme.primary
                            )
                        }
                    }
                }

                // Continue Watching hero card
                viewModel.continueWatching?.let { cw ->
                    item {
                        ContinueWatchingHero(
                            info = cw,
                            theme = theme,
                            onResume = { onVodSelected(cw.vod.id) }
                        )
                    }
                }

                // VOD rows
                items(viewModel.rows.size) { index ->
                    val row = viewModel.rows[index]
                    Column {
                        Text(
                            row.title,
                            modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.onSurface.copy(alpha = 0.9f)
                        )
                        TvLazyRow(
                            contentPadding = PaddingValues(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(row.vods) { vod ->
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
        animationSpec = tween(durationMillis = 200),
        label = "heroBorder"
    )

    val progressFraction = if (info.duration > 0) (info.currentTime / info.duration).toFloat().coerceIn(0f, 1f) else 0f
    val mins = (info.currentTime / 60).toInt()
    val secs = (info.currentTime % 60).toInt()
    val totalMins = (info.duration / 60).toInt()

    Column(modifier = Modifier.padding(horizontal = 32.dp)) {
        Text(
            "Continue Watching",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = theme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
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
                        color = if (isFocused) Color.White else theme.onSurface,
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
                        Box(
                            modifier = Modifier
                                .background(theme.primary.copy(alpha = 0.15f), theme.shape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (isFocused) "Resume" else info.vod.era,
                                fontSize = 11.sp,
                                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
                                color = if (isFocused) theme.primary else theme.onSurface.copy(alpha = 0.6f)
                            )
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
