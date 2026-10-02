package com.example.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.EventEntity
import com.example.data.local.NoteEntity
import com.example.ui.EventFilter
import com.example.ui.MainViewModel
import com.example.ui.notes.AddEditNoteDialog
import com.example.ui.theme.SigeonPrimary
import com.example.ui.theme.SigeonPrimaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var isCalendarExpanded by remember { mutableStateOf(true) }

    // Human-readable date title
    val formattedDateTitle = remember(selectedDate) {
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateObj = parser.parse(selectedDate) ?: Date()
            val formatter = SimpleDateFormat("EEEE, d MMMM", Locale("ru"))
            formatter.format(dateObj).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            selectedDate
        }
    }

    // Filter items based on active chip
    val filteredCollegeClasses = remember(dayEvents, activeFilter) {
        if (activeFilter == EventFilter.ALL || activeFilter == EventFilter.COLLEGE) {
            dayEvents.filter { it.type == "COLLEGE" }
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Расписание & Заметки",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

            // Selected Date Banner & Filter Chips
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$totalItemsCount событий / пар на этот день",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                label = { Text(filter.label) },
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Нажмите + чтобы добавить событие, пару или заметку",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                items(filteredCollegeClasses, key = { "class_${it.id}" }) { event ->
                    CollegeClassCard(
                        event = event,
                        onExportToGoogle = { viewModel.exportEventToGoogle(event) },
                        onDelete = { viewModel.deleteEvent(event) },
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
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                items(filteredOtherEvents, key = { "event_${it.id}" }) { event ->
                    EventItemCard(
                        event = event,
                        onDelete = { viewModel.deleteEvent(event) },
                        onExportToGoogle = { viewModel.exportEventToGoogle(event) }
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
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                items(filteredNotes, key = { "note_${it.id}" }) { note ->
                    CalendarNoteCard(
                        note = note,
                        onDelete = { viewModel.deleteNote(note) }
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
            onConfirm = { title, desc, date, start, end, loc, color ->
                viewModel.addEvent(title, desc, date, start, end, loc, color)
                showAddEventDialog = false
            }
        )
    }

    // Add Note Dialog
    if (showAddNoteDialog) {
        AddEditNoteDialog(
            initialDate = selectedDate,
            onDismiss = { showAddNoteDialog = false },
            onConfirm = { title, content, date, tag, color, isPinned ->
                viewModel.addNote(title, content, date, tag, color, isPinned)
                showAddNoteDialog = false
            }
        )
    }
}

@Composable
fun CollegeClassCard(
    event: EventEntity,
    onExportToGoogle: () -> Unit,
    onDelete: () -> Unit,
    onAddNoteForClass: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .testTag("college_class_card_${event.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pair Number Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SigeonPrimary.copy(alpha = 0.12f),
                modifier = Modifier.size(46.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "№${event.collegePairNumber}",
                        fontWeight = FontWeight.Bold,
                        color = SigeonPrimary,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "пара",
                        fontSize = 9.sp,
                        color = SigeonPrimaryDark
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
                        color = SigeonPrimary
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
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = event.collegeSubject.ifBlank { event.title },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick actions (Add note to class / Export to Google)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onAddNoteForClass,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Д/З к паре",
                        tint = SigeonPrimary
                    )
                }

                IconButton(
                    onClick = onExportToGoogle,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "В Google Календарь",
                        tint = Color(0xFF4285F4)
                    )
                }
            }
        }
    }
}

@Composable
fun EventItemCard(
    event: EventEntity,
    onDelete: () -> Unit,
    onExportToGoogle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (event.description.isNotBlank()) {
                    Text(
                        text = event.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    }
}

@Composable
fun CalendarNoteCard(
    note: NoteEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(note.colorHex)).copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(android.graphics.Color.parseColor(note.colorHex)).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = note.tag,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(android.graphics.Color.parseColor(note.colorHex)),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (note.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = note.content,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
