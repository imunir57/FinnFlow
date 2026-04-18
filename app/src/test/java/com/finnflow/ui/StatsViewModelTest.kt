package com.finnflow.ui

import app.cash.turbine.test
import com.finnflow.data.model.CategorySummary
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.TransactionRepository
import com.finnflow.ui.stats.StatsPeriod
import com.finnflow.ui.stats.StatsViewModel
import io.mockk.every
import io.mockk.mockk
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

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: TransactionRepository

    private val incomeSummaries = listOf(CategorySummary(1, "Salary", 3000.0, 1))
    private val expenseSummaries = listOf(
        CategorySummary(2, "Food", 500.0, 10),
        CategorySummary(3, "Transport", 200.0, 5)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        every { repo.getCategorySummary(any(), any(), TransactionType.INCOME) } returns flowOf(incomeSummaries)
        every { repo.getCategorySummary(any(), any(), TransactionType.EXPENSE) } returns flowOf(expenseSummaries)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun initialState_loadsMonthlyExpenses() = runTest {
        val vm = StatsViewModel(repo)
        vm.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(StatsPeriod.MONTHLY, state.period)
            assertEquals(TransactionType.EXPENSE, state.selectedType)
            assertEquals(2, state.activeSummary.size)
            assertEquals(700.0, state.totalAmount, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noDailyOption_inStatsPeriod() {
        val values = StatsPeriod.entries.map { it.name }
        assertFalse("DAILY should not exist", values.contains("DAILY"))
        assertTrue(values.contains("MONTHLY"))
        assertTrue(values.contains("ANNUALLY"))
        assertTrue(values.contains("CUSTOM"))
    }

    @Test
    fun onTypeChange_switchesToIncome() = runTest {
        val vm = StatsViewModel(repo)
        vm.onTypeChange(TransactionType.INCOME)
        assertEquals(1, vm.state.value.activeSummary.size)
        assertEquals(3000.0, vm.state.value.totalAmount, 0.001)
    }

    @Test
    fun onPeriodChange_monthly_setsCurrentMonthRange() {
        val vm = StatsViewModel(repo)
        vm.onPeriodChange(StatsPeriod.MONTHLY)
        val state = vm.state.value
        assertEquals(YearMonth.now().atDay(1), state.from)
        assertEquals(YearMonth.now().atEndOfMonth(), state.to)
    }

    @Test
    fun onPeriodChange_annually_setsCurrentYearRange() {
        val vm = StatsViewModel(repo)
        vm.onPeriodChange(StatsPeriod.ANNUALLY)
        val state = vm.state.value
        val today = LocalDate.now()
        assertEquals(today.withDayOfYear(1), state.from)
        assertEquals(today.withDayOfYear(today.lengthOfYear()), state.to)
    }

    @Test
    fun onCustomRange_setsCustomPeriodAndDates() {
        val vm = StatsViewModel(repo)
        val from = LocalDate.of(2024, 1, 1)
        val to = LocalDate.of(2024, 3, 31)
        vm.onCustomRangeChange(from, to)
        assertEquals(StatsPeriod.CUSTOM, vm.state.value.period)
        assertEquals(from, vm.state.value.from)
        assertEquals(to, vm.state.value.to)
    }

    @Test
    fun percentOf_calculatesCorrectly() = runTest {
        val vm = StatsViewModel(repo)
        vm.state.test {
            val state = awaitItem()
            // Food = 500 / 700 = ~71%
            assertEquals(71, state.percentOf(500.0))
            // Transport = 200 / 700 = ~28%
            assertEquals(28, state.percentOf(200.0))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun percentOf_returnsZero_whenTotalIsZero() = runTest {
        every { repo.getCategorySummary(any(), any(), TransactionType.EXPENSE) } returns flowOf(emptyList())
        val vm = StatsViewModel(repo)
        vm.state.test {
            val state = awaitItem()
            assertEquals(0, state.percentOf(100.0))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun currentFrom_currentTo_currentType_exposedCorrectly() {
        val vm = StatsViewModel(repo)
        assertEquals(YearMonth.now().atDay(1), vm.currentFrom)
        assertEquals(YearMonth.now().atEndOfMonth(), vm.currentTo)
        assertEquals(TransactionType.EXPENSE, vm.currentType)
    }
}
