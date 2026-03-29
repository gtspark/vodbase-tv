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

        val ThorBottom = AppDimensions(
            profile = DeviceUiProfile.THOR_BOTTOM,
            vodCardWidth = 104.dp,
            vodCardImageSize = 208,
            channelCardHeight = 76.dp,
            channelAvatarSize = 28.dp,
            channelNameFontSp = 11f,
            homeLogoFontSp = 16f,
            homePad = 10.dp,
            homeGridMaxWidth = 360.dp,
            screenHPad = 8.dp,
            rowSpacing = 8.dp,
            rowLabelFontSp = 11f,
            headerFontSp = 14f,
            searchPad = 10.dp,
            searchTitleFontSp = 16f,
            searchInputFontSp = 12f,
            searchChunkSize = 2,
            detailHPad = 10.dp,
            detailVPad = 8.dp,
            detailTitleFontSp = 12f,
            detailRadialRadius = 280f,
            playerHPad = 10.dp,
            playerVPad = 8.dp,
            playerTitleFontSp = 12f,
            playerTimeFontSp = 10f,
            playerSeekFontSp = 15f,
            playerSeekPreviewWidth = 84.dp,
            playerPauseSize = 40.dp,
            playerPauseFontSp = 16f,
            playerUpNextWidth = 144.dp,
            playerUpNextPad = 8.dp,
            authHPad = 12.dp,
            authVPad = 8.dp,
            authQrSize = 88.dp,
            authLogoFontSp = 16f,
            authStatusFontSp = 11f,
            settingsPad = 10.dp,
            settingsTitleFontSp = 16f,
            menuPanelWidth = 160.dp,
            heroHeight = 82.dp,
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
        isThorDevice && width < 500 -> AppDimensions.ThorBottom
        width < 500 -> AppDimensions.Handheld
        else -> AppDimensions.TV
    }
}
