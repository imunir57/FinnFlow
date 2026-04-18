package com.finnflow.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.finnflow.data.model.Category
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.CategoryRepository
import com.finnflow.data.repository.TransactionRepository
import com.finnflow.ui.transaction.TransactionViewModel
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
class TransactionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var transactionRepo: TransactionRepository
    private lateinit var categoryRepo: CategoryRepository

    private val categories = listOf(
        Category(id = 1, name = "Food", type = TransactionType.EXPENSE),
        Category(id = 2, name = "Transport", type = TransactionType.EXPENSE)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        transactionRepo = mockk(relaxed = true)
        categoryRepo = mockk(relaxed = true)
        every { categoryRepo.getCategoriesByType(any()) } returns flowOf(categories)
        every { categoryRepo.getSubCategories(any()) } returns flowOf(emptyList())
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun makeVm(transactionId: Long? = null) = TransactionViewModel(
        transactionRepo, categoryRepo,
        SavedStateHandle(if (transactionId != null) mapOf("transactionId" to transactionId) else emptyMap())
    )

    @Test
    fun initialState_loadsCategories() = runTest {
        val vm = makeVm()
        vm.state.test {
            val state = awaitItem()
            assertEquals(2, state.categories.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onAmountChange_updatesState() {
        val vm = makeVm()
        vm.onAmountChange("250.5")
        assertEquals("250.5", vm.state.value.amount)
    }

    @Test
    fun invalidAmount_setsError() {
        val vm = makeVm()
        vm.onAmountChange("abc")
        assertNotNull(vm.state.value.amountError)
    }

    @Test
    fun isValid_requiresAmountAndCategory() {
        val vm = makeVm()
        assertFalse(vm.state.value.isValid)
        vm.onAmountChange("100")
        vm.onCategoryChange(1L)
        assertTrue(vm.state.value.isValid)
    }

    @Test
    fun save_callsAddTransactionForNewTx() = runTest {
        val vm = makeVm()
        vm.onAmountChange("100")
        vm.onCategoryChange(1L)
        vm.save()
        coVerify { transactionRepo.addTransaction(any()) }
    }

    @Test
    fun save_callsUpdateForExistingTx() = runTest {
        val existingTx = Transaction(id = 5L, type = TransactionType.EXPENSE, amount = 50.0,
            date = LocalDate.now(), categoryId = 1L)
        coEvery { transactionRepo.getTransactionById(5L) } returns existingTx

        val vm = makeVm(transactionId = 5L)
        vm.onAmountChange("200")
        vm.onCategoryChange(1L)
        vm.save()
        coVerify { transactionRepo.updateTransaction(any()) }
    }

    @Test
    fun onTypeChange_resetsCategoryAndSubCategory() {
        val vm = makeVm()
        vm.onCategoryChange(1L)
        vm.onTypeChange(TransactionType.INCOME)
        assertNull(vm.state.value.categoryId)
        assertNull(vm.state.value.subCategoryId)
    }
}
