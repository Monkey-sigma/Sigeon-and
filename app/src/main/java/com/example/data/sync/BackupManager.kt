package com.example.data.sync

import com.example.data.local.CalendarDao
import com.example.data.local.EventEntity
import com.example.data.local.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BackupManager(private val dao: CalendarDao) {

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("appName", "SIGEON Calendar")

        // Events
        val events = dao.getAllEvents().first()
        val eventsArray = JSONArray()
        for (e in events) {
            val obj = JSONObject().apply {
                put("title", e.title)
                put("description", e.description)
                put("date", e.date)
                put("startTime", e.startTime)
                put("endTime", e.endTime)
                put("location", e.location)
                put("type", e.type)
                put("collegeSubject", e.collegeSubject)
                put("collegeTeacher", e.collegeTeacher)
                put("collegeRoom", e.collegeRoom)
                put("collegeLessonType", e.collegeLessonType)
                put("collegePairNumber", e.collegePairNumber)
                put("colorHex", e.colorHex)
                put("isCompleted", e.isCompleted)
            }
            eventsArray.put(obj)
        }
        root.put("events", eventsArray)

        // Notes
        val notes = dao.getAllNotes().first()
        val notesArray = JSONArray()
        for (n in notes) {
            val obj = JSONObject().apply {
                put("title", n.title)
                put("content", n.content)
                put("date", n.date ?: JSONObject.NULL)
                put("tag", n.tag)
                put("colorHex", n.colorHex)
                put("isPinned", n.isPinned)
                put("checklistJson", n.checklistJson)
            }
            notesArray.put(obj)
        }
        root.put("notes", notesArray)

        root.toString(2)
    }

    suspend fun importFromJson(jsonString: String, replaceExisting: Boolean = false): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (replaceExisting) {
                dao.clearCollegeSchedule()
                dao.clearGoogleCalendarEvents()
            }

            val eventsArray = root.optJSONArray("events")
            val importedEvents = mutableListOf<EventEntity>()
            if (eventsArray != null) {
                for (i in 0 until eventsArray.length()) {
                    val obj = eventsArray.getJSONObject(i)
                    importedEvents.add(
                        EventEntity(
                            title = obj.optString("title", ""),
                            description = obj.optString("description", ""),
                            date = obj.optString("date", ""),
                            startTime = obj.optString("startTime", ""),
                            endTime = obj.optString("endTime", ""),
                            location = obj.optString("location", ""),
                            type = obj.optString("type", "EVENT"),
                            collegeSubject = obj.optString("collegeSubject", ""),
                            collegeTeacher = obj.optString("collegeTeacher", ""),
                            collegeRoom = obj.optString("collegeRoom", ""),
                            collegeLessonType = obj.optString("collegeLessonType", ""),
                            collegePairNumber = obj.optInt("collegePairNumber", 0),
                            colorHex = obj.optString("colorHex", "#6366F1"),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                    )
                }
            }
            if (importedEvents.isNotEmpty()) {
                dao.insertEvents(importedEvents)
            }

            val notesArray = root.optJSONArray("notes")
            var importedNotesCount = 0
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    val obj = notesArray.getJSONObject(i)
                    val note = NoteEntity(
                        title = obj.optString("title", ""),
                        content = obj.optString("content", ""),
                        date = if (obj.isNull("date")) null else obj.optString("date", null),
                        tag = obj.optString("tag", "Учеба"),
                        colorHex = obj.optString("colorHex", "#6366F1"),
                        isPinned = obj.optBoolean("isPinned", false),
                        checklistJson = obj.optString("checklistJson", "")
                    )
                    dao.insertNote(note)
                    importedNotesCount++
                }
            }

            Result.success(Pair(importedEvents.size, importedNotesCount))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
