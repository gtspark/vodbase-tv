package net.vodbase.tv.ui.browse

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.vodbase.tv.data.model.Channel
import net.vodbase.tv.data.model.Vod
import net.vodbase.tv.data.repository.ProgressRepository
import net.vodbase.tv.data.repository.VodRepository
import net.vodbase.tv.ui.theme.ChannelThemes
import javax.inject.Inject

data class VodRow(val title: String, val vods: List<Vod>)

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

    fun loadChannel(channelId: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                val vods = vodRepository.getVods(channelId)
                totalVods = vods.size
                watchedIds = progressRepository.getWatchedIds(channelId)

                val eras = listOf("Latest", "Peak Era", "Golden Era", "Classic Era")
                val rowList = mutableListOf<VodRow>()

                // Recently Added
                rowList.add(VodRow("Recently Added", vods.take(20)))

                // Per era
                for (era in eras) {
                    val eraVods = vods.filter { it.era == era }
                    if (eraVods.isNotEmpty()) {
                        rowList.add(VodRow("$era (${eraVods.size})", eraVods.take(30)))
                    }
                }

                rows = rowList
            } catch (e: Exception) {
                // Handle error
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
            .background(theme.background)
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = theme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading VODs...", color = theme.onSurface.copy(alpha = 0.5f), fontSize = 16.sp)
                }
            }
        } else {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            theme.channelName,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primary
                        )
                        if (viewModel.totalVods > 0) {
                            Text(
                                "${viewModel.totalVods} VODs",
                                fontSize = 16.sp,
                                color = theme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                    Text(
                        "Menu = Search",
                        fontSize = 14.sp,
                        color = theme.onSurface.copy(alpha = 0.3f)
                    )
                }

                // Rows
                TvLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    items(viewModel.rows.size) { index ->
                        val row = viewModel.rows[index]
                        Column {
                            Text(
                                row.title,
                                modifier = Modifier.padding(start = 48.dp, bottom = 14.dp),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = theme.onSurface
                            )
                            TvLazyRow(
                                contentPadding = PaddingValues(horizontal = 48.dp),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
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
}
