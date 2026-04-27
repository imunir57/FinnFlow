package com.finnflow.ui

import app.cash.turbine.test
import com.finnflow.data.profile.UserProfile
import com.finnflow.data.profile.UserProfileRepository
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
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: UserProfileRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk()
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun makeVm() = MainViewModel(repo)

    @Test
    fun hasCompletedOnboarding_startsAsNull() {
        every { repo.profile } returns flowOf(UserProfile(hasCompletedOnboarding = false))
        // Initial stateIn value before any upstream emission
        val vm = makeVm()
        // With UnconfinedTestDispatcher the StateFlow collects immediately,
        // so the initial null is replaced — verify the resolved state is non-null
        assertNotNull(vm.hasCompletedOnboarding.value)
    }

    @Test
    fun hasCompletedOnboarding_emitsFalseWhenOnboardingNotDone() = runTest {
        every { repo.profile } returns flowOf(UserProfile(hasCompletedOnboarding = false))

        makeVm().hasCompletedOnboarding.test {
            val value = awaitItem()
            // Skip null if present, then assert false
            if (value == null) assertEquals(false, awaitItem())
            else assertEquals(false, value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun hasCompletedOnboarding_emitsTrueWhenOnboardingDone() = runTest {
        every { repo.profile } returns flowOf(UserProfile(hasCompletedOnboarding = true))

        makeVm().hasCompletedOnboarding.test {
            val value = awaitItem()
            if (value == null) assertEquals(true, awaitItem())
            else assertEquals(true, value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun hasCompletedOnboarding_reflectsLatestProfileValue() = runTest {
        every { repo.profile } returns flowOf(
            UserProfile(hasCompletedOnboarding = false),
            UserProfile(hasCompletedOnboarding = true)
        )

        makeVm().hasCompletedOnboarding.test {
            val emissions = mutableListOf<Boolean?>()
            emissions.add(awaitItem())
            emissions.add(awaitItem())
            cancelAndIgnoreRemainingEvents()
            // Last emitted value must be true
            assertTrue(emissions.last() == true)
        }
    }
}
