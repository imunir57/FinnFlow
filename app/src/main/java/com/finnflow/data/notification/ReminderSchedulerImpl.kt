package com.finnflow.data.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.finnflow.data.logger.SecureLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "ReminderScheduler"

class ReminderSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ReminderScheduler {

    override fun schedule() {
        try {
            val delayMillis = initialDelayMillis()
            SecureLogger.d(TAG, "Scheduling daily reminder work: initial delay=${delayMillis}ms (${delayMillis / 1000 / 60} minutes)")
            val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            SecureLogger.d(TAG, "Daily reminder work scheduled successfully with WorkManager")
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to schedule daily reminder work", e)
            throw e
        }
    }

    override fun cancel() {
        try {
            SecureLogger.d(TAG, "Cancelling daily reminder work")
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            SecureLogger.d(TAG, "Daily reminder work cancelled successfully")
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to cancel daily reminder work", e)
            throw e
        }
    }

    private fun initialDelayMillis(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val delay = target.timeInMillis - now.timeInMillis
        SecureLogger.d(TAG, "Initial delay calculation: target time=21:00, delay=${delay}ms")
        return delay
    }

    companion object {
        const val WORK_NAME = "daily_spending_reminder"
    }
}
