package com.finnflow.ui.settings

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.auth.GoogleAuthClient
import com.finnflow.data.biometric.BiometricAuthenticator
import com.finnflow.data.notification.ReminderScheduler
import com.finnflow.data.profile.UserProfile
import com.finnflow.data.profile.UserProfileRepository
import com.finnflow.data.repository.BackupRepository
import com.finnflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository,
    private val transactionRepository: TransactionRepository,
    private val reminderScheduler: ReminderScheduler,
    private val biometricAuthenticator: BiometricAuthenticator,
    private val backupRepository: BackupRepository,
    private val googleAuthClient: GoogleAuthClient
) : ViewModel() {

    val profile: StateFlow<UserProfile> = profileRepository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    /**
     * Exports all transactions as CSV to [outputStream]. [outputStream] is provided by the
     * screen (from a SAF `CreateDocument` result), since the ViewModel has no Context access.
     * [onComplete] fires on the main thread once the write finishes, so the screen can
     * offer a share sheet.
     */
    fun exportCsv(outputStream: OutputStream, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            transactionRepository.exportTransactionsCsv(outputStream)
            onComplete()
        }
    }

    fun onCurrencySelected(code: String) {
        viewModelScope.launch { profileRepository.setCurrencyCode(code) }
    }

    fun onThemeModeSelected(mode: String) {
        viewModelScope.launch { profileRepository.setThemeMode(mode) }
    }

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
                "Set up a screen lock or biometrics in your device settings to use App Lock"
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

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun performBackup(outputStream: OutputStream?) {
        if (outputStream == null) {
            viewModelScope.launch { _messages.send("Couldn't open the selected file") }
            return
        }
        viewModelScope.launch {
            try {
                backupRepository.exportBackup(outputStream)
                profileRepository.setLastBackupTimestamp(System.currentTimeMillis())
                _messages.send("Backup saved")
            } catch (e: Exception) {
                _messages.send("Backup failed: ${e.message ?: "unknown error"}")
            }
        }
    }

    fun performRestore(inputStream: InputStream?) {
        if (inputStream == null) {
            viewModelScope.launch { _messages.send("Couldn't open the selected file") }
            return
        }
        viewModelScope.launch {
            backupRepository.restoreBackup(inputStream).fold(
                onSuccess = { _messages.send("Restore complete") },
                onFailure = { _messages.send("Restore failed: ${it.message ?: "invalid backup file"}") }
            )
        }
    }

    fun onSignOut(context: Context) {
        viewModelScope.launch {
            googleAuthClient.signOut(context)
            profileRepository.signOutGoogle()
            _messages.send("Signed out")
        }
    }
}
