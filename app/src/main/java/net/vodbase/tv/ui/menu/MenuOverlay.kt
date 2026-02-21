package net.vodbase.tv.ui.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AccentGreen = Color(0xFF00FF41)
private val PanelBackground = Color(0xFF1A1A1A)
private val ButtonSurface = Color(0xFF2A2A2A)
private val ButtonShape = RoundedCornerShape(8.dp)

@Composable
fun MenuOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    userEmail: String?,
    onSignOut: () -> Unit,
    onSignIn: () -> Unit
) {
    if (isVisible) {
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
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Back) {
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
                    .width(280.dp)
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
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Title
                    Text(
                        "VodBase",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = Color.White.copy(alpha = 0.12f))

                    Spacer(modifier = Modifier.height(20.dp))

                    // Auth section
                    if (userEmail != null) {
                        Text(
                            userEmail,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        MenuButton(
                            text = "Sign Out",
                            isBright = false,
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
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        MenuButton(
                            text = "Sign In",
                            isBright = true,
                            onClick = {
                                onSignIn()
                                onDismiss()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Divider(color = Color.White.copy(alpha = 0.12f))

                    Spacer(modifier = Modifier.height(20.dp))

                    // About section
                    Text(
                        "About",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.4f),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        "VodBase for Fire TV",
                        fontSize = 14.sp,
                        color = Color.White
                    )

                    Text(
                        "v1.0.0",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuButton(
    text: String,
    isBright: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bgColor = when {
        isFocused && isBright -> AccentGreen
        isFocused -> AccentGreen.copy(alpha = 0.2f)
        isBright -> AccentGreen.copy(alpha = 0.8f)
        else -> ButtonSurface
    }
    val textColor = when {
        isFocused && isBright -> Color.Black
        isFocused -> Color.White
        isBright -> Color.Black
        else -> Color.White.copy(alpha = 0.7f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ButtonShape)
            .background(bgColor)
            .then(
                if (isFocused) Modifier.border(
                    width = 2.dp,
                    color = AccentGreen,
                    shape = ButtonShape
                ) else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = textColor
        )
    }
}
