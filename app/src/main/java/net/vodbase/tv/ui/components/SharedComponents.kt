package net.vodbase.tv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import net.vodbase.tv.data.model.Vod
import net.vodbase.tv.ui.theme.AnimationConstants
import net.vodbase.tv.ui.theme.ChannelTheme
import net.vodbase.tv.ui.theme.DeviceUiProfile
import net.vodbase.tv.ui.theme.LocalAppDimensions

/**
 * A themed button with animated focus ring, used across Detail, Shuffle, and Menu screens.
 */
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    isBright: Boolean,
    theme: ChannelTheme,
    enabled: Boolean = true,
    autoFocus: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "btnBorder"
    )

    val focusRequester = remember { FocusRequester() }
    if (autoFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) AnimationConstants.BUTTON_SCALE_FOCUSED else 1.0f,
        animationSpec = tween(durationMillis = AnimationConstants.FOCUS_DURATION_MS, easing = FastOutSlowInEasing),
        label = "btnScale"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            isFocused && isBright -> theme.primary
            isFocused -> theme.primary.copy(alpha = 0.25f)
            isBright -> theme.primary.copy(alpha = 0.8f)
            else -> theme.surface
        },
        animationSpec = tween(durationMillis = AnimationConstants.COLOR_DURATION_MS),
        label = "btnBg"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            isFocused && isBright -> theme.background
            isFocused -> Color.White
            isBright -> theme.background
            else -> theme.onSurface.copy(alpha = 0.7f)
        },
        animationSpec = tween(durationMillis = AnimationConstants.COLOR_DURATION_MS),
        label = "btnText"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(theme.shape)
            .background(bgColor)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = theme.focusRing.copy(alpha = borderAlpha),
                shape = theme.shape
            )
            .then(if (autoFocus) Modifier.focusRequester(focusRequester) else Modifier)
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() }
                else Modifier.focusable(interactionSource = interactionSource)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = if (!enabled) textColor.copy(alpha = 0.5f) else textColor
        )
    }
}

/**
 * A small pill-shaped badge for displaying VOD metadata (duration, era, game content).
 */
@Composable
fun MetaBadge(text: String, theme: ChannelTheme) {
    Box(
        modifier = Modifier
            .background(theme.surface, theme.shape)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 11.sp, color = theme.onSurface.copy(alpha = 0.6f))
    }
}

/**
 * Shared VOD detail layout: thumbnail + title + meta badges + series info.
 * Used by DetailScreen and ShuffleScreen to eliminate duplicated layout code.
 *
 * @param vod             The VOD to display.
 * @param theme           The channel's theme.
 * @param label           Optional label shown above the title (e.g. "Shuffle Pick").
 * @param actions         Slot for action buttons rendered below the metadata.
 */
@Composable
fun VodDetailCard(
    vod: Vod,
    theme: ChannelTheme,
    label: String? = null,
    actions: @Composable ColumnScope.() -> Unit
) {
    val dims = LocalAppDimensions.current
    val isThor = dims.profile == DeviceUiProfile.THOR_BOTTOM
    val arrangement = if (dims.detailHPad < 24.dp) 12.dp else 24.dp

    if (isThor) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(
                model = vod.thumbnail,
                contentDescription = vod.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(theme.shape),
                contentScale = ContentScale.Crop
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (label != null) {
                    Text(
                        label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.primary.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    vod.title,
                    fontSize = dims.detailTitleFontSp.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = (dims.detailTitleFontSp * 1.15f).sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetaBadge(vod.duration, theme)
                    MetaBadge(vod.era, theme)
                }

                vod.series?.let { series ->
                    Text(
                        "Part ${series.part}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.primary
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                actions()
            }
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(arrangement),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        AsyncImage(
            model = vod.thumbnail,
            contentDescription = vod.title,
            modifier = Modifier
                .weight(1.4f)
                .aspectRatio(16f / 9f)
                .clip(theme.shape),
            contentScale = ContentScale.Crop
        )

        // Metadata + buttons
        Column(
            modifier = Modifier.weight(0.6f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Optional label (e.g. "Shuffle Pick")
            if (label != null) {
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primary.copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )
            }

            Text(
                vod.title,
                fontSize = dims.detailTitleFontSp.sp,
                fontWeight = FontWeight.Bold,
                color = theme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = (dims.detailTitleFontSp * 1.22f).sp
            )

            // Meta row
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetaBadge(vod.duration, theme)
                MetaBadge(vod.era, theme)
                vod.gameContent?.let { MetaBadge(it, theme) }
            }

            vod.formattedDate?.let { date ->
                Text(
                    date,
                    fontSize = 12.sp,
                    color = theme.onSurface.copy(alpha = 0.4f)
                )
            }

            vod.series?.let { series ->
                Text(
                    "${series.name} - Part ${series.part}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = theme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action buttons slot (no wrapper Column - avoids double-press focus trap)
            actions()
        }
    }
}
