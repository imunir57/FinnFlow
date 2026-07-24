package com.finnflow.ui.settings

import app.cash.turbine.test
import com.finnflow.data.biometric.BiometricAuthenticator
import com.finnflow.data.notification.ReminderScheduler
import com.finnflow.data.profile.UserProfile
import com.finnflow.data.profile.UserProfileRepository
import io.mockk.coVerify
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: UserProfileRepository
    private lateinit var reminderScheduler: ReminderScheduler
    private lateinit var biometricAuthenticator: BiometricAuthenticator

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        reminderScheduler = mockk(relaxed = true)
        biometricAuthenticator = mockk(relaxed = true)
        every { repo.profile } returns flowOf(UserProfile())
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun makeVm() = SettingsViewModel(repo, reminderScheduler, biometricAuthenticator)

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
}
