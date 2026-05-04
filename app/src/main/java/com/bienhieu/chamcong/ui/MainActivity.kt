package com.bienhieu.chamcong.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bienhieu.chamcong.TimeKeepingApp
import com.bienhieu.chamcong.ui.navigation.Route
import com.bienhieu.chamcong.ui.theme.ChamCongTheme

/**
 * Single-activity entry point for the ChamCong kiosk application.
 *
 * Responsibilities:
 *  - Request camera permission at launch.
 *  - Initialize the ViewModel with app-level singletons.
 *  - Host the Compose UI tree.
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModel: AttendanceViewModel

    /** Permission launcher for CAMERA. */
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            setupUi()
        } else {
            // In a kiosk app, camera permission is critical.
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Initialize ViewModel from app-level singletons ──
        val app = application as TimeKeepingApp
        viewModel = ViewModelProvider(
            this,
            AttendanceViewModel.Factory(app.database, app.faceEmbeddingHelper)
        )[AttendanceViewModel::class.java]

        // ── Check camera permission ──
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            setupUi()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * Set up the Compose UI tree once camera permission is granted.
     */
    private fun setupUi() {
        setContent {
            ChamCongTheme {
                val navController = rememberNavController()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = Route.Attendance.route) {
                        composable(Route.Attendance.route) {
                            AttendanceScreen(
                                viewModel = viewModel,
                                onNavigateToEmployees = { navController.navigate(Route.Employees.route) }
                            )
                        }
                        composable(Route.Employees.route) {
                            EmployeeListScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}