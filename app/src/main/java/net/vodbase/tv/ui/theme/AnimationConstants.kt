package net.vodbase.tv.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.Color

object AnimationConstants {
    // Focus scale factors
    const val CARD_SCALE_FOCUSED = 1.08f
    const val CARD_SCALE_UNFOCUSED = 1.0f
    const val CHANNEL_SCALE_FOCUSED = 1.06f
    const val BUTTON_SCALE_FOCUSED = 1.04f
    const val HERO_SCALE_FOCUSED = 1.03f

    // Durations (ms)
    const val FOCUS_DURATION_MS = 200
    const val COLOR_DURATION_MS = 200
    const val NAV_TRANSITION_MS = 300
    const val PLAYER_TRANSITION_MS = 100
    const val SHIMMER_DURATION_MS = 1200

    // Easing
    val FOCUS_EASING = FastOutSlowInEasing

    // Shimmer colors (dark blue-gray)
    val SHIMMER_COLORS = listOf(
        Color(0xFF1A1A2E),
        Color(0xFF2A2A4A),
        Color(0xFF1A1A2E)
    )
}
