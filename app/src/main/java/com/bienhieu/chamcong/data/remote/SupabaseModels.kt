package com.bienhieu.chamcong.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class RemoteEmployee(
    val id: String,
    val name: String,
    @SerialName("face_vector")
    val faceVector: JsonElement? = null,
    @SerialName("is_active")
    val isActive: Boolean = true
)

@Serializable
data class RemoteAttendanceLog(
    @SerialName("employee_id")
    val employeeId: String,
    @SerialName("scan_time")
    val scanTime: String, // ISO8601 string
    @SerialName("device_id")
    val deviceId: String = "KIOSK-01",
    val status: String // "IN" or "OUT"
)
