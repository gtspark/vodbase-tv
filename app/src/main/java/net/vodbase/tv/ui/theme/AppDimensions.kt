package net.vodbase.tv.ui.theme

import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppDimensions(
    val profile: DeviceUiProfile,

    // VodCard
    val vodCardWidth: Dp,
    val vodCardImageSize: Int,

    // HomeScreen
    val channelCardHeight: Dp,
    val channelAvatarSize: Dp,
    val channelNameFontSp: Float,
    val homeLogoFontSp: Float,
    val homePad: Dp,
    val homeGridMaxWidth: Dp,

    // Browse
    val screenHPad: Dp,
    val rowSpacing: Dp,
    val rowLabelFontSp: Float,
    val headerFontSp: Float,

    // Search
    val searchPad: Dp,
    val searchTitleFontSp: Float,
    val searchInputFontSp: Float,
    val searchChunkSize: Int,

    // Detail / Shuffle
    val detailHPad: Dp,
    val detailVPad: Dp,
    val detailTitleFontSp: Float,
    val detailRadialRadius: Float,

    // Player
    val playerHPad: Dp,
    val playerVPad: Dp,
    val playerTitleFontSp: Float,
    val playerTimeFontSp: Float,
    val playerSeekFontSp: Float,
    val playerSeekPreviewWidth: Dp,
    val playerPauseSize: Dp,
    val playerPauseFontSp: Float,
    val playerUpNextWidth: Dp,
    val playerUpNextPad: Dp,

    // Auth
    val authHPad: Dp,
    val authVPad: Dp,
    val authQrSize: Dp,
    val authLogoFontSp: Float,
    val authStatusFontSp: Float,

    // Settings
    val settingsPad: Dp,
    val settingsTitleFontSp: Float,

    // Menu
    val menuPanelWidth: Dp,

    // ContinueWatching hero
    val heroHeight: Dp,
) {
    companion object {
        val TV = AppDimensions(
            profile = DeviceUiProfile.TV,
            vodCardWidth = 200.dp,
            vodCardImageSize = 400,
            channelCardHeight = 170.dp,
            channelAvatarSize = 72.dp,
            channelNameFontSp = 22f,
            homeLogoFontSp = 34f,
            homePad = 40.dp,
            homeGridMaxWidth = 760.dp,
            screenHPad = 32.dp,
            rowSpacing = 16.dp,
            rowLabelFontSp = 16f,
            headerFontSp = 24f,
            searchPad = 48.dp,
            searchTitleFontSp = 28f,
            searchInputFontSp = 18f,
            searchChunkSize = 5,
            detailHPad = 40.dp,
            detailVPad = 24.dp,
            detailTitleFontSp = 18f,
            detailRadialRadius = 800f,
            playerHPad = 40.dp,
            playerVPad = 24.dp,
            playerTitleFontSp = 20f,
            playerTimeFontSp = 15f,
            playerSeekFontSp = 28f,
            playerSeekPreviewWidth = 160.dp,
            playerPauseSize = 80.dp,
            playerPauseFontSp = 32f,
            playerUpNextWidth = 320.dp,
            playerUpNextPad = 32.dp,
            authHPad = 48.dp,
            authVPad = 24.dp,
            authQrSize = 200.dp,
            authLogoFontSp = 32f,
            authStatusFontSp = 16f,
            settingsPad = 48.dp,
            settingsTitleFontSp = 28f,
            menuPanelWidth = 280.dp,
            heroHeight = 160.dp,
        )

        val Handheld = AppDimensions(
            profile = DeviceUiProfile.HANDHELD,
            vodCardWidth = 120.dp,
            vodCardImageSize = 240,
            channelCardHeight = 100.dp,
            channelAvatarSize = 44.dp,
            channelNameFontSp = 14f,
            homeLogoFontSp = 22f,
            homePad = 16.dp,
            homeGridMaxWidth = 400.dp,
            screenHPad = 12.dp,
            rowSpacing = 10.dp,
            rowLabelFontSp = 13f,
            headerFontSp = 18f,
            searchPad = 16.dp,
            searchTitleFontSp = 20f,
            searchInputFontSp = 14f,
            searchChunkSize = 3,
            detailHPad = 12.dp,
            detailVPad = 12.dp,
            detailTitleFontSp = 14f,
            detailRadialRadius = 400f,
            playerHPad = 16.dp,
            playerVPad = 12.dp,
            playerTitleFontSp = 14f,
            playerTimeFontSp = 11f,
            playerSeekFontSp = 18f,
            playerSeekPreviewWidth = 100.dp,
            playerPauseSize = 48.dp,
            playerPauseFontSp = 20f,
            playerUpNextWidth = 180.dp,
            playerUpNextPad = 12.dp,
            authHPad = 20.dp,
            authVPad = 12.dp,
            authQrSize = 120.dp,
            authLogoFontSp = 22f,
            authStatusFontSp = 13f,
            settingsPad = 16.dp,
            settingsTitleFontSp = 20f,
            menuPanelWidth = 200.dp,
            heroHeight = 96.dp,
        )

        // Thor bottom: 538x420dp — normal TV layout, just slightly tighter
        val ThorBottom = AppDimensions(
            profile = DeviceUiProfile.THOR_BOTTOM,
            vodCardWidth = 150.dp,
            vodCardImageSize = 300,
            channelCardHeight = 120.dp,
            channelAvatarSize = 48.dp,
            channelNameFontSp = 16f,
            homeLogoFontSp = 26f,
            homePad = 24.dp,
            homeGridMaxWidth = 520.dp,
            screenHPad = 20.dp,
            rowSpacing = 12.dp,
            rowLabelFontSp = 13f,
            headerFontSp = 18f,
            searchPad = 24.dp,
            searchTitleFontSp = 22f,
            searchInputFontSp = 15f,
            searchChunkSize = 4,
            detailHPad = 24.dp,
            detailVPad = 16.dp,
            detailTitleFontSp = 15f,
            detailRadialRadius = 500f,
            playerHPad = 24.dp,
            playerVPad = 16.dp,
            playerTitleFontSp = 16f,
            playerTimeFontSp = 12f,
            playerSeekFontSp = 22f,
            playerSeekPreviewWidth = 120.dp,
            playerPauseSize = 56.dp,
            playerPauseFontSp = 24f,
            playerUpNextWidth = 220.dp,
            playerUpNextPad = 16.dp,
            authHPad = 32.dp,
            authVPad = 16.dp,
            authQrSize = 140.dp,
            authLogoFontSp = 26f,
            authStatusFontSp = 14f,
            settingsPad = 24.dp,
            settingsTitleFontSp = 22f,
            menuPanelWidth = 220.dp,
            heroHeight = 120.dp,
        )
    }
}

enum class DeviceUiProfile {
    TV,
    HANDHELD,
    THOR_BOTTOM
}

val LocalAppDimensions = staticCompositionLocalOf { AppDimensions.TV }

@Composable
fun rememberAppDimensions(): AppDimensions {
    val config = LocalConfiguration.current
    val width = config.screenWidthDp
    val isThorDevice = Build.MODEL.contains("Thor", ignoreCase = true)
    return when {
        isThorDevice && width < 600 -> AppDimensions.ThorBottom
        width < 500 -> AppDimensions.Handheld
        else -> AppDimensions.TV
    }
}
