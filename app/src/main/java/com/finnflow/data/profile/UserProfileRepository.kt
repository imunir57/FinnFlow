package com.finnflow.data.profile

import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    val profile: Flow<UserProfile>
    suspend fun saveProfile(name: String)
    suspend fun completeOnboarding()
    suspend fun clearProfile()
    suspend fun setCurrencyCode(code: String)
    suspend fun setThemeMode(mode: String)
}
