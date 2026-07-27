package com.finnflow.data.profile

import kotlinx.coroutines.flow.Flow

/**
 * Manages user profile and app preferences via DataStore.
 *
 * Logging: All mutations are logged at INFO level via SecureLogger, including:
 * - Profile updates (display name, email, avatar) with field names only (not values)
 * - Preference changes (currency, theme mode, notifications, app lock)
 * - Google sign-in/sign-out lifecycle events
 * - Onboarding completion status
 * - Backup timestamp updates
 * Field values and sensitive data are never logged (only field names for which changes occurred).
 */
interface UserProfileRepository {
    val profile: Flow<UserProfile>
    suspend fun saveProfile(name: String)
    suspend fun completeOnboarding()
    suspend fun clearProfile()
    suspend fun setCurrencyCode(code: String)
    suspend fun setThemeMode(mode: String)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setAppLockEnabled(enabled: Boolean)
    suspend fun setLastBackupTimestamp(timestamp: Long)
    suspend fun signInWithGoogle(displayName: String, email: String, avatarUrl: String?, googleAccountId: String)
    suspend fun signOutGoogle()
}
