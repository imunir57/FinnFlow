package com.finnflow.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
    }

    override val profile: Flow<UserProfile> = dataStore.data.map { prefs ->
        val name = prefs[Keys.DISPLAY_NAME] ?: ""
        UserProfile(
            displayName = name,
            initials = name.toInitials(),
            hasCompletedOnboarding = prefs[Keys.ONBOARDING_DONE] ?: false
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
}

private fun String.toInitials(): String {
    val parts = trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}
