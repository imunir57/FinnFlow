package com.finnflow.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnflow.data.db.AppDatabase
import com.finnflow.data.db.dao.TransactionDao
import com.finnflow.data.db.entity.CategoryEntity
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
class TransactionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TransactionDao
    private var testCategoryId: Long = 0L

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.transactionDao()

        // Insert a category to satisfy foreign key
        testCategoryId = db.categoryDao().insertCategory(
            CategoryEntity(name = "Food", type = TransactionType.EXPENSE)
        )
    }

    @After
    fun teardown() = db.close()

    private fun buildTx(
        amount: Double = 100.0,
        type: TransactionType = TransactionType.EXPENSE,
        date: LocalDate = LocalDate.of(2024, 4, 1)
    ) = TransactionEntity(type = type, amount = amount, date = date, categoryId = testCategoryId)

    @Test
    fun insertAndGetById() = runTest {
        val id = dao.insert(buildTx())
        val result = dao.getById(id)
        assertNotNull(result)
        assertEquals(100.0, result!!.amount, 0.001)
    }

    @Test
    fun getByMonth_returnsOnlyMatchingMonth() = runTest {
        dao.insert(buildTx(date = LocalDate.of(2024, 4, 10)))
        dao.insert(buildTx(date = LocalDate.of(2024, 5, 1)))

        val april = dao.getByMonth("2024-04").first()
        assertEquals(1, april.size)
    }

    @Test
    fun deleteTransaction_removesFromDb() = runTest {
        val id = dao.insert(buildTx())
        val tx = dao.getById(id)!!
        dao.delete(tx)
        assertNull(dao.getById(id))
    }

    @Test
    fun updateTransaction_persists() = runTest {
        val id = dao.insert(buildTx(amount = 50.0))
        val tx = dao.getById(id)!!
        dao.update(tx.copy(amount = 200.0))
        assertEquals(200.0, dao.getById(id)!!.amount, 0.001)
    }

    @Test
    fun getByDateRange_filtersCorrectly() = runTest {
        dao.insert(buildTx(date = LocalDate.of(2024, 3, 15)))
        dao.insert(buildTx(date = LocalDate.of(2024, 4, 5)))
        dao.insert(buildTx(date = LocalDate.of(2024, 4, 20)))
        dao.insert(buildTx(date = LocalDate.of(2024, 5, 1)))

        val results = dao.getByDateRange(
            from = LocalDate.of(2024, 4, 1),
            to = LocalDate.of(2024, 4, 30)
        ).first()
        assertEquals(2, results.size)
    }

    @Test
    fun categorySummary_aggregatesCorrectly() = runTest {
        dao.insert(buildTx(amount = 100.0))
        dao.insert(buildTx(amount = 200.0))

        val summary = dao.getCategorySummary(
            from = LocalDate.of(2024, 1, 1),
            to = LocalDate.of(2024, 12, 31),
            type = TransactionType.EXPENSE
        ).first()

        assertEquals(1, summary.size)
        assertEquals(300.0, summary[0].totalAmount, 0.001)
        assertEquals(2, summary[0].transactionCount)
    }
}
