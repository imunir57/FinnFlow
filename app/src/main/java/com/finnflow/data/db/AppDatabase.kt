package com.finnflow.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.finnflow.data.db.dao.CategoryDao
import com.finnflow.data.db.dao.TransactionDao
import com.finnflow.data.db.entity.CategoryEntity
import com.finnflow.data.db.entity.SubCategoryEntity
import com.finnflow.data.db.entity.TransactionEntity
import com.finnflow.data.logger.SecureLogger

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        SubCategoryEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        private const val TAG = "AppDatabase"

        // The name is load-bearing — backup_rules.xml and data_extraction_rules.xml must
        // list exactly this filename (and its -wal/-shm sidecars).
        const val DATABASE_NAME = "finnflow.db"

        /**
         * Every schema change must bump [Database.version] and register a Migration here,
         * written against the schema JSONs exported to app/schemas. There is deliberately
         * no fallbackToDestructiveMigration(): a missing migration must fail loudly in
         * testing rather than silently wiping user data in production. Cover each new
         * migration with a MigrationTestHelper test under src/androidTest.
         */
        val MIGRATIONS: Array<Migration> = emptyArray()

        init {
            SecureLogger.d(TAG, "AppDatabase initialized with schema version 2, database name: $DATABASE_NAME")
        }
    }
}
