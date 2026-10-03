package com.example.ui.college

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.EventEntity
import com.example.ui.theme.SigeonPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DayAcademicStats(
    val dayName: String,
    val dateString: String,
    val formattedDateNum: String,
    val lectureHours: Float,
    val labHours: Float,
    val totalHours: Float
)

data class WeeklyAcademicSummary(
    val startDateStr: String,
    val endDateStr: String,
    val rangeFormatted: String,
    val totalLectureHours: Float,
    val totalLabHours: Float,
    val totalHours: Float,
    val dayStats: List<DayAcademicStats>,
    val busiestDayName: String
)

@Composable
fun WeeklyAcademicDashboardDialog(
    allEvents: List<EventEntity>,
    onDismiss: () -> Unit
) {
    var weekOffset by remember { mutableIntStateOf(0) }

    val weeklySummary = remember(allEvents, weekOffset) {
        calculateWeeklySummary(allEvents, weekOffset)
    }

    var selectedDayStat by remember(weeklySummary) {
        mutableStateOf(weeklySummary.dayStats.firstOrNull { it.totalHours > 0 } ?: weeklySummary.dayStats.firstOrNull())
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("weekly_academic_dashboard")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SigeonPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = SigeonPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Академическая аналитика",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Часы лекций и лабораторных",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Week Navigation Controller
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { weekOffset-- },
                            modifier = Modifier.size(34.dp).testTag("prev_week_button")
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущая неделя")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = SigeonPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = weeklySummary.rangeFormatted,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (weekOffset != 0) {
                                IconButton(
                                    onClick = { weekOffset = 0 },
                                    modifier = Modifier.size(32.dp).testTag("today_week_button")
                                ) {
                                    Icon(
                                        Icons.Default.Today,
                                        contentDescription = "Текущая неделя",
                                        tint = SigeonPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { weekOffset++ },
                                modifier = Modifier.size(34.dp).testTag("next_week_button")
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Следующая неделя")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Metric Summary Badges Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Total Hours Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SigeonPrimary.copy(alpha = 0.12f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Всего за неделю",
                                fontSize = 10.sp,
                                color = SigeonPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = "${formatHours(weeklySummary.totalHours)} ч",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SigeonPrimary,
                                maxLines = 1
                            )
                        }
                    }

                    // Lectures Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3B82F6).copy(alpha = 0.12f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Лекции",
                                    fontSize = 10.sp,
                                    color = Color(0xFF3B82F6),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "${formatHours(weeklySummary.totalLectureHours)} ч",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B82F6),
                                maxLines = 1
                            )
                        }
                    }

                    // Labs Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF8B5CF6).copy(alpha = 0.12f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Science,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Лабораторные",
                                    fontSize = 10.sp,
                                    color = Color(0xFF8B5CF6),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "${formatHours(weeklySummary.totalLabHours)} ч",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5CF6),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bar Chart Visual Canvas Component
                Text(
                    text = "Распределение учебных часов по дням:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(10.dp))

                WeeklyAcademicBarChart(
                    dayStats = weeklySummary.dayStats,
                    selectedDay = selectedDayStat,
                    onDaySelected = { selectedDayStat = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                )

                // Chart Legend
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6))
                        )
                        Text("Лекции", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6))
                        )
                        Text("Лабораторные & Практики", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Selected Day Details Card
                selectedDayStat?.let { day ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Детализация: ${day.dayName} (${day.formattedDateNum})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SigeonPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Итого: ${formatHours(day.totalHours)} ч",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SigeonPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "• Лекции: ${formatHours(day.lectureHours)} ч",
                                    fontSize = 12.sp,
                                    color = Color(0xFF3B82F6),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Text(
                                    text = "• Лабораторные: ${formatHours(day.labHours)} ч",
                                    fontSize = 12.sp,
                                    color = Color(0xFF8B5CF6),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SigeonPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Готово", maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun WeeklyAcademicBarChart(
    dayStats: List<DayAcademicStats>,
    selectedDay: DayAcademicStats?,
    onDaySelected: (DayAcademicStats) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxHours = remember(dayStats) {
        val maxInStats = dayStats.maxOfOrNull { it.totalHours } ?: 0f
        maxOf(6f, maxInStats).coerceAtLeast(4f)
    }

    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val lectureColor = Color(0xFF3B82F6)
    val labColor = Color(0xFF8B5CF6)
    val selectedHighlightColor = SigeonPrimary.copy(alpha = 0.15f)

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val labelMarginLeft = 32.dp.toPx()
                val bottomMargin = 8.dp.toPx()

                val chartWidth = canvasWidth - labelMarginLeft
                val chartHeight = canvasHeight - bottomMargin

                // Draw Y-Axis Grid Lines & Labels (0h, 2h, 4h, 6h, 8h)
                val gridSteps = 4
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                for (i in 0..gridSteps) {
                    val fraction = i.toFloat() / gridSteps.toFloat()
                    val y = chartHeight - (fraction * chartHeight)
                    val hourVal = fraction * maxHours

                    // Dashed Horizontal Grid Line
                    drawLine(
                        color = gridColor,
                        start = Offset(labelMarginLeft, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                // Draw Bars for each day column (Mon..Sat)
                val columnCount = dayStats.size.coerceAtLeast(1)
                val colWidth = chartWidth / columnCount
                val barWidth = (colWidth * 0.45f).coerceIn(16.dp.toPx(), 36.dp.toPx())

                dayStats.forEachIndexed { index, stat ->
                    val centerX = labelMarginLeft + (index * colWidth) + (colWidth / 2f)
                    val barLeft = centerX - (barWidth / 2f)

                    val lectureHeight = (stat.lectureHours / maxHours) * chartHeight
                    val labHeight = (stat.labHours / maxHours) * chartHeight

                    val totalBarHeight = (stat.totalHours / maxHours) * chartHeight

                    // Bottom: Lecture Segment
                    if (lectureHeight > 0f) {
                        val topY = chartHeight - lectureHeight
                        drawRoundRect(
                            color = lectureColor,
                            topLeft = Offset(barLeft, topY),
                            size = Size(barWidth, lectureHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    // Top: Lab Segment (stacked above lectures)
                    if (labHeight > 0f) {
                        val bottomY = chartHeight - lectureHeight
                        val topY = bottomY - labHeight
                        drawRoundRect(
                            color = labColor,
                            topLeft = Offset(barLeft, topY),
                            size = Size(barWidth, labHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    // Base empty placeholder if day has 0 hours
                    if (totalBarHeight <= 0f) {
                        drawRoundRect(
                            color = gridColor.copy(alpha = 0.3f),
                            topLeft = Offset(barLeft, chartHeight - 4.dp.toPx()),
                            size = Size(barWidth, 4.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }

            // Clickable Day Columns Row
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 32.dp)
            ) {
                dayStats.forEach { stat ->
                    val isSelected = selectedDay?.dateString == stat.dateString
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isSelected) Modifier.background(selectedHighlightColor) else Modifier
                            )
                            .clickable { onDaySelected(stat) }
                    )
                }
            }
        }

        // X-Axis Day Labels Row below chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, top = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dayStats.forEach { stat ->
                val isSelected = selectedDay?.dateString == stat.dateString
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stat.dayName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) SigeonPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = if (stat.totalHours > 0) "${formatHours(stat.totalHours)}ч" else "—",
                        fontSize = 9.sp,
                        color = if (isSelected) SigeonPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun calculateWeeklySummary(
    events: List<EventEntity>,
    weekOffset: Int
): WeeklyAcademicSummary {
    val cal = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        add(Calendar.WEEK_OF_YEAR, weekOffset)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val nameFormatter = SimpleDateFormat("EEE", Locale.forLanguageTag("ru"))
    val numFormatter = SimpleDateFormat("d MMM", Locale.forLanguageTag("ru"))

    val daysList = mutableListOf<DayAcademicStats>()
    var sumLectures = 0f
    var sumLabs = 0f
    var maxDayHours = -1f
    var busiestDay = "—"

    val startDateStr = df.format(cal.time)
    val startFormatted = numFormatter.format(cal.time)
    var endDateStr = ""
    var endFormatted = ""

    for (i in 0 until 6) { // Mon - Sat
        val dateStr = df.format(cal.time)
        val dayName = nameFormatter.format(cal.time).uppercase()
        val dayNum = numFormatter.format(cal.time)
        endDateStr = dateStr
        endFormatted = dayNum

        val dayEvents = events.filter { it.date == dateStr && (it.type == "COLLEGE" || it.collegePairNumber > 0) }

        var dayLectures = 0f
        var dayLabs = 0f

        for (event in dayEvents) {
            val durationHours = calculateDurationInHours(event.startTime, event.endTime)
            val type = event.collegeLessonType.lowercase()
            if (type.contains("лекци")) {
                dayLectures += durationHours
            } else if (type.contains("лаб") || type.contains("практик")) {
                dayLabs += durationHours
            } else {
                dayLectures += durationHours
            }
        }

        val dayTotal = dayLectures + dayLabs
        sumLectures += dayLectures
        sumLabs += dayLabs

        if (dayTotal > maxDayHours && dayTotal > 0f) {
            maxDayHours = dayTotal
            busiestDay = dayName
        }

        daysList.add(
            DayAcademicStats(
                dayName = dayName,
                dateString = dateStr,
                formattedDateNum = dayNum,
                lectureHours = dayLectures,
                labHours = dayLabs,
                totalHours = dayTotal
            )
        )

        cal.add(Calendar.DAY_OF_YEAR, 1)
    }

    val rangeStr = "$startFormatted – $endFormatted"

    return WeeklyAcademicSummary(
        startDateStr = startDateStr,
        endDateStr = endDateStr,
        rangeFormatted = rangeStr,
        totalLectureHours = sumLectures,
        totalLabHours = sumLabs,
        totalHours = sumLectures + sumLabs,
        dayStats = daysList,
        busiestDayName = busiestDay
    )
}

private fun calculateDurationInHours(startStr: String, endStr: String): Float {
    return try {
        val sParts = startStr.trim().split(":")
        val eParts = endStr.trim().split(":")
        val sMin = sParts[0].toInt() * 60 + sParts[1].toInt()
        val eMin = eParts[0].toInt() * 60 + eParts[1].toInt()
        val diffMin = (eMin - sMin).coerceAtLeast(0)
        if (diffMin > 0) diffMin / 60.0f else 1.5f
    } catch (_: Exception) {
        1.5f
    }
}

private fun formatHours(hours: Float): String {
    return if (hours % 1f == 0f) {
        hours.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", hours)
    }
}
