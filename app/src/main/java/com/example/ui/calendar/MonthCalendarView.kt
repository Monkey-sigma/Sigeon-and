package com.example.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.EventEntity
import com.example.data.local.NoteEntity
import com.example.ui.CalendarMonth
import com.example.ui.theme.SigeonPrimary
import com.example.ui.theme.SigeonPrimaryDark
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun MonthCalendarView(
    currentMonth: CalendarMonth,
    selectedDate: String,
    events: List<EventEntity>,
    notes: List<NoteEntity>,
    onDateSelected: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthNames = remember {
        listOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        )
    }

    val daysOfWeek = remember { listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс") }

    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
    }

    // Prepare calendar grid days
    val calendarDays = remember(currentMonth.year, currentMonth.month) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentMonth.year)
            set(Calendar.MONTH, currentMonth.month)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        // Get 1-based day of week where Monday is 1, Sunday is 7
        val firstDayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }

        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Previous month filler days
        val prevCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val maxPrevDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val days = mutableListOf<CalendarDayInfo>()
        val leadingDaysCount = firstDayOfWeek - 1

        for (i in (maxPrevDays - leadingDaysCount + 1)..maxPrevDays) {
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", prevCal.get(Calendar.YEAR), prevCal.get(Calendar.MONTH) + 1, i)
            days.add(CalendarDayInfo(dayNumber = i, dateString = dateStr, isCurrentMonth = false))
        }

        for (i in 1..maxDays) {
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", currentMonth.year, currentMonth.month + 1, i)
            days.add(CalendarDayInfo(dayNumber = i, dateString = dateStr, isCurrentMonth = true))
        }

        // Trailing days to fill 35 or 42 grid items
        val totalCells = if (days.size > 35) 42 else 35
        val nextCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        var nextDayNum = 1
        while (days.size < totalCells) {
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", nextCal.get(Calendar.YEAR), nextCal.get(Calendar.MONTH) + 1, nextDayNum)
            days.add(CalendarDayInfo(dayNumber = nextDayNum, dateString = dateStr, isCurrentMonth = false))
            nextDayNum++
        }

        days
    }

    // Map of dates to indicator dot counts/types
    val eventsByDate = remember(events) { events.groupBy { it.date } }
    val notesByDate = remember(notes) { notes.filter { it.date != null }.groupBy { it.date!! } }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Month, Year, and Nav buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${monthNames[currentMonth.month]} ${currentMonth.year}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onJumpToToday,
                        modifier = Modifier.testTag("jump_today_button")
                    ) {
                        Text(
                            text = "Сегодня",
                            color = SigeonPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier.size(36.dp).testTag("prev_month_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Предыдущий месяц",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier.size(36.dp).testTag("next_month_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Следующий месяц",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Days of week header (Пн, Вт, Ср...)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                daysOfWeek.forEachIndexed { index, day ->
                    val isWeekend = index >= 5
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isWeekend) Color(0xFFF43F5E) else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar grid
            val rows = calendarDays.chunked(7)
            rows.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    week.forEach { dayInfo ->
                        val isSelected = dayInfo.dateString == selectedDate
                        val isToday = dayInfo.dateString == todayStr
                        val hasCollegeClasses = eventsByDate[dayInfo.dateString]?.any { it.type == "COLLEGE" } == true
                        val hasEvents = eventsByDate[dayInfo.dateString]?.any { it.type == "EVENT" || it.type == "GOOGLE" } == true
                        val hasNotes = notesByDate.containsKey(dayInfo.dateString)

                        CalendarDayCell(
                            dayInfo = dayInfo,
                            isSelected = isSelected,
                            isToday = isToday,
                            hasCollegeClasses = hasCollegeClasses,
                            hasEvents = hasEvents,
                            hasNotes = hasNotes,
                            onClick = { onDateSelected(dayInfo.dateString) }
                        )
                    }
                }
            }
        }
    }
}

data class CalendarDayInfo(
    val dayNumber: Int,
    val dateString: String,
    val isCurrentMonth: Boolean
)

@Composable
fun CalendarDayCell(
    dayInfo: CalendarDayInfo,
    isSelected: Boolean,
    isToday: Boolean,
    hasCollegeClasses: Boolean,
    hasEvents: Boolean,
    hasNotes: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> SigeonPrimary
        isToday -> SigeonPrimary.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> Color.White
        isToday -> SigeonPrimaryDark
        !dayInfo.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .testTag("calendar_day_${dayInfo.dateString}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${dayInfo.dayNumber}",
                fontSize = 13.sp,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center
            )

            // Activity indicators dots
            if (hasCollegeClasses || hasEvents || hasNotes) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    if (hasCollegeClasses) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else Color(0xFF8B5CF6))
                        )
                    }
                    if (hasEvents) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else Color(0xFF06B6D4))
                        )
                    }
                    if (hasNotes) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else Color(0xFF10B981))
                        )
                    }
                }
            }
        }
    }
}
