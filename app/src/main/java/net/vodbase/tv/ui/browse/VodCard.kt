package net.vodbase.tv.ui.browse

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "vodCardScale"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "vodBorderAlpha"
    )

    androidx.tv.material3.Card(
        onClick = onClick,
        modifier = modifier
            .width(280.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .scale(scale)
            .border(
                width = 2.dp,
                color = theme.focusRing.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = androidx.tv.material3.CardDefaults.shape(RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.background(theme.surface)
        ) {
            // Thumbnail
            Box {
                AsyncImage(
                    model = vod.thumbnail,
                    contentDescription = vod.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(
                            Color.Black.copy(alpha = 0.85f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        vod.duration,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                // Watched indicator
                if (isWatched) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(
                                Color(0xFF1A5E1A).copy(alpha = 0.9f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("Watched", fontSize = 11.sp, color = Color(0xFF90EE90))
                    }
                }
            }

            // Title
            Text(
                vod.title,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                fontSize = 14.sp,
                fontWeight = if (isFocused) FontWeight.Medium else FontWeight.Normal,
                color = if (isFocused) Color.White else theme.onSurface.copy(alpha = 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
