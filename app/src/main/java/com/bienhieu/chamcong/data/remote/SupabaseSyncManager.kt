package com.bienhieu.chamcong.data.remote

import android.util.Log
import com.bienhieu.chamcong.data.local.AttendanceDao
import com.bienhieu.chamcong.data.local.EmployeeDao
import com.bienhieu.chamcong.data.local.EmployeeEntity
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

class SupabaseSyncManager(
    private val employeeDao: EmployeeDao,
    private val attendanceDao: AttendanceDao
) {
    companion object {
        private const val TAG = "SupabaseSyncManager"
    }

    /**
     * Tải danh sách nhân viên từ Supabase và lưu vào Room DB.
     */
    suspend fun fetchEmployees() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching employees from Supabase...")
            val remoteEmployees = SupabaseClient.client.from("employees")
                .select()
                .decodeList<RemoteEmployee>()

            Log.d(TAG, "Fetched ${remoteEmployees.size} employees.")

            val localEmployees = mutableListOf<EmployeeEntity>()

            // Chuyển đổi trên Dispatchers.Default để không block luồng IO/UI
            withContext(Dispatchers.Default) {
                val vectorConverter = com.bienhieu.chamcong.data.local.VectorTypeConverter()
                remoteEmployees.forEach { remote ->
                    if (remote.isActive) {
                        try {
                            val vectors = vectorConverter.toFaceVectors(remote.faceVector?.toString() ?: "")

                            localEmployees.add(
                                EmployeeEntity(
                                    id = remote.id,
                                    name = remote.name,
                                    faceVectors = vectors
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing face vectors for ${remote.name}", e)
                        }
                    }
                }
            }

            // ── Upsert Strategy để bảo toàn dữ liệu offline ──
            val existingEmployees = employeeDao.getAll().associateBy { it.id }
            val toInsert = mutableListOf<EmployeeEntity>()
            val toUpdate = mutableListOf<EmployeeEntity>()

            localEmployees.forEach { remote ->
                val local = existingEmployees[remote.id]
                if (local != null) {
                    // Cập nhật tên. Nếu Supabase gửi về vector rỗng (ví dụ: tạo tay trên web), 
                    // giữ lại vector cũ đã quét từ trước trên máy. Nếu có vector mới thì lấy vector mới.
                    val mergedVectors = if (!remote.faceVectors.isNullOrEmpty()) remote.faceVectors else local.faceVectors
                    toUpdate.add(
                        local.copy(
                            name = remote.name,
                            faceVectors = mergedVectors
                        )
                    )
                } else {
                    toInsert.add(remote)
                }
            }

            // Xóa các nhân viên đã bị vô hiệu hóa (isActive = false) hoặc bị xóa trên server
            val remoteIds = localEmployees.map { it.id }.toSet()
            val toDelete = existingEmployees.keys - remoteIds

            if (toDelete.isNotEmpty()) {
                toDelete.forEach { employeeDao.deleteById(it) }
            }
            if (toInsert.isNotEmpty()) {
                employeeDao.insertAll(toInsert)
            }
            if (toUpdate.isNotEmpty()) {
                toUpdate.forEach { employeeDao.update(it) }
            }

            Log.d(TAG, "Sync summary: Inserted ${toInsert.size}, Updated ${toUpdate.size}, Deleted ${toDelete.size}.")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch employees", e)
        }
    }

    /**
     * Lấy các bản ghi chấm công chưa được sync trong DB và đẩy lên Supabase.
     */
    suspend fun syncAttendance() = withContext(Dispatchers.IO) {
        try {
            val unsynced = attendanceDao.getUnsyncedRecords()
            if (unsynced.isEmpty()) {
                Log.d(TAG, "No unsynced attendance records.")
                return@withContext
            }

            Log.d(TAG, "Pushing ${unsynced.size} attendance records to Supabase...")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val remoteLogs = unsynced.map { local ->
                RemoteAttendanceLog(
                    employeeId = local.employeeId,
                    scanTime = dateFormat.format(Date(local.timestamp)),
                    status = local.status
                )
            }

            // Gửi mảng lên Supabase
            SupabaseClient.client.from("attendance_logs").insert(remoteLogs)

            // Đánh dấu đã sync
            unsynced.forEach {
                attendanceDao.markAsSynced(it.id)
            }

            Log.d(TAG, "Successfully synced ${unsynced.size} attendance records.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync attendance logs", e)
        }
    }

    /**
     * Cập nhật vector khuôn mặt của nhân viên lên Supabase.
     */
    suspend fun updateEmployeeFace(employeeId: String, faceVectors: List<FloatArray>) = withContext(Dispatchers.IO) {
        try {
            val vectorConverter = com.bienhieu.chamcong.data.local.VectorTypeConverter()
            val vectorString = vectorConverter.fromFaceVectors(faceVectors)
            val jsonElement = Json.parseToJsonElement(vectorString)
            
            SupabaseClient.client.from("employees").update(
                {
                    set("face_vector", jsonElement)
                }
            ) {
                filter {
                    eq("id", employeeId)
                }
            }
            Log.d(TAG, "Successfully updated face vectors on Supabase for $employeeId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update face vectors on Supabase for $employeeId", e)
        }
    }
}
