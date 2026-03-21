package net.vodbase.tv.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import android.view.KeyEvent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import net.vodbase.tv.data.model.Channel
import net.vodbase.tv.ui.theme.AnimationConstants
import net.vodbase.tv.ui.theme.ChannelThemes
import net.vodbase.tv.ui.theme.LocalAppDimensions

@Composable
fun HomeScreen(
    onChannelSelected: (String) -> Unit,
    onSearch: (() -> Unit)? = null
) {
    val dims = LocalAppDimensions.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                    event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_SEARCH &&
                    onSearch != null
                ) {
                    onSearch()
                    true
                } else {
                    false
                }
            }
            .padding(horizontal = dims.homePad, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (dims.homePad < 24.dp) 8.dp else 14.dp)
        ) {
            // Logo with gradient
            Text(
                "VODBASE",
                fontSize = dims.homeLogoFontSp.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF4444),
                            Color(0xFFFF6666),
                            Color.White
                        )
                    )
                )
            )

            // Channel grid - 2x2
            Column(
                verticalArrangement = Arrangement.spacedBy(if (dims.homePad < 24.dp) 6.dp else 10.dp),
                modifier = Modifier.widthIn(max = dims.homeGridMaxWidth)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ChannelCard(Channel.JERMA, Modifier.weight(1f)) { onChannelSelected(it.id) }
                    ChannelCard(Channel.SIPS, Modifier.weight(1f)) { onChannelSelected(it.id) }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ChannelCard(Channel.NL, Modifier.weight(1f)) { onChannelSelected(it.id) }
                    ChannelCard(Channel.MOONMOON, Modifier.weight(1f)) { onChannelSelected(it.id) }
                }
            }

            if (onSearch != null) {
                Text(
                    "Press Search to find VODs",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.25f)
                )
            }
        }
    }
}

@Composable
fun ChannelCard(channel: Channel, modifier: Modifier = Modifier, onClick: (Channel) -> Unit) {
    val theme = ChannelThemes.forChannel(channel)
    val dims = LocalAppDimensions.current
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) AnimationConstants.CHANNEL_SCALE_FOCUSED else 1f,
        animationSpec = tween(AnimationConstants.FOCUS_DURATION_MS, easing = AnimationConstants.FOCUS_EASING),
        label = "channelScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) theme.focusRing else theme.primary.copy(alpha = 0.15f),
        animationSpec = tween(AnimationConstants.COLOR_DURATION_MS),
        label = "channelBorder"
    )
    val avatarRingColor by animateColorAsState(
        targetValue = if (isFocused) theme.primary else theme.primary.copy(alpha = 0.5f),
        animationSpec = tween(AnimationConstants.COLOR_DURATION_MS),
        label = "avatarRing"
    )
    val nameColor by animateColorAsState(
        targetValue = if (isFocused) theme.primary else theme.primary.copy(alpha = 0.8f),
        animationSpec = tween(AnimationConstants.COLOR_DURATION_MS),
        label = "nameColor"
    )

    Box(
        modifier = modifier
            .height(dims.channelCardHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(theme.shape)
            .background(
                Brush.horizontalGradient(
                    colors = if (isFocused)
                        listOf(theme.primary.copy(alpha = 0.1f), theme.surface, theme.background)
                    else
                        listOf(theme.surface, theme.background)
                )
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = theme.shape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick(channel) }
            .padding(if (dims.channelCardHeight < 140.dp) 10.dp else 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (dims.channelCardHeight < 140.dp) 8.dp else 14.dp)
        ) {
            // Avatar with colored ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(dims.channelAvatarSize)
                    .border(2.dp, avatarRingColor, CircleShape)
                    .padding(3.dp)
            ) {
                AsyncImage(
                    model = channel.avatarUrl,
                    contentDescription = channel.displayName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            // Text content
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    channel.displayName,
                    fontSize = dims.channelNameFontSp.sp,
                    fontWeight = FontWeight.Bold,
                    color = nameColor
                )

                // Stats row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${channel.approxVods} VODs",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.primary.copy(alpha = if (isFocused) 0.7f else 0.4f)
                    )
                    Text(
                        "${channel.years} Years",
                        fontSize = 13.sp,
                        color = theme.onSurface.copy(alpha = 0.3f)
                    )
                }

                Text(
                    channel.tagline,
                    fontSize = 13.sp,
                    color = theme.onSurface.copy(alpha = if (isFocused) 0.5f else 0.25f),
                    maxLines = 1
                )

                // Enter hint (crossfades in on focus)
                Crossfade(targetState = isFocused, label = "enterHint") { focused ->
                    if (focused) {
                        Text(
                            "${channel.enterText} →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primary.copy(alpha = 0.8f)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
