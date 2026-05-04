package com.bienhieu.chamcong.data.repository

import com.bienhieu.chamcong.data.local.AttendanceDao
import com.bienhieu.chamcong.data.local.AttendanceEntity
import com.bienhieu.chamcong.data.local.EmployeeDao
import com.bienhieu.chamcong.data.local.EmployeeEntity
import com.bienhieu.chamcong.data.remote.SupabaseSyncManager
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * Repository handling all data operations for Attendance and Employees.
 * Abstracts away the local Room DB and remote Supabase Sync details.
 */
class AttendanceRepository(
    private val employeeDao: EmployeeDao,
    private val attendanceDao: AttendanceDao,
    private val syncManager: SupabaseSyncManager
) {
    // ─── Observables for UI ───

    fun observeTodayRecords(): Flow<List<AttendanceEntity>> =
        attendanceDao.observeToday(startOfTodayMillis())

    fun observeAllEmployees(): Flow<List<EmployeeEntity>> =
        employeeDao.observeAll()

    // ─── Employee Operations ───

    suspend fun getAllEmployees(): List<EmployeeEntity> =
        employeeDao.getAll()

    suspend fun insertEmployee(employee: EmployeeEntity) {
        employeeDao.insert(employee)
    }

    suspend fun updateEmployeeWithSync(employee: EmployeeEntity) {
        employeeDao.update(employee)
        employee.faceVectors?.let { syncManager.updateEmployeeFace(employee.id, it) }
    }

    suspend fun deleteEmployee(employeeId: String) {
        employeeDao.deleteById(employeeId)
    }

    // ─── Attendance Operations ───

    suspend fun insertAttendanceWithSync(record: AttendanceEntity) {
        attendanceDao.insert(record)
        syncManager.syncAttendance() // Push offline records to server
    }

    suspend fun getLatestAttendanceToday(employeeId: String): AttendanceEntity? =
        attendanceDao.getLatestToday(employeeId, startOfTodayMillis())

    // ─── Global Sync ───

    suspend fun syncRemoteEmployees() {
        syncManager.fetchEmployees()
    }

    suspend fun syncOfflineAttendance() {
        syncManager.syncAttendance()
    }

    // ─── Helper ───

    private fun startOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
