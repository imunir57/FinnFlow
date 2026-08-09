package com.finnflow.ui.settings

import app.cash.turbine.test
import com.finnflow.data.auth.GoogleAuthClient
import com.finnflow.data.biometric.BiometricAuthenticator
import com.finnflow.data.notification.ReminderScheduler
import com.finnflow.data.profile.UserProfile
import com.finnflow.data.profile.UserProfileRepository
import com.finnflow.data.repository.BackupRepository
import com.finnflow.data.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: UserProfileRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var reminderScheduler: ReminderScheduler
    private lateinit var biometricAuthenticator: BiometricAuthenticator
    private lateinit var backupRepo: BackupRepository
    private lateinit var googleAuthClient: GoogleAuthClient

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        transactionRepository = mockk(relaxed = true)
        reminderScheduler = mockk(relaxed = true)
        biometricAuthenticator = mockk(relaxed = true)
        backupRepo = mockk(relaxed = true)
        googleAuthClient = mockk(relaxed = true)
        every { repo.profile } returns flowOf(UserProfile())
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun makeVm() = SettingsViewModel(
        repo, transactionRepository, reminderScheduler, biometricAuthenticator, backupRepo, googleAuthClient
    )

    @Test
    fun profile_hasDefaultInitialValue() {
        val vm = makeVm()
        assertEquals(UserProfile(), vm.profile.value)
    }

    @Test
    fun profile_reflectsRepositoryData() = runTest {
        val expected = UserProfile(
            displayName = "Munir",
            initials = "MU",
            hasCompletedOnboarding = true
        )
        every { repo.profile } returns flowOf(expected)

        makeVm().profile.test {
            assertEquals(expected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun profile_emitsUpdatesFromRepository() = runTest {
        val first  = UserProfile(displayName = "Alice", initials = "AL")
        val second = UserProfile(displayName = "Alice Smith", initials = "AS")
        every { repo.profile } returns flowOf(first, second)

        makeVm().profile.test {
            // StateFlow may conflate first+second; loop until second arrives
            var item: UserProfile? = null
            while (item != second) { item = awaitItem() }
            assertEquals(second, item)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun profile_withBlankName_usesDefaults() = runTest {
        every { repo.profile } returns flowOf(UserProfile())

        makeVm().profile.test {
            val p = awaitItem()
            assertEquals("", p.displayName)
            assertEquals("", p.initials)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun exportCsv_delegatesToTransactionRepositoryAndInvokesCallback() = runTest {
        val outputStream = ByteArrayOutputStream()
        coEvery { transactionRepository.exportTransactionsCsv(outputStream) } returns Unit
        var completed = false

        makeVm().exportCsv(outputStream) { completed = true }

        coVerify { transactionRepository.exportTransactionsCsv(outputStream) }
        assertEquals(true, completed)
    }

    // ── onCurrencySelected ───────────────────────────────────────────────

    @Test
    fun onCurrencySelected_callsRepository() = runTest {
        val vm = makeVm()

        vm.onCurrencySelected("USD")

        coVerify { repo.setCurrencyCode("USD") }
    }

    // ── onThemeModeSelected ──────────────────────────────────────────────

    @Test
    fun onThemeModeSelected_callsRepository() = runTest {
        val vm = makeVm()

        vm.onThemeModeSelected("dark")

        coVerify { repo.setThemeMode("dark") }
    }

    // ── onNotificationsToggled ───────────────────────────────────────────

    @Test
    fun onNotificationsToggled_enabled_persistsAndSchedules() = runTest {
        makeVm().onNotificationsToggled(true)

        coVerify { repo.setNotificationsEnabled(true) }
        verify { reminderScheduler.schedule() }
        verify(exactly = 0) { reminderScheduler.cancel() }
    }

    @Test
    fun onNotificationsToggled_disabled_persistsAndCancels() = runTest {
        makeVm().onNotificationsToggled(false)

        coVerify { repo.setNotificationsEnabled(false) }
        verify { reminderScheduler.cancel() }
        verify(exactly = 0) { reminderScheduler.schedule() }
    }

    // ── onAppLockToggled ──────────────────────────────────────────────────

    @Test
    fun onAppLockToggled_disable_persistsFalseWithoutBiometricCheck() = runTest {
        val vm = makeVm()

        vm.onAppLockToggled(false, activity = null)

        coVerify { repo.setAppLockEnabled(false) }
        verify(exactly = 0) { biometricAuthenticator.canAuthenticate() }
        assertNull(vm.appLockMessage.value)
    }

    @Test
    fun onAppLockToggled_enable_whenBiometricsUnavailable_doesNotPersistAndSetsMessage() = runTest {
        every { biometricAuthenticator.canAuthenticate() } returns false
        val vm = makeVm()

        vm.onAppLockToggled(true, activity = null)

        coVerify(exactly = 0) { repo.setAppLockEnabled(true) }
        assert(vm.appLockMessage.value != null)
    }

    @Test
    fun onAppLockToggled_enable_whenActivityNull_doesNotPersist() = runTest {
        every { biometricAuthenticator.canAuthenticate() } returns true
        val vm = makeVm()

        vm.onAppLockToggled(true, activity = null)

        coVerify(exactly = 0) { repo.setAppLockEnabled(true) }
        assert(vm.appLockMessage.value != null)
    }

    @Test
    fun onAppLockToggled_enable_onAuthSuccess_persistsTrue() = runTest {
        every { biometricAuthenticator.canAuthenticate() } returns true
        val onSuccessSlot = slot<() -> Unit>()
        every {
            biometricAuthenticator.authenticate(any(), capture(onSuccessSlot), any())
        } answers { onSuccessSlot.captured.invoke() }

        val activity = mockk<androidx.fragment.app.FragmentActivity>(relaxed = true)
        val vm = makeVm()

        vm.onAppLockToggled(true, activity = activity)

        coVerify { repo.setAppLockEnabled(true) }
        assertNull(vm.appLockMessage.value)
    }

    @Test
    fun onAppLockToggled_enable_onAuthError_doesNotPersistAndSetsMessage() = runTest {
        every { biometricAuthenticator.canAuthenticate() } returns true
        val onErrorSlot = slot<(String) -> Unit>()
        every {
            biometricAuthenticator.authenticate(any(), any(), capture(onErrorSlot))
        } answers { onErrorSlot.captured.invoke("No biometrics enrolled") }

        val activity = mockk<androidx.fragment.app.FragmentActivity>(relaxed = true)
        val vm = makeVm()

        vm.onAppLockToggled(true, activity = activity)

        coVerify(exactly = 0) { repo.setAppLockEnabled(true) }
        assertEquals("No biometrics enrolled", vm.appLockMessage.value)
    }

    // ── performBackup ─────────────────────────────────────────────────────

    @Test
    fun performBackup_withNullStream_emitsErrorMessage() = runTest {
        val vm = makeVm()

        vm.messages.test {
            vm.performBackup(null)
            assertEquals("Couldn't open the selected file", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun performBackup_success_exportsAndUpdatesTimestampAndNotifies() = runTest {
        val out = ByteArrayOutputStream()
        coEvery { backupRepo.exportBackup(out) } returns Unit
        val vm = makeVm()

        vm.messages.test {
            vm.performBackup(out)
            assertEquals("Backup saved", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { backupRepo.exportBackup(out) }
        coVerify { repo.setLastBackupTimestamp(any()) }
    }

    @Test
    fun performBackup_failure_emitsFailureMessage() = runTest {
        val out = ByteArrayOutputStream()
        coEvery { backupRepo.exportBackup(out) } throws RuntimeException("disk full")
        val vm = makeVm()

        vm.messages.test {
            vm.performBackup(out)
            assertEquals("Backup failed: disk full", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { repo.setLastBackupTimestamp(any()) }
    }

    // ── performRestore ────────────────────────────────────────────────────

    @Test
    fun performRestore_withNullStream_emitsErrorMessage() = runTest {
        val vm = makeVm()

        vm.messages.test {
            vm.performRestore(null)
            assertEquals("Couldn't open the selected file", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun performRestore_success_emitsSuccessMessage() = runTest {
        val input = ByteArrayInputStream(ByteArray(0))
        coEvery { backupRepo.restoreBackup(input) } returns Result.success(Unit)
        val vm = makeVm()

        vm.messages.test {
            vm.performRestore(input)
            assertEquals("Restore complete", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun performRestore_failure_emitsFailureMessage() = runTest {
        val input = ByteArrayInputStream(ByteArray(0))
        coEvery { backupRepo.restoreBackup(input) } returns Result.failure(IllegalArgumentException("bad file"))
        val vm = makeVm()

        vm.messages.test {
            vm.performRestore(input)
            assertEquals("Restore failed: bad file", awaitItem())
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
            repo.clearProfile()
        }
    }

    @Test
    fun onSignOut_whenCredentialClearFails_stillErasesDataAndProfile() = runTest {
        val context = mockk<android.content.Context>(relaxed = true)
        // A device with no credential provider throws from outside ClearCredentialException;
        // the local sign out must not depend on it.
        coEvery { googleAuthClient.signOut(context) } throws IllegalStateException("no provider")
        val vm = makeVm()

        vm.onSignOut(context)

        coVerify { backupRepo.eraseAllData() }
        coVerify { repo.clearProfile() }
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

        coVerify(exactly = 0) { repo.clearProfile() }
    }
}
