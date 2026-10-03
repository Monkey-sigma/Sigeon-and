package com.example.data.api

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

object IcsCalendarParser {

    private val teacherPattern = Pattern.compile("(?i)Преподаватель:\\s*([^\\\\\\n]+)")
    private val groupPattern = Pattern.compile("(?i)Группа:\\s*([^\\\\\\n]+)")
    private val subgroupPattern = Pattern.compile("(?i)Подгруппа:\\s*([^\\\\\\n]+)")

    /**
     * Parses iCalendar (.ics) string from Planovo or other college schedule systems into CollegeLesson list.
     */
    fun parseIcs(icsString: String): List<CollegeLesson> {
        val unfoldedLines = unfoldLines(icsString)
        val lessons = mutableListOf<CollegeLesson>()

        var inEvent = false
        var uid = ""
        var summary = ""
        var location = ""
        var description = ""
        var dtStartRaw = ""
        var dtEndRaw = ""

        for (line in unfoldedLines) {
            val upper = line.uppercase()
            if (upper.startsWith("BEGIN:VEVENT")) {
                inEvent = true
                uid = ""
                summary = ""
                location = ""
                description = ""
                dtStartRaw = ""
                dtEndRaw = ""
                continue
            }

            if (upper.startsWith("END:VEVENT")) {
                if (inEvent && summary.isNotBlank() && dtStartRaw.isNotBlank()) {
                    val lesson = buildLessonFromEvent(
                        uid = uid,
                        summary = unescapeIcsText(summary),
                        location = unescapeIcsText(location),
                        description = unescapeIcsText(description),
                        dtStartRaw = dtStartRaw,
                        dtEndRaw = dtEndRaw
                    )
                    if (lesson != null) {
                        lessons.add(lesson)
                    }
                }
                inEvent = false
                continue
            }

            if (!inEvent) continue

            val colonIdx = line.indexOf(':')
            if (colonIdx == -1) continue

            val keyPart = line.substring(0, colonIdx)
            val valuePart = line.substring(colonIdx + 1)
            val keyUpper = keyPart.uppercase()

            when {
                keyUpper == "UID" -> uid = valuePart
                keyUpper == "SUMMARY" || keyUpper.startsWith("SUMMARY;") -> summary = valuePart
                keyUpper == "LOCATION" || keyUpper.startsWith("LOCATION;") -> location = valuePart
                keyUpper == "DESCRIPTION" || keyUpper.startsWith("DESCRIPTION;") -> description = valuePart
                keyUpper == "DTSTART" || keyUpper.startsWith("DTSTART;") -> dtStartRaw = valuePart
                keyUpper == "DTEND" || keyUpper.startsWith("DTEND;") -> dtEndRaw = valuePart
            }
        }

        // Sort lessons by date and start time, then assign accurate chronological pair numbers per day
        return sortAndAssignPairNumbers(lessons)
    }

    private fun buildLessonFromEvent(
        uid: String,
        summary: String,
        location: String,
        description: String,
        dtStartRaw: String,
        dtEndRaw: String
    ): CollegeLesson? {
        val startDateTime = parseIcsDateTime(dtStartRaw) ?: return null
        val endDateTime = parseIcsDateTime(dtEndRaw)

        val date = startDateTime.first // YYYY-MM-DD
        val startTime = startDateTime.second // HH:mm
        val endTime = endDateTime?.second ?: calculateEndTime(startTime)

        // Extract Teacher from description if present
        var teacher = ""
        val teacherMatcher = teacherPattern.matcher(description)
        if (teacherMatcher.find()) {
            teacher = teacherMatcher.group(1)?.trim() ?: ""
        }

        // Clean room / location
        var room = location.trim()
        if (room.startsWith("Кабинет ", ignoreCase = true)) {
            room = "Каб. " + room.substring(8).trim()
        }

        // Detect Lesson Type
        val lessonType = detectLessonType(summary, description)

        // Estimated pair number based on start time
        val estimatedPair = timeToPairNumber(startTime)

        return CollegeLesson(
            id = uid,
            subject = summary.trim(),
            teacher = teacher,
            room = room,
            lessonType = lessonType,
            pairNumber = estimatedPair,
            startTime = startTime,
            endTime = endTime,
            date = date,
            note = description.trim()
        )
    }

