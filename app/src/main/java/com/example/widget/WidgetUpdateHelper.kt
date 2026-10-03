package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WidgetUpdateHelper {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateDisplayFormat = SimpleDateFormat("EEE, d MMM", Locale.forLanguageTag("ru"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    suspend fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_college_schedule)
        val todayStr = dateFormat.format(Date())
        val dateDisplayText = dateDisplayFormat.format(Date()).replaceFirstChar { it.uppercase() }

        val prefs = context.getSharedPreferences("sigeon_prefs", Context.MODE_PRIVATE)
        val groupName = prefs.getString("selected_group_name", "ои31-09/24") ?: "ои31-09/24"
        val collegeName = prefs.getString("selected_college_name", "Planovo") ?: "Planovo"
        val coverImageUri = prefs.getString("cover_image_uri", null)

        // Custom Wallpaper / Background in Widget if set
        if (!coverImageUri.isNullOrBlank()) {
            val bitmap = withContext(Dispatchers.IO) {
                ImageStorageHelper.loadScaledBitmap(coverImageUri, maxDim = 600)
            }
            if (bitmap != null) {
                views.setViewVisibility(R.id.widget_bg_image, View.VISIBLE)
                views.setImageViewBitmap(R.id.widget_bg_image, bitmap)
                views.setViewVisibility(R.id.widget_scrim_overlay, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_bg_image, View.GONE)
                views.setViewVisibility(R.id.widget_scrim_overlay, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.widget_bg_image, View.GONE)
            views.setViewVisibility(R.id.widget_scrim_overlay, View.GONE)
        }

        // Click to open main app (College schedule tab)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_header_clickable, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_active_card, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_empty_view, openAppPendingIntent)

        // Click to refresh widget
        val refreshIntent = Intent(context, CollegeScheduleWidgetProvider::class.java).apply {
            action = CollegeScheduleWidgetProvider.ACTION_REFRESH_WIDGET
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPendingIntent)

        views.setTextViewText(R.id.widget_title, "SIGEON • $collegeName")
        views.setTextViewText(R.id.widget_subtitle, "Группа $groupName")
        views.setTextViewText(R.id.widget_date, dateDisplayText)

        // Query database on background thread
        try {
            val db = AppDatabase.getDatabase(context)
            val todayClasses = withContext(Dispatchers.IO) {
                db.calendarDao().getEventsByDate(todayStr).first()
                    .filter { it.type == "COLLEGE" }
                    .sortedBy { it.collegePairNumber }
            }

            if (todayClasses.isEmpty()) {
                // Empty state (no classes scheduled)
                views.setViewVisibility(R.id.widget_active_card, View.GONE)
                views.setViewVisibility(R.id.widget_next_pair_hint, View.GONE)
                views.setViewVisibility(R.id.widget_empty_view, View.VISIBLE)
                views.setTextViewText(R.id.widget_empty_title, "Пар на сегодня нет 🎉")
                views.setTextViewText(R.id.widget_empty_subtitle, "Нажмите чтобы открыть SIGEON")
            } else {
                views.setViewVisibility(R.id.widget_empty_view, View.GONE)
                views.setViewVisibility(R.id.widget_active_card, View.VISIBLE)

                val currentTime = timeFormat.format(Date())
                val currentMinutes = timeToMinutes(currentTime)

                // Find active or next upcoming pair
                var activeIndex = -1
                var statusText = "СЛЕДУЮЩАЯ ПАРА"

                for (i in todayClasses.indices) {
                    val lesson = todayClasses[i]
                    val startMin = timeToMinutes(lesson.startTime)
                    val endMin = timeToMinutes(lesson.endTime)

                    if (currentMinutes in startMin..endMin) {
                        activeIndex = i
                        statusText = "ИДЕТ СЕЙЧАС"
                        break
                    } else if (currentMinutes < startMin) {
                        activeIndex = i
                        statusText = "СЛЕДУЮЩАЯ ПАРА"
                        break
                    }
                }

                // If all pairs have ended for today
                if (activeIndex == -1) {
                    views.setViewVisibility(R.id.widget_active_card, View.GONE)
                    views.setViewVisibility(R.id.widget_next_pair_hint, View.GONE)
                    views.setViewVisibility(R.id.widget_empty_view, View.VISIBLE)
                    views.setTextViewText(R.id.widget_empty_title, "Все пары завершены! 🎓")
                    views.setTextViewText(R.id.widget_empty_subtitle, "Отличный день! Открыть SIGEON")
                } else {
                    val mainLesson = todayClasses[activeIndex]
                    views.setTextViewText(R.id.widget_status_badge, statusText)
                    views.setTextViewText(R.id.widget_pair_time, "${mainLesson.startTime} – ${mainLesson.endTime}")
                    
                    val subjectName = mainLesson.collegeSubject.ifBlank { mainLesson.title }
                    views.setTextViewText(
                        R.id.widget_subject,
                        "№${mainLesson.collegePairNumber}. $subjectName"
                    )

                    val details = buildString {
                        if (mainLesson.collegeRoom.isNotBlank()) append("📍 ${mainLesson.collegeRoom}  ")
                        if (mainLesson.collegeTeacher.isNotBlank()) append("👨‍🏫 ${mainLesson.collegeTeacher}  ")
                        if (mainLesson.collegeLessonType.isNotBlank()) append("(${mainLesson.collegeLessonType})")
                    }
                    views.setTextViewText(R.id.widget_details, details.ifBlank { "Пара колледжа" })

                    // Secondary preview: next pair if exists
                    if (activeIndex + 1 < todayClasses.size) {
                        val nextLesson = todayClasses[activeIndex + 1]
                        val nextSub = nextLesson.collegeSubject.ifBlank { nextLesson.title }
                        views.setViewVisibility(R.id.widget_next_pair_hint, View.VISIBLE)
                        views.setTextViewText(
                            R.id.widget_next_pair_hint,
                            "Далее: №${nextLesson.collegePairNumber} • $nextSub (${nextLesson.startTime})"
                        )
                    } else {
                        views.setViewVisibility(R.id.widget_next_pair_hint, View.VISIBLE)
                        views.setTextViewText(
                            R.id.widget_next_pair_hint,
                            "Это последняя пара на сегодня ✨"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun timeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.trim().split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            0
        }
    }

    suspend fun updateAllWidgets(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CollegeScheduleWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in appWidgetIds) {
                updateWidget(context, appWidgetManager, id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
