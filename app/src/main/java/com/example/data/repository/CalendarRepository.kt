package com.example.data.repository

import com.example.data.api.CollegeScheduleFetcher
import com.example.data.local.CalendarDao
import com.example.data.local.ChecklistItem
import com.example.data.local.EventEntity
import com.example.data.local.NoteEntity
import com.example.data.sync.BackupManager
import com.example.data.sync.DeviceCalendar
import com.example.data.sync.GoogleCalendarSyncManager
import com.example.data.sync.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CalendarRepository(
    private val calendarDao: CalendarDao,
    private val collegeFetcher: CollegeScheduleFetcher,
    private val syncManager: GoogleCalendarSyncManager
) {
    private val backupManager = BackupManager(calendarDao)

    // Events
    val allEvents: Flow<List<EventEntity>> = calendarDao.getAllEvents()

    fun getEventsByDate(date: String): Flow<List<EventEntity>> = calendarDao.getEventsByDate(date)

    fun getCollegeSchedule(): Flow<List<EventEntity>> = calendarDao.getCollegeSchedule()

    suspend fun addEvent(event: EventEntity): Long = calendarDao.insertEvent(event)

    suspend fun updateEvent(event: EventEntity) = calendarDao.updateEvent(event)

    suspend fun deleteEvent(event: EventEntity) = calendarDao.deleteEvent(event)

    suspend fun deleteEventById(id: Long) = calendarDao.deleteEventById(id)

    // Notes
    val allNotes: Flow<List<NoteEntity>> = calendarDao.getAllNotes()

    fun getNotesByDate(date: String): Flow<List<NoteEntity>> = calendarDao.getNotesByDate(date)

    fun getNotesForClassSlot(classSlotId: Long): Flow<List<NoteEntity>> = calendarDao.getNotesForClassSlot(classSlotId)

    fun getNotesByDateAndPair(date: String, pairNumber: Int): Flow<List<NoteEntity>> = calendarDao.getNotesByDateAndPair(date, pairNumber)

    suspend fun addNote(note: NoteEntity): Long = calendarDao.insertNote(note)

    suspend fun updateNote(note: NoteEntity) = calendarDao.updateNote(note)

    suspend fun deleteNote(note: NoteEntity) = calendarDao.deleteNote(note)

    suspend fun togglePinNote(id: Long, isPinned: Boolean) = calendarDao.updateNotePinned(id, isPinned)

    suspend fun toggleChecklistItem(note: NoteEntity, itemId: String, isDone: Boolean) {
        val currentItems = note.getChecklistItems().map {
            if (it.id == itemId) it.copy(isDone = isDone) else it
        }
        val serialized = NoteEntity.serializeChecklist(currentItems)
        calendarDao.updateNote(note.copy(checklistJson = serialized, updatedAt = System.currentTimeMillis()))
    }

    // Offline Backup & Restore
    suspend fun exportBackupJson(): String = backupManager.exportToJson()

    suspend fun importBackupJson(json: String, replaceExisting: Boolean = false): Result<Pair<Int, Int>> =
        backupManager.importFromJson(json, replaceExisting)

    // College Schedule API Sync
    suspend fun syncCollegeSchedule(
        apiUrl: String,
        groupName: String,
        profileId: String? = null
    ): Result<Int> {
        val result = collegeFetcher.fetchSchedule(apiUrl, groupName, profileId)
        if (result.isSuccess) {
            val lessons = result.getOrNull() ?: emptyList()
            if (lessons.isNotEmpty()) {
                // Clear existing college schedule and replace with fresh lessons
                calendarDao.clearCollegeSchedule()
                val entities = lessons.map { lesson ->
                    val color = when (lesson.lessonType.lowercase()) {
                        "лекция" -> "#8B5CF6" // Violet
                        "лабораторная" -> "#06B6D4" // Cyan
                        "практика" -> "#6366F1" // Indigo
                        "зачет", "экзамен" -> "#F43F5E" // Rose
                        else -> "#10B981" // Emerald
                    }
                    EventEntity(
                        title = "${lesson.pairNumber}. ${lesson.subject}",
                        description = "Кабинет: ${lesson.room}, Преподаватель: ${lesson.teacher}",
                        date = lesson.date,
                        startTime = lesson.startTime,
                        endTime = lesson.endTime,
                        location = lesson.room,
                        type = "COLLEGE",
                        collegeSubject = lesson.subject,
                        collegeTeacher = lesson.teacher,
                        collegeRoom = lesson.room,
                        collegeLessonType = lesson.lessonType,
                        collegePairNumber = lesson.pairNumber,
                        colorHex = color
                    )
                }
                calendarDao.insertEvents(entities)
                return Result.success(entities.size)
            }
        }
        return Result.failure(result.exceptionOrNull() ?: Exception("Не удалось загрузить расписание"))
    }

    // Google Calendar Sync
    fun hasCalendarPermissions(): Boolean = syncManager.hasCalendarPermissions()

    suspend fun getAvailableGoogleCalendars(): List<DeviceCalendar> = syncManager.getAvailableCalendars()

    suspend fun importGoogleCalendarEvents(): Int {
        if (!hasCalendarPermissions()) return 0
        calendarDao.clearGoogleCalendarEvents()
        val googleEvents = syncManager.importEventsFromGoogleCalendar(daysAhead = 30)
        calendarDao.insertEvents(googleEvents)
        return googleEvents.size
    }

    suspend fun exportEventToGoogleCalendar(event: EventEntity, calendarId: Long? = null): Long? {
        val googleId = syncManager.exportEventToGoogleCalendar(event, calendarId)
        if (googleId != null) {
            calendarDao.updateEvent(event.copy(googleEventId = googleId))
        }
        return googleId
    }

    suspend fun exportCollegeScheduleToGoogle(calendarId: Long? = null): SyncResult {
        val collegeEvents = calendarDao.getCollegeSchedule().first()
        return syncManager.exportMultipleEvents(collegeEvents, calendarId)
    }

    suspend fun exportAllEventsToGoogle(calendarId: Long? = null): SyncResult {
        val events = calendarDao.getAllEvents().first()
        return syncManager.exportMultipleEvents(events, calendarId)
    }
}
