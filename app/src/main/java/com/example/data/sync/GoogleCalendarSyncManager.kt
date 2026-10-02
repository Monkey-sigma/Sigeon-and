package com.example.data.sync

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.data.local.EventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DeviceCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val isGoogle: Boolean,
    val isPrimary: Boolean
)

data class SyncResult(
    val success: Boolean,
    val exportedCount: Int = 0,
    val importedCount: Int = 0,
    val message: String = ""
)

class GoogleCalendarSyncManager(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun hasCalendarPermissions(): Boolean {
        val readPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
        val writePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
        return readPerm == PackageManager.PERMISSION_GRANTED && writePerm == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Lists available calendars on device, prioritizing Google calendars
     */
    suspend fun getAvailableCalendars(): List<DeviceCalendar> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions()) return@withContext emptyList()

        val list = mutableListOf<DeviceCalendar>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY
        )

        try {
            val cursor: Cursor? = contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                "${CalendarContract.Calendars.IS_PRIMARY} DESC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameCol = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountCol = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val typeCol = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
                val primaryCol = it.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val name = it.getString(nameCol) ?: "Calendar $id"
                    val account = it.getString(accountCol) ?: ""
                    val type = it.getString(typeCol) ?: ""
                    val isPrimary = if (primaryCol >= 0) it.getInt(primaryCol) == 1 else false
                    val isGoogle = type.contains("google", ignoreCase = true) || account.contains("@gmail.com")

                    list.add(
                        DeviceCalendar(
                            id = id,
                            displayName = name,
                            accountName = account,
                            isGoogle = isGoogle,
                            isPrimary = isPrimary
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Put Google calendars first
        list.sortedWith(compareByDescending<DeviceCalendar> { it.isGoogle }.thenByDescending { it.isPrimary })
    }

    /**
     * Exports a SIGEON event or college class to Google Calendar
     */
    suspend fun exportEventToGoogleCalendar(
        event: EventEntity,
        targetCalendarId: Long? = null
    ): Long? = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions()) return@withContext null

        try {
            val calendarId = targetCalendarId ?: getPrimaryGoogleCalendarId() ?: return@withContext null

            val startMillis = parseDateTimeToMillis(event.date, event.startTime)
            val endMillis = if (event.endTime.isNotBlank()) {
                parseDateTimeToMillis(event.date, event.endTime)
            } else {
                startMillis + (90 * 60 * 1000) // Default 1.5h pair length
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, event.title)
                put(
                    CalendarContract.Events.DESCRIPTION,
                    buildString {
                        if (event.collegeSubject.isNotBlank()) append("Дисциплина: ${event.collegeSubject}\n")
                        if (event.collegeTeacher.isNotBlank()) append("Преподаватель: ${event.collegeTeacher}\n")
                        if (event.collegeLessonType.isNotBlank()) append("Тип: ${event.collegeLessonType}\n")
                        if (event.collegePairNumber > 0) append("Пара: №${event.collegePairNumber}\n")
                        if (event.description.isNotBlank()) append(event.description)
                        append("\n(Синхронизировано из SIGEON Calendar)")
                    }
                )
                put(CalendarContract.Events.EVENT_LOCATION, event.collegeRoom.ifBlank { event.location })
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            val uri: Uri? = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Bulk exports multiple events (e.g. college schedule for current semester/month) into Google Calendar
     */
    suspend fun exportMultipleEvents(
        events: List<EventEntity>,
        targetCalendarId: Long? = null
    ): SyncResult = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions()) {
            return@withContext SyncResult(false, 0, 0, "Нет разрешения на запись в календарь")
        }

        var successCount = 0
        val calendarId = targetCalendarId ?: getPrimaryGoogleCalendarId()
        if (calendarId == null) {
            return@withContext SyncResult(false, 0, 0, "Не найден Google Календарь на устройстве")
        }

        for (event in events) {
            val newId = exportEventToGoogleCalendar(event, calendarId)
            if (newId != null) {
                successCount++
            }
        }

        SyncResult(
            success = successCount > 0,
            exportedCount = successCount,
            message = "Успешно экспортировано в Google Календарь: $successCount занятий/событий"
        )
    }

    /**
     * Imports events from device's Google Calendar into SIGEON
     */
    suspend fun importEventsFromGoogleCalendar(
        daysAhead: Int = 30
    ): List<EventEntity> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions()) return@withContext emptyList()

        val events = mutableListOf<EventEntity>()
        val startCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        val endCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, daysAhead) }

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.CALENDAR_ID
        )

        val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?)"
        val selectionArgs = arrayOf(startCal.timeInMillis.toString(), endCal.timeInMillis.toString())

        try {
            val cursor = contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val titleCol = it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val descCol = it.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
                val startCol = it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val endCol = it.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                val locCol = it.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)

                while (it.moveToNext()) {
                    val googleId = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Событие Google"
                    val desc = it.getString(descCol) ?: ""
                    val startMillis = it.getLong(startCol)
                    val endMillis = it.getLong(endCol)
                    val location = it.getString(locCol) ?: ""

                    // Don't re-import events that came from SIGEON to avoid duplicates
                    if (desc.contains("SIGEON Calendar")) continue

                    val startDate = Date(startMillis)
                    val dateStr = dateFormat.format(startDate)
                    val startTimeStr = timeFormat.format(startDate)
                    val endTimeStr = if (endMillis > startMillis) timeFormat.format(Date(endMillis)) else ""

                    events.add(
                        EventEntity(
                            title = title,
                            description = desc,
                            date = dateStr,
                            startTime = startTimeStr,
                            endTime = endTimeStr,
                            location = location,
                            type = "GOOGLE",
                            googleEventId = googleId,
                            colorHex = "#4285F4" // Google Blue
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        events
    }

    private suspend fun getPrimaryGoogleCalendarId(): Long? {
        val calendars = getAvailableCalendars()
        return calendars.firstOrNull { it.isGoogle }?.id ?: calendars.firstOrNull()?.id
    }

    private fun parseDateTimeToMillis(dateStr: String, timeStr: String): Long {
        val cal = Calendar.getInstance()
        try {
            val dateParts = dateStr.split("-")
            val year = dateParts[0].toInt()
            val month = dateParts[1].toInt() - 1
            val day = dateParts[2].toInt()

            var hour = 9
            var minute = 0
            if (timeStr.contains(":")) {
                val timeParts = timeStr.split(":")
                hour = timeParts[0].trim().toIntOrNull() ?: 9
                minute = timeParts[1].trim().toIntOrNull() ?: 0
            }

            cal.set(year, month, day, hour, minute, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        } catch (e: Exception) {
            return cal.timeInMillis
        }
    }
}
