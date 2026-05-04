package com.bienhieu.chamcong.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a registered employee.
 *
 * @property id          Unique employee identifier (UUID from Supabase).
 * @property name        Full display name of the employee.
 * @property faceVector  The face embedding vector extracted by MobileFaceNet.
 *                       Stored as FloatArray in memory; persisted via [VectorTypeConverter].
 * @property createdAt   Epoch millis when the employee was registered.
 */
@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val faceVectors: List<FloatArray>,
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    // ── equals/hashCode must be overridden because data class + FloatArray ──

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmployeeEntity) return false
        if (id != other.id) return false
        if (name != other.name) return false
        if (faceVectors.size != other.faceVectors.size) return false
        for (i in faceVectors.indices) {
            if (!faceVectors[i].contentEquals(other.faceVectors[i])) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        var vectorsHash = 1
        faceVectors.forEach { vectorsHash = 31 * vectorsHash + it.contentHashCode() }
        result = 31 * result + vectorsHash
        return result
    }
}
