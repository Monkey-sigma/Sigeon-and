package com.example

import com.example.data.api.IcsCalendarParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IcsParserTest {

    @Test
    fun `parse Planovo ICS format with folded lines and description`() {
        val rawIcs = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Planovo KEMS//Schedule subscription//RU
            X-WR-CALNAME:Расписание ои31-09/24
            BEGIN:VEVENT
            UID:lesson-d520a862-0729-54a1-9b2e-d5b4ffd057b4-20260901@schedule.planovo.pro
            DTSTAMP:20261002T174711Z
            SUMMARY:МДК 01.02 Поддержка и тестирование про
             граммных модулей
            DTSTART;TZID=Europe/Moscow:20260901T100000
            DTEND;TZID=Europe/Moscow:20260901T113000
            DESCRIPTION:Группа: ои21-11/25\, ои31-09/24\nПреподаватель: Сур
             кова Алла Викторовна\nПериодичность: по учебному календарю
            LOCATION:Кабинет 31
            CATEGORIES:Занятие
            END:VEVENT
            BEGIN:VEVENT
            UID:lesson-11b88bf7-07ab-5cf1-96d2-fb263e2d85a7-20260901@schedule.planovo.pro
            SUMMARY:Иностранный язык в профессиональной
              деятельности\, Подгруппа 1
            DTSTART;TZID=Europe/Moscow:20260901T114000
            DTEND;TZID=Europe/Moscow:20260901T131000
            DESCRIPTION:Преподаватель: Павлова Н.Ю.
            LOCATION:Кабинет 12
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val lessons = IcsCalendarParser.parseIcs(rawIcs)
        assertEquals(2, lessons.size)

        val first = lessons[0]
        assertEquals("МДК 01.02 Поддержка и тестирование программных модулей", first.subject)
        assertEquals("2026-09-01", first.date)
        assertEquals("10:00", first.startTime)
        assertEquals("11:30", first.endTime)
        assertEquals("Каб. 31", first.room)
        assertEquals("Суркова Алла Викторовна", first.teacher)
        assertEquals("МДК", first.lessonType)
        assertEquals(1, first.pairNumber)

        val second = lessons[1]
        assertTrue(second.subject.contains("Иностранный язык"))
        assertEquals("11:40", second.startTime)
        assertEquals("Павлова Н.Ю.", second.teacher)
        assertEquals("Каб. 12", second.room)
        assertEquals(2, second.pairNumber)
    }
}
