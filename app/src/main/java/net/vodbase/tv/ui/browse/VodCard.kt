package net.vodbase.tv.ui.browse

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import net.vodbase.tv.data.model.Vod
import net.vodbase.tv.ui.theme.ChannelTheme

@Composable
fun VodCard(
    vod: Vod,
    theme: ChannelTheme,
    isWatched: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "vodBorderAlpha"
    )
    val glowElevation by animateDpAsState(
        targetValue = if (isFocused && theme.glowColor != Color.Transparent) 16.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "vodGlow"
    )

    Box(
        modifier = modifier
            .width(200.dp)
            .shadow(
                elevation = glowElevation,
                shape = theme.shape,
                ambientColor = theme.glowColor.copy(alpha = 0.5f),
                spotColor = theme.glowColor.copy(alpha = 0.5f)
            )
            .clip(theme.shape)
            .background(if (isFocused) theme.primary.copy(alpha = 0.06f) else theme.surface)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = theme.focusRing.copy(alpha = borderAlpha),
                shape = theme.shape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
    ) {
        Column {
            // Thumbnail
            Box {
                AsyncImage(
                    model = vod.thumbnail,
                    contentDescription = vod.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop
                )

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(
                            Color.Black.copy(alpha = 0.85f),
                            theme.shape
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(vod.duration, fontSize = 10.sp, color = Color.White)
                }

                // Watched indicator
                if (isWatched) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                Color(0xFF43B581).copy(alpha = 0.9f),
                                theme.shape
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("Watched", fontSize = 9.sp, color = Color.White)
                    }
                }
            }

            // Title
            Text(
                vod.title,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                fontSize = 12.sp,
                fontWeight = if (isFocused) FontWeight.Medium else FontWeight.Normal,
                color = if (isFocused) Color.White else theme.onSurface.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )
        }
    }
}
