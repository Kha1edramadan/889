package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.MainActivity
import com.example.R
import java.util.concurrent.TimeUnit

class SittingReminderWorker(
    private val ctx: Context,
    params: WorkerParameters
) : Worker(ctx, params) {

    companion object {
        const val CHANNEL_ID = "igym_sitting_reminder"
        const val WORK_TAG   = "igym_movement_reminder"

        // رسائل الحركة — مباشرة وبدون تصنع
        private val MOVEMENT_MESSAGES = listOf(
            "قاعد أكتر من ساعة؟ قوم امشي دورة",
            "جسمك مش اتصمم للقعدة الطويلة — هاته وقوف",
            "ظهرك بيقولك حاجة — قوم وافرده",
            "خمس دقايق وقوف أحسن من ساعة ندم",
            "خد نفس بعيد عن الشاشة — دقيقتين بتكفي",
            "الجلوس أكتر من 90 دقيقة بيضر بالدورة الدموية — وقفة بسيطة تكفي",
            "قوم دور على كوباية مياه — لجسمك ولظهرك",
            "التوتر بيتخزن في الجسم — حركة بسيطة تطلعه",
            "مشيت النهارده؟ حتى 10 دقايق تعمل فرق",
            "عيونك شايلة شاشات من ساعات؟ دقيقتين بعيد تكفي",
            "لو قلقان — امشي. اللي بيتحرك بيفكر أحسن",
            "الجسم اللي بيتحرك كتير أقل تعباً في الليل",
            "ضغطة بطن صغيرة وانت واقف — ده تمرين كمان",
            "استنّ لما تفرغ — بس مش لما تتوجع"
        )

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SittingReminderWorker>(
                30, TimeUnit.MINUTES,
                5,  TimeUnit.MINUTES
            )
                .addTag(WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
        }

        fun isScheduled(context: Context): Boolean {
            val infos = WorkManager.getInstance(context)
                .getWorkInfosByTag(WORK_TAG).get()
            return infos.any {
                it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
            }
        }
    }

    override fun doWork(): Result {
        createChannel()

        val msg = MOVEMENT_MESSAGES.random()

        val tapIntent = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_igym_notification)      // أيقونة التطبيق
            .setContentTitle("IGYM")
            .setContentText(msg)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .build()

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify((System.currentTimeMillis() % 100).toInt() + 7100, notif)
        return Result.success()
    }

    private fun createChannel() {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "تذكيرات الحركة",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "تذكير بالحركة كل نص ساعة"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 100, 200)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        nm.createNotificationChannel(channel)
    }
}