    private fun detectLessonType(summary: String, description: String): String {
        val text = "$summary $description".lowercase(Locale.getDefault())
        return when {
            text.contains("лаб") || text.contains("лабораторн") -> "Лабораторная"
            text.contains("лек") || text.contains("лекция") -> "Лекция"
            text.contains("прак") || text.contains("практическ") -> "Практика"
            text.contains("мдк") -> "МДК"
            text.contains("зачет") || text.contains("зачёт") -> "Зачет"
            text.contains("экзамен") -> "Экзамен"
            text.contains("семинар") -> "Семинар"
            text.contains("консультац") -> "Консультация"
            else -> "Пара"
        }
    }

    private fun timeToPairNumber(startTime: String): Int {
        val minutes = try {
            val parts = startTime.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (_: Exception) {
            510 // 08:30
        }

        return when {
            minutes < 570 -> 1 // Before 09:30 (e.g. 08:30)
            minutes < 680 -> 2 // 09:30 - 11:20 (e.g. 10:00, 10:15)
            minutes < 790 -> 3 // 11:20 - 13:10 (e.g. 11:40, 12:15)
            minutes < 900 -> 4 // 13:10 - 15:00 (e.g. 13:50, 14:00)
            minutes < 1010 -> 5 // 15:00 - 16:50 (e.g. 15:30)
            minutes < 1120 -> 6 // 16:50 - 18:40 (e.g. 17:10)
            else -> 7
        }
    }

    private fun calculateEndTime(startTime: String): String {
        return try {
            val parts = startTime.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            val totalM = h * 60 + m + 90 // +1.5h
            String.format(Locale.US, "%02d:%02d", (totalM / 60) % 24, totalM % 60)
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Parses ICS Date/Time string: e.g. 20260901T100000 or 20260901T100000Z or 20260901
     * Returns Pair(Date: YYYY-MM-DD, Time: HH:mm)
     */
    private fun parseIcsDateTime(raw: String): Pair<String, String>? {
        val clean = raw.trim().replace("Z", "")
        if (clean.length < 8) return null

        val year = clean.substring(0, 4)
        val month = clean.substring(4, 6)
        val day = clean.substring(6, 8)
        val dateStr = "$year-$month-$day"

        val timeStr = if (clean.length >= 13 && clean.contains("T")) {
            val tIdx = clean.indexOf('T')
            if (clean.length >= tIdx + 5) {
                val hour = clean.substring(tIdx + 1, tIdx + 3)
                val min = clean.substring(tIdx + 3, tIdx + 5)
                "$hour:$min"
            } else "08:30"
        } else {
            "08:30"
        }

        return Pair(dateStr, timeStr)
    }

    private fun unfoldLines(rawIcs: String): List<String> {
        val lines = rawIcs.lines()
        val result = mutableListOf<String>()
        for (line in lines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                if (result.isNotEmpty()) {
                    val last = result.removeAt(result.size - 1)
                    result.add(last + line.substring(1))
                }
            } else if (line.isNotBlank()) {
                result.add(line.trimEnd('\r'))
            }
        }
        return result
    }

    private fun unescapeIcsText(text: String): String {
        return text
            .replace("\\n", "\n")
            .replace("\\N", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
    }

    private fun sortAndAssignPairNumbers(lessons: List<CollegeLesson>): List<CollegeLesson> {
        val groupedByDate = lessons.groupBy { it.date }
        val result = mutableListOf<CollegeLesson>()

        groupedByDate.keys.sorted().forEach { date ->
            val dayLessons = groupedByDate[date]!!.sortedBy { it.startTime }
            dayLessons.forEachIndexed { index, lesson ->
                result.add(lesson.copy(pairNumber = index + 1))
            }
        }

        return result
    }
}
