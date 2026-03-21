package net.vodbase.tv.ui.settings

import android.view.KeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.vodbase.tv.data.repository.AuthRepository
import net.vodbase.tv.data.repository.SettingsRepository
import net.vodbase.tv.ui.theme.AnimationConstants
import net.vodbase.tv.ui.theme.ChannelTheme
import net.vodbase.tv.ui.theme.LocalAppDimensions
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val videoQuality = settingsRepository.videoQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "auto")

    val playbackSpeed = settingsRepository.playbackSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val autoplayNext = settingsRepository.autoplayNext
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isAuthenticated = authRepository.isAuthenticated
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    var deleteState by mutableStateOf<DeleteState>(DeleteState.Idle)

    fun cycleQuality() {
        viewModelScope.launch {
            val options = SettingsRepository.QUALITY_OPTIONS
            val current = videoQuality.value
            val next = options[(options.indexOf(current) + 1) % options.size]
            settingsRepository.setVideoQuality(next)
        }
    }

    fun cycleSpeed() {
        viewModelScope.launch {
            val options = SettingsRepository.SPEED_OPTIONS
            val current = playbackSpeed.value
            val next = options[(options.indexOf(current) + 1) % options.size]
            settingsRepository.setPlaybackSpeed(next)
        }
    }

    fun toggleAutoplay() {
        viewModelScope.launch {
            settingsRepository.setAutoplayNext(!autoplayNext.value)
        }
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            deleteState = DeleteState.Deleting
            val success = authRepository.deleteAccount()
            deleteState = if (success) {
                onDeleted()
                DeleteState.Done
            } else {
                DeleteState.Error
            }
        }
    }

    fun resetDeleteState() {
        deleteState = DeleteState.Idle
    }
}

sealed interface DeleteState {
    data object Idle : DeleteState
    data object Confirming : DeleteState
    data object Deleting : DeleteState
    data object Done : DeleteState
    data object Error : DeleteState
}

private fun formatQuality(quality: String): String = when (quality) {
    "auto" -> "Auto"
    else -> "${quality}p"
}

private fun formatSpeed(speed: Float): String = when {
    speed == 1.0f -> "1.0x"
    speed == 0.75f -> "0.75x"
    else -> "${speed}x"
}

@Composable
fun SettingsScreen(
    theme: ChannelTheme,
    onBack: () -> Unit,
    onAccountDeleted: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val dims = LocalAppDimensions.current
    val quality by viewModel.videoQuality.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()
    val autoplay by viewModel.autoplayNext.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val deleteState = viewModel.deleteState

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                    event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK
                ) {
                    if (deleteState is DeleteState.Confirming) {
                        viewModel.resetDeleteState()
                    } else {
                        onBack()
                    }
                    true
                } else {
                    false
                }
            }
            .padding(dims.settingsPad)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (dims.settingsPad < 32.dp) 10.dp else 16.dp)) {
            Text(
                "Settings",
                fontSize = dims.settingsTitleFontSp.sp,
                fontWeight = FontWeight.Bold,
                color = theme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Section header
            Text(
                "PLAYBACK",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )

            SettingRow(
                label = "Video Quality",
                value = formatQuality(quality),
                onClick = { viewModel.cycleQuality() },
                theme = theme,
                autoFocus = true
            )

            SettingRow(
                label = "Playback Speed",
                value = formatSpeed(speed),
                onClick = { viewModel.cycleSpeed() },
                theme = theme
            )

            SettingRow(
                label = "Autoplay Next",
                value = if (autoplay) "On" else "Off",
                onClick = { viewModel.toggleAutoplay() },
                theme = theme
            )

            if (isAuthenticated) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "PRIVACY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 1.sp
                )

                when (deleteState) {
                    is DeleteState.Idle -> {
                        SettingRow(
                            label = "Delete My Data",
                            value = "Remove all",
                            onClick = { viewModel.deleteState = DeleteState.Confirming },
                            theme = theme,
                            destructive = true
                        )
                    }
                    is DeleteState.Confirming -> {
                        SettingRow(
                            label = "Are you sure? This is permanent.",
                            value = "Confirm delete",
                            onClick = { viewModel.deleteAccount { onAccountDeleted?.invoke() } },
                            theme = theme,
                            destructive = true
                        )
                        SettingRow(
                            label = "Cancel",
                            value = "",
                            onClick = { viewModel.resetDeleteState() },
                            theme = theme
                        )
                    }
                    is DeleteState.Deleting -> {
                        SettingRow(
                            label = "Deleting your data...",
                            value = "",
                            onClick = {},
                            theme = theme
                        )
                    }
                    is DeleteState.Done -> {
                        SettingRow(
                            label = "All data deleted. You've been signed out.",
                            value = "",
                            onClick = {},
                            theme = theme
                        )
                    }
                    is DeleteState.Error -> {
                        SettingRow(
                            label = "Something went wrong. Try again.",
                            value = "Retry",
                            onClick = { viewModel.deleteAccount { onAccountDeleted?.invoke() } },
                            theme = theme,
                            destructive = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    theme: ChannelTheme,
    autoFocus: Boolean = false,
    destructive: Boolean = false
) {
    val accentColor = if (destructive) Color(0xFFCC4444) else theme.primary
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }

    if (autoFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) AnimationConstants.BUTTON_SCALE_FOCUSED else 1.0f,
        animationSpec = tween(AnimationConstants.FOCUS_DURATION_MS),
        label = "settingScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) accentColor.copy(alpha = 0.15f) else theme.surface,
        animationSpec = tween(AnimationConstants.COLOR_DURATION_MS),
        label = "settingBg"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(AnimationConstants.FOCUS_DURATION_MS),
        label = "settingBorder"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(theme.shape)
            .background(bgColor)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = (if (destructive) accentColor else theme.focusRing).copy(alpha = borderAlpha),
                shape = theme.shape
            )
            .then(if (autoFocus) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (destructive && isFocused) accentColor
                    else if (isFocused) Color.White
                    else theme.onSurface.copy(alpha = 0.85f)
        )
        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .background(
                        if (isFocused) accentColor else accentColor.copy(alpha = 0.2f),
                        theme.shape
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFocused) theme.background else accentColor
                )
            }
        }
    }
}
