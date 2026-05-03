package com.finnflow.ui.profile

import app.cash.turbine.test
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import com.finnflow.data.profile.UserProfile
import com.finnflow.data.profile.UserProfileRepository
import com.finnflow.data.repository.TransactionRepository
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var profileRepo: UserProfileRepository
    private lateinit var txRepo: TransactionRepository

    private val sampleTransactions = listOf(
        Transaction(id = 1, amount = 5000.0, type = TransactionType.INCOME,
            categoryId = 1, date = LocalDate.now(), note = ""),
        Transaction(id = 2, amount = 2000.0, type = TransactionType.EXPENSE,
            categoryId = 2, date = LocalDate.now(), note = ""),
        Transaction(id = 3, amount = 1000.0, type = TransactionType.EXPENSE,
            categoryId = 2, date = LocalDate.now(), note = ""),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepo = mockk(relaxed = true)
        txRepo = mockk(relaxed = true)
        every { profileRepo.profile } returns flowOf(UserProfile())
        every { txRepo.getAllTransactions() } returns flowOf(emptyList())
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun makeVm() = ProfileViewModel(profileRepo, txRepo)

    // ── uiState ───────────────────────────────────────────────────────────

    @Test
    fun uiState_hasDefaultInitialValues() {
        val vm = makeVm()
        assertEquals(UserProfile(), vm.uiState.value.profile)
        assertEquals(0.0, vm.uiState.value.totalIncome, 0.001)
        assertEquals(0.0, vm.uiState.value.totalExpense, 0.001)
        assertEquals(0, vm.uiState.value.entryCount)
    }

    @Test
    fun uiState_reflectsProfileFromRepository() = runTest {
        val expected = UserProfile(displayName = "Munir", initials = "MU", hasCompletedOnboarding = true)
        every { profileRepo.profile } returns flowOf(expected)

        makeVm().uiState.test {
            assertEquals(expected, awaitItem().profile)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_computesTotalsFromTransactions() = runTest {
        every { txRepo.getAllTransactions() } returns flowOf(sampleTransactions)

        makeVm().uiState.test {
            val state = awaitItem()
            assertEquals(5000.0, state.totalIncome, 0.001)
            assertEquals(3000.0, state.totalExpense, 0.001)
            assertEquals(3, state.entryCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_withNoTransactions_hasZeroTotals() = runTest {
        makeVm().uiState.test {
            val state = awaitItem()
            assertEquals(0.0, state.totalIncome, 0.001)
            assertEquals(0.0, state.totalExpense, 0.001)
            assertEquals(0, state.entryCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── saveName ──────────────────────────────────────────────────────────

    @Test
    fun saveName_delegatesToRepository() = runTest {
        makeVm().saveName("Alice")
        coVerify { profileRepo.saveProfile("Alice") }
    }

    @Test
    fun saveName_withEmptyString_stillDelegates() = runTest {
        makeVm().saveName("")
        coVerify { profileRepo.saveProfile("") }
    }
}
