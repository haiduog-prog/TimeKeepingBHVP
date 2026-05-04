package com.bienhieu.chamcong.ui

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.bienhieu.chamcong.camera.FaceAnalyzer
import com.bienhieu.chamcong.ui.theme.ChamCongColors
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

/**
 * Main attendance scanning screen.
 *
 * Layout:
 * ┌──────────────────────────┐
 * │       Header/Clock       │
 * ├──────────────────────────┤
 * │                          │
 * │     Camera Preview       │
 * │     (with overlay)       │
 * │                          │
 * ├──────────────────────────┤
 * │     Status Feedback      │
 * │  (Scanning/Match/Error)  │
 * └──────────────────────────┘
 */
@Composable
fun AttendanceScreen(viewModel: AttendanceViewModel, onNavigateToEmployees: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    var showRegisterDialog by remember { mutableStateOf(false) }
    val employeeToRegister by viewModel.employeeToRegister.collectAsState()
    val context = LocalContext.current
    
    val isLivenessEnabled by viewModel.isLivenessEnabled.collectAsState()
    val showLivenessPrompt by viewModel.showLivenessPrompt.collectAsState()

    // ── Registration Dialog ──
    if (showRegisterDialog) {
        var name by remember { mutableStateOf("") }
        val latestFace by viewModel.latestDetectedFace.collectAsState()

        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            title = { Text("Đăng ký nhân viên mới") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Họ và tên") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Khuôn mặt hiện tại:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (latestFace != null) {
                            androidx.compose.foundation.Image(
                                bitmap = latestFace!!.asImageBitmap(),
                                contentDescription = "Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Face, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (latestFace == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Vui lòng nhìn vào camera...",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        latestFace?.let { face ->
                            viewModel.registerEmployee(name, face, context)
                            showRegisterDialog = false
                        }
                    },
                    enabled = name.isNotBlank() && latestFace != null
                ) {
                    Text("Chụp & Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegisterDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // ── Update Existing Employee Face Dialog ──
    if (employeeToRegister != null) {
        val latestFace by viewModel.latestDetectedFace.collectAsState()

        AlertDialog(
            onDismissRequest = { viewModel.cancelRegistration() },
            title = { Text("Xác nhận gán khuôn mặt") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Nhân viên: ${employeeToRegister?.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (latestFace != null) {
                            androidx.compose.foundation.Image(
                                bitmap = latestFace!!.asImageBitmap(),
                                contentDescription = "Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Face, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (latestFace == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Vui lòng nhìn vào camera...",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (latestFace != null) {
                            viewModel.updateEmployeeFace(context)
                        }
                    },
                    enabled = latestFace != null
                ) {
                    Text("Gán & Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelRegistration() }) {
                    Text("Hủy")
                }
            }
        )
    }

    // ── Matched Result Popup (blocks scanning until dismissed) ──
    if (uiState is AttendanceUiState.Matched) {
        val matchedState = uiState as AttendanceUiState.Matched
        val isCheckIn = matchedState.type == AttendanceType.IN

        AlertDialog(
            onDismissRequest = { /* Block outside dismiss — must tap button */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = if (isCheckIn) ChamCongColors.success.copy(alpha = 0.15f)
                                        else ChamCongColors.warning.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCheckIn) Icons.Default.Login else Icons.Default.Logout,
                            contentDescription = null,
                            tint = if (isCheckIn) ChamCongColors.success else ChamCongColors.warning,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (isCheckIn) "CHECK IN" else "CHECK OUT",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        matchedState.employeeName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (isCheckIn) "\u2705 Chấm công vào thành công"
                        else "\uD83D\uDD36 Chấm công ra thành công",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Độ chính xác: ${String.format("%.1f", matchedState.score * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetToScanning() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCheckIn) ChamCongColors.success else ChamCongColors.warning
                    )
                ) {
                    Text("Đóng", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header with Clock ──
        HeaderSection(
            isLivenessEnabled = isLivenessEnabled,
            onLivenessToggle = { viewModel.toggleLiveness(it) },
            onAddClick = { showRegisterDialog = true },
            onNavigateToEmployees = onNavigateToEmployees
        )

        // ── Camera Preview ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            CameraPreviewWithOverlay(viewModel = viewModel)

            // ── Scanning animation overlay ──
            if (uiState is AttendanceUiState.Scanning) {
                ScanningOverlay()
                
                if (showLivenessPrompt) {
                    Text(
                        text = "Vui lòng chớp mắt chậm rãi...",
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Status Feedback Card ──
        StatusFeedbackCard(state = uiState)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// Header Section
// ─────────────────────────────────────────────────────────────

@Composable
private fun HeaderSection(
    isLivenessEnabled: Boolean,
    onLivenessToggle: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onNavigateToEmployees: () -> Unit
) {
    val currentTime = remember { mutableStateOf("") }
    val currentDate = remember { mutableStateOf("") }

    // Update time every second
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi"))
        while (true) {
            val now = Date()
            currentTime.value = timeFormat.format(now)
            currentDate.value = dateFormat.format(now)
            kotlinx.coroutines.delay(1000L)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CHẤM CÔNG",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 4.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentTime.value,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = currentDate.value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // ── Liveness Toggle (Anti-spoofing) ──
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chống giả mạo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    androidx.compose.material3.Switch(
                        checked = isLivenessEnabled,
                        onCheckedChange = onLivenessToggle,
                        modifier = Modifier.scale(0.8f)
                    )
                }
            } // End of Column

            // ── Employee List Navigation Button ──
            IconButton(
                onClick = onNavigateToEmployees,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Employee List",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // ── Add Employee Button ──
            IconButton(
                onClick = onAddClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Employee",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Camera Preview
// ─────────────────────────────────────────────────────────────

@Composable
private fun CameraPreviewWithOverlay(viewModel: AttendanceViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Analysis executor – single-thread for sequential frame processing
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }

            // ── Set up CameraX ──
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // Preview use case
                val preview = Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }

                // Image analysis use case with FaceAnalyzer
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                    .apply {
                        setAnalyzer(analysisExecutor, FaceAnalyzer(
                            isLivenessEnabled = { viewModel.isLivenessEnabled.value },
                            onLivenessStateChange = { show -> viewModel.updateLivenessPrompt(show) },
                            onFaceDetected = { faceBitmap ->
                                // This callback runs on the analysis thread.
                                // The ViewModel handles thread switching internally.
                                viewModel.onFaceDetected(faceBitmap)
                            },
                            onNoFace = {
                                viewModel.updateLivenessPrompt(false)
                            }
                        ))
                    }

                // Use front camera for kiosk (employee faces the device)
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    android.util.Log.e("CameraPreview", "Camera bind failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

// ─────────────────────────────────────────────────────────────
// Scanning Overlay Animation
// ─────────────────────────────────────────────────────────────

@Composable
private fun ScanningOverlay() {
    // Pulsing animation for the scan indicator
    val infiniteTransition = rememberInfiniteTransition(label = "scan_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_alpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_scale"
    )

    Box(
        modifier = Modifier
            .size((200 * scale).dp)
            .border(
                width = 3.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Face,
            contentDescription = "Scan face",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Status Feedback Card
// ─────────────────────────────────────────────────────────────

@Composable
private fun StatusFeedbackCard(state: AttendanceUiState) {
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            fadeIn(tween(300)) togetherWith fadeOut(tween(300))
        },
        label = "status_transition"
    ) { currentState ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            when (currentState) {
                is AttendanceUiState.Scanning -> {
                    StatusContent(
                        icon = Icons.Default.Face,
                        iconColor = MaterialTheme.colorScheme.primary,
                        title = "Đang quét...",
                        subtitle = "Đưa khuôn mặt vào khung hình",
                        showProgress = false
                    )
                }

                is AttendanceUiState.Processing -> {
                    StatusContent(
                        icon = Icons.Default.Face,
                        iconColor = MaterialTheme.colorScheme.secondary,
                        title = "Đang xử lý...",
                        subtitle = "Nhận diện khuôn mặt",
                        showProgress = true
                    )
                }

                is AttendanceUiState.Matched -> {
                    val isCheckIn = currentState.type == AttendanceType.IN
                    StatusContent(
                        icon = if (isCheckIn) Icons.Default.Login else Icons.Default.Logout,
                        iconColor = if (isCheckIn) ChamCongColors.success else ChamCongColors.warning,
                        title = currentState.employeeName,
                        subtitle = if (isCheckIn) "\u2705 CHECK IN thành công" else "\uD83D\uDD36 CHECK OUT thành công",
                        detail = "Độ chính xác: ${String.format("%.1f", currentState.score * 100)}%"
                    )
                }

                is AttendanceUiState.Unknown -> {
                    StatusContent(
                        icon = Icons.Default.ErrorOutline,
                        iconColor = MaterialTheme.colorScheme.error,
                        title = "Không nhận diện được",
                        subtitle = "Khuôn mặt chưa được đăng ký",
                        detail = "Điểm: ${String.format("%.2f", currentState.score)}"
                    )
                }

                is AttendanceUiState.Paused -> {
                    StatusContent(
                        icon = Icons.Default.Face,
                        iconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        title = "Tạm dừng...",
                        subtitle = "Sẵn sàng quét tiếp",
                        showProgress = false
                    )
                }

                is AttendanceUiState.Error -> {
                    StatusContent(
                        icon = Icons.Default.Warning,
                        iconColor = MaterialTheme.colorScheme.error,
                        title = "Lỗi",
                        subtitle = currentState.message
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusContent(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    detail: String? = null,
    showProgress: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = iconColor.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = iconColor
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            if (showProgress) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
