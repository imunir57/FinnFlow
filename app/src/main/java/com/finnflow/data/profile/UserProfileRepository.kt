package com.finnflow.data.profile

import kotlinx.coroutines.flow.Flow

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
