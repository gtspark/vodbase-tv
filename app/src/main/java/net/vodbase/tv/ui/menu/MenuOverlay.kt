package net.vodbase.tv.ui.menu

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.vodbase.tv.ui.components.ActionButton
import androidx.compose.ui.text.style.TextOverflow
import net.vodbase.tv.ui.theme.ChannelTheme
import net.vodbase.tv.ui.theme.DeviceUiProfile
import net.vodbase.tv.ui.theme.LocalAppDimensions

private val PanelBackground = Color(0xFF1A1A1A)

/**
 * A slide-in overlay menu panel. Uses the channel theme for button styling so colors are
 * consistent with the rest of the channel UI.
 *
 * MenuButton has been replaced with the shared ActionButton composable.
 */
@Composable
fun MenuOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    userEmail: String?,
    onSignOut: () -> Unit,
    onSignIn: () -> Unit,
    onSettings: () -> Unit,
    onSearch: (() -> Unit)?,
    theme: ChannelTheme
) {
    if (isVisible) {
        val dims = LocalAppDimensions.current
        val isThor = dims.profile == DeviceUiProfile.THOR_BOTTOM
        val focusRequester = remember { FocusRequester() }

        // Request focus on the panel when it opens
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        // Full-screen scrim - clicking or pressing BACK dismisses the menu
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .onPreviewKeyEvent { event ->
                    if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                        event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK
                    ) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            // Right-aligned panel
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(dims.menuPanelWidth)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PanelBackground)
                        // Intercept clicks inside the panel so they don't dismiss the menu
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .focusRequester(focusRequester)
                        .focusable()
                        .padding(if (isThor) 12.dp else 24.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Title
                    Text(
                        "VodBase",
                        fontSize = if (isThor) 16.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.primary
                    )

                    Spacer(modifier = Modifier.height(if (isThor) 8.dp else 16.dp))

                    Divider(color = Color.White.copy(alpha = 0.12f))

                    Spacer(modifier = Modifier.height(if (isThor) 10.dp else 20.dp))

                    // Auth section
                    if (userEmail != null) {
                        Text(
                            userEmail,
                            fontSize = if (isThor) 10.sp else 13.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = if (isThor) 6.dp else 12.dp)
                        )
                        ActionButton(
                            text = "Sign Out",
                            isBright = false,
                            theme = theme,
                            onClick = {
                                onSignOut()
                                onDismiss()
                            }
                        )
                    } else {
                        Text(
                            "Not signed in",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.padding(bottom = if (isThor) 6.dp else 12.dp)
                        )
                        ActionButton(
                            text = "Sign In",
                            isBright = true,
                            theme = theme,
                            onClick = {
                                onSignIn()
                                onDismiss()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick actions
                    if (onSearch != null) {
                        ActionButton(
                            text = "Search",
                            isBright = false,
                            theme = theme,
                            onClick = {
                                onSearch()
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    ActionButton(
                        text = "Settings",
                        isBright = false,
                        theme = theme,
                        onClick = {
                            onSettings()
                            onDismiss()
                        }
                    )

                    Spacer(modifier = Modifier.height(if (isThor) 8.dp else 16.dp))

                    Divider(color = Color.White.copy(alpha = 0.12f))

                    Spacer(modifier = Modifier.height(if (isThor) 10.dp else 20.dp))

                    // About section
                    Text(
                        "About",
                        fontSize = if (isThor) 9.sp else 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.4f),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = if (isThor) 4.dp else 8.dp)
                    )

                    Text(
                        if (isThor) "VodBase" else "VodBase for Fire TV",
                        fontSize = if (isThor) 11.sp else 14.sp,
                        color = Color.White
                    )

                    Text(
                        "v1.1.0",
                        fontSize = if (isThor) 10.sp else 12.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
