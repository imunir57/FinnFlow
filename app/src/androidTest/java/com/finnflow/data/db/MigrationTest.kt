package com.finnflow.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "migration-test.db"

/**
 * Migrations run against the schema JSONs exported to app/schemas. There is no
 * fallbackToDestructiveMigration(), so a broken migration must fail here rather than wipe a
 * user's data in production.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate2To3_addsArchiveColumnsAndKeepsRows() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO categories (id, name, type, iconName, colorHex)
                VALUES (1, 'Food', 'EXPENSE', 'utensils', '#C44536')
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO sub_categories (id, categoryId, name)
                VALUES (1, 1, 'Groceries')
                """.trimIndent()
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, *AppDatabase.MIGRATIONS)

        db.query("SELECT name, isArchived FROM categories WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Food", cursor.getString(0))
            // Anything that exists at upgrade time is still in use, so it must come across active.
            assertEquals(0, cursor.getInt(1))
        }
        db.query("SELECT name, isArchived FROM sub_categories WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Groceries", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
        }
    }

    @Test
    fun migrate3To4_addsSortOrderColumnsDefaultedToZero() {
        helper.createDatabase(TEST_DB, 3).use { db ->
            db.execSQL(
                """
                INSERT INTO categories (id, name, type, iconName, colorHex, isArchived)
                VALUES (1, 'Food', 'EXPENSE', 'utensils', '#C44536', 0)
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO sub_categories (id, categoryId, name, isArchived)
                VALUES (1, 1, 'Groceries', 0)
                """.trimIndent()
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, *AppDatabase.MIGRATIONS)

        // Zero across the board is the intended backfill: every list query falls back to name,
        // so an existing install keeps the alphabetical order it already had.
        db.query("SELECT name, sortOrder FROM categories WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Food", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
        }
        db.query("SELECT name, sortOrder FROM sub_categories WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Groceries", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
        }
    }

    @Test
    fun migrate2To4_runsBothMigrationsInSequence() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO categories (id, name, type, iconName, colorHex)
                VALUES (1, 'Food', 'EXPENSE', 'utensils', '#C44536')
                """.trimIndent()
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, *AppDatabase.MIGRATIONS)

        db.query("SELECT isArchived, sortOrder FROM categories WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
            assertEquals(0, cursor.getInt(1))
        }
    }

    @Test
    fun migrate2To3_leavesTransactionsUntouched() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO categories (id, name, type, iconName, colorHex)
                VALUES (1, 'Food', 'EXPENSE', 'utensils', '#C44536')
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO transactions (id, type, amount, date, categoryId, subCategoryId, note, fromAccountId, toAccountId)
                VALUES (1, 'EXPENSE', 42.5, '2026-01-01', 1, NULL, 'lunch', NULL, NULL)
                """.trimIndent()
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, *AppDatabase.MIGRATIONS)

        db.query("SELECT amount, note, categoryId FROM transactions WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(42.5, cursor.getDouble(0), 0.001)
            assertEquals("lunch", cursor.getString(1))
            assertEquals(1, cursor.getInt(2))
        }
    }
}
