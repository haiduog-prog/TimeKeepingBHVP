package com.bienhieu.chamcong.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AttendanceDao_Impl(
  __db: RoomDatabase,
) : AttendanceDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfAttendanceEntity: EntityInsertAdapter<AttendanceEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfAttendanceEntity = object : EntityInsertAdapter<AttendanceEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `attendance_records` (`id`,`employeeId`,`employeeName`,`timestamp`,`status`,`confidence`,`isSynced`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: AttendanceEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.employeeId)
        statement.bindText(3, entity.employeeName)
        statement.bindLong(4, entity.timestamp)
        statement.bindText(5, entity.status)
        statement.bindDouble(6, entity.confidence.toDouble())
        val _tmp: Int = if (entity.isSynced) 1 else 0
        statement.bindLong(7, _tmp.toLong())
      }
    }
  }

  public override suspend fun insert(record: AttendanceEntity): Long = performSuspending(__db,
      false, true) { _connection ->
    val _result: Long = __insertAdapterOfAttendanceEntity.insertAndReturnId(_connection, record)
    _result
  }

  public override fun observeToday(startOfDay: Long): Flow<List<AttendanceEntity>> {
    val _sql: String =
        "SELECT * FROM attendance_records WHERE timestamp >= ? ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("attendance_records")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, startOfDay)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfEmployeeId: Int = getColumnIndexOrThrow(_stmt, "employeeId")
        val _columnIndexOfEmployeeName: Int = getColumnIndexOrThrow(_stmt, "employeeName")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfConfidence: Int = getColumnIndexOrThrow(_stmt, "confidence")
        val _columnIndexOfIsSynced: Int = getColumnIndexOrThrow(_stmt, "isSynced")
        val _result: MutableList<AttendanceEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AttendanceEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpEmployeeId: String
          _tmpEmployeeId = _stmt.getText(_columnIndexOfEmployeeId)
          val _tmpEmployeeName: String
          _tmpEmployeeName = _stmt.getText(_columnIndexOfEmployeeName)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpConfidence: Float
          _tmpConfidence = _stmt.getDouble(_columnIndexOfConfidence).toFloat()
          val _tmpIsSynced: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsSynced).toInt()
          _tmpIsSynced = _tmp != 0
          _item =
              AttendanceEntity(_tmpId,_tmpEmployeeId,_tmpEmployeeName,_tmpTimestamp,_tmpStatus,_tmpConfidence,_tmpIsSynced)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getByEmployee(employeeId: String): List<AttendanceEntity> {
    val _sql: String =
        "SELECT * FROM attendance_records WHERE employeeId = ? ORDER BY timestamp DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, employeeId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfEmployeeId: Int = getColumnIndexOrThrow(_stmt, "employeeId")
        val _columnIndexOfEmployeeName: Int = getColumnIndexOrThrow(_stmt, "employeeName")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfConfidence: Int = getColumnIndexOrThrow(_stmt, "confidence")
        val _columnIndexOfIsSynced: Int = getColumnIndexOrThrow(_stmt, "isSynced")
        val _result: MutableList<AttendanceEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AttendanceEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpEmployeeId: String
          _tmpEmployeeId = _stmt.getText(_columnIndexOfEmployeeId)
          val _tmpEmployeeName: String
          _tmpEmployeeName = _stmt.getText(_columnIndexOfEmployeeName)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpConfidence: Float
          _tmpConfidence = _stmt.getDouble(_columnIndexOfConfidence).toFloat()
          val _tmpIsSynced: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsSynced).toInt()
          _tmpIsSynced = _tmp != 0
          _item =
              AttendanceEntity(_tmpId,_tmpEmployeeId,_tmpEmployeeName,_tmpTimestamp,_tmpStatus,_tmpConfidence,_tmpIsSynced)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getLatestToday(employeeId: String, startOfDay: Long):
      AttendanceEntity? {
    val _sql: String =
        "SELECT * FROM attendance_records WHERE employeeId = ? AND timestamp >= ? ORDER BY timestamp DESC LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, employeeId)
        _argIndex = 2
        _stmt.bindLong(_argIndex, startOfDay)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfEmployeeId: Int = getColumnIndexOrThrow(_stmt, "employeeId")
        val _columnIndexOfEmployeeName: Int = getColumnIndexOrThrow(_stmt, "employeeName")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfConfidence: Int = getColumnIndexOrThrow(_stmt, "confidence")
        val _columnIndexOfIsSynced: Int = getColumnIndexOrThrow(_stmt, "isSynced")
        val _result: AttendanceEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpEmployeeId: String
          _tmpEmployeeId = _stmt.getText(_columnIndexOfEmployeeId)
          val _tmpEmployeeName: String
          _tmpEmployeeName = _stmt.getText(_columnIndexOfEmployeeName)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpConfidence: Float
          _tmpConfidence = _stmt.getDouble(_columnIndexOfConfidence).toFloat()
          val _tmpIsSynced: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsSynced).toInt()
          _tmpIsSynced = _tmp != 0
          _result =
              AttendanceEntity(_tmpId,_tmpEmployeeId,_tmpEmployeeName,_tmpTimestamp,_tmpStatus,_tmpConfidence,_tmpIsSynced)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getUnsyncedRecords(): List<AttendanceEntity> {
    val _sql: String = "SELECT * FROM attendance_records WHERE isSynced = 0 ORDER BY timestamp ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfEmployeeId: Int = getColumnIndexOrThrow(_stmt, "employeeId")
        val _columnIndexOfEmployeeName: Int = getColumnIndexOrThrow(_stmt, "employeeName")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfConfidence: Int = getColumnIndexOrThrow(_stmt, "confidence")
        val _columnIndexOfIsSynced: Int = getColumnIndexOrThrow(_stmt, "isSynced")
        val _result: MutableList<AttendanceEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AttendanceEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpEmployeeId: String
          _tmpEmployeeId = _stmt.getText(_columnIndexOfEmployeeId)
          val _tmpEmployeeName: String
          _tmpEmployeeName = _stmt.getText(_columnIndexOfEmployeeName)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpConfidence: Float
          _tmpConfidence = _stmt.getDouble(_columnIndexOfConfidence).toFloat()
          val _tmpIsSynced: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsSynced).toInt()
          _tmpIsSynced = _tmp != 0
          _item =
              AttendanceEntity(_tmpId,_tmpEmployeeId,_tmpEmployeeName,_tmpTimestamp,_tmpStatus,_tmpConfidence,_tmpIsSynced)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markAsSynced(id: Long) {
    val _sql: String = "UPDATE attendance_records SET isSynced = 1 WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
