package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class ClassNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val lessonId = intent.getLongExtra(EXTRA_LESSON_ID, 0L)
        val subject = intent.getStringExtra(EXTRA_SUBJECT) ?: "Пара"
        val pairNumber = intent.getIntExtra(EXTRA_PAIR_NUMBER, 0)
        val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
        val room = intent.getStringExtra(EXTRA_ROOM) ?: ""
        val teacher = intent.getStringExtra(EXTRA_TEACHER) ?: ""
        val lessonType = intent.getStringExtra(EXTRA_LESSON_TYPE) ?: ""

        createNotificationChannel(context)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            lessonId.toInt(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pairPrefix = if (pairNumber > 0) "№$pairNumber " else ""
        val typeSuffix = if (lessonType.isNotBlank()) " ($lessonType)" else ""
        val titleText = "🔔 Через 15 мин пара: $pairPrefix$subject"
        val bodyText = buildString {
            if (startTime.isNotBlank()) append("Начало в $startTime")
            if (room.isNotBlank()) append(" • Каб. $room")
            if (teacher.isNotBlank()) append(" • Преподаватель: $teacher")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 350, 200, 350))

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(lessonId.toInt(), builder.build())
    }

    companion object {
        const val CHANNEL_ID = "college_classes_channel"
        const val CHANNEL_NAME = "Напоминания о парах"
        const val ACTION_CLASS_ALERT = "com.example.util.ACTION_CLASS_ALERT_NOTIFICATION"

        const val EXTRA_LESSON_ID = "extra_lesson_id"
        const val EXTRA_SUBJECT = "extra_subject"
        const val EXTRA_PAIR_NUMBER = "extra_pair_number"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_ROOM = "extra_room"
        const val EXTRA_TEACHER = "extra_teacher"
        const val EXTRA_LESSON_TYPE = "extra_lesson_type"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = CHANNEL_NAME
                val descriptionText = "Уведомления за 15 минут до начала учебных занятий"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
