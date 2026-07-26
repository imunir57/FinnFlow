package com.finnflow.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserProfileRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserProfileRepository {

    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("profile_display_name")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
        val CURRENCY_CODE = stringPreferencesKey("profile_currency_code")
        val THEME_MODE = stringPreferencesKey("profile_theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val EMAIL = stringPreferencesKey("profile_email")
        val AVATAR_URL = stringPreferencesKey("profile_avatar_url")
        val GOOGLE_ACCOUNT_ID = stringPreferencesKey("profile_google_account_id")
        val IS_SIGNED_IN = booleanPreferencesKey("profile_is_signed_in")
    }

    override val profile: Flow<UserProfile> get() = dataStore.data.map { prefs ->
        val name = prefs[Keys.DISPLAY_NAME] ?: ""
        UserProfile(
            displayName = name,
            initials = name.toInitials(),
            hasCompletedOnboarding = prefs[Keys.ONBOARDING_DONE] ?: false,
            currencyCode = prefs[Keys.CURRENCY_CODE] ?: "BDT",
            themeMode = prefs[Keys.THEME_MODE] ?: "system",
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            lastBackupTimestamp = prefs[Keys.LAST_BACKUP_TIMESTAMP],
            email = prefs[Keys.EMAIL] ?: "",
            avatarUrl = prefs[Keys.AVATAR_URL],
            googleAccountId = prefs[Keys.GOOGLE_ACCOUNT_ID],
            isSignedIn = prefs[Keys.IS_SIGNED_IN] ?: false
        )
    }

    override suspend fun saveProfile(name: String) {
        dataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = name.trim()
        }
    }

    override suspend fun completeOnboarding() {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_DONE] = true
        }
    }

    override suspend fun clearProfile() {
        dataStore.edit { it.clear() }
    }

    override suspend fun setCurrencyCode(code: String) {
        dataStore.edit { prefs ->
            prefs[Keys.CURRENCY_CODE] = code
        }
    }

    override suspend fun setThemeMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.APP_LOCK_ENABLED] = enabled
        }
    }

    override suspend fun setLastBackupTimestamp(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }

    override suspend fun signInWithGoogle(
        displayName: String,
        email: String,
        avatarUrl: String?,
        googleAccountId: String
    ) {
        dataStore.edit { prefs ->
            // Don't clobber a name the user already customized locally.
            if (prefs[Keys.DISPLAY_NAME].isNullOrBlank() && displayName.isNotBlank()) {
                prefs[Keys.DISPLAY_NAME] = displayName
            }
            prefs[Keys.EMAIL] = email
            if (avatarUrl != null) prefs[Keys.AVATAR_URL] = avatarUrl
            prefs[Keys.GOOGLE_ACCOUNT_ID] = googleAccountId
            prefs[Keys.IS_SIGNED_IN] = true
        }
    }

    override suspend fun signOutGoogle() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.EMAIL)
            prefs.remove(Keys.AVATAR_URL)
            prefs.remove(Keys.GOOGLE_ACCOUNT_ID)
            prefs[Keys.IS_SIGNED_IN] = false
        }
    }
}

private fun String.toInitials(): String {
    val parts = trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}
