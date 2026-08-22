package com.finnflow.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnflow.data.model.TransactionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Provider

@RunWith(AndroidJUnit4::class)
class DatabaseSeederTest {

    private companion object {
        /** Long enough for a cold emulator, short enough to fail rather than hang. */
        const val SEED_TIMEOUT_MS = 10_000L
    }

    private lateinit var db: AppDatabase

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Build with a real seeder so the callback fires on onCreate
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(DatabaseSeeder(Provider { db }))
            .build()

        // Trigger onCreate by touching the database
        db.openHelper.writableDatabase

        // The seeder runs on its own IO coroutine and reports no completion, so wait for the
        // last row it writes to appear. Not `runTest` + `delay`: that skips delays outright,
        // and every assertion here then read a table still being filled in.
        withTimeout(SEED_TIMEOUT_MS) {
            while (db.categoryDao().getAllCategories().first().size < SeedData.categories.size) {
                delay(20)
            }
            val lastCategory = db.categoryDao().getAllCategories().first()
                .first { it.name == SeedData.categories.last().name }
            val lastSubCount = SeedData.categories.last().subCategories.size
            while (db.categoryDao().getSubCategories(lastCategory.id).first().size < lastSubCount) {
                delay(20)
            }
        }
    }

    @After
    fun teardown() = db.close()

    @Test
    fun seeder_insertsExpectedCategoryCount() = runTest {
        val all = db.categoryDao().getAllCategories().first()
        assertEquals(SeedData.categories.size, all.size)
    }

    @Test
    fun seeder_preservesCatalogueOrder() = runTest {
        // Seeded rows carry an explicit sortOrder, so the first screen a user sees lists them in
        // the order SeedData declares rather than alphabetically.
        val expenses = db.categoryDao().getCategoriesByType(TransactionType.EXPENSE).first()
        val expected = SeedData.categories
            .filter { it.type == TransactionType.EXPENSE }
            .map { it.name }
        assertEquals(expected, expenses.map { it.name })
    }

    @Test
    fun seeder_insertsAllExpenseCategories() = runTest {
        val expenses = db.categoryDao().getCategoriesByType(TransactionType.EXPENSE).first()
        val expectedCount = SeedData.categories.count { it.type == TransactionType.EXPENSE }
        assertEquals(expectedCount, expenses.size)
    }

    @Test
    fun seeder_insertsAllIncomeCategories() = runTest {
        val income = db.categoryDao().getCategoriesByType(TransactionType.INCOME).first()
        val expectedCount = SeedData.categories.count { it.type == TransactionType.INCOME }
        assertEquals(expectedCount, income.size)
    }

    @Test
    fun seeder_insertsSubCategoriesForTransportation() = runTest {
        val allCats = db.categoryDao().getAllCategories().first()
        val transport = allCats.first { it.name == "Transportation" }
        val subs = db.categoryDao().getSubCategories(transport.id).first()
        val expectedSubs = SeedData.categories.first { it.name == "Transportation" }.subCategories
        assertEquals(expectedSubs.size, subs.size)
        assertTrue(subs.any { it.name == "Bus" })
        assertTrue(subs.any { it.name == "Train" })
    }

    @Test
    fun seeder_insertsSubCategoriesForFood() = runTest {
        val allCats = db.categoryDao().getAllCategories().first()
        val food = allCats.first { it.name == "Food & Dining" }
        val subs = db.categoryDao().getSubCategories(food.id).first()
        assertTrue(subs.any { it.name == "Restaurant" })
        assertTrue(subs.any { it.name == "Groceries" })
    }

    @Test
    fun seeder_totalSubCategoryCount_matchesSeedData() = runTest {
        val allCats = db.categoryDao().getAllCategories().first()
        var totalSubs = 0
        allCats.forEach { cat ->
            totalSubs += db.categoryDao().getSubCategories(cat.id).first().size
        }
        val expectedTotal = SeedData.categories.sumOf { it.subCategories.size }
        assertEquals(expectedTotal, totalSubs)
    }

    @Test
    fun seeder_categoryColors_arePreserved() = runTest {
        val allCats = db.categoryDao().getAllCategories().first()
        val transport = allCats.first { it.name == "Transportation" }
        assertEquals("#2196F3", transport.colorHex)
    }

    @Test
    fun seeder_categoryTypes_arePreserved() = runTest {
        val allCats = db.categoryDao().getAllCategories().first()
        val salary = allCats.first { it.name == "Salary" }
        assertEquals(TransactionType.INCOME, salary.type)
        val food = allCats.first { it.name == "Food & Dining" }
        assertEquals(TransactionType.EXPENSE, food.type)
    }
}
