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
    version = 4,
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
        /**
         * Adds the archive flag to both category tables.
         *
         * Everything that exists at upgrade time is by definition still in use, so the default
         * of 0 (not archived) is the correct backfill — no data pass is needed.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                SecureLogger.d(TAG, "Migrating schema 2 -> 3: adding isArchived columns")
                db.execSQL("ALTER TABLE categories ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sub_categories ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Adds the manual ordering column to both category tables.
         *
         * A default of 0 across the board is the correct backfill: every list query orders by
         * `sortOrder` then `name`, so an existing install keeps the alphabetical order it had
         * until the user actually reorders something.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                SecureLogger.d(TAG, "Migrating schema 3 -> 4: adding sortOrder columns")
                db.execSQL("ALTER TABLE categories ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sub_categories ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_2_3, MIGRATION_3_4)

        init {
            SecureLogger.d(TAG, "AppDatabase initialized with schema version 4, database name: $DATABASE_NAME")
        }
    }
}
