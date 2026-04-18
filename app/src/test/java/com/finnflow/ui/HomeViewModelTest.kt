package com.finnflow.ui

import app.cash.turbine.test
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.TransactionRepository
import com.finnflow.ui.home.HomeViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: TransactionRepository
    private lateinit var viewModel: HomeViewModel

    private val april2024 = "2024-04"
    private val sampleTransactions = listOf(
        Transaction(id = 1, type = TransactionType.INCOME, amount = 500.0, date = LocalDate.of(2024, 4, 1), categoryId = 1),
        Transaction(id = 2, type = TransactionType.EXPENSE, amount = 150.0, date = LocalDate.of(2024, 4, 5), categoryId = 2),
        Transaction(id = 3, type = TransactionType.EXPENSE, amount = 75.0, date = LocalDate.of(2024, 4, 5), categoryId = 2)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        every { repo.getTransactionsByMonth(any()) } returns flowOf(sampleTransactions)
        viewModel = HomeViewModel(repo)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun initialState_loadsCurrentMonthTransactions() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(3, state.transactions.size)
            assertEquals(500.0, state.totalIncome, 0.001)
            assertEquals(225.0, state.totalExpense, 0.001)
            assertEquals(275.0, state.balance, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dailyGroups_groupsTransactionsByDate() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.dailyGroups.size) // April 1 and April 5
            assertEquals(1, state.dailyGroups[LocalDate.of(2024, 4, 1)]?.size)
            assertEquals(2, state.dailyGroups[LocalDate.of(2024, 4, 5)]?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun previousMonth_updatesSelectedMonth() = runTest {
        val currentMonth = viewModel.uiState.value.selectedMonth
        viewModel.previousMonth()
        assertEquals(currentMonth.minusMonths(1), viewModel.uiState.value.selectedMonth)
    }

    @Test
    fun nextMonth_updatesSelectedMonth() = runTest {
        val currentMonth = viewModel.uiState.value.selectedMonth
        viewModel.nextMonth()
        assertEquals(currentMonth.plusMonths(1), viewModel.uiState.value.selectedMonth)
    }

    @Test
    fun deleteTransaction_callsRepository() = runTest {
        val tx = sampleTransactions[0]
        viewModel.deleteTransaction(tx)
        verify { repo.deleteTransaction(tx) }
    }
}
