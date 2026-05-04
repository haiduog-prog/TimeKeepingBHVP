package com.bienhieu.chamcong.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity storing each attendance check-in/check-out event.
 *
 * @property id         Auto-generated primary key.
 * @property employeeId Foreign key referencing [EmployeeEntity.id] (UUID).
 * @property timestamp  Epoch millis when the attendance was recorded.
 * @property status     "IN" or "OUT".
 * @property confidence The cosine similarity score at time of match.
 * @property isSynced   Whether this record has been synced to Supabase.
 */
@Entity(tableName = "attendance_records")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: String,
    val employeeName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "IN" or "OUT"
    val confidence: Float,
    val isSynced: Boolean = false
)
