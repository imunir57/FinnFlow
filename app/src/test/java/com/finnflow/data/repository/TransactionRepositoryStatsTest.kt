package com.finnflow.data.repository

import app.cash.turbine.test
import com.finnflow.data.db.dao.TransactionDao
import com.finnflow.data.db.entity.TransactionEntity
import com.finnflow.data.model.SubCategorySummary
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class TransactionRepositoryStatsTest {

    private lateinit var dao: TransactionDao
    private lateinit var repo: TransactionRepositoryImpl

    private val date = LocalDate.of(2025, 4, 1)
    private val to = LocalDate.of(2025, 4, 30)
    private val categoryId = 1L
    private val subCategoryId = 10L
    private val type = TransactionType.EXPENSE

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        repo = TransactionRepositoryImpl(dao)
    }

    // ── getSubCategorySummary ─────────────────────────────────────────────────

    @Test
    fun getSubCategorySummary_delegatesToDao() = runTest {
        val summaries = listOf(
            SubCategorySummary(10L, "Restaurant", 3000.0, 5),
            SubCategorySummary(null, "Uncategorised", 500.0, 1)
        )
        every { dao.getSubCategorySummary(categoryId, date, to, type) } returns flowOf(summaries)

        repo.getSubCategorySummary(categoryId, date, to, type).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("Restaurant", result[0].subCategoryName)
            assertNull(result[1].subCategoryId)
            awaitComplete()
        }

        verify { dao.getSubCategorySummary(categoryId, date, to, type) }
    }

    @Test
    fun getSubCategorySummary_emptyList_whenNoData() = runTest {
        every { dao.getSubCategorySummary(any(), any(), any(), any()) } returns flowOf(emptyList())

        repo.getSubCategorySummary(categoryId, date, to, type).test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    // ── getTransactionsBySubCategory ──────────────────────────────────────────

    @Test
    fun getTransactionsBySubCategory_mapsToDomain() = runTest {
        val entity = TransactionEntity(
            id = 1L,
            type = type,
            amount = 890.0,
            date = date,
            categoryId = categoryId,
            subCategoryId = subCategoryId,
            note = "Dinner"
        )
        every {
            dao.getTransactionsBySubCategory(categoryId, subCategoryId, date, to, type)
        } returns flowOf(listOf(entity))

        repo.getTransactionsBySubCategory(categoryId, subCategoryId, date, to, type).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            val tx = result[0]
            assertEquals(1L, tx.id)
            assertEquals(890.0, tx.amount, 0.001)
            assertEquals(subCategoryId, tx.subCategoryId)
            assertEquals("Dinner", tx.note)
            awaitComplete()
        }
    }

    @Test
    fun getTransactionsBySubCategory_nullSubCat_passedCorrectlyToDao() = runTest {
        every {
            dao.getTransactionsBySubCategory(categoryId, null, date, to, type)
        } returns flowOf(emptyList())

        repo.getTransactionsBySubCategory(categoryId, null, date, to, type).test {
            awaitItem()
            awaitComplete()
        }

        verify { dao.getTransactionsBySubCategory(categoryId, null, date, to, type) }
    }

    @Test
    fun getTransactionsBySubCategory_mapsMultipleEntities() = runTest {
        val entities = listOf(
            TransactionEntity(id = 1L, type = type, amount = 500.0, date = date,
                categoryId = categoryId, subCategoryId = subCategoryId),
            TransactionEntity(id = 2L, type = type, amount = 300.0, date = date.plusDays(1),
                categoryId = categoryId, subCategoryId = subCategoryId)
        )
        every {
            dao.getTransactionsBySubCategory(categoryId, subCategoryId, date, to, type)
        } returns flowOf(entities)

        repo.getTransactionsBySubCategory(categoryId, subCategoryId, date, to, type).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals(500.0, result[0].amount, 0.001)
            assertEquals(300.0, result[1].amount, 0.001)
            awaitComplete()
        }
    }
}
