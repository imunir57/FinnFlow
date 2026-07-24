package com.finnflow.ui.settings

import app.cash.turbine.test
import com.finnflow.data.profile.UserProfile
import com.finnflow.data.profile.UserProfileRepository
import com.finnflow.data.repository.BackupRepository
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: UserProfileRepository
    private lateinit var backupRepo: BackupRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        backupRepo = mockk(relaxed = true)
        every { repo.profile } returns flowOf(UserProfile())
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun makeVm() = SettingsViewModel(repo, backupRepo)

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
}
