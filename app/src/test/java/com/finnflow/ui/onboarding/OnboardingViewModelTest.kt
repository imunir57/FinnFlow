package com.finnflow.ui.onboarding

import app.cash.turbine.test
import com.finnflow.data.auth.GoogleAuthClient
import com.finnflow.data.auth.GoogleAuthResult
import com.finnflow.data.auth.GoogleIdentity
import com.finnflow.data.profile.UserProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: UserProfileRepository
    private lateinit var googleAuthClient: GoogleAuthClient
    private lateinit var vm: OnboardingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        googleAuthClient = mockk(relaxed = true)
        vm = OnboardingViewModel(repo, googleAuthClient)
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

    // ── onSignInWithGoogle ────────────────────────────────────────────────

    @Test
    fun onSignInWithGoogle_success_savesIdentityCompletesOnboardingAndNavigates() = runTest {
        val identity = GoogleIdentity("sub1", "Jane", "jane@gmail.com", null)
        coEvery { googleAuthClient.signIn(any()) } returns GoogleAuthResult.Success(identity)

        vm.navigateHome.test {
            vm.onSignInWithGoogle(mockk(relaxed = true), "")
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { repo.signInWithGoogle("Jane", "jane@gmail.com", null, "sub1") }
        coVerify { repo.completeOnboarding() }
    }

    @Test
    fun onSignInWithGoogle_withTypedName_savesItBeforeGoogleIdentity() = runTest {
        val identity = GoogleIdentity("sub1", "Jane", "jane@gmail.com", null)
        coEvery { googleAuthClient.signIn(any()) } returns GoogleAuthResult.Success(identity)

        vm.navigateHome.test {
            vm.onSignInWithGoogle(mockk(relaxed = true), "Munir")
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        // Order matters: signInWithGoogle only fills a blank display name, so saveProfile
        // has to land first for the typed name to survive.
        coVerifyOrder {
            repo.saveProfile("Munir")
            repo.signInWithGoogle("Jane", "jane@gmail.com", null, "sub1")
        }
    }

    @Test
    fun onSignInWithGoogle_withBlankName_doesNotSaveProfile() = runTest {
        val identity = GoogleIdentity("sub1", "Jane", "jane@gmail.com", null)
        coEvery { googleAuthClient.signIn(any()) } returns GoogleAuthResult.Success(identity)

        vm.navigateHome.test {
            vm.onSignInWithGoogle(mockk(relaxed = true), "   ")
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { repo.saveProfile(any()) }
    }

    @Test
    fun onSignInWithGoogle_cancelled_doesNotSaveOrNavigate() = runTest {
        coEvery { googleAuthClient.signIn(any()) } returns GoogleAuthResult.Cancelled

        vm.onSignInWithGoogle(mockk(relaxed = true), "Munir")

        coVerify(exactly = 0) { repo.saveProfile(any()) }
        coVerify(exactly = 0) { repo.signInWithGoogle(any(), any(), any(), any()) }
        coVerify(exactly = 0) { repo.completeOnboarding() }
    }

    @Test
    fun onSignInWithGoogle_error_emitsMessageAndDoesNotSave() = runTest {
        coEvery { googleAuthClient.signIn(any()) } returns GoogleAuthResult.Error("network error")

        vm.messages.test {
            vm.onSignInWithGoogle(mockk(relaxed = true), "Munir")
            assertEquals("network error", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { repo.saveProfile(any()) }
        coVerify(exactly = 0) { repo.signInWithGoogle(any(), any(), any(), any()) }
    }
}
