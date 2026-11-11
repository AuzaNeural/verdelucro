package com.auza.giguclone

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class GigUCloneApplication : Application() {

    companion object {
        const val CHANNEL_ID = "gigu_overlay_channel"
        lateinit var instance: GigUCloneApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_overlay_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_overlay_desc)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}

