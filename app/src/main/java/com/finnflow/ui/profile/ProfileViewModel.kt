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
import com.finnflow.data.repository.BackupRepository
import com.finnflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.OutputStream
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
    private val backupRepository: BackupRepository,
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

    /**
     * Writes a JSON backup to [outputStream], offered from the sign-out confirmation so the user
     * can keep their data before it is erased. [outputStream] comes from a SAF result on the
     * screen, since the ViewModel has no Context.
     */
    fun performBackup(outputStream: OutputStream?) {
        if (outputStream == null) {
            SecureLogger.w(TAG, "Backup before sign out failed: output stream is null")
            viewModelScope.launch { _messages.send("Couldn't open the selected file") }
            return
        }
        viewModelScope.launch {
            try {
                backupRepository.exportBackup(outputStream)
                profileRepository.setLastBackupTimestamp(System.currentTimeMillis())
                SecureLogger.i(TAG, "Backup before sign out completed")
                _messages.send("Backup saved")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Backup before sign out failed", e)
                _messages.send("Backup failed: ${e.message ?: "unknown error"}")
            }
        }
    }

    /**
     * Signs out and erases everything on the device. Applies to local profiles too — there the
     * Google credential clear is a no-op and the erase is the whole point. The screen confirms
     * first and offers a backup, so by the time this runs the user has agreed to lose the data.
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
