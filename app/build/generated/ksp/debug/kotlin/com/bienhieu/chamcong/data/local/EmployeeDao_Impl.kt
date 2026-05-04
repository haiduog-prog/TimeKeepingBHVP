package com.bienhieu.chamcong.`data`.local

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.FloatArray
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class EmployeeDao_Impl(
  __db: RoomDatabase,
) : EmployeeDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfEmployeeEntity: EntityInsertAdapter<EmployeeEntity>

  private val __vectorTypeConverter: VectorTypeConverter = VectorTypeConverter()

  private val __updateAdapterOfEmployeeEntity: EntityDeleteOrUpdateAdapter<EmployeeEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfEmployeeEntity = object : EntityInsertAdapter<EmployeeEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `employees` (`id`,`name`,`faceVectors`,`photoPath`,`createdAt`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: EmployeeEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        val _tmp: String = __vectorTypeConverter.fromFaceVectors(entity.faceVectors)
        statement.bindText(3, _tmp)
        val _tmpPhotoPath: String? = entity.photoPath
        if (_tmpPhotoPath == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpPhotoPath)
        }
        statement.bindLong(5, entity.createdAt)
      }
    }
    this.__updateAdapterOfEmployeeEntity = object : EntityDeleteOrUpdateAdapter<EmployeeEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `employees` SET `id` = ?,`name` = ?,`faceVectors` = ?,`photoPath` = ?,`createdAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: EmployeeEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        val _tmp: String = __vectorTypeConverter.fromFaceVectors(entity.faceVectors)
        statement.bindText(3, _tmp)
        val _tmpPhotoPath: String? = entity.photoPath
        if (_tmpPhotoPath == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpPhotoPath)
        }
        statement.bindLong(5, entity.createdAt)
        statement.bindText(6, entity.id)
      }
    }
  }

  public override suspend fun insert(employee: EmployeeEntity): Long = performSuspending(__db,
      false, true) { _connection ->
    val _result: Long = __insertAdapterOfEmployeeEntity.insertAndReturnId(_connection, employee)
    _result
  }

  public override suspend fun insertAll(employees: List<EmployeeEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfEmployeeEntity.insert(_connection, employees)
  }

  public override suspend fun update(employee: EmployeeEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __updateAdapterOfEmployeeEntity.handle(_connection, employee)
  }

  public override suspend fun getAll(): List<EmployeeEntity> {
    val _sql: String = "SELECT * FROM employees ORDER BY name ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfFaceVectors: Int = getColumnIndexOrThrow(_stmt, "faceVectors")
        val _columnIndexOfPhotoPath: Int = getColumnIndexOrThrow(_stmt, "photoPath")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<EmployeeEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: EmployeeEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpFaceVectors: List<FloatArray>
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfFaceVectors)
          _tmpFaceVectors = __vectorTypeConverter.toFaceVectors(_tmp)
          val _tmpPhotoPath: String?
          if (_stmt.isNull(_columnIndexOfPhotoPath)) {
            _tmpPhotoPath = null
          } else {
            _tmpPhotoPath = _stmt.getText(_columnIndexOfPhotoPath)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = EmployeeEntity(_tmpId,_tmpName,_tmpFaceVectors,_tmpPhotoPath,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeAll(): Flow<List<EmployeeEntity>> {
    val _sql: String = "SELECT * FROM employees ORDER BY name ASC"
    return createFlow(__db, false, arrayOf("employees")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfFaceVectors: Int = getColumnIndexOrThrow(_stmt, "faceVectors")
        val _columnIndexOfPhotoPath: Int = getColumnIndexOrThrow(_stmt, "photoPath")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<EmployeeEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: EmployeeEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpFaceVectors: List<FloatArray>
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfFaceVectors)
          _tmpFaceVectors = __vectorTypeConverter.toFaceVectors(_tmp)
          val _tmpPhotoPath: String?
          if (_stmt.isNull(_columnIndexOfPhotoPath)) {
            _tmpPhotoPath = null
          } else {
            _tmpPhotoPath = _stmt.getText(_columnIndexOfPhotoPath)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = EmployeeEntity(_tmpId,_tmpName,_tmpFaceVectors,_tmpPhotoPath,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: String): EmployeeEntity? {
    val _sql: String = "SELECT * FROM employees WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfFaceVectors: Int = getColumnIndexOrThrow(_stmt, "faceVectors")
        val _columnIndexOfPhotoPath: Int = getColumnIndexOrThrow(_stmt, "photoPath")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: EmployeeEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpFaceVectors: List<FloatArray>
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfFaceVectors)
          _tmpFaceVectors = __vectorTypeConverter.toFaceVectors(_tmp)
          val _tmpPhotoPath: String?
          if (_stmt.isNull(_columnIndexOfPhotoPath)) {
            _tmpPhotoPath = null
          } else {
            _tmpPhotoPath = _stmt.getText(_columnIndexOfPhotoPath)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _result = EmployeeEntity(_tmpId,_tmpName,_tmpFaceVectors,_tmpPhotoPath,_tmpCreatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun count(): Int {
    val _sql: String = "SELECT COUNT(*) FROM employees"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: String) {
    val _sql: String = "DELETE FROM employees WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM employees"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
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
