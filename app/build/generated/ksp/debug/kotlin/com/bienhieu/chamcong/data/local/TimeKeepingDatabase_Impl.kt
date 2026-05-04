package com.bienhieu.chamcong.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TimeKeepingDatabase_Impl : TimeKeepingDatabase() {
  private val _employeeDao: Lazy<EmployeeDao> = lazy {
    EmployeeDao_Impl(this)
  }

  private val _attendanceDao: Lazy<AttendanceDao> = lazy {
    AttendanceDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(4,
        "f33a1c98dc4f627cf9789ce5ffca3dd8", "a7f54e96b572e2a688518c9523dbdf2b") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `employees` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `faceVectors` TEXT NOT NULL, `photoPath` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `attendance_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `employeeId` TEXT NOT NULL, `employeeName` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `status` TEXT NOT NULL, `confidence` REAL NOT NULL, `isSynced` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f33a1c98dc4f627cf9789ce5ffca3dd8')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `employees`")
        connection.execSQL("DROP TABLE IF EXISTS `attendance_records`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsEmployees: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsEmployees.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEmployees.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEmployees.put("faceVectors", TableInfo.Column("faceVectors", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEmployees.put("photoPath", TableInfo.Column("photoPath", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEmployees.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysEmployees: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesEmployees: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoEmployees: TableInfo = TableInfo("employees", _columnsEmployees,
            _foreignKeysEmployees, _indicesEmployees)
        val _existingEmployees: TableInfo = read(connection, "employees")
        if (!_infoEmployees.equals(_existingEmployees)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |employees(com.bienhieu.chamcong.data.local.EmployeeEntity).
              | Expected:
              |""".trimMargin() + _infoEmployees + """
              |
              | Found:
              |""".trimMargin() + _existingEmployees)
        }
        val _columnsAttendanceRecords: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsAttendanceRecords.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAttendanceRecords.put("employeeId", TableInfo.Column("employeeId", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAttendanceRecords.put("employeeName", TableInfo.Column("employeeName", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAttendanceRecords.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAttendanceRecords.put("status", TableInfo.Column("status", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAttendanceRecords.put("confidence", TableInfo.Column("confidence", "REAL", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAttendanceRecords.put("isSynced", TableInfo.Column("isSynced", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysAttendanceRecords: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesAttendanceRecords: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoAttendanceRecords: TableInfo = TableInfo("attendance_records",
            _columnsAttendanceRecords, _foreignKeysAttendanceRecords, _indicesAttendanceRecords)
        val _existingAttendanceRecords: TableInfo = read(connection, "attendance_records")
        if (!_infoAttendanceRecords.equals(_existingAttendanceRecords)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |attendance_records(com.bienhieu.chamcong.data.local.AttendanceEntity).
              | Expected:
              |""".trimMargin() + _infoAttendanceRecords + """
              |
              | Found:
              |""".trimMargin() + _existingAttendanceRecords)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "employees",
        "attendance_records")
  }

  public override fun clearAllTables() {
    super.performClear(false, "employees", "attendance_records")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(EmployeeDao::class, EmployeeDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(AttendanceDao::class, AttendanceDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun employeeDao(): EmployeeDao = _employeeDao.value

  public override fun attendanceDao(): AttendanceDao = _attendanceDao.value
}
