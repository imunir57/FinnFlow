package com.finnflow.ui.settings

import app.cash.turbine.test
import com.finnflow.data.profile.UserProfile
import com.finnflow.data.profile.UserProfileRepository
import com.finnflow.data.repository.TransactionRepository
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
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: UserProfileRepository
    private lateinit var transactionRepository: TransactionRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        transactionRepository = mockk(relaxed = true)
        every { repo.profile } returns flowOf(UserProfile())
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun makeVm() = SettingsViewModel(repo, transactionRepository)

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
}
