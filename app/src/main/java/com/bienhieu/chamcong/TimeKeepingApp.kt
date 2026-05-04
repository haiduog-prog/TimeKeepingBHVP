package com.bienhieu.chamcong

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bienhieu.chamcong.data.remote.SyncWorker
import java.util.concurrent.TimeUnit

/**
 * Application class for ChamCong.
 *
 * Serves as the single source of truth for app-wide singletons
 * (database, TFLite interpreter) to avoid re-initialization overhead.
 */
class TimeKeepingApp : Application() {

    /** Lazy-initialized Room database instance. */
    val database: com.bienhieu.chamcong.data.local.TimeKeepingDatabase by lazy {
        com.bienhieu.chamcong.data.local.TimeKeepingDatabase.getInstance(this)
    }

    /** Lazy-initialized TFLite face embedding helper. */
    val faceEmbeddingHelper: com.bienhieu.chamcong.ml.FaceEmbeddingHelper by lazy {
        com.bienhieu.chamcong.ml.FaceEmbeddingHelper(this)
    }
    override fun onCreate() {
        super.onCreate()
        setupWorkManager()
    }

    private fun setupWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Sync every 4 hours (minimum periodic interval allowed by WorkManager)
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            4,
            TimeUnit.HOURS
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AttendanceSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
