package net.vodbase.tv.ui.browse

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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

    androidx.tv.material3.Card(
        onClick = onClick,
        modifier = modifier
            .width(280.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.border(
                    2.dp,
                    theme.focusRing,
                    RoundedCornerShape(8.dp)
                ) else Modifier
            ),
        shape = androidx.tv.material3.CardDefaults.shape(RoundedCornerShape(8.dp))
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
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                    contentScale = ContentScale.Crop
                )

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(
                            Color.Black.copy(alpha = 0.8f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        vod.duration,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }

                // Watched indicator
                if (isWatched) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(
                                Color(0xFF1A5E1A).copy(alpha = 0.9f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Watched", fontSize = 10.sp, color = Color(0xFF90EE90))
                    }
                }
            }

            // Title
            Text(
                vod.title,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                fontSize = 13.sp,
                color = theme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
