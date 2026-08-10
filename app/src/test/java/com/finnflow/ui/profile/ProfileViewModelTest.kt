package com.finnflow.ui.profile

import app.cash.turbine.test
import com.finnflow.data.auth.GoogleAuthClient
import com.finnflow.data.auth.GoogleAuthResult
import com.finnflow.data.auth.GoogleIdentity
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import com.finnflow.data.profile.UserProfile
import com.finnflow.data.profile.UserProfileRepository
import com.finnflow.data.repository.BackupRepository
import com.finnflow.data.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var profileRepo: UserProfileRepository
    private lateinit var txRepo: TransactionRepository
    private lateinit var backupRepo: BackupRepository
    private lateinit var googleAuthClient: GoogleAuthClient

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
        backupRepo = mockk(relaxed = true)
        googleAuthClient = mockk(relaxed = true)
        every { profileRepo.profile } returns flowOf(UserProfile())
        every { txRepo.getAllTransactions() } returns flowOf(emptyList())
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun makeVm() = ProfileViewModel(profileRepo, txRepo, backupRepo, googleAuthClient)

    // ── uiState ───────────────────────────────────────────────────────────

    @Test
    fun uiState_hasDefaultInitialValues() {
        val vm = makeVm()
        assertEquals(UserProfile(), vm.uiState.value.profile)
        assertEquals(0.0, vm.uiState.value.totalIncome, 0.001)
        assertEquals(0.0, vm.uiState.value.totalExpense, 0.001)
        assertEquals(0, vm.uiState.value.entryCount)
    }

    // ── memberSince ───────────────────────────────────────────────────────

    @Test
    fun memberSince_isNull_whenNothingToDateTheAccountFrom() = runTest {
        val vm = makeVm()
        vm.uiState.test {
            assertNull(awaitItem().memberSince)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun memberSince_usesCreatedAt_whenNoTransactionsExist() = runTest {
        val created = LocalDate.of(2025, 3, 14)
        every { profileRepo.profile } returns flowOf(UserProfile(createdAtMillis = created.toMillis()))
        val vm = makeVm()

        vm.uiState.test {
            assertEquals(YearMonth.of(2025, 3), awaitItem().memberSince)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun memberSince_prefersOlderTransaction_overBackfilledCreatedAt() = runTest {
        // The stamp is backfilled at app start, so on an install that predates the key it reads as
        // "today" while the records go back years.
        every { profileRepo.profile } returns
            flowOf(UserProfile(createdAtMillis = LocalDate.of(2026, 8, 9).toMillis()))
        every { txRepo.getAllTransactions() } returns flowOf(
            listOf(
                Transaction(id = 1, amount = 10.0, type = TransactionType.EXPENSE,
                    categoryId = 1, date = LocalDate.of(2023, 11, 2), note = ""),
                Transaction(id = 2, amount = 20.0, type = TransactionType.EXPENSE,
                    categoryId = 1, date = LocalDate.of(2024, 1, 5), note = "")
            )
        )
        val vm = makeVm()

        vm.uiState.test {
            assertEquals(YearMonth.of(2023, 11), awaitItem().memberSince)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun memberSince_usesCreatedAt_whenItPredatesEveryTransaction() = runTest {
        every { profileRepo.profile } returns
            flowOf(UserProfile(createdAtMillis = LocalDate.of(2024, 2, 1).toMillis()))
        every { txRepo.getAllTransactions() } returns flowOf(
            listOf(
                Transaction(id = 1, amount = 10.0, type = TransactionType.EXPENSE,
                    categoryId = 1, date = LocalDate.of(2024, 6, 9), note = "")
            )
        )
        val vm = makeVm()

        vm.uiState.test {
            assertEquals(YearMonth.of(2024, 2), awaitItem().memberSince)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun LocalDate.toMillis(): Long =
        atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

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

    // ── onSignInWithGoogle ────────────────────────────────────────────────

    @Test
    fun onSignInWithGoogle_success_savesIdentity() = runTest {
        val identity = GoogleIdentity("sub1", "Jane", "jane@gmail.com", null)
        coEvery { googleAuthClient.signIn(any()) } returns GoogleAuthResult.Success(identity)

        makeVm().onSignInWithGoogle(mockk(relaxed = true))

        coVerify { profileRepo.signInWithGoogle("Jane", "jane@gmail.com", null, "sub1") }
    }

    @Test
    fun onSignInWithGoogle_cancelled_doesNotSave() = runTest {
        coEvery { googleAuthClient.signIn(any()) } returns GoogleAuthResult.Cancelled

        makeVm().onSignInWithGoogle(mockk(relaxed = true))

        coVerify(exactly = 0) { profileRepo.signInWithGoogle(any(), any(), any(), any()) }
    }

    @Test
    fun onSignInWithGoogle_error_emitsMessageAndDoesNotSave() = runTest {
        coEvery { googleAuthClient.signIn(any()) } returns GoogleAuthResult.Error("network error")
        val vm = makeVm()

        vm.messages.test {
            vm.onSignInWithGoogle(mockk(relaxed = true))
            assertEquals("network error", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { profileRepo.signInWithGoogle(any(), any(), any(), any()) }
    }

    @Test
    fun onSignInWithGoogle_cancelled_emitsNoMessage() = runTest {
        coEvery { googleAuthClient.signIn(any()) } returns GoogleAuthResult.Cancelled
        val vm = makeVm()

        vm.messages.test {
            vm.onSignInWithGoogle(mockk(relaxed = true))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── onSignOut ─────────────────────────────────────────────────────────

    @Test
    fun onSignOut_clearsCredentialStateThenErasesDataThenProfile() = runTest {
        val vm = makeVm()
        val context = mockk<android.content.Context>(relaxed = true)

        vm.onSignOut(context)

        coVerifyOrder {
            googleAuthClient.signOut(context)
            backupRepo.eraseAllData()
            profileRepo.clearProfile()
        }
    }

    @Test
    fun onSignOut_whenCredentialClearFails_stillErasesDataAndProfile() = runTest {
        val context = mockk<android.content.Context>(relaxed = true)
        // A local profile has no credential to clear and a device with no credential provider
        // throws from outside ClearCredentialException; the erase must not depend on either.
        coEvery { googleAuthClient.signOut(context) } throws IllegalStateException("no provider")
        val vm = makeVm()

        vm.onSignOut(context)

        coVerify { backupRepo.eraseAllData() }
        coVerify { profileRepo.clearProfile() }
    }

    @Test
    fun onSignOut_whenEraseFails_leavesProfileIntactAndReportsFailure() = runTest {
        val context = mockk<android.content.Context>(relaxed = true)
        coEvery { backupRepo.eraseAllData() } throws IllegalStateException("db locked")
        val vm = makeVm()

        vm.messages.test {
            vm.onSignOut(context)
            assertEquals("Sign out failed: db locked", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { profileRepo.clearProfile() }
    }

    // ── performBackup ─────────────────────────────────────────────────────

    @Test
    fun performBackup_writesBackupAndRecordsTimestamp() = runTest {
        val output = java.io.ByteArrayOutputStream()
        val vm = makeVm()

        vm.messages.test {
            vm.performBackup(output)
            assertEquals("Backup saved", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { backupRepo.exportBackup(output) }
        coVerify { profileRepo.setLastBackupTimestamp(any()) }
    }

    @Test
    fun performBackup_nullStream_reportsFailure() = runTest {
        val vm = makeVm()

        vm.messages.test {
            vm.performBackup(null)
            assertEquals("Couldn't open the selected file", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { backupRepo.exportBackup(any()) }
    }
}
