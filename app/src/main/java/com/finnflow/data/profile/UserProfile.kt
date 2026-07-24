package com.finnflow.data.profile

import com.finnflow.data.model.Currency

data class UserProfile(
    val displayName: String = "",
    val initials: String = "",
    val hasCompletedOnboarding: Boolean = false,
    val currencyCode: String = "BDT",
    val themeMode: String = "system",
    val notificationsEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val lastBackupTimestamp: Long? = null
) {
    val currencySymbol: String
        get() = Currency.fromCode(currencyCode).symbol
}
