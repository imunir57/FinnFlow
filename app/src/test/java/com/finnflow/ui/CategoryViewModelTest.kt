package com.finnflow.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.finnflow.data.model.Category
import com.finnflow.data.model.SubCategory
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.CategoryRepository
import com.finnflow.ui.category.CategoryViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: CategoryRepository

    private val categories = listOf(
        Category(id = 1L, name = "Food", type = TransactionType.EXPENSE),
        Category(id = 2L, name = "Transport", type = TransactionType.EXPENSE)
    )
    private val subCategories = listOf(
        SubCategory(id = 1L, categoryId = 1L, name = "Restaurant"),
        SubCategory(id = 2L, categoryId = 1L, name = "Groceries")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        every { repo.getAllCategories() } returns flowOf(categories)
        every { repo.getSubCategories(any()) } returns flowOf(subCategories)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun makeVm(categoryId: Long? = null) = CategoryViewModel(
        repo,
        SavedStateHandle(if (categoryId != null) mapOf("categoryId" to categoryId) else emptyMap())
    )

    @Test
    fun noParentCategoryId_loadsAllCategories() = runTest {
        val vm = makeVm()
        vm.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.categories.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun withParentCategoryId_loadsSubCategories() = runTest {
        val vm = makeVm(categoryId = 1L)
        vm.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.subCategories.size)
            assertEquals(1L, state.selectedCategoryId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addCategory_callsRepository() = runTest {
        val vm = makeVm()
        coEvery { repo.addCategory(any()) } returns 3L
        vm.addCategory("Entertainment", TransactionType.EXPENSE)
        coVerify { repo.addCategory(match { it.name == "Entertainment" && it.type == TransactionType.EXPENSE }) }
    }

    @Test
    fun deleteCategory_callsRepository() = runTest {
        val vm = makeVm()
        vm.deleteCategory(categories[0])
        coVerify { repo.deleteCategory(categories[0]) }
    }

    @Test
    fun updateCategory_callsRepository() = runTest {
        val vm = makeVm()
        val updated = categories[0].copy(name = "Dining")
        vm.updateCategory(updated)
        coVerify { repo.updateCategory(updated) }
    }

    @Test
    fun addSubCategory_usesParentId() = runTest {
        val vm = makeVm(categoryId = 1L)
        coEvery { repo.addSubCategory(any()) } returns 5L
        vm.addSubCategory("Taxi")
        coVerify { repo.addSubCategory(match { it.name == "Taxi" && it.categoryId == 1L }) }
    }

    @Test
    fun addSubCategory_noParentId_doesNothing() = runTest {
        val vm = makeVm(categoryId = null)
        vm.addSubCategory("Taxi")
        coVerify(exactly = 0) { repo.addSubCategory(any()) }
    }
}
