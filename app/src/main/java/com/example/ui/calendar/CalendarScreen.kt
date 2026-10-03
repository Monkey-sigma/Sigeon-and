package com.example.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.local.EventEntity
import com.example.data.local.NoteEntity
import com.example.ui.EventFilter
import com.example.ui.MainViewModel
import com.example.ui.college.EditCollegeClassDialog
import com.example.ui.components.PulsatingLiveBadge
import com.example.ui.components.bounceClick
import com.example.ui.notes.AddEditNoteDialog
import com.example.ui.theme.SigeonPrimary
import com.example.ui.theme.SigeonPrimaryDark
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class QuickDayItem(
    val dateString: String,
    val dayName: String,
    val dayNumber: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentMonth by viewModel.currentMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val allEvents by viewModel.allEvents.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()

    val dayEvents by viewModel.eventsForSelectedDate.collectAsState()
    val dayNotes by viewModel.notesForSelectedDate.collectAsState()

    var showAddEventDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var editingCollegeEvent by remember { mutableStateOf<EventEntity?>(null) }
    var isCalendarExpanded by remember { mutableStateOf(true) }
    var fullscreenImageUri by remember { mutableStateOf<String?>(null) }

    // Human-readable date title
    val formattedDateTitle = remember(selectedDate) {
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateObj = parser.parse(selectedDate) ?: Date()
            val formatter = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("ru"))
            formatter.format(dateObj).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            selectedDate
        }
    }

    // Quick Day Strip around selected date (7 days window)
    val quickDays = remember(selectedDate) {
        val list = mutableListOf<QuickDayItem>()
        val cal = Calendar.getInstance()
        try {
            val parts = selectedDate.split("-")
            cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        } catch (_: Exception) {}

        cal.add(Calendar.DAY_OF_YEAR, -3)
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dName = SimpleDateFormat("EEE", Locale.forLanguageTag("ru"))
        val dNum = SimpleDateFormat("d", Locale.getDefault())

        for (i in 0 until 7) {
            list.add(
                QuickDayItem(
                    dateString = df.format(cal.time),
                    dayName = dName.format(cal.time).uppercase(),
                    dayNumber = dNum.format(cal.time)
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    // Filter items based on active chip
    val filteredCollegeClasses = remember(dayEvents, activeFilter) {
        if (activeFilter == EventFilter.ALL || activeFilter == EventFilter.COLLEGE) {
            dayEvents.filter { it.type == "COLLEGE" }.sortedBy { it.collegePairNumber }
        } else emptyList()
    }

    val filteredOtherEvents = remember(dayEvents, activeFilter) {
        if (activeFilter == EventFilter.ALL || activeFilter == EventFilter.EVENTS) {
            dayEvents.filter { it.type != "COLLEGE" }
        } else emptyList()
    }

    val filteredNotes = remember(dayNotes, activeFilter) {
        if (activeFilter == EventFilter.ALL || activeFilter == EventFilter.NOTES) {
            dayNotes
        } else emptyList()
    }

    val totalItemsCount = filteredCollegeClasses.size + filteredOtherEvents.size + filteredNotes.size

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SigeonPrimary,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SIGEON Calendar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                            Text(
                                text = "Офлайн-календарь & Заметки",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { isCalendarExpanded = !isCalendarExpanded }) {
                        Icon(
                            imageVector = if (isCalendarExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Свернуть/Развернуть календарь"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FloatingActionButton(
                    onClick = { showAddNoteDialog = true },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = SigeonPrimary,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("fab_add_note")
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = "Добавить заметку")
                }

                FloatingActionButton(
                    onClick = { showAddEventDialog = true },
                    containerColor = SigeonPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_event")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить событие")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Collapsible Month Calendar
            item {
                AnimatedVisibility(visible = isCalendarExpanded) {
                    MonthCalendarView(
                        currentMonth = currentMonth,
                        selectedDate = selectedDate,
                        events = allEvents,
                        notes = allNotes,
                        onDateSelected = { viewModel.selectDate(it) },
                        onPreviousMonth = { viewModel.changeMonth(-1) },
                        onNextMonth = { viewModel.changeMonth(1) },
                        onJumpToToday = { viewModel.jumpToToday() }
                    )
                }
            }

            // Quick Day Strip (Horizontal Day Carousel)
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickDays, key = { it.dateString }) { day ->
                        val isSelected = day.dateString == selectedDate
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.selectDate(day.dateString) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SigeonPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = day.dayName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = day.dayNumber,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Selected Date Banner & Filter Chips
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = formattedDateTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                            Text(
                                text = "$totalItemsCount событий / пар на этот день",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        EventFilter.values().forEach { filter ->
                            val isSelected = activeFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setFilter(filter) },
                                label = { Text(filter.label, maxLines = 1, softWrap = false) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SigeonPrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = SigeonPrimary
                                ),
                                modifier = Modifier.testTag("filter_${filter.name}")
                            )
                        }
                    }
                }
            }

            // Empty state if nothing scheduled
            if (totalItemsCount == 0) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "На этот день ничего не запланировано",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Нажмите + чтобы добавить событие, пару или заметку",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // College Classes Section
            if (filteredCollegeClasses.isNotEmpty()) {
                item {
                    Text(
                        text = "Расписание пар в колледже",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SigeonPrimary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                items(filteredCollegeClasses, key = { "class_${it.id}" }) { event ->
                    CalendarCollegeClassItem(
                        event = event,
                        onEdit = { editingCollegeEvent = event },
                        onExportToGoogle = { viewModel.exportEventToGoogle(event) },
                        onDelete = { viewModel.deleteEvent(event) },
                        onImageClick = { fullscreenImageUri = it },
                        onAddNoteForClass = {
                            viewModel.addNote(
                                title = "Д/З: ${event.collegeSubject}",
                                content = "Задание по паре №${event.collegePairNumber} (${event.collegeLessonType})",
                                date = event.date,
                                tag = "Д/З",
                                colorHex = "#6366F1"
                            )
                        }
                    )
                }
            }

            // Other Events (Personal / Google)
            if (filteredOtherEvents.isNotEmpty()) {
                item {
                    Text(
                        text = "События и встречи",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                items(filteredOtherEvents, key = { "event_${it.id}" }) { event ->
                    EventItemCard(
                        event = event,
                        onDelete = { viewModel.deleteEvent(event) },
                        onExportToGoogle = { viewModel.exportEventToGoogle(event) },
                        onImageClick = { fullscreenImageUri = it }
                    )
                }
            }

            // Day Notes Section
            if (filteredNotes.isNotEmpty()) {
                item {
                    Text(
                        text = "Заметки на эту дату",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                items(filteredNotes, key = { "note_${it.id}" }) { note ->
                    CalendarInteractiveNoteCard(
                        note = note,
                        onToggleChecklistItem = { itemId, isDone ->
                            viewModel.toggleChecklistItem(note, itemId, isDone)
                        },
                        onDelete = { viewModel.deleteNote(note) },
                        onImageClick = { fullscreenImageUri = it }
                    )
                }
            }
        }
    }

    // Fullscreen Image Dialog
    fullscreenImageUri?.let { imgPath ->
        Dialog(onDismissRequest = { fullscreenImageUri = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { fullscreenImageUri = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
                        }
                    }
                    AsyncImage(
                        model = imgPath,
                        contentDescription = "Фото",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().height(350.dp).clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        }
    }

    // Add Event Dialog
    if (showAddEventDialog) {
        AddEditEventDialog(
            initialDate = selectedDate,
            onDismiss = { showAddEventDialog = false },
            onConfirm = { title, desc, date, start, end, loc, color, imageUri ->
                viewModel.addEvent(
                    title = title,
                    description = desc,
                    date = date,
                    startTime = start,
                    endTime = end,
                    location = loc,
                    colorHex = color,
                    imageUri = imageUri
                )
                showAddEventDialog = false
            }
        )
    }

    // Add Note Dialog
    if (showAddNoteDialog) {
        AddEditNoteDialog(
            initialDate = selectedDate,
            onDismiss = { showAddNoteDialog = false },
            onConfirm = { title, content, date, tag, color, isPinned, items, imageUri, slotId, pairNum, subj ->
                viewModel.addNote(title, content, date, tag, color, isPinned, items, imageUri, slotId, pairNum, subj)
                showAddNoteDialog = false
            }
        )
    }

    // Edit College Class Dialog
    editingCollegeEvent?.let { eventToEdit ->
        EditCollegeClassDialog(
            event = eventToEdit,
            onDismiss = { editingCollegeEvent = null },
            onConfirm = { updated ->
                viewModel.updateCollegeClass(updated)
                editingCollegeEvent = null
            }
        )
    }
}

@Composable
fun EventItemCard(
    event: EventEntity,
    onDelete: () -> Unit,
    onExportToGoogle: () -> Unit,
    onImageClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(event.colorHex)))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (event.startTime.isNotBlank()) {
                        Text(
                            text = if (event.endTime.isNotBlank()) "${event.startTime} - ${event.endTime}" else event.startTime,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )

                    if (event.description.isNotBlank()) {
                        Text(
                            text = event.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }

                    if (event.location.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = event.location,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                }

                Row {
                    if (event.type != "GOOGLE") {
                        IconButton(
                            onClick = onExportToGoogle,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = "В Google",
                                tint = Color(0xFF4285F4)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Display attached photo if present
            event.imageUri?.let { imgPath ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onImageClick(imgPath) }
                ) {
                    AsyncImage(
                        model = imgPath,
                        contentDescription = "Фото к событию",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarCollegeClassItem(
    event: EventEntity,
    onEdit: () -> Unit,
    onExportToGoogle: () -> Unit,
    onDelete: () -> Unit,
    onImageClick: (String) -> Unit = {},
    onAddNoteForClass: () -> Unit
) {
    val isLiveNow = remember(event.startTime, event.endTime, event.date) {
        try {
            val now = Calendar.getInstance()
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            if (df.format(now.time) == event.date) {
                val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                val startParts = event.startTime.split(":")
                val endParts = event.endTime.split(":")
                val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
                val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
                currentMinutes in startMinutes..endMinutes
            } else false
        } catch (_: Exception) {
            false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .bounceClick(scaleDown = 0.98f) { onEdit() }
            .testTag("college_class_card_${event.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLiveNow) SigeonPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLiveNow) 4.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (isLiveNow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PulsatingLiveBadge(text = "ИДЕТ СЕЙЧАС")
                    Text(
                        text = "До окончания: ${event.endTime}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFEF4444),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLiveNow) Color(0xFFEF4444).copy(alpha = 0.15f) else SigeonPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "№${event.collegePairNumber}",
                            fontWeight = FontWeight.Bold,
                            color = if (isLiveNow) Color(0xFFEF4444) else SigeonPrimary,
                            fontSize = 15.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "пара",
                            fontSize = 9.sp,
                            color = if (isLiveNow) Color(0xFFEF4444) else SigeonPrimaryDark,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${event.startTime} - ${event.endTime}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isLiveNow) Color(0xFFEF4444) else SigeonPrimary,
                            maxLines = 1,
                            softWrap = false
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(android.graphics.Color.parseColor(event.colorHex)).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = event.collegeLessonType.ifBlank { "Пара" },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(android.graphics.Color.parseColor(event.colorHex)),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (event.isCustomEdited) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "изменено",
                                    fontSize = 9.sp,
                                    color = Color(0xFFF59E0B),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = event.collegeSubject.ifBlank { event.title },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (event.collegeRoom.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = event.collegeRoom,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        }

                        if (event.collegeTeacher.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = event.collegeTeacher,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onAddNoteForClass,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Д/З к паре",
                            tint = SigeonPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onExportToGoogle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "В Google Календарь",
                            tint = Color(0xFF4285F4),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Attached Photo for College Class
            event.imageUri?.let { imgPath ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onImageClick(imgPath) }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = imgPath,
                            contentDescription = "Фото к паре",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            shape = RoundedCornerShape(topStart = 8.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.BottomEnd)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Фото пары",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarInteractiveNoteCard(
    note: NoteEntity,
    onToggleChecklistItem: (itemId: String, isDone: Boolean) -> Unit,
    onDelete: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val checklist = remember(note.checklistJson) { note.getChecklistItems() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(note.colorHex)).copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(android.graphics.Color.parseColor(note.colorHex)).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = note.tag,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(android.graphics.Color.parseColor(note.colorHex)),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = note.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = note.content,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            }

            // Display attached image preview if present
            note.imageUri?.let { imgPath ->
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onImageClick(imgPath) }
                ) {
                    AsyncImage(
                        model = imgPath,
                        contentDescription = "Фото к заметке",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Interactive Checklists
            if (checklist.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    checklist.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onToggleChecklistItem(item.id, !item.isDone) }
                                .padding(vertical = 1.dp)
                        ) {
                            Checkbox(
                                checked = item.isDone,
                                onCheckedChange = { onToggleChecklistItem(item.id, it) },
                                colors = CheckboxDefaults.colors(checkedColor = SigeonPrimary),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.text,
                                fontSize = 12.sp,
                                textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (item.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
