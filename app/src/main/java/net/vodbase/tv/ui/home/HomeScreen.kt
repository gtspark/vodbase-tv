package net.vodbase.tv.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
            verticalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            // Logo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "VOD",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFEF4444)
                )
                Text(
                    "BASE",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Text(
                "Select a channel",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.4f)
            )

            // Channel grid
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.widthIn(max = 900.dp)
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
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "cardScale"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "borderAlpha"
    )

    androidx.tv.material3.Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .scale(scale)
            .border(
                width = 3.dp,
                color = theme.focusRing.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = androidx.tv.material3.CardDefaults.shape(RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(theme.surface, theme.background)
                    )
                )
                .padding(28.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    channel.displayName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) theme.primary else theme.primary.copy(alpha = 0.8f)
                )
                Text(
                    channel.theaterName,
                    fontSize = 16.sp,
                    color = theme.onSurface.copy(alpha = if (isFocused) 0.7f else 0.4f)
                )
            }
        }
    }
}
