package com.finnflow.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import io.mockk.coAnswers
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
class UserProfileRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: UserProfileRepositoryImpl

    // Mirror the private keys used in the impl
    private val KEY_NAME = stringPreferencesKey("profile_display_name")
    private val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dataStore = mockk(relaxed = true)
        repo = UserProfileRepositoryImpl(dataStore)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    // ── profile Flow mapping ──────────────────────────────────────────────

    @Test
    fun profile_withEmptyPrefs_returnsDefaults() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())

        repo.profile.test {
            val p = awaitItem()
            assertEquals("", p.displayName)
            assertEquals("?", p.initials)
            assertFalse(p.hasCompletedOnboarding)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun profile_withSavedName_mapsNameAndInitials() = runTest {
        every { dataStore.data } returns flowOf(preferencesOf(KEY_NAME to "Jane Smith"))

        repo.profile.test {
            val p = awaitItem()
            assertEquals("Jane Smith", p.displayName)
            assertEquals("JS", p.initials)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun profile_withSingleWordName_takesTwoCharsAsInitials() = runTest {
        every { dataStore.data } returns flowOf(preferencesOf(KEY_NAME to "Munir"))

        repo.profile.test {
            assertEquals("MU", awaitItem().initials)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun profile_withOnboardingDone_mapsFlag() = runTest {
        every { dataStore.data } returns flowOf(preferencesOf(KEY_ONBOARDING to true))

        repo.profile.test {
            assertTrue(awaitItem().hasCompletedOnboarding)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── saveProfile ───────────────────────────────────────────────────────

    @Test
    fun saveProfile_callsUpdateData() = runTest {
        coEvery { dataStore.updateData(any()) } coAnswers { emptyPreferences() }

        repo.saveProfile("Alice")

        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun saveProfile_storesNameTrimmed() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } coAnswers {
            transformSlot.captured(emptyPreferences())
        }

        repo.saveProfile("  Alice  ")

        val result = transformSlot.captured(emptyPreferences())
        assertEquals("Alice", result[KEY_NAME])
    }

    // ── completeOnboarding ────────────────────────────────────────────────

    @Test
    fun completeOnboarding_callsUpdateData() = runTest {
        coEvery { dataStore.updateData(any()) } coAnswers { emptyPreferences() }

        repo.completeOnboarding()

        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun completeOnboarding_setsOnboardingFlagTrue() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } coAnswers {
            transformSlot.captured(emptyPreferences())
        }

        repo.completeOnboarding()

        val result = transformSlot.captured(emptyPreferences())
        assertTrue(result[KEY_ONBOARDING] == true)
    }

    // ── clearProfile ──────────────────────────────────────────────────────

    @Test
    fun clearProfile_callsUpdateData() = runTest {
        coEvery { dataStore.updateData(any()) } coAnswers { emptyPreferences() }

        repo.clearProfile()

        coVerify { dataStore.updateData(any()) }
    }
}
