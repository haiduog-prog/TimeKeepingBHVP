package com.bienhieu.chamcong.data.remote

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bienhieu.chamcong.data.local.TimeKeepingDatabase

/**
 * Background worker responsible for syncing offline attendance records and employees.
 * This runs periodically via WorkManager when the device has network connectivity.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker started doWork()")
        return try {
            // Retrieve DB instance using application context
            val database = TimeKeepingDatabase.getInstance(applicationContext)
            val syncManager = SupabaseSyncManager(database.employeeDao(), database.attendanceDao())

            // Execute push (attendance) and pull (employees)
            syncManager.syncAttendance()
            syncManager.fetchEmployees()

            Log.d(TAG, "SyncWorker finished successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker failed", e)
            // Retry with exponential backoff if failed less than 3 times
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
