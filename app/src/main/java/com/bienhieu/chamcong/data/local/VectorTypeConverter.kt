package com.bienhieu.chamcong.data.local

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Room TypeConverter to persist [FloatArray] as [ByteArray] in SQLite.
 *
 * Strategy: Convert each Float (4 bytes) into raw bytes using a ByteBuffer.
 * This is more space-efficient and faster than storing as a CSV string.
 *
 * Storage cost: N floats × 4 bytes  (e.g., 128-dim → 512 bytes).
 */
class VectorTypeConverter {

    /**
     * FloatArray → ByteArray for Room storage.
     *
     * Each float is written in LITTLE_ENDIAN order into a ByteBuffer,
     * then the backing array is returned.
     */
    @TypeConverter
    fun fromFloatArray(vector: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(vector.size * Float.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
        vector.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    /**
     * ByteArray → FloatArray when reading from Room.
     */
    @TypeConverter
    fun toFloatArray(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(bytes.size / Float.SIZE_BYTES) { buffer.getFloat() }
    }
}
