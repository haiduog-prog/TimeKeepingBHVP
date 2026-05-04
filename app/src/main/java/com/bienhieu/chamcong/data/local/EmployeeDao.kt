package com.bienhieu.chamcong.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Employee operations.
 */
@Dao
interface EmployeeDao {

    /** Insert a new employee. Returns the auto-generated row ID (if Long) or -1. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(employee: EmployeeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(employees: List<EmployeeEntity>)

    @Update
    suspend fun update(employee: EmployeeEntity)

    /** Retrieve all registered employees (used for face matching). */
    @Query("SELECT * FROM employees ORDER BY name ASC")
    suspend fun getAll(): List<EmployeeEntity>

    /** Observable stream of all employees for UI display. */
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun observeAll(): Flow<List<EmployeeEntity>>

    /** Find a single employee by ID. */
    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getById(id: String): EmployeeEntity?

    /** Delete an employee by ID. */
    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM employees")
    suspend fun deleteAll()

    /** Total count of registered employees. */
    @Query("SELECT COUNT(*) FROM employees")
    suspend fun count(): Int
}
