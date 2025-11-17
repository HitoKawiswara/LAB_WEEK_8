package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SecondNotificationService : Service() {

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationBuilder = createAndStartForeground()
        val thread = HandlerThread("SecondNotifThread").apply { start() }
        serviceHandler = Handler(thread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val rv = super.onStartCommand(intent, flags, startId)
        val Id = intent?.getStringExtra(EXTRA_ID) ?: "002"
        serviceHandler.post {
            countDownFromFiveToZero(notificationBuilder)
            notifyCompletion(Id)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return rv
    }

    private fun createAndStartForeground(): NotificationCompat.Builder {
        val pending = getPendingIntent()
        val channelId = ensureChannel()
        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("All processes completed")
            .setContentText("Wrapping up...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        startForeground(NOTIF_ID, builder.build())
        return builder
    }

    private fun getPendingIntent(): PendingIntent {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), flags
        )
    }

    private fun ensureChannel(): String {
        val channelId = "002"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId, "002 Channel", NotificationManager.IMPORTANCE_DEFAULT
            )
            val nm = requireNotNull(
                ContextCompat.getSystemService(this, NotificationManager::class.java)
            )
            nm.createNotificationChannel(ch)
        }
        return channelId
    }

    private fun countDownFromFiveToZero(builder: NotificationCompat.Builder) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        for (i in 5 downTo 0) {
            Thread.sleep(700L)
            builder.setContentText("$i seconds to finish").setSilent(true)
            nm.notify(NOTIF_ID, builder.build())
        }
    }

    private fun notifyCompletion(id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = id
        }
    }

    companion object {
        const val NOTIF_ID = 0xBEEF
        const val EXTRA_ID = "Id2"

        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}
