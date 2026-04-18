package com.finnflow.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.finnflow.data.model.Category
import com.finnflow.data.model.SubCategorySummary
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.CategoryRepository
import com.finnflow.data.repository.TransactionRepository
import com.finnflow.ui.stats.CategoryDetailUiState
import com.finnflow.ui.stats.CategoryDetailViewModel
import io.mockk.*
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
class CategoryDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var transactionRepo: TransactionRepository
    private lateinit var categoryRepo: CategoryRepository

    private val categoryId = 1L
    private val from = LocalDate.of(2025, 4, 1)
    private val to = LocalDate.of(2025, 4, 30)
    private val type = TransactionType.EXPENSE

    private val summaries = listOf(
        SubCategorySummary(subCategoryId = 10L, subCategoryName = "Restaurant", totalAmount = 3000.0, transactionCount = 5),
        SubCategorySummary(subCategoryId = 11L, subCategoryName = "Groceries", totalAmount = 2000.0, transactionCount = 3),
        SubCategorySummary(subCategoryId = null, subCategoryName = "Uncategorised", totalAmount = 500.0, transactionCount = 1)
    )

    private val restaurantTx = listOf(
        Transaction(id = 1, type = type, amount = 890.0, date = from, categoryId = categoryId, subCategoryId = 10L, note = "Dinner"),
        Transaction(id = 2, type = type, amount = 710.0, date = from.plusDays(3), categoryId = categoryId, subCategoryId = 10L, note = "Lunch")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        transactionRepo = mockk(relaxed = true)
        categoryRepo = mockk(relaxed = true)
        every { transactionRepo.getSubCategorySummary(any(), any(), any(), any()) } returns flowOf(summaries)
        every { transactionRepo.getTransactionsBySubCategory(any(), any(), any(), any(), any()) } returns flowOf(restaurantTx)
        coEvery { categoryRepo.getCategoryById(categoryId) } returns Category(
            id = categoryId, name = "Food & Dining", type = type
        )
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun makeVm() = CategoryDetailViewModel(
        transactionRepo, categoryRepo,
        SavedStateHandle(mapOf(
            "categoryId" to categoryId,
            "from" to from.toString(),
            "to" to to.toString(),
            "type" to type.name
        ))
    )

    @Test
    fun initialState_loadsSubCategorySummaries() = runTest {
        val vm = makeVm()
        vm.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(3, state.summaries.size)
            assertEquals(5500.0, state.totalAmount, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun initialState_nothingIsExpanded() = runTest {
        val vm = makeVm()
        vm.state.test {
            val state = awaitItem()
            assertFalse(state.isExpanded(10L))
            assertFalse(state.isExpanded(11L))
            assertFalse(state.isExpanded(null))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleSubCategory_expandsRow_andLoadsTransactions() = runTest {
        val vm = makeVm()
        vm.toggleSubCategory(10L)
        vm.state.test {
            val state = awaitItem()
            assertTrue(state.isExpanded(10L))
            assertFalse(state.isExpanded(11L))
            cancelAndIgnoreRemainingEvents()
        }
        verify { transactionRepo.getTransactionsBySubCategory(categoryId, 10L, from, to, type) }
    }

    @Test
    fun toggleSubCategory_collapses_whenTappedAgain() = runTest {
        val vm = makeVm()
        vm.toggleSubCategory(10L)
        vm.toggleSubCategory(10L)
        vm.state.test {
            val state = awaitItem()
            assertFalse(state.isExpanded(10L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleSubCategory_switchesExpansion_toNewRow() = runTest {
        val vm = makeVm()
        vm.toggleSubCategory(10L)
        vm.toggleSubCategory(11L)
        vm.state.test {
            val state = awaitItem()
            assertFalse(state.isExpanded(10L))
            assertTrue(state.isExpanded(11L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun transactionsLoaded_cachedOnSecondExpand() = runTest {
        val vm = makeVm()
        vm.toggleSubCategory(10L)
        vm.toggleSubCategory(10L) // collapse
        vm.toggleSubCategory(10L) // re-expand — should use cache
        verify(exactly = 1) { transactionRepo.getTransactionsBySubCategory(categoryId, 10L, from, to, type) }
    }

    @Test
    fun percentOf_calculatesRelativeToTotal() = runTest {
        val vm = makeVm()
        vm.state.test {
            val state = awaitItem()
            // Restaurant 3000 / 5500 ≈ 54%
            assertEquals(54, state.percentOf(3000.0))
            // Uncategorised 500 / 5500 ≈ 9%
            assertEquals(9, state.percentOf(500.0))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun categoryName_loadedFromRepository() = runTest {
        val vm = makeVm()
        vm.categoryName.test {
            skipItems(1) // empty initial
            val name = awaitItem()
            assertEquals("Food & Dining", name)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
