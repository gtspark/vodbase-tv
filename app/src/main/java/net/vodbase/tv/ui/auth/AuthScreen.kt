package net.vodbase.tv.ui.auth

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.vodbase.tv.data.repository.AuthRepository
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var qrBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var status by mutableStateOf("Generating QR code...")
        private set
    var isPolling by mutableStateOf(false)
        private set

    private var currentToken: String? = null

    fun startQrLogin(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = authRepository.createQrSession()
                if (response.success && response.token != null) {
                    currentToken = response.token
                    val url = "https://vodbase.net/qr?t=${response.token}"
                    qrBitmap = generateQrBitmap(url)
                    status = "Scan with your phone"
                    startPolling(onSuccess)
                } else {
                    status = "Failed to create QR session"
                }
            } catch (e: Exception) {
                status = "Network error. Check connection."
            }
        }
    }

    private fun startPolling(onSuccess: () -> Unit) {
        isPolling = true
        viewModelScope.launch {
            val token = currentToken ?: return@launch
            repeat(150) { // 5 min at 2s intervals
                if (!isPolling) return@launch
                delay(2000)
                try {
                    val poll = authRepository.pollQrSession(token)
                    when (poll.status) {
                        "approved" -> {
                            isPolling = false
                            if (poll.deviceToken != null && poll.email != null) {
                                authRepository.saveAuth(poll.deviceToken, poll.email)
                                status = "Signed in as ${poll.email}"
                                delay(500)
                                onSuccess()
                            }
                            return@launch
                        }
                        "expired" -> {
                            isPolling = false
                            status = "QR code expired. Generating new one..."
                            delay(1000)
                            startQrLogin(onSuccess)
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    // Keep polling on error
                }
            }
        }
    }

    private fun generateQrBitmap(text: String): Bitmap {
        val size = 512
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF1A1A2E.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        return bitmap
    }
}

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    onSkip: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.startQrLogin(onAuthenticated)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(horizontal = 48.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Logo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "VOD",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFEF4444)
                )
                Text(
                    "BASE",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            // QR Code with background
            viewModel.qrBitmap?.let { bitmap ->
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                }
            }

            // Status
            Text(
                viewModel.status,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Skip button
            Button(
                onClick = onSkip,
                colors = ButtonDefaults.colors(
                    containerColor = Color(0xFF2A2A3E),
                    contentColor = Color(0xFF7A7A9A)
                )
            ) {
                Text("Continue without signing in", fontSize = 14.sp)
            }
        }
    }
}
