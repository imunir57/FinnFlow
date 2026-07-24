package com.finnflow.ui.settings

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.biometric.BiometricAuthenticator
import com.finnflow.data.notification.ReminderScheduler
import com.finnflow.data.profile.UserProfile
import com.finnflow.data.profile.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository,
    private val reminderScheduler: ReminderScheduler,
    private val biometricAuthenticator: BiometricAuthenticator
) : ViewModel() {

    val profile: StateFlow<UserProfile> = profileRepository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    // Explains why App Lock couldn't be enabled (unsupported device, no biometrics enrolled, etc).
    private val _appLockMessage = MutableStateFlow<String?>(null)
    val appLockMessage: StateFlow<String?> = _appLockMessage.asStateFlow()

    fun onNotificationsToggled(enabled: Boolean) {
        viewModelScope.launch {
            profileRepository.setNotificationsEnabled(enabled)
            if (enabled) reminderScheduler.schedule() else reminderScheduler.cancel()
        }
    }

    fun onAppLockToggled(enabled: Boolean, activity: FragmentActivity?) {
        if (!enabled) {
            _appLockMessage.value = null
            viewModelScope.launch { profileRepository.setAppLockEnabled(false) }
            return
        }

        if (!biometricAuthenticator.canAuthenticate()) {
            _appLockMessage.value =
                "Set up fingerprint or face unlock in your device settings to use App Lock"
            return
        }

        if (activity == null) {
            _appLockMessage.value = "Couldn't start authentication"
            return
        }

        biometricAuthenticator.authenticate(
            activity = activity,
            onSuccess = {
                _appLockMessage.value = null
                viewModelScope.launch { profileRepository.setAppLockEnabled(true) }
            },
            onError = { message -> _appLockMessage.value = message }
        )
    }
}
