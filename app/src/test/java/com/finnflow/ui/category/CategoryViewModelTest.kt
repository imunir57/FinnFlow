package com.finnflow.ui.category

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.finnflow.data.model.Category
import com.finnflow.data.model.SubCategory
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.CategoryRepository
import io.mockk.coEvery
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

    // ── Archived split ─────────────────────────────────────────────────────────

    @Test
    fun `archived categories are listed separately from active ones`() = runTest {
        val archived = Category(id = 3, name = "Old Gym", type = TransactionType.EXPENSE, isArchived = true)
        every { repo.getAllCategories() } returns flowOf(listOf(expenseCat, archived, incomeCat))

        val vm = makeVm()
        vm.state.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()

            assertEquals(listOf("Food"), state.displayItems.map { it.category.name })
            assertEquals(listOf("Old Gym"), state.archivedItems.map { it.category.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `type toggle counts only pickable categories`() = runTest {
        val archived = Category(id = 3, name = "Old Gym", type = TransactionType.EXPENSE, isArchived = true)
        every { repo.getAllCategories() } returns flowOf(listOf(expenseCat, archived, incomeCat))

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
    fun `archived sub-categories are listed separately from active ones`() = runTest {
        every { repo.getSubCategories(1L) } returns flowOf(
            listOf(foodSub1, foodSub2.copy(isArchived = true))
        )

        val vm = makeVm(categoryId = 1L)
        vm.state.test {
            var state = awaitItem()
            if (state.isLoading) state = awaitItem()

            assertEquals(listOf("Restaurant"), state.subCategories.map { it.name })
            assertEquals(listOf("Groceries"), state.archivedSubCategories.map { it.name })
            cancelAndIgnoreRemainingEvents()
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
    fun `archiveCategory archives rather than deletes`() = runTest {
        val vm = makeVm()
        vm.archiveCategory(expenseCat)
        coVerify { repo.setCategoryArchived(expenseCat.id, true) }
    }

    @Test
    fun `archiveCategory reports the result`() = runTest {
        val vm = makeVm()
        vm.messages.test {
            vm.archiveCategory(expenseCat)
            assertEquals("\"Food\" archived", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `archiveCategory reports a failure`() = runTest {
        coEvery { repo.setCategoryArchived(any(), any()) } throws IllegalStateException("db locked")
        val vm = makeVm()
        vm.messages.test {
            vm.archiveCategory(expenseCat)
            assertEquals("Couldn't archive \"Food\"", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restoreCategory clears the archive flag`() = runTest {
        val vm = makeVm()
        vm.restoreCategory(expenseCat.copy(isArchived = true))
        coVerify { repo.setCategoryArchived(expenseCat.id, false) }
    }

    @Test
    fun `archiveSubCategory archives rather than deletes`() = runTest {
        every { repo.getSubCategories(1L) } returns flowOf(listOf(foodSub1))
        val vm = makeVm(categoryId = 1L)
        vm.archiveSubCategory(foodSub1)
        coVerify { repo.setSubCategoryArchived(foodSub1.id, true) }
    }

    @Test
    fun `restoreSubCategory clears the archive flag`() = runTest {
        every { repo.getSubCategories(1L) } returns flowOf(listOf(foodSub1))
        val vm = makeVm(categoryId = 1L)
        vm.restoreSubCategory(foodSub1.copy(isArchived = true))
        coVerify { repo.setSubCategoryArchived(foodSub1.id, false) }
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

    // ── Reordering ─────────────────────────────────────────────────────────────

    private val rentCat = Category(id = 3, name = "Rent", type = TransactionType.EXPENSE)
    private val travelCat = Category(id = 4, name = "Travel", type = TransactionType.EXPENSE)

    @Test
    fun `moveCategory rearranges state without touching the repository`() = runTest {
        every { repo.getAllCategories() } returns flowOf(listOf(expenseCat, rentCat, travelCat))
        val vm = makeVm()

        vm.moveCategory(2, 0)

        assertEquals(
            listOf("Travel", "Food", "Rent"),
            vm.state.value.displayItems.map { it.category.name }
        )
        // A drag calls this once per row crossed; writing here would be a write per crossing.
        coVerify(exactly = 0) { repo.reorderCategories(any()) }
    }

    @Test
    fun `successive moves compose, as they do during one drag`() = runTest {
        every { repo.getAllCategories() } returns flowOf(listOf(expenseCat, rentCat, travelCat))
        val vm = makeVm()

        vm.moveCategory(0, 1)
        vm.moveCategory(1, 2)

        assertEquals(
            listOf("Rent", "Travel", "Food"),
            vm.state.value.displayItems.map { it.category.name }
        )
    }

    @Test
    fun `commitCategoryOrder writes every id in list order`() = runTest {
        every { repo.getAllCategories() } returns flowOf(listOf(expenseCat, rentCat, travelCat))
        val vm = makeVm()

        vm.moveCategory(0, 1)
        vm.commitCategoryOrder()

        // Every id is written, not just the moved one — untouched rows still sit at the
        // default 0 and would otherwise fall back to alphabetical around the moved row.
        coVerify { repo.reorderCategories(listOf(3L, 1L, 4L)) }
    }

    @Test
    fun `moveCategory ignores out-of-range indices`() = runTest {
        every { repo.getAllCategories() } returns flowOf(listOf(expenseCat, rentCat))
        val vm = makeVm()

        vm.moveCategory(0, 5)
        vm.moveCategory(-1, 0)
        vm.moveCategory(1, 1)

        assertEquals(listOf("Food", "Rent"), vm.state.value.displayItems.map { it.category.name })
    }

    @Test
    fun `commitCategoryOrder reports a failed write`() = runTest {
        every { repo.getAllCategories() } returns flowOf(listOf(expenseCat, rentCat))
        coEvery { repo.reorderCategories(any()) } throws IllegalStateException("db locked")
        val vm = makeVm()

        vm.messages.test {
            vm.moveCategory(0, 1)
            vm.commitCategoryOrder()
            assertEquals("Couldn't save the new order", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `commitCategoryOrder on an empty list writes nothing`() = runTest {
        every { repo.getAllCategories() } returns flowOf(emptyList())
        val vm = makeVm()

        vm.commitCategoryOrder()

        coVerify(exactly = 0) { repo.reorderCategories(any()) }
    }

    @Test
    fun `moveSubCategory rearranges state and commit writes the new order`() = runTest {
        every { repo.getSubCategories(1L) } returns flowOf(listOf(foodSub1, foodSub2, foodSub3))
        val vm = makeVm(categoryId = 1L)

        vm.moveSubCategory(2, 0)

        assertEquals(
            listOf("Coffee", "Restaurant", "Groceries"),
            vm.state.value.subCategories.map { it.name }
        )
        coVerify(exactly = 0) { repo.reorderSubCategories(any()) }

        vm.commitSubCategoryOrder()
        coVerify { repo.reorderSubCategories(listOf(12L, 10L, 11L)) }
    }

    @Test
    fun `moveSubCategory ignores out-of-range indices`() = runTest {
        every { repo.getSubCategories(1L) } returns flowOf(listOf(foodSub1, foodSub2))
        val vm = makeVm(categoryId = 1L)

        vm.moveSubCategory(0, 9)

        assertEquals(listOf("Restaurant", "Groceries"), vm.state.value.subCategories.map { it.name })
    }
}
