package com.bienhieu.chamcong.data.local

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONException

/**
 * Room TypeConverter to persist [List<FloatArray>] as a JSON [String] in SQLite.
 *
 * Strategy: Convert list of vectors into a nested JSON array string "[[0.1, 0.2...], [...]]".
 */
class VectorTypeConverter {

    /**
     * List<FloatArray> → String for Room storage.
     */
    @TypeConverter
    fun fromFaceVectors(vectors: List<FloatArray>): String {
        val rootArray = JSONArray()
        vectors.forEach { vec ->
            val arr = JSONArray()
            vec.forEach { arr.put(it.toDouble()) }
            rootArray.put(arr)
        }
        return rootArray.toString()
    }

    /**
     * String → List<FloatArray> when reading from Room.
     */
    @TypeConverter
    fun toFaceVectors(jsonStr: String): List<FloatArray> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<FloatArray>()
        try {
            val rootArray = JSONArray(jsonStr)
            // Check if it's a nested array [[...]] or single array [...]
            if (rootArray.length() > 0 && rootArray.optJSONArray(0) != null) {
                // It's a nested array [[...]]
                for (i in 0 until rootArray.length()) {
                    val arr = rootArray.getJSONArray(i)
                    val floatArr = FloatArray(arr.length())
                    for (j in 0 until arr.length()) {
                        floatArr[j] = arr.getDouble(j).toFloat()
                    }
                    list.add(floatArr)
                }
            } else {
                // It's a single array [...]
                val floatArr = FloatArray(rootArray.length())
                for (j in 0 until rootArray.length()) {
                    floatArr[j] = rootArray.getDouble(j).toFloat()
                }
                list.add(floatArr)
            }
        } catch (e: JSONException) {
            // Ignore format errors
        }
        return list
    }
}
