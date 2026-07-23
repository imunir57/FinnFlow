package com.finnflow.ui.category

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.finnflow.data.model.Category
import com.finnflow.data.model.SubCategory
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.CategoryRepository
import io.mockk.coVerify
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

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: CategoryRepository

    private val expenseCat = Category(id = 1, name = "Food", type = TransactionType.EXPENSE, iconName = "utensils", colorHex = "#C44536")
    private val incomeCat  = Category(id = 2, name = "Salary", type = TransactionType.INCOME, iconName = "wallet", colorHex = "#4A8A5C")
    private val foodSub1   = SubCategory(id = 10, categoryId = 1, name = "Restaurant")
    private val foodSub2   = SubCategory(id = 11, categoryId = 1, name = "Groceries")
    private val foodSub3   = SubCategory(id = 12, categoryId = 1, name = "Coffee")
    private val foodSub4   = SubCategory(id = 13, categoryId = 1, name = "Bakery")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        every { repo.getAllCategories() }    returns flowOf(listOf(expenseCat, incomeCat))
        every { repo.getAllSubCategories() } returns flowOf(listOf(foodSub1, foodSub2, foodSub3, foodSub4))
        every { repo.getSubCategories(any()) } returns flowOf(emptyList())
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun makeVm(categoryId: Long? = null): CategoryViewModel {
        val handle = if (categoryId != null)
            SavedStateHandle(mapOf("categoryId" to categoryId))
        else
            SavedStateHandle()
        return CategoryViewModel(repo, handle)
    }

    // ── Initial state ──────────────────────────────────────────────────────────

    @Test
    fun `initial state shows expense categories by default`() = runTest {
        val vm = makeVm()
        vm.state.test {
            val state = awaitItem()
            if (state.isLoading) {
                val loaded = awaitItem()
                assertEquals(TransactionType.EXPENSE, loaded.selectedType)
                assertEquals(1, loaded.displayItems.size)
                assertEquals("Food", loaded.displayItems[0].category.name)
            } else {
                assertEquals(TransactionType.EXPENSE, state.selectedType)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `expense and income counts are correct`() = runTest {
        val vm = makeVm()
        vm.state.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()
            assertEquals(1, state.expenseCount)
            assertEquals(1, state.incomeCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `display items include sub count and preview names`() = runTest {
        val vm = makeVm()
        vm.state.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()
            val item = state.displayItems.first()
            assertEquals(4, item.subCount)
            assertEquals(listOf("Restaurant", "Groceries", "Coffee"), item.subPreviewNames)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Type toggle ────────────────────────────────────────────────────────────

    @Test
    fun `setSelectedType switches to income categories`() = runTest {
        val vm = makeVm()
        vm.state.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()

            vm.setSelectedType(TransactionType.INCOME)

            state = awaitItem()
            assertEquals(TransactionType.INCOME, state.selectedType)
            assertEquals(1, state.displayItems.size)
            assertEquals("Salary", state.displayItems[0].category.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `switching back to expense restores expense list`() = runTest {
        val vm = makeVm()
        vm.state.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()

            vm.setSelectedType(TransactionType.INCOME)
            state = awaitItem()
            vm.setSelectedType(TransactionType.EXPENSE)
            state = awaitItem()

            assertEquals(TransactionType.EXPENSE, state.selectedType)
            assertEquals(1, state.displayItems.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Edit sheet ─────────────────────────────────────────────────────────────

    @Test
    fun `openEditSheet with null opens new-category sheet`() = runTest {
        val vm = makeVm()
        vm.state.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()

            vm.openEditSheet(null)
            state = awaitItem()

            assertTrue(state.isEditSheetOpen)
            assertTrue(state.isNewCategory)
            assertNull(state.editingCategory)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `openEditSheet with category opens edit sheet`() = runTest {
        val vm = makeVm()
        vm.state.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()

            vm.openEditSheet(expenseCat)
            state = awaitItem()

            assertTrue(state.isEditSheetOpen)
            assertFalse(state.isNewCategory)
            assertEquals(expenseCat, state.editingCategory)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `closeEditSheet clears sheet state`() = runTest {
        val vm = makeVm()
        vm.state.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()

            vm.openEditSheet(expenseCat)
            state = awaitItem()
            vm.closeEditSheet()
            state = awaitItem()

            assertFalse(state.isEditSheetOpen)
            assertNull(state.editingCategory)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Reorder ────────────────────────────────────────────────────────────────

    @Test
    fun `moveItem swaps two items in memory`() = runTest {
        every { repo.getAllCategories() } returns flowOf(
            listOf(
                expenseCat,
                Category(id = 3, name = "Transport", type = TransactionType.EXPENSE, iconName = "car", colorHex = "#3A6EA5")
            )
        )
        val vm = makeVm()
        vm.state.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()

            val firstName = state.displayItems[0].category.name
            vm.moveItem(0, 1)
            state = awaitItem()

            assertNotEquals(firstName, state.displayItems[0].category.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `moveItem with out-of-bounds index is a no-op`() = runTest {
        val vm = makeVm()
        vm.state.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()
            val before = state.displayItems.size

            vm.moveItem(0, 99)
            // No new emission expected; ensure no crash
            cancelAndIgnoreRemainingEvents()
            assertEquals(before, state.displayItems.size)
        }
    }

    // ── CRUD actions ───────────────────────────────────────────────────────────

    @Test
    fun `addCategory calls repository with correct fields`() = runTest {
        val vm = makeVm()
        vm.addCategory("Gym", TransactionType.EXPENSE, "heart", "#B5456E")
        coVerify { repo.addCategory(Category(name = "Gym", type = TransactionType.EXPENSE, iconName = "heart", colorHex = "#B5456E")) }
    }

    @Test
    fun `deleteCategory calls repository`() = runTest {
        val vm = makeVm()
        vm.deleteCategory(expenseCat)
        coVerify { repo.deleteCategory(expenseCat) }
    }

    @Test
    fun `updateCategory calls repository`() = runTest {
        val updated = expenseCat.copy(name = "Dining")
        val vm = makeVm()
        vm.updateCategory(updated)
        coVerify { repo.updateCategory(updated) }
    }

    // ── Sub-category mode ──────────────────────────────────────────────────────

    @Test
    fun `sub-category mode loads subs for the parent category`() = runTest {
        val subs = listOf(foodSub1, foodSub2)
        every { repo.getSubCategories(1L) } returns flowOf(subs)

        val vm = makeVm(categoryId = 1L)
        vm.state.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()
            assertEquals(1L, state.selectedCategoryId)
            assertEquals(subs, state.subCategories)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addSubCategory calls repository with parent id`() = runTest {
        every { repo.getSubCategories(5L) } returns flowOf(emptyList())
        val vm = makeVm(categoryId = 5L)
        vm.addSubCategory("Breakfast")
        coVerify { repo.addSubCategory(SubCategory(categoryId = 5L, name = "Breakfast")) }
    }
}
