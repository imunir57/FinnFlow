package com.finnflow.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
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
    private val KEY_CURRENCY = stringPreferencesKey("profile_currency_code")
    private val KEY_THEME = stringPreferencesKey("profile_theme_mode")
    private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    private val KEY_APP_LOCK = booleanPreferencesKey("app_lock_enabled")
    private val KEY_LAST_BACKUP = longPreferencesKey("last_backup_timestamp")
    private val KEY_EMAIL = stringPreferencesKey("profile_email")
    private val KEY_AVATAR = stringPreferencesKey("profile_avatar_url")
    private val KEY_GOOGLE_ID = stringPreferencesKey("profile_google_account_id")
    private val KEY_SIGNED_IN = booleanPreferencesKey("profile_is_signed_in")
    private val KEY_NAME_IS_CUSTOM = booleanPreferencesKey("profile_name_is_custom")

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

    @Test
    fun profile_withEmptyPrefs_defaultsCurrencyAndTheme() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())

        repo.profile.test {
            val p = awaitItem()
            assertEquals("BDT", p.currencyCode)
            assertEquals("system", p.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun profile_withNoBackup_hasNullTimestamp() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())

        repo.profile.test {
            assertNull(awaitItem().lastBackupTimestamp)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun profile_withEmptyPrefs_notificationsDefaultTrue() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())

        repo.profile.test {
            assertTrue(awaitItem().notificationsEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun profile_withSavedCurrencyAndTheme_mapsThem() = runTest {
        every { dataStore.data } returns flowOf(
            preferencesOf(KEY_CURRENCY to "USD", KEY_THEME to "dark")
        )

        repo.profile.test {
            val p = awaitItem()
            assertEquals("USD", p.currencyCode)
            assertEquals("dark", p.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun profile_withNotificationsDisabled_mapsFlag() = runTest {
        every { dataStore.data } returns flowOf(preferencesOf(KEY_NOTIFICATIONS to false))

        repo.profile.test {
            assertFalse(awaitItem().notificationsEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun profile_withEmptyPrefs_appLockDefaultFalse() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())

        repo.profile.test {
            assertFalse(awaitItem().appLockEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun profile_withAppLockEnabled_mapsFlag() = runTest {
        every { dataStore.data } returns flowOf(preferencesOf(KEY_APP_LOCK to true))

        repo.profile.test {
            assertTrue(awaitItem().appLockEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun profile_withSavedBackupTimestamp_mapsValue() = runTest {
        every { dataStore.data } returns flowOf(preferencesOf(KEY_LAST_BACKUP to 12345L))

        repo.profile.test {
            assertEquals(12345L, awaitItem().lastBackupTimestamp)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun profile_withEmptyPrefs_isSignedInFalseAndEmailBlank() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())

        repo.profile.test {
            val p = awaitItem()
            assertEquals("", p.email)
            assertNull(p.avatarUrl)
            assertNull(p.googleAccountId)
            assertFalse(p.isSignedIn)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun profile_withSignedInPrefs_mapsAccountFields() = runTest {
        every { dataStore.data } returns flowOf(
            preferencesOf(
                KEY_EMAIL to "jane@gmail.com",
                KEY_GOOGLE_ID to "sub123",
                KEY_SIGNED_IN to true
            )
        )

        repo.profile.test {
            val p = awaitItem()
            assertEquals("jane@gmail.com", p.email)
            assertEquals("sub123", p.googleAccountId)
            assertTrue(p.isSignedIn)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── saveProfile ───────────────────────────────────────────────────────

    @Test
    fun saveProfile_callsUpdateData() = runTest {
        coEvery { dataStore.updateData(any()) } returns emptyPreferences()

        repo.saveProfile("Alice")

        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun saveProfile_storesNameTrimmed() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } returns emptyPreferences()

        repo.saveProfile("  Alice  ")

        val result = transformSlot.captured(emptyPreferences())
        assertEquals("Alice", result[KEY_NAME])
    }

    @Test
    fun saveProfile_marksNameAsCustom() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } returns emptyPreferences()

        repo.saveProfile("Alice")

        val result = transformSlot.captured(emptyPreferences())
        assertEquals(true, result[KEY_NAME_IS_CUSTOM])
    }

    @Test
    fun saveProfile_withBlankName_doesNotMarkNameAsCustom() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } returns emptyPreferences()

        repo.saveProfile("   ")

        val result = transformSlot.captured(preferencesOf(KEY_NAME_IS_CUSTOM to true))
        assertEquals(false, result[KEY_NAME_IS_CUSTOM])
    }

    // ── completeOnboarding ────────────────────────────────────────────────

    @Test
    fun completeOnboarding_callsUpdateData() = runTest {
        coEvery { dataStore.updateData(any()) } returns emptyPreferences()

        repo.completeOnboarding()

        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun completeOnboarding_setsOnboardingFlagTrue() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } returns emptyPreferences()

        repo.completeOnboarding()

        val result = transformSlot.captured(emptyPreferences())
        assertTrue(result[KEY_ONBOARDING] == true)
    }

    // ── clearProfile ──────────────────────────────────────────────────────

    @Test
    fun clearProfile_callsUpdateData() = runTest {
        coEvery { dataStore.updateData(any()) } returns emptyPreferences()

        repo.clearProfile()

        coVerify { dataStore.updateData(any()) }
    }

    // ── setCurrencyCode ───────────────────────────────────────────────────

    @Test
    fun setCurrencyCode_callsUpdateData() = runTest {
        coEvery { dataStore.updateData(any()) } returns emptyPreferences()

        repo.setCurrencyCode("USD")

        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun setCurrencyCode_storesCode() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } returns emptyPreferences()

        repo.setCurrencyCode("EUR")

        val result = transformSlot.captured(emptyPreferences())
        assertEquals("EUR", result[KEY_CURRENCY])
    }

    // ── setThemeMode ──────────────────────────────────────────────────────

    @Test
    fun setThemeMode_callsUpdateData() = runTest {
        coEvery { dataStore.updateData(any()) } returns emptyPreferences()

        repo.setThemeMode("dark")

        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun setThemeMode_storesMode() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } returns emptyPreferences()

        repo.setThemeMode("light")

        val result = transformSlot.captured(emptyPreferences())
        assertEquals("light", result[KEY_THEME])
    }

    // ── setNotificationsEnabled ──────────────────────────────────────────

    @Test
    fun setNotificationsEnabled_callsUpdateData() = runTest {
        coEvery { dataStore.updateData(any()) } returns emptyPreferences()

        repo.setNotificationsEnabled(false)

        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun setNotificationsEnabled_storesFlag() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } returns emptyPreferences()

        repo.setNotificationsEnabled(false)

        val result = transformSlot.captured(emptyPreferences())
        assertEquals(false, result[KEY_NOTIFICATIONS])
    }

    // ── setAppLockEnabled ─────────────────────────────────────────────────

    @Test
    fun setAppLockEnabled_callsUpdateData() = runTest {
        coEvery { dataStore.updateData(any()) } returns emptyPreferences()

        repo.setAppLockEnabled(true)

        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun setAppLockEnabled_storesFlag() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } returns emptyPreferences()

        repo.setAppLockEnabled(true)

        val result = transformSlot.captured(emptyPreferences())
        assertEquals(true, result[KEY_APP_LOCK])
    }

    // ── setLastBackupTimestamp ───────────────────────────────────────────

    @Test
    fun setLastBackupTimestamp_callsUpdateData() = runTest {
        coEvery { dataStore.updateData(any()) } returns emptyPreferences()

        repo.setLastBackupTimestamp(999L)

        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun setLastBackupTimestamp_storesValue() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } returns emptyPreferences()

        repo.setLastBackupTimestamp(999L)

        val result = transformSlot.captured(emptyPreferences())
        assertEquals(999L, result[KEY_LAST_BACKUP])
    }

    // ── signInWithGoogle ──────────────────────────────────────────────────

    @Test
    fun signInWithGoogle_callsUpdateData() = runTest {
        coEvery { dataStore.updateData(any()) } returns emptyPreferences()

        repo.signInWithGoogle("Jane", "jane@gmail.com", "https://example.com/pic.jpg", "sub123")

        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun signInWithGoogle_setsAllFieldsAndSignedInTrue() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } returns emptyPreferences()

        repo.signInWithGoogle("Jane", "jane@gmail.com", "https://example.com/pic.jpg", "sub123")

        val result = transformSlot.captured(emptyPreferences())
        assertEquals("Jane", result[KEY_NAME])
        assertEquals("jane@gmail.com", result[KEY_EMAIL])
        assertEquals("https://example.com/pic.jpg", result[KEY_AVATAR])
        assertEquals("sub123", result[KEY_GOOGLE_ID])
        assertTrue(result[KEY_SIGNED_IN] == true)
    }

    @Test
    fun signInWithGoogle_doesNotOverwriteUserTypedDisplayName() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } returns emptyPreferences()

        repo.signInWithGoogle("Jane", "jane@gmail.com", null, "sub123")

        val existing = preferencesOf(KEY_NAME to "Custom Name", KEY_NAME_IS_CUSTOM to true)
        val result = transformSlot.captured(existing)
        assertEquals("Custom Name", result[KEY_NAME])
        assertEquals(true, result[KEY_NAME_IS_CUSTOM])
    }

    @Test
    fun signInWithGoogle_treatsPreFlagNameAsUserTyped() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } returns emptyPreferences()

        repo.signInWithGoogle("Jane", "jane@gmail.com", null, "sub123")

        // Installs that predate NAME_IS_CUSTOM have no flag; a name there can only have
        // been typed by the user, so it must survive.
        val existing = preferencesOf(KEY_NAME to "Custom Name")
        val result = transformSlot.captured(existing)
        assertEquals("Custom Name", result[KEY_NAME])
        assertEquals(true, result[KEY_NAME_IS_CUSTOM])
    }

    @Test
    fun signInWithGoogle_replacesNameLeftByPreviousGoogleAccount() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } returns emptyPreferences()

        repo.signInWithGoogle("Sam Roy", "sam@gmail.com", null, "sub456")

        // Account A signed in earlier and left its name behind with the flag false.
        val existing = preferencesOf(KEY_NAME to "Jane Doe", KEY_NAME_IS_CUSTOM to false)
        val result = transformSlot.captured(existing)
        assertEquals("Sam Roy", result[KEY_NAME])
        assertEquals("sam@gmail.com", result[KEY_EMAIL])
        assertEquals(false, result[KEY_NAME_IS_CUSTOM])
    }

    @Test
    fun signInWithGoogle_withBlankGoogleName_keepsExistingName() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } returns emptyPreferences()

        repo.signInWithGoogle("", "sam@gmail.com", null, "sub456")

        val existing = preferencesOf(KEY_NAME to "Jane Doe", KEY_NAME_IS_CUSTOM to false)
        val result = transformSlot.captured(existing)
        assertEquals("Jane Doe", result[KEY_NAME])
    }

    @Test
    fun signInWithGoogle_withBlankExistingName_fillsItIn() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } returns emptyPreferences()

        repo.signInWithGoogle("Jane", "jane@gmail.com", null, "sub123")

        val result = transformSlot.captured(emptyPreferences())
        assertEquals("Jane", result[KEY_NAME])
        assertEquals(false, result[KEY_NAME_IS_CUSTOM])
    }

    // ── signOutGoogle ─────────────────────────────────────────────────────

    @Test
    fun signOutGoogle_callsUpdateData() = runTest {
        coEvery { dataStore.updateData(any()) } returns emptyPreferences()

        repo.signOutGoogle()

        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun signOutGoogle_clearsAccountFieldsButKeepsName() = runTest {
        val transformSlot = slot<suspend (Preferences) -> Preferences>()
        coEvery { dataStore.updateData(capture(transformSlot)) } returns emptyPreferences()

        repo.signOutGoogle()

        val existing = preferencesOf(
            KEY_NAME to "Jane",
            KEY_EMAIL to "jane@gmail.com",
            KEY_GOOGLE_ID to "sub123",
            KEY_SIGNED_IN to true
        )
        val result = transformSlot.captured(existing)
        assertEquals("Jane", result[KEY_NAME])
        assertNull(result[KEY_EMAIL])
        assertNull(result[KEY_GOOGLE_ID])
        assertEquals(false, result[KEY_SIGNED_IN])
    }
}
