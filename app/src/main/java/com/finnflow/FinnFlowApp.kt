package com.finnflow

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.getSystemService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FinnFlowApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createReminderNotificationChannel()
    }

    private fun createReminderNotificationChannel() {
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            "Daily reminder",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminds you to log today's spending"
        }
        getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "daily_reminder_channel"
    }
}
