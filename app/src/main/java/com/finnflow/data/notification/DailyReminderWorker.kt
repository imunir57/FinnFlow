package com.finnflow.data.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finnflow.FinnFlowApp
import com.finnflow.data.logger.SecureLogger

private const val TAG = "DailyReminderWorker"

/**
 * Fires once a day (see [ReminderSchedulerImpl]) and posts a local notification
 * reminding the user to log today's spending.
 */
class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            SecureLogger.d(TAG, "DailyReminderWorker.doWork() executing")
            postNotification()
            SecureLogger.d(TAG, "DailyReminderWorker.doWork() completed successfully")
            Result.success()
        } catch (e: Exception) {
            SecureLogger.e(TAG, "DailyReminderWorker.doWork() failed", e)
            Result.retry()
        }
    }

    private fun postNotification() {
        val context = applicationContext
        SecureLogger.d(TAG, "Attempting to post daily reminder notification")

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            SecureLogger.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping notification")
            return
        }

        try {
            SecureLogger.d(TAG, "POST_NOTIFICATIONS permission granted, creating notification")
            val openAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val contentIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, FinnFlowApp.REMINDER_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Don't forget to log today's spending")
                .setContentText("Take a moment to add today's transactions to FinnFlow.")
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            SecureLogger.d(TAG, "Daily reminder notification posted successfully, notification_id=$NOTIFICATION_ID")
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to post daily reminder notification", e)
            throw e
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
