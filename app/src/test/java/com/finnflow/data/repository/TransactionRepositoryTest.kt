package com.finnflow.data.repository

import app.cash.turbine.test
import com.finnflow.data.db.dao.TransactionDao
import com.finnflow.data.db.entity.TransactionEntity
import com.finnflow.data.model.CategorySummary
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class TransactionRepositoryTest {

    private lateinit var dao: TransactionDao
    private lateinit var repo: TransactionRepositoryImpl

    private val date = LocalDate.of(2024, 4, 1)
    private val domainTx = Transaction(id = 1L, type = TransactionType.EXPENSE, amount = 100.0, date = date, categoryId = 1L)
    private val entityTx = TransactionEntity(id = 1L, type = TransactionType.EXPENSE, amount = 100.0, date = date, categoryId = 1L)

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        repo = TransactionRepositoryImpl(dao)
    }

    @Test
    fun addTransaction_delegatesToDao() = runTest {
        coEvery { dao.insert(any()) } returns 1L
        val id = repo.addTransaction(domainTx)
        assertEquals(1L, id)
        coVerify { dao.insert(TransactionEntity.fromDomain(domainTx)) }
    }

    @Test
    fun updateTransaction_delegatesToDao() = runTest {
        repo.updateTransaction(domainTx)
        coVerify { dao.update(TransactionEntity.fromDomain(domainTx)) }
    }

    @Test
    fun deleteTransaction_delegatesToDao() = runTest {
        repo.deleteTransaction(domainTx)
        coVerify { dao.delete(TransactionEntity.fromDomain(domainTx)) }
    }

    @Test
    fun getTransactionById_mapsToDomain() = runTest {
        coEvery { dao.getById(1L) } returns entityTx
        val result = repo.getTransactionById(1L)
        assertNotNull(result)
        assertEquals(domainTx, result)
    }

    @Test
    fun getTransactionById_returnsNullWhenNotFound() = runTest {
        coEvery { dao.getById(99L) } returns null
        assertNull(repo.getTransactionById(99L))
    }

    @Test
    fun getTransactionsByMonth_emitsMapedDomainList() = runTest {
        every { dao.getByMonth("2024-04") } returns flowOf(listOf(entityTx))
        repo.getTransactionsByMonth("2024-04").test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(domainTx, list[0])
            awaitComplete()
        }
    }

    @Test
    fun getTransactionsByDateRange_emitsMappedList() = runTest {
        every { dao.getByDateRange(any(), any()) } returns flowOf(listOf(entityTx))
        repo.getTransactionsByDateRange(date, date).test {
            assertEquals(1, awaitItem().size)
            awaitComplete()
        }
    }

    @Test
    fun getCategorySummary_delegatesToDao() = runTest {
        val summaries = listOf(CategorySummary(1L, "Food", 300.0, 3))
        every { dao.getCategorySummary(any(), any(), any()) } returns flowOf(summaries)
        repo.getCategorySummary(date, date, TransactionType.EXPENSE).test {
            assertEquals(summaries, awaitItem())
            awaitComplete()
        }
    }
}
