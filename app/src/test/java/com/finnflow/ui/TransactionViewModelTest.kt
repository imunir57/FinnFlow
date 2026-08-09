package com.finnflow.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.finnflow.data.model.Category
import com.finnflow.data.model.SubCategory
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.CategoryRepository
import com.finnflow.data.repository.TransactionRepository
import com.finnflow.ui.transaction.TransactionViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
        every { categoryRepo.getActiveSubCategories(any()) } returns flowOf(emptyList())
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
    fun onAmountDecimal_seedsLeadingZeroWhenEmpty() {
        val vm = makeVm()
        vm.onAmountDecimal()
        // "0." is an in-progress prefix, not a finished amount: it must stay parseable so
        // the next digit completes it, but it is not yet valid to save.
        assertEquals("0.", vm.state.value.amount)
        assertEquals("Amount must be positive", vm.state.value.amountError)

        vm.onAmountDigit("5")
        assertEquals("0.5", vm.state.value.amount)
        assertNull(vm.state.value.amountError)
    }

    @Test
    fun onAmountDecimal_appendsOnceOnly() {
        val vm = makeVm()
        vm.onAmountDigit("1")
        vm.onAmountDigit("2")
        vm.onAmountDecimal()
        vm.onAmountDigit("5")
        vm.onAmountDecimal()
        assertEquals("12.5", vm.state.value.amount)
    }

    @Test
    fun onAmountClear_wipesTheAmount() {
        val vm = makeVm()
        vm.onAmountChange("450.75")
        vm.onAmountClear()
        assertEquals("", vm.state.value.amount)
        assertFalse(vm.state.value.isValid)
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

    @Test
    fun onCategoryChange_marksSubCategoriesLoadingUntilTheyArrive() = runTest {
        val subs = MutableSharedFlow<List<SubCategory>>()
        every { categoryRepo.getActiveSubCategories(1L) } returns subs

        val vm = makeVm()
        vm.onCategoryChange(1L)
        assertTrue(vm.state.value.isLoadingSubCategories)

        subs.emit(listOf(SubCategory(id = 10L, categoryId = 1L, name = "Groceries")))
        assertFalse(vm.state.value.isLoadingSubCategories)
    }

    @Test
    fun onCategoryChange_ignoresLateEmissionFromPreviousCategory() = runTest {
        val firstSubs = MutableSharedFlow<List<SubCategory>>()
        val secondSubs = MutableSharedFlow<List<SubCategory>>()
        every { categoryRepo.getActiveSubCategories(1L) } returns firstSubs
        every { categoryRepo.getActiveSubCategories(2L) } returns secondSubs

        val vm = makeVm()
        vm.onCategoryChange(1L)
        vm.onCategoryChange(2L)

        secondSubs.emit(listOf(SubCategory(id = 20L, categoryId = 2L, name = "Bus")))
        // Arrives after the user already moved on — the old collector is cancelled, so
        // this must not clobber the selected category's list.
        firstSubs.emit(listOf(SubCategory(id = 10L, categoryId = 1L, name = "Groceries")))

        assertEquals(listOf(20L), vm.state.value.subCategories.map { it.id })
    }

    @Test
    fun onTypeChange_stopsCollectingTheOldCategorysSubCategories() = runTest {
        val subs = MutableSharedFlow<List<SubCategory>>()
        every { categoryRepo.getActiveSubCategories(1L) } returns subs

        val vm = makeVm()
        vm.onCategoryChange(1L)
        vm.onTypeChange(TransactionType.INCOME)

        subs.emit(listOf(SubCategory(id = 10L, categoryId = 1L, name = "Groceries")))

        assertTrue(vm.state.value.subCategories.isEmpty())
        assertFalse(vm.state.value.isLoadingSubCategories)
    }
}
