package com.finnflow.ui.onboarding

import app.cash.turbine.test
import com.finnflow.data.profile.UserProfileRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: UserProfileRepository
    private lateinit var vm: OnboardingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        vm = OnboardingViewModel(repo)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    // ── onGetStarted ──────────────────────────────────────────────────────

    @Test
    fun onGetStarted_withName_savesProfileAndCompletesOnboarding() = runTest {
        vm.onGetStarted("Alice")

        coVerify { repo.saveProfile("Alice") }
        coVerify { repo.completeOnboarding() }
    }

    @Test
    fun onGetStarted_withBlankName_skipsProfileSaveButCompletesOnboarding() = runTest {
        vm.onGetStarted("   ")

        coVerify(exactly = 0) { repo.saveProfile(any()) }
        coVerify { repo.completeOnboarding() }
    }

    @Test
    fun onGetStarted_withEmptyString_skipsProfileSave() = runTest {
        vm.onGetStarted("")

        coVerify(exactly = 0) { repo.saveProfile(any()) }
        coVerify { repo.completeOnboarding() }
    }

    @Test
    fun onGetStarted_emitsNavigateHome() = runTest {
        vm.navigateHome.test {
            vm.onGetStarted("Alice")
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── onSkip ────────────────────────────────────────────────────────────

    @Test
    fun onSkip_completesOnboardingWithoutSavingProfile() = runTest {
        vm.onSkip()

        coVerify(exactly = 0) { repo.saveProfile(any()) }
        coVerify { repo.completeOnboarding() }
    }

    @Test
    fun onSkip_emitsNavigateHome() = runTest {
        vm.navigateHome.test {
            vm.onSkip()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun consecutiveCalls_emitNavigateHomeEachTime() = runTest {
        vm.navigateHome.test {
            vm.onGetStarted("Alice")
            awaitItem()
            vm.onSkip()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
