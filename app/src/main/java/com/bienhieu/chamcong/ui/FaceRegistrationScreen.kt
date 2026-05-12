package com.bienhieu.chamcong.ui

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.bienhieu.chamcong.camera.FaceAnalyzer
import com.bienhieu.chamcong.ui.theme.ChamCongColors
import java.util.concurrent.Executors

/**
 * Dedicated screen for multi-angle face registration.
 *
 * Flow:
 *  1. Camera preview with guided overlay
 *  2. Step 0: "Nhìn thẳng" → Capture
 *  3. Step 1: "Quay trái"  → Capture
 *  4. Step 2: "Quay phải"  → Capture
 *  5. Step 3: Name input dialog (new) or confirm dialog (existing) → Save
 *  6. Navigate back
 *
 * Uses its own [FaceRegistrationViewModel] — independent from [AttendanceViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceRegistrationScreen(
    viewModel: FaceRegistrationViewModel,
    employeeId: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val registrationStep by viewModel.registrationStep.collectAsState()
    val cameraPrompt by viewModel.cameraPrompt.collectAsState()
    val latestFace by viewModel.latestDetectedFace.collectAsState()
    val employeeToRegister by viewModel.employeeToRegister.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    // Start registration flow on enter, clean up on exit
    LaunchedEffect(employeeId) {
        if (employeeId != null) {
            viewModel.startRegistrationFor(employeeId)
        } else {
            viewModel.startRegistration()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetRegistration()
        }
    }

    // ── Step 3: Name Input (New Employee) ──
    if (registrationStep == 3 && employeeToRegister == null) {
        var name by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { onBack() },
            title = { Text("Hoàn tất đăng ký") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (latestFace != null) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = latestFace!!.asImageBitmap(),
                                contentDescription = "Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Text(
                        "✅ Đã chụp 3 góc mặt thành công!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChamCongColors.success
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Họ và tên") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveNewEmployee(name, context) { onBack() }
                    },
                    enabled = name.isNotBlank() && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Lưu")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { onBack() }) {
                    Text("Hủy")
                }
            }
        )
    }

    // ── Step 3: Confirm (Existing Employee) ──
    if (registrationStep == 3 && employeeToRegister != null) {
        AlertDialog(
            onDismissRequest = { onBack() },
            title = { Text("Xác nhận gán khuôn mặt") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Nhân viên: ${employeeToRegister?.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "✅ Đã chụp 3 góc mặt thành công!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChamCongColors.success
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateExistingEmployee(context) { onBack() }
                    },
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Gán & Lưu")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { onBack() }) {
                    Text("Hủy")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (employeeToRegister != null) "Gán khuôn mặt: ${employeeToRegister?.name}"
                        else "Đăng ký khuôn mặt mới",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Camera Preview ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                RegistrationCameraPreview(viewModel = viewModel)

                // ── Step Overlay (Steps 0-2) ──
                if (registrationStep in 0..2) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f))
                    )

                    // Step indicator at top
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0..2) {
                            val stepLabel = when (i) {
                                0 -> "Thẳng"
                                1 -> "Trái"
                                else -> "Phải"
                            }
                            val isCurrent = i == registrationStep
                            val isDone = i < registrationStep

                            Box(
                                modifier = Modifier
                                    .background(
                                        when {
                                            isDone -> ChamCongColors.success
                                            isCurrent -> MaterialTheme.colorScheme.primary
                                            else -> Color.Gray
                                        },
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isDone) "✓ $stepLabel" else stepLabel,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Instruction prompt
                    Text(
                        text = cameraPrompt ?: "",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 60.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // Capture button
                    Button(
                        onClick = { viewModel.captureStep(context) },
                        enabled = latestFace != null,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .height(56.dp)
                            .fillMaxWidth(0.6f),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text(
                            "📸 Chụp (${registrationStep + 1}/3)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Dedicated Camera Preview for Registration
// ─────────────────────────────────────────────────────────────

@Composable
private fun RegistrationCameraPreview(viewModel: FaceRegistrationViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF6366F1).copy(alpha = 0.5f),
                        Color(0xFF8B5CF6).copy(alpha = 0.5f)
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

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                    .apply {
                        setAnalyzer(analysisExecutor, FaceAnalyzer(
                            isLivenessEnabled = { false },
                            onLivenessStateChange = { },
                            onFaceDetected = { faceBitmap, yaw ->
                                viewModel.onFaceDetected(faceBitmap, yaw)
                            },
                            onNoFace = { }
                        ))
                    }

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
                    android.util.Log.e("RegCameraPreview", "Camera bind failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}
