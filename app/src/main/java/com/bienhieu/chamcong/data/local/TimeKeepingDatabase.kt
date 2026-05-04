package com.bienhieu.chamcong.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database for the ChamCong application.
 *
 * Contains two tables:
 *  - [EmployeeEntity]   – registered employees with face embeddings
 *  - [AttendanceEntity]  – time attendance log
 *
 * The [VectorTypeConverter] handles FloatArray ↔ ByteArray serialization.
 */
@Database(
    entities = [EmployeeEntity::class, AttendanceEntity::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(VectorTypeConverter::class)
abstract class TimeKeepingDatabase : RoomDatabase() {

    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: TimeKeepingDatabase? = null

        // ─── Migrations ───────────────────────────────────────────────

        /**
         * Migration from version 1 → 2.
         * Added employeeName column to attendance_records table
         * and photoPath column to employees table.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add employeeName to attendance_records (default empty for existing rows)
                db.execSQL(
                    "ALTER TABLE attendance_records ADD COLUMN employeeName TEXT NOT NULL DEFAULT ''"
                )
                // Add photoPath to employees (nullable)
                db.execSQL(
                    "ALTER TABLE employees ADD COLUMN photoPath TEXT DEFAULT NULL"
                )
            }
        }

        /**
         * Migration from version 2 → 3.
         * Added isSynced column to attendance_records for offline-first sync.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE attendance_records ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        // ─── Singleton accessor ───────────────────────────────────────

        /**
         * Thread-safe singleton accessor.
         *
         * Uses explicit migrations to preserve data across schema changes.
         * fallbackToDestructiveMigration is deliberately NOT used to prevent
         * accidental data loss in production.
         */
        fun getInstance(context: Context): TimeKeepingDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TimeKeepingDatabase::class.java,
                    "chamcong.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
