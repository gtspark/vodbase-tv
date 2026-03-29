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
import android.os.Build
import androidx.compose.ui.platform.LocalConfiguration
import net.vodbase.tv.data.model.Channel
import net.vodbase.tv.ui.theme.AnimationConstants
import net.vodbase.tv.ui.theme.ChannelThemes
import net.vodbase.tv.ui.theme.DeviceUiProfile
import net.vodbase.tv.ui.theme.LocalAppDimensions

@Composable
fun HomeScreen(
    onChannelSelected: (String) -> Unit,
    onSearch: (() -> Unit)? = null
) {
    val dims = LocalAppDimensions.current
    val dbgConfig = LocalConfiguration.current
    val dbgInfo = "M:${Build.MODEL} W:${dbgConfig.screenWidthDp} H:${dbgConfig.screenHeightDp} P:${dims.profile}"
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
        if (dims.profile == DeviceUiProfile.THOR_BOTTOM) {
            ThorBottomHomeScreen(
                onChannelSelected = onChannelSelected,
                onSearch = onSearch
            )
        } else {
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

        // DEBUG: remove after confirming detection works
        Text(
            dbgInfo,
            fontSize = 8.sp,
            color = Color.Yellow.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun ThorBottomHomeScreen(
    onChannelSelected: (String) -> Unit,
    onSearch: (() -> Unit)?
) {
    val dims = LocalAppDimensions.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "VODBASE",
            fontSize = dims.homeLogoFontSp.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp,
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ThorChannelPill(Channel.JERMA, Modifier.weight(1f)) { onChannelSelected(it.id) }
            ThorChannelPill(Channel.SIPS, Modifier.weight(1f)) { onChannelSelected(it.id) }
            ThorChannelPill(Channel.NL, Modifier.weight(1f)) { onChannelSelected(it.id) }
            ThorChannelPill(Channel.MOONMOON, Modifier.weight(1f)) { onChannelSelected(it.id) }
        }

        if (onSearch != null) {
            Text(
                "Search button opens VOD search",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
private fun ThorChannelPill(
    channel: Channel,
    modifier: Modifier = Modifier,
    onClick: (Channel) -> Unit
) {
    val theme = ChannelThemes.forChannel(channel)
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) theme.focusRing else theme.primary.copy(alpha = 0.2f),
        animationSpec = tween(AnimationConstants.COLOR_DURATION_MS),
        label = "thorPillBorder"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.22f else 0.1f,
        animationSpec = tween(AnimationConstants.COLOR_DURATION_MS),
        label = "thorPillAlpha"
    )

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(theme.primary.copy(alpha = bgAlpha))
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick(channel) }
            .padding(horizontal = 6.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(theme.primary)
            )
            Text(
                text = channel.displayName.take(6),
                fontSize = 9.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                color = Color.White,
                maxLines = 1
            )
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
