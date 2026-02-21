package net.vodbase.tv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import net.vodbase.tv.data.model.Channel
import net.vodbase.tv.ui.theme.ChannelThemes

@Composable
fun HomeScreen(onChannelSelected: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Logo
            Row {
                Text(
                    "VOD",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
                Text(
                    "BASE",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Channel grid
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.widthIn(max = 800.dp)
            ) {
                items(Channel.entries.toList()) { channel ->
                    ChannelCard(
                        channel = channel,
                        onClick = { onChannelSelected(channel.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelCard(channel: Channel, onClick: () -> Unit) {
    val theme = ChannelThemes.forChannel(channel)
    var isFocused by remember { mutableStateOf(false) }

    androidx.tv.material3.Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.border(
                    3.dp,
                    theme.focusRing,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        shape = androidx.tv.material3.CardDefaults.shape(RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(theme.background, theme.surface)
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    channel.displayName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primary
                )
                Text(
                    channel.theaterName,
                    fontSize = 14.sp,
                    color = theme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
