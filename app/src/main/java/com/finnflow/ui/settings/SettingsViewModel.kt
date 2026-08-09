package com.finnflow.ui.settings

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.auth.GoogleAuthClient
import com.finnflow.data.biometric.BiometricAuthenticator
import com.finnflow.data.logger.SecureLogger
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

private const val TAG = "SettingsViewModel"

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
        SecureLogger.d(TAG, "CSV export started")
        viewModelScope.launch {
            try {
                transactionRepository.exportTransactionsCsv(outputStream)
                SecureLogger.d(TAG, "CSV export completed successfully")
                onComplete()
            } catch (e: Exception) {
                SecureLogger.e(TAG, "CSV export failed", e)
            }
        }
    }

    fun onCurrencySelected(code: String) {
        SecureLogger.d(TAG, "Currency changed to: $code")
        viewModelScope.launch {
            try {
                profileRepository.setCurrencyCode(code)
                SecureLogger.d(TAG, "Currency preference saved: $code")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Failed to save currency preference", e)
            }
        }
    }

    fun onThemeModeSelected(mode: String) {
        SecureLogger.d(TAG, "Theme mode changed to: $mode")
        viewModelScope.launch {
            try {
                profileRepository.setThemeMode(mode)
                SecureLogger.d(TAG, "Theme mode preference saved: $mode")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Failed to save theme mode preference", e)
            }
        }
    }

    // Explains why App Lock couldn't be enabled (unsupported device, no biometrics enrolled, etc).
    private val _appLockMessage = MutableStateFlow<String?>(null)
    val appLockMessage: StateFlow<String?> = _appLockMessage.asStateFlow()

    fun onNotificationsToggled(enabled: Boolean) {
        SecureLogger.d(TAG, "Notifications toggled: enabled=$enabled")
        viewModelScope.launch {
            try {
                profileRepository.setNotificationsEnabled(enabled)
                if (enabled) {
                    reminderScheduler.schedule()
                    SecureLogger.d(TAG, "Notification reminders scheduled")
                } else {
                    reminderScheduler.cancel()
                    SecureLogger.d(TAG, "Notification reminders cancelled")
                }
                SecureLogger.i(TAG, "Notification preference saved: enabled=$enabled")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Failed to toggle notifications", e)
            }
        }
    }

    fun onAppLockToggled(enabled: Boolean, activity: FragmentActivity?) {
        SecureLogger.d(TAG, "App Lock toggle attempted: enabled=$enabled")
        if (!enabled) {
            _appLockMessage.value = null
            viewModelScope.launch {
                try {
                    profileRepository.setAppLockEnabled(false)
                    SecureLogger.d(TAG, "App Lock disabled successfully")
                } catch (e: Exception) {
                    SecureLogger.e(TAG, "Failed to disable App Lock", e)
                }
            }
            return
        }

        if (!biometricAuthenticator.canAuthenticate()) {
            SecureLogger.w(TAG, "App Lock enable failed: biometric authentication not available")
            _appLockMessage.value =
                "Set up a screen lock or biometrics in your device settings to use App Lock"
            return
        }

        if (activity == null) {
            SecureLogger.w(TAG, "App Lock enable failed: activity is null")
            _appLockMessage.value = "Couldn't start authentication"
            return
        }

        SecureLogger.d(TAG, "Initiating biometric authentication for App Lock enable")
        biometricAuthenticator.authenticate(
            activity = activity,
            onSuccess = {
                _appLockMessage.value = null
                SecureLogger.d(TAG, "Biometric authentication succeeded, enabling App Lock")
                viewModelScope.launch {
                    try {
                        profileRepository.setAppLockEnabled(true)
                        SecureLogger.d(TAG, "App Lock enabled successfully")
                    } catch (e: Exception) {
                        SecureLogger.e(TAG, "Failed to enable App Lock", e)
                    }
                }
            },
            onError = { message ->
                SecureLogger.w(TAG, "Biometric authentication failed: $message")
                _appLockMessage.value = message
            }
        )
    }

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun performBackup(outputStream: OutputStream?) {
        if (outputStream == null) {
            SecureLogger.w(TAG, "Backup operation failed: output stream is null")
            viewModelScope.launch { _messages.send("Couldn't open the selected file") }
            return
        }
        viewModelScope.launch {
            try {
                SecureLogger.i(TAG, "Backup operation initiated by user")
                val startTime = System.currentTimeMillis()
                backupRepository.exportBackup(outputStream)
                val timestamp = System.currentTimeMillis()
                profileRepository.setLastBackupTimestamp(timestamp)
                val duration = timestamp - startTime
                SecureLogger.i(TAG, "Backup operation completed successfully in ${duration}ms, timestamp recorded: $timestamp")
                _messages.send("Backup saved")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Backup operation failed", e)
                _messages.send("Backup failed: ${e.message ?: "unknown error"}")
            }
        }
    }

    fun performRestore(inputStream: InputStream?) {
        if (inputStream == null) {
            SecureLogger.w(TAG, "Restore operation failed: input stream is null")
            viewModelScope.launch { _messages.send("Couldn't open the selected file") }
            return
        }
        viewModelScope.launch {
            SecureLogger.i(TAG, "Restore operation initiated by user")
            val startTime = System.currentTimeMillis()
            backupRepository.restoreBackup(inputStream).fold(
                onSuccess = {
                    val duration = System.currentTimeMillis() - startTime
                    SecureLogger.i(TAG, "Restore operation completed successfully in ${duration}ms")
                    _messages.send("Restore complete")
                },
                onFailure = { error ->
                    val duration = System.currentTimeMillis() - startTime
                    SecureLogger.e(TAG, "Restore operation failed after ${duration}ms: ${error.message}", error)
                    _messages.send("Restore failed: ${error.message ?: "invalid backup file"}")
                }
            )
        }
    }

    /**
     * Signs out and erases everything on the device. The screen confirms first and offers a
     * backup — by the time this runs the user has agreed to lose the data.
     *
     * Order matters. Clearing the credential cache is best-effort and must never gate the rest,
     * so it runs first in its own try. The profile clear runs last because wiping the onboarding
     * flag sends the app back to onboarding, which tears this ViewModel down mid-coroutine —
     * anything after it would be cancelled.
     */
    fun onSignOut(context: Context) {
        SecureLogger.i(TAG, "User initiated sign out with data erase")
        viewModelScope.launch {
            try {
                googleAuthClient.signOut(context)
                SecureLogger.d(TAG, "Google authentication sign out completed")
            } catch (e: Exception) {
                SecureLogger.w(TAG, "Credential cache clear failed, continuing with local sign out", e)
            }
            try {
                backupRepository.eraseAllData()
                profileRepository.clearProfile()
                SecureLogger.i(TAG, "Sign out and data erase completed successfully")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Sign out operation failed", e)
                _messages.send("Sign out failed: ${e.message ?: "unknown error"}")
            }
        }
    }
}
