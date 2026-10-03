package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.EventEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ClassNotificationScheduler {

    private const val PREFS_NAME = "sigeon_prefs"
    private const val PREF_NOTIF_ENABLED = "class_notifications_enabled"

    fun isNotificationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_NOTIF_ENABLED, true)
    }

    fun setNotificationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_NOTIF_ENABLED, enabled).apply()
    }

    /**
     * Schedules 15-minute advance local alarms for all upcoming college classes.
     */
    fun scheduleAllUpcomingClassAlerts(context: Context, collegeLessons: List<EventEntity>) {
        if (!isNotificationEnabled(context)) {
            cancelAllClassAlerts(context, collegeLessons)
            return
        }

        ClassNotificationReceiver.createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()

        for (lesson in collegeLessons) {
            if (lesson.type != "COLLEGE" && lesson.collegePairNumber == 0) continue

            val alertMillis = calculate15MinAlertTime(lesson.date, lesson.startTime)
            if (alertMillis > now) {
                val intent = Intent(context, ClassNotificationReceiver::class.java).apply {
                    action = ClassNotificationReceiver.ACTION_CLASS_ALERT
                    putExtra(ClassNotificationReceiver.EXTRA_LESSON_ID, lesson.id)
                    putExtra(ClassNotificationReceiver.EXTRA_SUBJECT, lesson.collegeSubject.ifBlank { lesson.title })
                    putExtra(ClassNotificationReceiver.EXTRA_PAIR_NUMBER, lesson.collegePairNumber)
                    putExtra(ClassNotificationReceiver.EXTRA_START_TIME, lesson.startTime)
                    putExtra(ClassNotificationReceiver.EXTRA_ROOM, lesson.collegeRoom)
                    putExtra(ClassNotificationReceiver.EXTRA_TEACHER, lesson.collegeTeacher)
                    putExtra(ClassNotificationReceiver.EXTRA_LESSON_TYPE, lesson.collegeLessonType)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    lesson.id.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            alertMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            alertMillis,
                            pendingIntent
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Cancels all scheduled class notification alarms.
     */
    fun cancelAllClassAlerts(context: Context, collegeLessons: List<EventEntity>) {
        val alarmManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? AlarmManager ?: return
        for (lesson in collegeLessons) {
            val intent = Intent(context, ClassNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                lesson.id.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    /**
     * Immediately triggers a test notification so the user can verify local alerts.
     */
    fun sendTestNotification(context: Context) {
        ClassNotificationReceiver.createNotificationChannel(context)
        val testIntent = Intent(context, ClassNotificationReceiver::class.java).apply {
            action = ClassNotificationReceiver.ACTION_CLASS_ALERT
            putExtra(ClassNotificationReceiver.EXTRA_LESSON_ID, 999999L)
            putExtra(ClassNotificationReceiver.EXTRA_SUBJECT, "Тестовая пара (Высшая математика)")
            putExtra(ClassNotificationReceiver.EXTRA_PAIR_NUMBER, 1)
            putExtra(ClassNotificationReceiver.EXTRA_START_TIME, "08:30")
            putExtra(ClassNotificationReceiver.EXTRA_ROOM, "305А")
            putExtra(ClassNotificationReceiver.EXTRA_TEACHER, "Иванов И.И.")
            putExtra(ClassNotificationReceiver.EXTRA_LESSON_TYPE, "Лекция")
        }
        context.sendBroadcast(testIntent)
    }

    private fun calculate15MinAlertTime(dateStr: String, startTimeStr: String): Long {
        return try {
            val cal = Calendar.getInstance()
            val dateParts = dateStr.split("-")
            val year = dateParts[0].toInt()
            val month = dateParts[1].toInt() - 1
            val day = dateParts[2].toInt()

            var hour = 8
            var minute = 0
            if (startTimeStr.contains(":")) {
                val timeParts = startTimeStr.split(":")
                hour = timeParts[0].trim().toIntOrNull() ?: 8
                minute = timeParts[1].trim().toIntOrNull() ?: 0
            }

            cal.set(year, month, day, hour, minute, 0)
            cal.set(Calendar.MILLISECOND, 0)

            // Subtract 15 minutes
            cal.timeInMillis - (15 * 60 * 1000)
        } catch (e: Exception) {
            0L
        }
    }
}
