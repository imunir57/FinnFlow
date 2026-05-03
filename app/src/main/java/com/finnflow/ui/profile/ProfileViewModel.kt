package com.finnflow.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.model.TransactionType
import com.finnflow.data.profile.UserProfile
import com.finnflow.data.profile.UserProfileRepository
import com.finnflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val transactionRepository: TransactionRepository
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

    fun saveName(name: String) {
        viewModelScope.launch { profileRepository.saveProfile(name) }
    }
}
