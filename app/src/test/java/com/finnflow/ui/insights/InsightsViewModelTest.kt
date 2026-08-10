package com.finnflow.ui.insights

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.finnflow.data.model.CategorySummary
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: TransactionRepository

    private val currentYm = YearMonth.now()
    private val prevYm = currentYm.minusMonths(1)
    private val today = LocalDate.now()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        every { repo.getTransactionsByMonth(any()) } returns flowOf(emptyList())
        every { repo.getCategorySummary(any(), any(), any()) } returns flowOf(emptyList())
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun makeVm(month: String? = null) = InsightsViewModel(
        repo,
        SavedStateHandle(month?.let { mapOf("month" to it) } ?: emptyMap())
    )

    // ── InsightsUiState computed properties (pure unit tests) ─────────────────

    @Test
    fun `savingsRate is zero when no income`() {
        val state = InsightsUiState(income = 0.0, expense = 500.0, isLoading = false)
        assertEquals(0.0, state.savingsRate, 0.001)
    }

    @Test
    fun `savingsRate positive when income exceeds expense`() {
        val state = InsightsUiState(income = 10000.0, expense = 7000.0, isLoading = false)
        assertEquals(30.0, state.savingsRate, 0.001)
    }

    @Test
    fun `savingsRate negative when expense exceeds income`() {
        val state = InsightsUiState(income = 1000.0, expense = 1500.0, isLoading = false)
        assertEquals(-50.0, state.savingsRate, 0.001)
    }

    @Test
    fun `peakDayIndex finds day with highest spend`() {
        val state = InsightsUiState(
            dailyTotals = listOf(100.0, 0.0, 500.0, 200.0, 50.0),
            todayDayIndex = 4, isLoading = false
        )
        assertEquals(2, state.peakDayIndex)
    }

    @Test
    fun `avgDailySpend divides total by days with data only`() {
        val state = InsightsUiState(
            dailyTotals = listOf(300.0, 0.0, 600.0, 0.0, 0.0),
            todayDayIndex = 4, isLoading = false
        )
        assertEquals(450.0, state.avgDailySpend, 0.01)
    }

    @Test
    fun `daysRecorded counts only days up to today with nonzero spend`() {
        val state = InsightsUiState(
            dailyTotals = listOf(100.0, 0.0, 200.0, 300.0, 0.0, 400.0),
            todayDayIndex = 3, isLoading = false
        )
        assertEquals(3, state.daysRecorded)
    }

    @Test
    fun `quietestDayIndex returns minus one when no spend`() {
        val state = InsightsUiState(
            dailyTotals = listOf(0.0, 0.0, 0.0),
            todayDayIndex = 2, isLoading = false
        )
        assertEquals(-1, state.quietestDayIndex)
    }

    @Test
    fun `quietestDayIndex finds minimum nonzero day`() {
        val state = InsightsUiState(
            dailyTotals = listOf(500.0, 100.0, 300.0),
            todayDayIndex = 2, isLoading = false
        )
        assertEquals(1, state.quietestDayIndex)
    }

    // ── ViewModel flow tests ──────────────────────────────────────────────────

    @Test
    fun `default InsightsUiState has isLoading true`() {
        assertTrue(InsightsUiState().isLoading)
    }

    @Test
    fun `state reflects income and expense from transactions`() = runTest {
        val txns = listOf(
            Transaction(id = 1, type = TransactionType.INCOME, amount = 50000.0, date = today, categoryId = 101),
            Transaction(id = 2, type = TransactionType.EXPENSE, amount = 10000.0, date = today, categoryId = 1),
            Transaction(id = 3, type = TransactionType.EXPENSE, amount = 5000.0, date = today, categoryId = 2),
        )
        every { repo.getTransactionsByMonth(any()) } returns flowOf(txns)

        val vm = makeVm()
        vm.uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()
            assertEquals(50000.0, state.income, 0.01)
            assertEquals(15000.0, state.expense, 0.01)
            assertEquals(false, state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `opens on the month passed in from Stats`() = runTest {
        val vm = makeVm("2025-03")
        vm.uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()
            assertEquals("March 2025", state.monthLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `falls back to the current month when the argument is unusable`() = runTest {
        val vm = makeVm("not-a-month")
        vm.uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()
            assertEquals(
                currentYm.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")),
                state.monthLabel
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty transactions yield zero income and expense`() = runTest {
        val vm = makeVm()
        vm.uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()
            assertEquals(0.0, state.income, 0.01)
            assertEquals(0.0, state.expense, 0.01)
            assertNull(state.biggestExpenseDate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `biggest expense is the transaction with highest amount`() = runTest {
        val txns = listOf(
            Transaction(id = 1, type = TransactionType.EXPENSE, amount = 1000.0, date = today, categoryId = 1),
            Transaction(id = 2, type = TransactionType.EXPENSE, amount = 5000.0, date = today, categoryId = 2),
            Transaction(id = 3, type = TransactionType.EXPENSE, amount = 500.0, date = today, categoryId = 1),
        )
        every { repo.getTransactionsByMonth(any()) } returns flowOf(txns)

        val vm = makeVm()
        vm.uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()
            assertEquals(5000.0, state.biggestExpenseAmount, 0.01)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `weekday totals aggregate expenses by day of week`() = runTest {
        // Find a Monday and Sunday within currentYm to ensure they pass the date range check
        val monday = (1..currentYm.lengthOfMonth())
            .map { currentYm.atDay(it) }
            .first { it.dayOfWeek == DayOfWeek.MONDAY }
        val sunday = (1..currentYm.lengthOfMonth())
            .map { currentYm.atDay(it) }
            .firstOrNull { it.dayOfWeek == DayOfWeek.SUNDAY } ?: monday.plusDays(6)

        val txns = listOf(
            Transaction(id = 1, type = TransactionType.EXPENSE, amount = 800.0, date = monday, categoryId = 1),
            Transaction(id = 2, type = TransactionType.EXPENSE, amount = 400.0, date = sunday, categoryId = 1),
        )
        every { repo.getTransactionsByMonth(any()) } returns flowOf(txns)

        val vm = makeVm()
        vm.uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()
            assertEquals(800.0, state.weekdayTotals[1], 0.01) // Monday = index 1
            assertEquals(400.0, state.weekdayTotals[0], 0.01) // Sunday = index 0
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `most frequent category is the one with most expense transactions`() = runTest {
        val txns = listOf(
            Transaction(id = 1, type = TransactionType.EXPENSE, amount = 100.0, date = today, categoryId = 1),
            Transaction(id = 2, type = TransactionType.EXPENSE, amount = 200.0, date = today, categoryId = 1),
            Transaction(id = 3, type = TransactionType.EXPENSE, amount = 300.0, date = today, categoryId = 2),
        )
        val summaries = listOf(
            CategorySummary(1, "Food & Dining", "#FF9800", 300.0, 2),
            CategorySummary(2, "Transport", "#3F51B5", 300.0, 1)
        )
        every { repo.getTransactionsByMonth(any()) } returns flowOf(txns)
        every { repo.getCategorySummary(currentYm.atDay(1), currentYm.atEndOfMonth(), TransactionType.EXPENSE) } returns flowOf(summaries)

        val vm = makeVm()
        vm.uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()
            assertEquals("Food & Dining", state.mostFrequentCategoryName)
            assertEquals(2, state.mostFrequentCategoryCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `MoM highlights category with biggest increase`() = runTest {
        val curExpense = listOf(
            CategorySummary(3, "Housing", "#795548", 22000.0, 2),
            CategorySummary(1, "Food", "#FF9800", 5000.0, 8)
        )
        val prevExpense = listOf(
            CategorySummary(3, "Housing", "#795548", 18000.0, 2),
            CategorySummary(1, "Food", "#FF9800", 4800.0, 7)
        )
        every { repo.getCategorySummary(currentYm.atDay(1), currentYm.atEndOfMonth(), TransactionType.EXPENSE) } returns flowOf(curExpense)
        every { repo.getCategorySummary(prevYm.atDay(1), prevYm.atEndOfMonth(), TransactionType.EXPENSE) } returns flowOf(prevExpense)

        val vm = makeVm()
        vm.uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()
            assertEquals("Housing", state.biggestMoMCategoryName)
            assertEquals(4000.0, state.biggestMoMDelta, 0.01)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `previousMonth changes month label`() = runTest {
        val vm = makeVm()
        vm.uiState.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()
            val originalLabel = state.monthLabel

            vm.previousMonth()

            state = awaitItem()
            if (state.isLoading) state = awaitItem()
            assertNotEquals(originalLabel, state.monthLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
