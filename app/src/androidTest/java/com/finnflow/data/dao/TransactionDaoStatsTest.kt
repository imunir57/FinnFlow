package com.finnflow.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnflow.data.db.AppDatabase
import com.finnflow.data.db.dao.TransactionDao
import com.finnflow.data.db.entity.CategoryEntity
import com.finnflow.data.db.entity.SubCategoryEntity
import com.finnflow.data.db.entity.TransactionEntity
import com.finnflow.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TransactionDaoStatsTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TransactionDao
    private var categoryId = 0L
    private var subCat1Id = 0L
    private var subCat2Id = 0L

    private val april1 = LocalDate.of(2025, 4, 1)
    private val april30 = LocalDate.of(2025, 4, 30)

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.transactionDao()

        categoryId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Food & Dining", type = TransactionType.EXPENSE)
        )
        subCat1Id = db.categoryDao().insertSubCategory(
            SubCategoryEntity(categoryId = categoryId, name = "Restaurant")
        )
        subCat2Id = db.categoryDao().insertSubCategory(
            SubCategoryEntity(categoryId = categoryId, name = "Groceries")
        )
    }

    @After
    fun teardown() = db.close()

    private fun tx(
        amount: Double,
        subCatId: Long? = null,
        date: LocalDate = april1
    ) = TransactionEntity(
        type = TransactionType.EXPENSE,
        amount = amount,
        date = date,
        categoryId = categoryId,
        subCategoryId = subCatId
    )

    // ── getSubCategorySummary ─────────────────────────────────────────────────

    @Test
    fun getSubCategorySummary_groupsBySubCategory() = runTest {
        dao.insert(tx(500.0, subCat1Id))
        dao.insert(tx(300.0, subCat1Id))
        dao.insert(tx(200.0, subCat2Id))

        val result = dao.getSubCategorySummary(
            categoryId, april1, april30, TransactionType.EXPENSE
        ).first()

        assertEquals(2, result.size)
        val restaurant = result.first { it.subCategoryName == "Restaurant" }
        assertEquals(800.0, restaurant.totalAmount, 0.001)
        assertEquals(2, restaurant.transactionCount)
    }

    @Test
    fun getSubCategorySummary_includesUncategorisedAsNull() = runTest {
        dao.insert(tx(400.0, null)) // no subcategory
        dao.insert(tx(200.0, subCat1Id))

        val result = dao.getSubCategorySummary(
            categoryId, april1, april30, TransactionType.EXPENSE
        ).first()

        val uncategorised = result.firstOrNull { it.subCategoryId == null }
        assertNotNull("Uncategorised row should exist", uncategorised)
        assertEquals("Uncategorised", uncategorised!!.subCategoryName)
        assertEquals(400.0, uncategorised.totalAmount, 0.001)
    }

    @Test
    fun getSubCategorySummary_orderedByAmountDesc() = runTest {
        dao.insert(tx(100.0, subCat1Id))
        dao.insert(tx(900.0, subCat2Id))

        val result = dao.getSubCategorySummary(
            categoryId, april1, april30, TransactionType.EXPENSE
        ).first()

        assertEquals(subCat2Id, result[0].subCategoryId)
        assertEquals(subCat1Id, result[1].subCategoryId)
    }

    @Test
    fun getSubCategorySummary_respectsDateRange() = runTest {
        dao.insert(tx(500.0, subCat1Id, april1))
        dao.insert(tx(500.0, subCat1Id, LocalDate.of(2025, 5, 1))) // outside range

        val result = dao.getSubCategorySummary(
            categoryId, april1, april30, TransactionType.EXPENSE
        ).first()

        assertEquals(1, result.size)
        assertEquals(500.0, result[0].totalAmount, 0.001)
    }

    @Test
    fun getSubCategorySummary_filtersOtherCategories() = runTest {
        val otherCatId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Transport", type = TransactionType.EXPENSE)
        )
        dao.insert(tx(500.0, subCat1Id))
        dao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 999.0,
                date = april1,
                categoryId = otherCatId
            )
        )

        val result = dao.getSubCategorySummary(
            categoryId, april1, april30, TransactionType.EXPENSE
        ).first()

        assertTrue("Should only return Food & Dining subcategories",
            result.all { it.subCategoryId == subCat1Id || it.subCategoryId == null })
    }

    @Test
    fun getSubCategorySummary_filtersIncomeFromExpenseQuery() = runTest {
        dao.insert(tx(500.0, subCat1Id))
        val incomeCatId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Salary", type = TransactionType.INCOME)
        )
        dao.insert(
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 3000.0,
                date = april1,
                categoryId = incomeCatId
            )
        )

        val result = dao.getSubCategorySummary(
            categoryId, april1, april30, TransactionType.EXPENSE
        ).first()

        assertEquals(1, result.size)
        assertEquals(500.0, result[0].totalAmount, 0.001)
    }

    @Test
    fun getSubCategorySummary_returnsEmpty_whenNoTransactions() = runTest {
        val result = dao.getSubCategorySummary(
            categoryId, april1, april30, TransactionType.EXPENSE
        ).first()

        assertTrue(result.isEmpty())
    }

    // ── getTransactionsBySubCategory ──────────────────────────────────────────

    @Test
    fun getTransactionsBySubCategory_returnsCorrectSubCat() = runTest {
        dao.insert(tx(500.0, subCat1Id))
        dao.insert(tx(300.0, subCat1Id))
        dao.insert(tx(200.0, subCat2Id))

        val result = dao.getTransactionsBySubCategory(
            categoryId, subCat1Id, april1, april30, TransactionType.EXPENSE
        ).first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.subCategoryId == subCat1Id })
    }

    @Test
    fun getTransactionsBySubCategory_nullSubCatId_returnsUncategorised() = runTest {
        dao.insert(tx(400.0, null))
        dao.insert(tx(300.0, subCat1Id))

        val result = dao.getTransactionsBySubCategory(
            categoryId, null, april1, april30, TransactionType.EXPENSE
        ).first()

        assertEquals(1, result.size)
        assertNull(result[0].subCategoryId)
        assertEquals(400.0, result[0].amount, 0.001)
    }

    @Test
    fun getTransactionsBySubCategory_orderedByDateDesc() = runTest {
        dao.insert(tx(100.0, subCat1Id, LocalDate.of(2025, 4, 5)))
        dao.insert(tx(200.0, subCat1Id, LocalDate.of(2025, 4, 20)))
        dao.insert(tx(150.0, subCat1Id, LocalDate.of(2025, 4, 10)))

        val result = dao.getTransactionsBySubCategory(
            categoryId, subCat1Id, april1, april30, TransactionType.EXPENSE
        ).first()

        assertEquals(LocalDate.of(2025, 4, 20), result[0].date)
        assertEquals(LocalDate.of(2025, 4, 10), result[1].date)
        assertEquals(LocalDate.of(2025, 4, 5), result[2].date)
    }

    @Test
    fun getTransactionsBySubCategory_respectsDateRange() = runTest {
        dao.insert(tx(500.0, subCat1Id, april1))
        dao.insert(tx(500.0, subCat1Id, LocalDate.of(2025, 3, 31))) // before range

        val result = dao.getTransactionsBySubCategory(
            categoryId, subCat1Id, april1, april30, TransactionType.EXPENSE
        ).first()

        assertEquals(1, result.size)
    }

    @Test
    fun getTransactionsBySubCategory_doesNotCrossCategories() = runTest {
        val otherCatId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Other", type = TransactionType.EXPENSE)
        )
        dao.insert(tx(500.0, subCat1Id))
        dao.insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 999.0,
                date = april1,
                categoryId = otherCatId,
                subCategoryId = subCat1Id
            )
        )

        val result = dao.getTransactionsBySubCategory(
            categoryId, subCat1Id, april1, april30, TransactionType.EXPENSE
        ).first()

        assertEquals(1, result.size)
        assertEquals(categoryId, result[0].categoryId)
    }
}
