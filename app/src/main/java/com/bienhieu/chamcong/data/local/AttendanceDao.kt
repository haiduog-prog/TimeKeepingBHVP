package com.bienhieu.chamcong.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Attendance records.
 */
@Dao
interface AttendanceDao {

    @Insert
    suspend fun insert(record: AttendanceEntity): Long

    /** Get today's attendance records (pass start-of-day epoch millis). */
    @Query("SELECT * FROM attendance_records WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun observeToday(startOfDay: Long): Flow<List<AttendanceEntity>>

    /** Get all records for a specific employee. */
    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId ORDER BY timestamp DESC")
    suspend fun getByEmployee(employeeId: String): List<AttendanceEntity>

    /** Get the latest record for a specific employee today (to toggle IN / OUT). */
    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId AND timestamp >= :startOfDay ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestToday(employeeId: String, startOfDay: Long): AttendanceEntity?

    /** Get all unsynced records to push to server. */
    @Query("SELECT * FROM attendance_records WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedRecords(): List<AttendanceEntity>

    /** Mark a record as synced. */
    @Query("UPDATE attendance_records SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)
}
