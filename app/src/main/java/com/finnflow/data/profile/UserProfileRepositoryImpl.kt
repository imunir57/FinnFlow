package com.finnflow.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.finnflow.data.logger.SecureLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserProfileRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserProfileRepository {
    companion object {
        private const val TAG = "UserProfileRepository"
    }

    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("profile_display_name")
        val NAME_IS_CUSTOM = booleanPreferencesKey("profile_name_is_custom")
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
            val trimmed = name.trim()
            prefs[Keys.DISPLAY_NAME] = trimmed
            // Only a name the user actually typed is worth protecting from a later
            // Google sign-in; clearing the field hands control back to Google.
            prefs[Keys.NAME_IS_CUSTOM] = trimmed.isNotBlank()
            SecureLogger.i(TAG, "User profile updated: displayName (custom=${trimmed.isNotBlank()})")
        }
    }

    override suspend fun completeOnboarding() {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_DONE] = true
            SecureLogger.i(TAG, "Onboarding completed")
        }
    }

    override suspend fun clearProfile() {
        dataStore.edit { it.clear() }
        SecureLogger.i(TAG, "All profile data cleared")
    }

    override suspend fun setCurrencyCode(code: String) {
        dataStore.edit { prefs ->
            prefs[Keys.CURRENCY_CODE] = code
            SecureLogger.i(TAG, "Preference updated: currencyCode")
        }
    }

    override suspend fun setThemeMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
            SecureLogger.i(TAG, "Preference updated: themeMode")
        }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
            SecureLogger.i(TAG, "Preference updated: notificationsEnabled")
        }
    }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.APP_LOCK_ENABLED] = enabled
            val action = if (enabled) "enabled" else "disabled"
            SecureLogger.i(TAG, "App lock $action")
        }
    }

    override suspend fun setLastBackupTimestamp(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_BACKUP_TIMESTAMP] = timestamp
            SecureLogger.i(TAG, "Backup completed at timestamp: $timestamp")
        }
    }

    override suspend fun signInWithGoogle(
        displayName: String,
        email: String,
        avatarUrl: String?,
        googleAccountId: String
    ) {
        dataStore.edit { prefs ->
            // Don't clobber a name the user typed themselves, but do replace one that a
            // previous Google sign-in wrote — otherwise switching accounts leaves the old
            // account's name sitting next to the new account's email. The flag is absent on
            // installs that predate it, where any existing name can only have been typed.
            val nameIsCustom = prefs[Keys.NAME_IS_CUSTOM]
                ?: !prefs[Keys.DISPLAY_NAME].isNullOrBlank()
            if (!nameIsCustom && displayName.isNotBlank()) {
                prefs[Keys.DISPLAY_NAME] = displayName
            }
            prefs[Keys.NAME_IS_CUSTOM] = nameIsCustom
            prefs[Keys.EMAIL] = email
            if (avatarUrl != null) prefs[Keys.AVATAR_URL] = avatarUrl
            prefs[Keys.GOOGLE_ACCOUNT_ID] = googleAccountId
            prefs[Keys.IS_SIGNED_IN] = true
            SecureLogger.i(TAG, "User signed in with Google [nameOverride=${!nameIsCustom && displayName.isNotBlank()}, avatar=${avatarUrl != null}]")
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
