package com.finnflow.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.auth.GoogleAuthClient
import com.finnflow.data.auth.GoogleAuthResult
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.Transaction
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
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

private const val TAG = "ProfileViewModel"

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val entryCount: Int = 0,
    /** Null until there is anything to date the account from — the screen then omits the line. */
    val memberSince: YearMonth? = null
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
        SecureLogger.d(TAG, "Profile state updated: displayName=${profile.displayName.take(1)}***, currencyCode=${profile.currencyCode}, isSignedIn=${profile.isSignedIn}")
        ProfileUiState(
            profile = profile,
            totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
            totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
            entryCount = transactions.size,
            memberSince = memberSince(profile, transactions)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    /**
     * The earlier of the stored creation date and the oldest transaction.
     *
     * The stored date is backfilled at app start, so on an install that predates the key it reads
     * as "today" — the oldest transaction is the better evidence there. Taking the minimum of the
     * two is right in both directions: a fresh install has no transactions, and an old one has
     * records going back further than the stamp.
     */
    private fun memberSince(profile: UserProfile, transactions: List<Transaction>): YearMonth? {
        val created = profile.createdAtMillis
            ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
        val firstEntry = transactions.minOfOrNull { it.date }
        return listOfNotNull(created, firstEntry).minOrNull()?.let { YearMonth.from(it) }
    }

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun saveName(name: String) {
        SecureLogger.d(TAG, "saveName called with name length=${name.length}")
        viewModelScope.launch {
            try {
                profileRepository.saveProfile(name)
                SecureLogger.d(TAG, "Profile name saved successfully")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Failed to save profile name", e)
            }
        }
    }

    fun onSignInWithGoogle(context: Context) {
        SecureLogger.d(TAG, "Google Sign-In initiated")
        viewModelScope.launch {
            try {
                when (val result = googleAuthClient.signIn(context)) {
                    is GoogleAuthResult.Success -> {
                        SecureLogger.d(TAG, "Google Sign-In successful, saving profile data")
                        profileRepository.signInWithGoogle(
                            displayName = result.identity.displayName,
                            email = result.identity.email,
                            avatarUrl = result.identity.avatarUrl,
                            googleAccountId = result.identity.googleAccountId
                        )
                        SecureLogger.d(TAG, "Profile updated with Google account data")
                    }
                    is GoogleAuthResult.Cancelled -> {
                        SecureLogger.d(TAG, "Google Sign-In cancelled by user")
                    }
                    is GoogleAuthResult.Error -> {
                        SecureLogger.w(TAG, "Google Sign-In error: ${result.message}")
                        _messages.send(result.message)
                    }
                }
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Exception during Google Sign-In", e)
            }
        }
    }
}
