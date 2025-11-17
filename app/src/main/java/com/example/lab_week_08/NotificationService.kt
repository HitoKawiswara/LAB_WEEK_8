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

class NotificationService : Service() {

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        // start foreground + siapkan builder
        notificationBuilder = createAndStartForeground()

        // thread terpisah buat kerjaan background
        val handlerThread = HandlerThread("SecondThread").apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    /**
     * Callback lifecycle saat service di-start (setelah startForeground() dipanggil).
     * Di sini kita jalankan countdown di thread worker lalu matikan service.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)

        // Ambil channel ID yang (katanya) dikirim dari Activity
        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        // Eksekusi kerjaan di thread terpisah biar UI gak ke-freeze
        serviceHandler.post {
            // update notifikasi: hitung mundur 10..0
            countDownFromTenToZero(notificationBuilder)
            // kabari UI via LiveData
            notifyCompletion(Id)
            // tutup notification foreground dan hentikan service
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return returnValue
    }

    /** Buat channel, builder, dan startForeground. */
    private fun createAndStartForeground(): NotificationCompat.Builder {
        val pendingIntent = getPendingIntent()
        val channelId = ensureNotificationChannel()
        val builder = buildNotification(pendingIntent, channelId)
        startForeground(NOTIFICATION_ID, builder.build())
        return builder
    }

    /** PendingIntent ke MainActivity saat notifikasi di-tap. */
    private fun getPendingIntent(): PendingIntent {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), flags
        )
    }

    /** Pastikan channel ada untuk API 26+. */
    private fun ensureNotificationChannel(): String {
        val channelId = "001"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "001 Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)
            val nm = requireNotNull(
                ContextCompat.getSystemService(this, NotificationManager::class.java)
            )
            nm.createNotificationChannel(channel)
        }
        return channelId
    }

    /** Build NotificationCompat.Builder. */
    private fun buildNotification(
        pendingIntent: PendingIntent,
        channelId: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Second worker process is done")
            .setContentText("Check it out!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Second worker process is done, check it out!")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    // ================== Tambahan yang kamu minta ==================

    // Update notifikasi: countdown 10..0
    private fun countDownFromTenToZero(notificationBuilder: NotificationCompat.Builder) {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        for (i in 10 downTo 0) {
            Thread.sleep(1000L)
            notificationBuilder
                .setContentText("$i seconds until last warning")
                .setSilent(true)
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    // Update LiveData di main thread setelah countdown selesai
    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
        }
    }

    // =============================================================

    companion object {
        const val NOTIFICATION_ID = 0xCA7
        const val EXTRA_ID = "Id"

        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}
