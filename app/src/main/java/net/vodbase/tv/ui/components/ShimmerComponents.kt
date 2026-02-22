package net.vodbase.tv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import net.vodbase.tv.ui.theme.AnimationConstants
import net.vodbase.tv.ui.theme.ChannelTheme

/**
 * Core shimmer building block. Animates a horizontal gradient sweep to convey loading state.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AnimationConstants.SHIMMER_DURATION_MS,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val brush = Brush.horizontalGradient(
        colors = AnimationConstants.SHIMMER_COLORS,
        startX = offset * 1000f,
        endX = (offset + 1f) * 1000f
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

/**
 * Skeleton screen mirroring BrowseScreen layout during data load.
 * Shows a header bar and three rows of card skeletons.
 */
@Composable
fun BrowseSkeletonScreen(theme: ChannelTheme) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp, horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header shimmer
        ShimmerBox(
            modifier = Modifier
                .width(200.dp)
                .height(28.dp),
            shape = theme.shape
        )

        // Three skeleton rows
        repeat(3) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row title shimmer
                ShimmerBox(
                    modifier = Modifier
                        .width(160.dp)
                        .height(18.dp),
                    shape = theme.shape
                )

                // Row of 5 card skeletons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(5) {
                        Column {
                            // Thumbnail placeholder
                            ShimmerBox(
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(113.dp),
                                shape = theme.shape
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Title bar placeholder
                            ShimmerBox(
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(28.dp),
                                shape = theme.shape
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Skeleton screen mirroring VodDetailCard layout during data load.
 * Used by DetailScreen and ShuffleScreen.
 */
@Composable
fun DetailSkeletonScreen(theme: ChannelTheme) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail placeholder (weight 1.4f, 16:9)
        ShimmerBox(
            modifier = Modifier
                .weight(1.4f)
                .aspectRatio(16f / 9f),
            shape = theme.shape
        )

        // Text + button column (weight 0.6f)
        Column(
            modifier = Modifier.weight(0.6f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title shimmer (80% width)
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(22.dp),
                shape = theme.shape
            )
            // Subtitle shimmer (50% width)
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(18.dp),
                shape = theme.shape
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Button placeholder 1
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = theme.shape
            )
            // Button placeholder 2
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = theme.shape
            )
        }
    }
}
