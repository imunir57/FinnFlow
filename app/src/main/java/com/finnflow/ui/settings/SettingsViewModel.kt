package com.finnflow.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.profile.UserProfile
import com.finnflow.data.profile.UserProfileRepository
import com.finnflow.data.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    val profile: StateFlow<UserProfile> = profileRepository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

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
}
