package net.vodbase.tv.ui.search

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import dagger.hilt.android.lifecycle.HiltViewModel
import net.vodbase.tv.data.model.Vod
import net.vodbase.tv.data.repository.VodRepository
import net.vodbase.tv.ui.browse.VodCard
import net.vodbase.tv.ui.theme.ChannelThemes
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val vodRepository: VodRepository
) : ViewModel() {
    var query by mutableStateOf("")
        private set
    var results by mutableStateOf<List<Vod>>(emptyList())
        private set

    fun updateQuery(channelId: String, newQuery: String) {
        query = newQuery
        results = if (newQuery.length >= 2) {
            vodRepository.searchVods(channelId, newQuery).take(50)
        } else {
            emptyList()
        }
    }
}

@Composable
fun SearchScreen(
    channel: String,
    initialQuery: String = "",
    onVodSelected: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val theme = ChannelThemes.forChannelId(channel)
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Auto-trigger search for voice queries
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            viewModel.updateQuery(channel, initialQuery)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                    event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK
                ) {
                    onBack()
                    true
                } else {
                    false
                }
            }
            .padding(48.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(
                "Search",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = theme.primary
            )

            // Search input
            BasicTextField(
                value = viewModel.query,
                onValueChange = { viewModel.updateQuery(channel, it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.surface, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(16.dp)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                cursorBrush = SolidColor(theme.primary),
                decorationBox = { innerTextField ->
                    if (viewModel.query.isEmpty()) {
                        Text("Type to search VODs...", color = Color(0xFF7A7A9A), fontSize = 18.sp)
                    }
                    innerTextField()
                }
            )

            // Results count
            if (viewModel.query.length >= 2) {
                Text(
                    "${viewModel.results.size} results",
                    fontSize = 14.sp,
                    color = theme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Results grid - chunked into rows of 5
            if (viewModel.results.isNotEmpty()) {
                val rows = viewModel.results.chunked(5)
                TvLazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(rows.size, key = { rows[it].first().id }) { rowIndex ->
                        val rowVods = rows[rowIndex]
                        TvLazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(rowVods, key = { it.id }) { vod ->
                                VodCard(
                                    vod = vod,
                                    theme = theme,
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
