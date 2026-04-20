package com.finnflow.ui.profile

import app.cash.turbine.test
import com.finnflow.data.profile.UserProfile
import com.finnflow.data.profile.UserProfileRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: UserProfileRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        every { repo.profile } returns flowOf(UserProfile())
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun makeVm() = ProfileViewModel(repo)

    // ── profile StateFlow ─────────────────────────────────────────────────

    @Test
    fun profile_hasDefaultInitialValue() {
        val vm = makeVm()
        assertEquals(UserProfile(), vm.profile.value)
    }

    @Test
    fun profile_reflectsRepositoryData() = runTest {
        val expected = UserProfile(
            displayName = "Munir",
            initials = "MN",
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
        val first = UserProfile(displayName = "Alice", initials = "AL")
        val second = UserProfile(displayName = "Alice Smith", initials = "AS")
        every { repo.profile } returns flowOf(first, second)

        makeVm().profile.test {
            assertEquals(first, awaitItem())
            assertEquals(second, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── saveName ──────────────────────────────────────────────────────────

    @Test
    fun saveName_delegatesToRepository() = runTest {
        makeVm().saveName("Alice")

        coVerify { repo.saveProfile("Alice") }
    }

    @Test
    fun saveName_withEmptyString_stillDelegates() = runTest {
        makeVm().saveName("")

        coVerify { repo.saveProfile("") }
    }
}
