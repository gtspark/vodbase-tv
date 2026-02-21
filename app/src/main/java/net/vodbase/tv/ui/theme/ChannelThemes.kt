package net.vodbase.tv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import net.vodbase.tv.data.model.Channel

data class ChannelTheme(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val focusRing: Color,
    val channelName: String
)

object ChannelThemes {
    val Jerma = ChannelTheme(
        primary = Color(0xFF00FF41),
        secondary = Color(0xFFFF1744),
        background = Color(0xFF0A0A0A),
        surface = Color(0xFF1A1A2E),
        onSurface = Color.White,
        focusRing = Color(0xFF00FF41),
        channelName = "Jerma985"
    )

    val Sips = ChannelTheme(
        primary = Color(0xFF00D9FF),
        secondary = Color(0xFFFF6EC7),
        background = Color(0xFF001A2E),
        surface = Color(0xFF0A1A2E),
        onSurface = Color.White,
        focusRing = Color(0xFF00D9FF),
        channelName = "Sips"
    )

    val NL = ChannelTheme(
        primary = Color(0xFFFF6B35),
        secondary = Color(0xFFF7931E),
        background = Color(0xFF1A0F00),
        surface = Color(0xFF2A1F10),
        onSurface = Color.White,
        focusRing = Color(0xFFFF6B35),
        channelName = "Northernlion"
    )

    val Moon = ChannelTheme(
        primary = Color(0xFFE8E8E8),
        secondary = Color(0xFF666666),
        background = Color(0xFF000000),
        surface = Color(0xFF1A1A1A),
        onSurface = Color.White,
        focusRing = Color(0xFFE8E8E8),
        channelName = "MOONMOON"
    )

    fun forChannel(channel: Channel): ChannelTheme = when (channel) {
        Channel.JERMA -> Jerma
        Channel.SIPS -> Sips
        Channel.NL -> NL
        Channel.MOONMOON -> Moon
    }

    fun forChannelId(id: String): ChannelTheme = forChannel(Channel.fromId(id))
}
