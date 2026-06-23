package com.example.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class RestTimerService : Service() {

    companion object {
        const val CHANNEL_TIMER  = "igym_rest_timer_fg"
        const val CHANNEL_DONE   = "igym_rest_done"
        const val NOTIF_TIMER    = 9001
        const val NOTIF_DONE     = 9002
        const val ACTION_START   = "igym.timer.START"
        const val ACTION_STOP    = "igym.timer.STOP"
        const val EXTRA_DURATION = "igym.timer.DURATION_SECS"
    }

    private val handler      = Handler(Looper.getMainLooper())
    private var startElapsed = 0L
    private var durationSecs = 0

    private val tick = object : Runnable {
        override fun run() {
            val elapsed   = (SystemClock.elapsedRealtime() - startElapsed) / 1000L
            val remaining = (durationSecs - elapsed).toInt().coerceAtLeast(0)
            updateNotification(remaining)
            if (remaining > 0) handler.postDelayed(this, 500L)
            else { fireDoneNotification(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
        }
    }

    override fun onCreate()  { super.onCreate(); createChannels() }
    override fun onDestroy() { handler.removeCallbacks(tick); super.onDestroy() }
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                durationSecs = intent.getIntExtra(EXTRA_DURATION, 180)
                startElapsed = SystemClock.elapsedRealtime()
                handler.removeCallbacks(tick)
                startForeground(NOTIF_TIMER, buildTimerNotif(durationSecs))
                handler.post(tick)
            }
            ACTION_STOP -> {
                handler.removeCallbacks(tick)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_TIMER) == null)
            nm.createNotificationChannel(NotificationChannel(
                CHANNEL_TIMER, "مؤقت الاستراحة", NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false); setSound(null, null) })
        if (nm.getNotificationChannel(CHANNEL_DONE) == null)
            nm.createNotificationChannel(NotificationChannel(
                CHANNEL_DONE, "انتهت الاستراحة", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setBypassDnd(true)
                vibrationPattern = longArrayOf(0, 400, 150, 400)
                enableVibration(true)
            })
    }

    private fun tapIntent() = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun stopIntent() = PendingIntent.getService(
        this, 1,
        Intent(this, RestTimerService::class.java).apply { action = ACTION_STOP },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun buildTimerNotif(remaining: Int): Notification {
        val m = remaining / 60; val s = remaining % 60
        return NotificationCompat.Builder(this, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_igym_notification)
            .setContentTitle("⏱ استراحة — %d:%02d".format(m, s))
            .setContentText("اضغط للرجوع للتمرين")
            .setContentIntent(tapIntent())
            .addAction(0, "إيقاف", stopIntent())
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setProgress(durationSecs, durationSecs - remaining, false)
            .build()
    }

    private fun updateNotification(r: Int) =
        getSystemService(NotificationManager::class.java).notify(NOTIF_TIMER, buildTimerNotif(r))

    private fun fireDoneNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.cancel(NOTIF_TIMER)
        nm.notify(NOTIF_DONE, NotificationCompat.Builder(this, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_igym_notification)
            .setContentTitle("IGYM — ابدأ المجموعة")
            .setContentText("وقت الراحة انتهى")
            .setContentIntent(tapIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVibrate(longArrayOf(0, 400, 150, 400))
            .build())
    }
}

fun Context.startRestTimer(durationSecs: Int) {
    val i = Intent(this, RestTimerService::class.java).apply {
        action = RestTimerService.ACTION_START
        putExtra(RestTimerService.EXTRA_DURATION, durationSecs)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
}

fun Context.stopRestTimer() =
    startService(Intent(this, RestTimerService::class.java).apply { action = RestTimerService.ACTION_STOP })
