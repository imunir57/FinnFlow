package com.finnflow.data.notification

/**
 * Schedules / cancels the daily "log today's spending" local reminder notification.
 * Kept behind an interface so WorkManager calls can be mocked in ViewModel tests.
 */
interface ReminderScheduler {
    fun schedule()
    fun cancel()
}
