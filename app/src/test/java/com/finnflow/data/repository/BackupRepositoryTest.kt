package com.finnflow.data.repository

import com.finnflow.data.db.AppDatabase
import com.finnflow.data.db.dao.CategoryDao
import com.finnflow.data.db.dao.TransactionDao
import com.finnflow.data.db.entity.CategoryEntity
import com.finnflow.data.db.entity.SubCategoryEntity
import com.finnflow.data.db.entity.TransactionEntity
import com.finnflow.data.model.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate

class BackupRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var repo: BackupRepositoryImpl

    private val category = CategoryEntity(
        id = 1L, name = "Food", type = TransactionType.EXPENSE, iconName = "food", colorHex = "#123456"
    )
    private val subCategory = SubCategoryEntity(id = 1L, categoryId = 1L, name = "Groceries")
    private val transaction = TransactionEntity(
        id = 1L,
        type = TransactionType.EXPENSE,
        amount = 42.5,
        date = LocalDate.of(2026, 1, 1),
        categoryId = 1L,
        subCategoryId = 1L,
        note = "test",
        fromAccountId = null,
        toAccountId = null
    )

    @Before
    fun setup() {
        db = mockk(relaxed = true)
        transactionDao = mockk(relaxed = true)
        categoryDao = mockk(relaxed = true)
        repo = BackupRepositoryImpl(db, transactionDao, categoryDao)

        // Room's withTransaction is an inline extension and can't be mocked directly;
        // swap the repo's transaction indirection for one that just runs the block.
        repo.transactionRunner = { block -> block() }
    }

    // ── exportBackup ─────────────────────────────────────────────────────

    @Test
    fun exportBackup_producesExpectedJsonShape() = runTest {
        every { categoryDao.getAllCategories() } returns flowOf(listOf(category))
        every { categoryDao.getAllSubCategories() } returns flowOf(listOf(subCategory))
        every { transactionDao.getAll() } returns flowOf(listOf(transaction))

        val out = ByteArrayOutputStream()
        repo.exportBackup(out)

        val json = out.toString(Charsets.UTF_8.name())
        assertTrue(json.contains("\"categories\""))
        assertTrue(json.contains("\"Food\""))
        assertTrue(json.contains("\"subCategories\""))
        assertTrue(json.contains("\"Groceries\""))
        assertTrue(json.contains("\"transactions\""))
        assertTrue(json.contains("\"2026-01-01\""))
        assertTrue(json.contains("\"EXPENSE\""))
    }

    // ── restoreBackup: malformed input ──────────────────────────────────

    @Test
    fun restoreBackup_withMalformedJson_leavesDbUntouched() = runTest {
        val input = ByteArrayInputStream("not valid json".toByteArray())

        val result = repo.restoreBackup(input)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { transactionDao.deleteAll() }
        coVerify(exactly = 0) { categoryDao.deleteAllCategories() }
        coVerify(exactly = 0) { categoryDao.deleteAllSubCategories() }
        coVerify(exactly = 0) { categoryDao.insertAllCategories(any()) }
        coVerify(exactly = 0) { categoryDao.insertAllSubCategories(any()) }
        coVerify(exactly = 0) { transactionDao.insertAll(any()) }
    }

    @Test
    fun restoreBackup_withDanglingCategoryReference_leavesDbUntouched() = runTest {
        val payloadJson = """
            {
              "version": 1,
              "categories": [],
              "subCategories": [],
              "transactions": [
                {"id":1,"type":"EXPENSE","amount":10.0,"date":"2026-01-01","categoryId":99,"subCategoryId":null,"note":"","fromAccountId":null,"toAccountId":null}
              ]
            }
        """.trimIndent()
        val input = ByteArrayInputStream(payloadJson.toByteArray())

        val result = repo.restoreBackup(input)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { transactionDao.deleteAll() }
        coVerify(exactly = 0) { transactionDao.insertAll(any()) }
    }

    // ── restoreBackup: round trip ────────────────────────────────────────

    @Test
    fun restoreBackup_roundTrip_callsDaoMethodsInOrder() = runTest {
        every { categoryDao.getAllCategories() } returns flowOf(listOf(category))
        every { categoryDao.getAllSubCategories() } returns flowOf(listOf(subCategory))
        every { transactionDao.getAll() } returns flowOf(listOf(transaction))

        val out = ByteArrayOutputStream()
        repo.exportBackup(out)

        val input = ByteArrayInputStream(out.toByteArray())
        val result = repo.restoreBackup(input)

        assertTrue(result.isSuccess)

        coVerifyOrder {
            transactionDao.deleteAll()
            categoryDao.deleteAllSubCategories()
            categoryDao.deleteAllCategories()
            categoryDao.insertAllCategories(listOf(category))
            categoryDao.insertAllSubCategories(listOf(subCategory))
            transactionDao.insertAll(listOf(transaction))
        }
    }

    @Test
    fun restoreBackup_dbFailure_returnsFailureResult() = runTest {
        val payloadJson = """
            {
              "version": 1,
              "categories": [{"id":1,"name":"Food","type":"EXPENSE","iconName":"","colorHex":"#607D8B"}],
              "subCategories": [],
              "transactions": []
            }
        """.trimIndent()
        coEvery { categoryDao.insertAllCategories(any()) } throws RuntimeException("db error")

        val result = repo.restoreBackup(ByteArrayInputStream(payloadJson.toByteArray()))

        assertTrue(result.isFailure)
        assertEquals("db error", result.exceptionOrNull()?.message)
    }

    // ── eraseAllData ─────────────────────────────────────────────────────

    @Test
    fun eraseAllData_clearsEveryTableThenReSeeds() = runTest {
        repo.eraseAllData()

        coVerifyOrder {
            transactionDao.deleteAll()
            categoryDao.deleteAllSubCategories()
            categoryDao.deleteAllCategories()
            // Emptying the tables leaves the database file in place, so Room's onCreate seeding
            // never fires again — the erase has to put the defaults back itself.
            categoryDao.insertCategory(any())
        }
        coVerify(atLeast = 1) { categoryDao.insertSubCategory(any()) }
    }

    @Test
    fun eraseAllData_propagatesDbFailure() = runTest {
        coEvery { transactionDao.deleteAll() } throws RuntimeException("db locked")

        val error = runCatching { repo.eraseAllData() }.exceptionOrNull()

        assertEquals("db locked", error?.message)
        coVerify(exactly = 0) { categoryDao.insertCategory(any()) }
    }
}
