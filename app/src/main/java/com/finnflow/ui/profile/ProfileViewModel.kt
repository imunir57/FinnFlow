package com.finnflow.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.auth.GoogleAuthClient
import com.finnflow.data.auth.GoogleAuthResult
import com.finnflow.data.model.TransactionType
import com.finnflow.data.profile.UserProfile
import com.finnflow.data.profile.UserProfileRepository
import com.finnflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val entryCount: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository,
    private val transactionRepository: TransactionRepository,
    private val googleAuthClient: GoogleAuthClient
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        profileRepository.profile,
        transactionRepository.getAllTransactions()
    ) { profile, transactions ->
        ProfileUiState(
            profile = profile,
            totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
            totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
            entryCount = transactions.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun saveName(name: String) {
        viewModelScope.launch { profileRepository.saveProfile(name) }
    }

    fun onSignInWithGoogle(context: Context) {
        viewModelScope.launch {
            when (val result = googleAuthClient.signIn(context)) {
                is GoogleAuthResult.Success -> profileRepository.signInWithGoogle(
                    displayName = result.identity.displayName,
                    email = result.identity.email,
                    avatarUrl = result.identity.avatarUrl,
                    googleAccountId = result.identity.googleAccountId
                )
                is GoogleAuthResult.Cancelled -> Unit
                is GoogleAuthResult.Error -> _messages.send(result.message)
            }
        }
    }
}
