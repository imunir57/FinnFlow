package com.finnflow.ui

import app.cash.turbine.test
import com.finnflow.data.db.dao.MonthlyCategoryTotal
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

    /**
     * January has four income categories so the top-3 cap is actually exercised, and Salary is
     * split across two months so the year fold has something to sum.
     */
    private val incomeCategories = listOf(
        MonthlyCategoryTotal("01", 1L, "Salary", "#4CAF50", 600.0),
        MonthlyCategoryTotal("01", 2L, "Freelance", "#00BCD4", 250.0),
        MonthlyCategoryTotal("01", 3L, "Investment", "#8BC34A", 100.0),
        MonthlyCategoryTotal("01", 4L, "Gifts", "#9E9E9E", 50.0),
        MonthlyCategoryTotal("03", 1L, "Salary", "#4CAF50", 2000.0)
    )
    private val expenseCategories = listOf(
        MonthlyCategoryTotal("02", 5L, "Housing", "#795548", 500.0),
        MonthlyCategoryTotal("04", 6L, "Food", "#F44336", 750.0)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        every { repo.getMonthlyTotalsByYear(any(), TransactionType.INCOME) } returns flowOf(incomeData)
        every { repo.getMonthlyTotalsByYear(any(), TransactionType.EXPENSE) } returns flowOf(expenseData)
        every { repo.getMonthlyCategoryTotals(any(), TransactionType.INCOME) } returns flowOf(incomeCategories)
        every { repo.getMonthlyCategoryTotals(any(), TransactionType.EXPENSE) } returns flowOf(expenseCategories)
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
        vm.state.test {
            val initial = awaitItem()
            val currentYear = initial.year
            vm.previousYear()
            val updated = awaitItem()
            assertEquals(currentYear - 1, updated.year)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun nextYear_incrementsYear() = runTest {
        val vm = YearlyViewModel(repo)
        vm.state.test {
            val initial = awaitItem()
            val currentYear = initial.year
            vm.nextYear()
            val updated = awaitItem()
            assertEquals(currentYear + 1, updated.year)
            cancelAndIgnoreRemainingEvents()
        }
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

    // ── Top categories ────────────────────────────────────────────────────

    @Test
    fun topIncome_sumsAcrossMonthsAndRanksByYearTotal() = runTest {
        val vm = YearlyViewModel(repo)
        vm.state.test {
            val state = awaitItem()
            // Salary is 600 in Jan + 2000 in Mar. The query only orders within a month, so
            // without the re-sort Freelance's single 250 could outrank a split total.
            assertEquals(listOf("Salary", "Freelance", "Investment"), state.topIncome.map { it.name })
            assertEquals(2600.0, state.topIncome.first().amount, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun topIncome_capsAtThree() = runTest {
        val vm = YearlyViewModel(repo)
        vm.state.test {
            val state = awaitItem()
            assertEquals(3, state.topIncome.size)
            assertFalse(state.topIncome.any { it.name == "Gifts" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun topByMonth_capsEachMonthAtThreeAndKeepsQueryOrder() = runTest {
        val vm = YearlyViewModel(repo)
        vm.state.test {
            val state = awaitItem()
            val jan = state.topIncomeByMonth.getValue("01")
            assertEquals(listOf("Salary", "Freelance", "Investment"), jan.map { it.name })
            assertEquals(1, state.topExpenseByMonth.getValue("02").size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun topByMonth_omitsMonthsWithNoActivity() = runTest {
        val vm = YearlyViewModel(repo)
        vm.state.test {
            val state = awaitItem()
            assertFalse(state.topIncomeByMonth.containsKey("12"))
            assertFalse(state.hasBreakdown("12"))
            assertTrue(state.hasBreakdown("01"))
            // February has expenses but no income — still expandable.
            assertTrue(state.hasBreakdown("02"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun categoryColour_isCarriedThroughForRendering() = runTest {
        val vm = YearlyViewModel(repo)
        vm.state.test {
            val state = awaitItem()
            assertEquals("#4CAF50", state.topIncome.first().colorHex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Expand / collapse ─────────────────────────────────────────────────

    /** Computed, not hard-coded — otherwise these pass only during one month of the year. */
    private val currentMonth = "%02d".format(LocalDate.now().monthValue)
    private val otherMonth = if (currentMonth == "01") "02" else "01"

    @Test
    fun currentMonth_isExpandedByDefault() = runTest {
        val vm = YearlyViewModel(repo)
        vm.expandedMonths.test {
            assertEquals(setOf(currentMonth), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun currentMonth_staysCollapsedOnceTheUserClosesIt() = runTest {
        // The reason expansion is stored as overrides rather than a seeded set: a default the
        // user can't override would spring back open.
        val vm = YearlyViewModel(repo)
        vm.expandedMonths.test {
            assertEquals(setOf(currentMonth), awaitItem())
            vm.toggleMonth(currentMonth)
            assertEquals(emptySet<String>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun otherYears_doNotInheritTheCurrentMonthDefault() = runTest {
        val vm = YearlyViewModel(repo)
        vm.expandedMonths.test {
            assertEquals(setOf(currentMonth), awaitItem())
            vm.previousYear()
            assertEquals(emptySet<String>(), awaitItem())
            vm.nextYear()
            assertEquals(setOf(currentMonth), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleMonth_addsThenRemoves() = runTest {
        val vm = YearlyViewModel(repo)
        vm.expandedMonths.test {
            assertEquals(setOf(currentMonth), awaitItem())
            vm.toggleMonth(otherMonth)
            assertEquals(setOf(currentMonth, otherMonth), awaitItem())
            vm.toggleMonth(otherMonth)
            assertEquals(setOf(currentMonth), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setAllExpanded_opensEveryMonthAndCollapsingClearsTheDefaultToo() = runTest {
        val vm = YearlyViewModel(repo)
        vm.state.test { awaitItem(); cancelAndIgnoreRemainingEvents() }

        vm.expandedMonths.test {
            assertEquals(setOf(currentMonth), awaitItem())
            vm.setAllExpanded(true)
            assertEquals(12, awaitItem().size)
            // Collapse all must beat the current-month default, or "Collapse all" would
            // visibly leave one row open.
            vm.setAllExpanded(false)
            assertEquals(emptySet<String>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── avgMonthly computed properties ────────────────────────────────────

    @Test
    fun avgMonthlyIncome_dividesTotalByMonthsWithData() = runTest {
        val vm = YearlyViewModel(repo)
        vm.state.test {
            val state = awaitItem()
            assertEquals(1500.0, state.avgMonthlyIncome, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun avgMonthlyExpense_dividesTotalByMonthsWithData() = runTest {
        val vm = YearlyViewModel(repo)
        vm.state.test {
            val state = awaitItem()
            assertEquals(625.0, state.avgMonthlyExpense, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun avgMonthlyIncome_withNoData_doesNotDivideByZero() = runTest {
        every { repo.getMonthlyTotalsByYear(any(), TransactionType.INCOME) } returns flowOf(emptyList())
        every { repo.getMonthlyTotalsByYear(any(), TransactionType.EXPENSE) } returns flowOf(emptyList())
        every { repo.getMonthlyCategoryTotals(any(), any()) } returns flowOf(emptyList())
        val vm = YearlyViewModel(repo)
        vm.state.test {
            val state = awaitItem()
            assertEquals(0.0, state.avgMonthlyIncome, 0.001)
            assertEquals(0.0, state.avgMonthlyExpense, 0.001)
            assertTrue(state.topIncome.isEmpty())
            assertTrue(state.topExpense.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
