package com.finnflow.ui

import app.cash.turbine.test
import com.finnflow.data.db.dao.MonthlyTotal
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.TransactionRepository
import com.finnflow.ui.yearly.YearlyViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class YearlyViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: TransactionRepository

    private val incomeData = listOf(
        MonthlyTotal("01", 1000.0),
        MonthlyTotal("03", 2000.0)
    )
    private val expenseData = listOf(
        MonthlyTotal("02", 500.0),
        MonthlyTotal("04", 750.0)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        every { repo.getMonthlyTotalsByYear(any(), TransactionType.INCOME) } returns flowOf(incomeData)
        every { repo.getMonthlyTotalsByYear(any(), TransactionType.EXPENSE) } returns flowOf(expenseData)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun initialState_loadsCurrentYear() = runTest {
        val vm = YearlyViewModel(repo)
        vm.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(LocalDate.now().year, state.year)
            assertEquals(3000.0, state.totalIncome, 0.001)
            assertEquals(1250.0, state.totalExpense, 0.001)
            assertEquals(1750.0, state.netBalance, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun previousYear_decrementsYear() = runTest {
        val vm = YearlyViewModel(repo)
        val currentYear = vm.state.value.year
        vm.previousYear()
        assertEquals(currentYear - 1, vm.state.value.year)
    }

    @Test
    fun nextYear_incrementsYear() = runTest {
        val vm = YearlyViewModel(repo)
        val currentYear = vm.state.value.year
        vm.nextYear()
        assertEquals(currentYear + 1, vm.state.value.year)
    }

    @Test
    fun monthlyData_correctlyMappedByMonth() = runTest {
        val vm = YearlyViewModel(repo)
        vm.state.test {
            val state = awaitItem()
            val jan = state.incomeByMonth.firstOrNull { it.month == "01" }
            assertNotNull(jan)
            assertEquals(1000.0, jan!!.total, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
