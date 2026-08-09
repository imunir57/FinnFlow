package com.finnflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.Currency
import com.finnflow.data.profile.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainViewModel"

@HiltViewModel
class MainViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository
) : ViewModel() {

    init {
        SecureLogger.d(TAG, "MainViewModel initialized")
        // Backfills the profile's creation date on installs that predate the key. Writes once —
        // every later start is a no-op, so this can safely run on every launch.
        viewModelScope.launch {
            try {
                profileRepository.ensureCreatedAt(System.currentTimeMillis())
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Failed to record profile creation timestamp", e)
            }
        }
    }

    // null = still loading, true/false = resolved
    val hasCompletedOnboarding = profileRepository.profile
        .map { profile ->
            val completed = profile.hasCompletedOnboarding as Boolean?
            if (completed != null) {
                SecureLogger.d(TAG, "Onboarding state: $completed")
            }
            completed
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val themeMode = profileRepository.profile
        .map { profile ->
            val mode = profile.themeMode
            SecureLogger.d(TAG, "Theme mode loaded: $mode")
            mode
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "system")

    // Seeds LocalCurrencyFormat for the whole composition, so a change in Settings reaches
    // every screen without a restart.
    val currency = profileRepository.profile
        .map { profile ->
            val currency = Currency.fromCode(profile.currencyCode)
            SecureLogger.d(TAG, "Currency loaded: ${currency.code}")
            currency
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.BDT)

    val appLockEnabled = profileRepository.profile
        .map { profile ->
            val enabled = profile.appLockEnabled
            SecureLogger.d(TAG, "App lock enabled: $enabled")
            enabled
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun onUnlocked() {
        SecureLogger.d(TAG, "App unlocked via biometric/pin")
        _isUnlocked.value = true
    }

    /** Called when the app leaves the foreground so the next return re-prompts App Lock. */
    fun onLocked() {
        SecureLogger.d(TAG, "App locked - session ended")
        _isUnlocked.value = false
    }
}
