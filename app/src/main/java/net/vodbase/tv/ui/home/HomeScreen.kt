package net.vodbase.tv.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import net.vodbase.tv.data.model.Channel
import net.vodbase.tv.ui.theme.ChannelThemes

@Composable
fun HomeScreen(onChannelSelected: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(horizontal = 48.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "VOD",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFEF4444)
                )
                Text(
                    "BASE",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            // Channel grid - 2x2
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.widthIn(max = 700.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ChannelCard(Channel.JERMA, Modifier.weight(1f)) { onChannelSelected(it.id) }
                    ChannelCard(Channel.SIPS, Modifier.weight(1f)) { onChannelSelected(it.id) }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ChannelCard(Channel.NL, Modifier.weight(1f)) { onChannelSelected(it.id) }
                    ChannelCard(Channel.MOONMOON, Modifier.weight(1f)) { onChannelSelected(it.id) }
                }
            }
        }
    }
}

@Composable
fun ChannelCard(channel: Channel, modifier: Modifier = Modifier, onClick: (Channel) -> Unit) {
    val theme = ChannelThemes.forChannel(channel)
    var isFocused by remember { mutableStateOf(false) }
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "borderAlpha"
    )

    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.horizontalGradient(
                    colors = if (isFocused)
                        listOf(theme.primary.copy(alpha = 0.15f), theme.surface, theme.background)
                    else
                        listOf(theme.surface, theme.background)
                )
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = theme.focusRing.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick(channel) }
            .padding(20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                channel.displayName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFocused) theme.primary else theme.primary.copy(alpha = 0.7f)
            )
            Text(
                channel.theaterName,
                fontSize = 13.sp,
                color = theme.onSurface.copy(alpha = if (isFocused) 0.6f else 0.3f)
            )
        }
    }
}
