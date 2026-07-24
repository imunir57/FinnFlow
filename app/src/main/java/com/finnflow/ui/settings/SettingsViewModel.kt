package com.finnflow.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.profile.UserProfile
import com.finnflow.data.profile.UserProfileRepository
import com.finnflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository,
    private val transactionRepository: TransactionRepository
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
}
