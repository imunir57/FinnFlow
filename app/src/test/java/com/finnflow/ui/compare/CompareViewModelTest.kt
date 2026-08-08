package com.finnflow.ui.compare

import app.cash.turbine.test
import com.finnflow.data.model.Category
import com.finnflow.data.model.CategorySummary
import com.finnflow.data.model.SubCategory
import com.finnflow.data.model.SubCategorySummary
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.CategoryRepository
import com.finnflow.data.repository.TransactionRepository
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
class CompareViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var transactions: TransactionRepository
    private lateinit var categories: CategoryRepository

    private val food = Category(id = 1L, name = "Food", type = TransactionType.EXPENSE, colorHex = "#F44336")
    private val transport = Category(id = 2L, name = "Transport", type = TransactionType.EXPENSE, colorHex = "#2196F3")

    private val foodItem = CompareItem(1L, null, "Food", "#F44336")
    private val transportItem = CompareItem(2L, null, "Transport", "#2196F3")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        transactions = mockk(relaxed = true)
        categories = mockk(relaxed = true)
        every { categories.getCategoriesByType(any()) } returns flowOf(listOf(food, transport))
        every { categories.getAllSubCategories() } returns flowOf(emptyList())
        every { transactions.getCategorySummary(any(), any(), any()) } returns flowOf(
            listOf(
                CategorySummary(1L, "Food", "#F44336", 500.0, 3),
                CategorySummary(2L, "Transport", "#2196F3", 200.0, 2)
            )
        )
        every { transactions.getSubCategorySummary(any(), any(), any(), any()) } returns flowOf(emptyList())
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    // ── Defaults ──────────────────────────────────────────────────────────

    @Test
    fun opensWithTheTwoMostRecentMonths() = runTest {
        val vm = CompareViewModel(transactions, categories)
        vm.state.test {
            val state = awaitItem()
            assertEquals(ComparePeriodMode.MONTH, state.mode)
            assertEquals(DEFAULT_COMPARE_PERIODS, state.periods.size)
            val thisMonth = YearMonth.now()
            assertEquals(
                listOf(ComparePeriod.month(thisMonth.minusMonths(1)), ComparePeriod.month(thisMonth)),
                state.periods
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun switchingToYearResetsToTheTwoMostRecentYears() = runTest {
        val vm = CompareViewModel(transactions, categories)
        vm.state.test {
            awaitItem()
            vm.setMode(ComparePeriodMode.YEAR)
            // setMode moves the mode and the periods, so the combine emits twice — the
            // intermediate frame still carries the old months.
            val state = expectMostRecentItem()
            val thisYear = LocalDate.now().year
            assertEquals(
                listOf(ComparePeriod.year(thisYear - 1), ComparePeriod.year(thisYear)),
                state.periods
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Caps ──────────────────────────────────────────────────────────────

    @Test
    fun periodsAreCappedAndDuplicatesIgnored() = runTest {
        val vm = CompareViewModel(transactions, categories)
        vm.state.test {
            awaitItem()
            val base = YearMonth.now()
            // Already holds 2; adding 3 more fills it, a 6th must be refused.
            (2..6).forEach { vm.addPeriod(ComparePeriod.month(base.minusMonths(it.toLong()))) }
            val state = expectMostRecentItem()
            assertEquals(MAX_COMPARE_PERIODS, state.periods.size)
            assertFalse(state.canAddPeriod)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addingTheSamePeriodTwiceIsANoOp() = runTest {
        val vm = CompareViewModel(transactions, categories)
        vm.state.test {
            val before = awaitItem()
            vm.addPeriod(before.periods.first())
            // A genuine no-op: the list is unchanged, so the StateFlow conflates and nothing
            // is re-emitted. Asserting on a new emission here would be asserting on a bug.
            expectNoEvents()
            assertEquals(before.periods, vm.state.value.periods)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun periodsStayInChronologicalOrderRegardlessOfInsertionOrder() = runTest {
        val vm = CompareViewModel(transactions, categories)
        vm.state.test {
            awaitItem()
            val base = YearMonth.now()
            vm.addPeriod(ComparePeriod.month(base.minusMonths(5)))
            vm.addPeriod(ComparePeriod.month(base.minusMonths(3)))
            val periods = expectMostRecentItem().periods
            assertEquals(periods.sorted(), periods)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun itemsAreCapped() = runTest {
        val vm = CompareViewModel(transactions, categories)
        vm.state.test {
            awaitItem()
            (1..MAX_COMPARE_ITEMS + 2).forEach { i ->
                vm.addItem(CompareItem(i.toLong(), null, "Cat $i", "#F44336"))
            }
            val state = expectMostRecentItem()
            assertEquals(MAX_COMPARE_ITEMS, state.items.size)
            assertFalse(state.canAddItem)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Series building ───────────────────────────────────────────────────

    @Test
    fun buildsOneAmountPerPeriodPerItem() = runTest {
        val vm = CompareViewModel(transactions, categories)
        vm.state.test {
            awaitItem()
            vm.addItem(foodItem)
            vm.addItem(transportItem)
            val state = expectMostRecentItem()
            assertEquals(2, state.series.size)
            state.series.forEach { assertEquals(state.periods.size, it.amounts.size) }
            assertEquals(500.0, state.series.first { it.item.key == foodItem.key }.amounts.first(), 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun anItemMissingFromAPeriodScoresZeroRatherThanBeingDropped() = runTest {
        // A category with no transactions in a period must still occupy its slot, or the bars
        // would silently shift and misalign with their period labels.
        every { transactions.getCategorySummary(any(), any(), any()) } returns flowOf(
            listOf(CategorySummary(1L, "Food", "#F44336", 500.0, 3))
        )
        val vm = CompareViewModel(transactions, categories)
        vm.state.test {
            awaitItem()
            vm.addItem(foodItem)
            vm.addItem(transportItem)
            val state = expectMostRecentItem()
            val transportSeries = state.series.first { it.item.key == transportItem.key }
            assertEquals(state.periods.size, transportSeries.amounts.size)
            assertTrue(transportSeries.amounts.all { it == 0.0 })
            assertFalse(transportSeries.hasAnyData)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun subCategoriesAreAvailableWithoutAnythingHavingCollectedThemFirst() = runTest {
        // Regression: this was exposed with SharingStarted.WhileSubscribed, so nothing started
        // it and the picker read an empty map — every category silently lost its drill-in and
        // subcategories could not be compared at all.
        every { categories.getAllSubCategories() } returns flowOf(
            listOf(
                SubCategory(id = 10L, categoryId = 1L, name = "Coffee"),
                SubCategory(id = 11L, categoryId = 1L, name = "Groceries")
            )
        )
        val vm = CompareViewModel(transactions, categories)
        assertEquals(listOf("Coffee", "Groceries"), vm.subCategoriesByCategory.value[1L]?.map { it.name })
        assertNull(vm.subCategoriesByCategory.value[2L])
    }

    @Test
    fun subCategoryItemsReadFromTheSubCategoryQuery() = runTest {
        every { categories.getAllSubCategories() } returns flowOf(
            listOf(SubCategory(id = 10L, categoryId = 1L, name = "Coffee"))
        )
        every { transactions.getSubCategorySummary(1L, any(), any(), any()) } returns flowOf(
            listOf(SubCategorySummary(10L, "Coffee", 120.0, 4))
        )
        val vm = CompareViewModel(transactions, categories)
        vm.state.test {
            awaitItem()
            vm.addItem(CompareItem(1L, 10L, "Coffee", "#F44336", parentName = "Food"))
            vm.addItem(transportItem)
            val state = expectMostRecentItem()
            val coffee = state.series.first { it.item.subCategoryId == 10L }
            assertEquals(120.0, coffee.amounts.first(), 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Category vs its own subcategories ─────────────────────────────────

    private val coffee = CompareItem(1L, 10L, "Coffee", "#F44336", parentName = "Food")
    private val groceries = CompareItem(1L, 11L, "Groceries", "#F44336", parentName = "Food")

    @Test
    fun pickingAWholeCategoryDropsItsAlreadyPickedSubcategories() = runTest {
        // Charting Food's total alongside part of that same total double-counts, and the
        // tri-state checkbox has no way to depict it.
        val vm = CompareViewModel(transactions, categories)
        vm.state.test {
            awaitItem()
            vm.addItem(coffee)
            vm.addItem(groceries)
            vm.addItem(foodItem)
            val items = expectMostRecentItem().items
            assertEquals(listOf(foodItem.key), items.map { it.key })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun pickingASubcategoryDropsItsWholeCategoryEntry() = runTest {
        val vm = CompareViewModel(transactions, categories)
        vm.state.test {
            awaitItem()
            vm.addItem(foodItem)
            vm.addItem(coffee)
            val items = expectMostRecentItem().items
            assertEquals(listOf(coffee.key), items.map { it.key })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun collapsingSubcategoriesIntoTheirParentIsNeverRefusedByTheCap() = runTest {
        // Five subcategories of one category fill the cap. Switching to the whole category is
        // a net decrease, so checking the limit before pruning would wrongly block it.
        val vm = CompareViewModel(transactions, categories)
        vm.state.test {
            awaitItem()
            (1..MAX_COMPARE_ITEMS).forEach { i ->
                vm.addItem(CompareItem(1L, i.toLong(), "Sub $i", "#F44336", parentName = "Food"))
            }
            assertEquals(MAX_COMPARE_ITEMS, expectMostRecentItem().items.size)
            vm.addItem(foodItem)
            assertEquals(listOf(foodItem.key), expectMostRecentItem().items.map { it.key })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectionHelpersReportWholeVersusPartialCoverage() = runTest {
        val vm = CompareViewModel(transactions, categories)
        vm.state.test {
            awaitItem()
            vm.addItem(coffee)
            assertFalse(vm.isWholeCategorySelected(1L))
            assertEquals(1, vm.selectedSubCount(1L))

            vm.addItem(foodItem)
            assertTrue(vm.isWholeCategorySelected(1L))
            assertEquals(0, vm.selectedSubCount(1L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun switchingTypeClearsItemsBecauseCategoriesAreTypeScoped() = runTest {
        val vm = CompareViewModel(transactions, categories)
        vm.state.test {
            awaitItem()
            vm.addItem(foodItem)
            assertEquals(1, expectMostRecentItem().items.size)
            vm.setType(TransactionType.INCOME)
            assertTrue(expectMostRecentItem().items.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun oneItemIsNotEnoughToCompare() = runTest {
        val vm = CompareViewModel(transactions, categories)
        vm.state.test {
            awaitItem()
            vm.addItem(foodItem)
            assertFalse(expectMostRecentItem().hasEnoughToCompare)
            vm.addItem(transportItem)
            assertTrue(expectMostRecentItem().hasEnoughToCompare)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
