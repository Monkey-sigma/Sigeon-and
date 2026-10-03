package com.example.ui.college

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.MainViewModel
import com.example.ui.components.PulsatingLiveBadge
import com.example.ui.components.bounceClick
import com.example.ui.notes.AddEditNoteDialog
import com.example.ui.theme.SigeonPrimary
import com.example.ui.theme.SigeonPrimaryDark
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayTabInfo(
    val dayName: String,
    val dayDateFormatted: String,
    val dateString: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollegeScheduleScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val collegeClasses by viewModel.collegeSchedule.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val selectedPlanovoGroup by viewModel.selectedPlanovoGroup.collectAsState()
    val apiUrl by viewModel.apiUrlInput.collectAsState()
    val groupName by viewModel.groupNameInput.collectAsState()

    var showConfigDialog by remember { mutableStateOf(false) }
    var showGroupSelectorSheet by remember { mutableStateOf(false) }
    var editingClassEvent by remember { mutableStateOf<EventEntity?>(null) }
    var noteDialogClassDate by remember { mutableStateOf<String?>(null) }
    var noteDialogSubject by remember { mutableStateOf("") }

    // Weekday tabs (dates from current week Monday to Saturday)
    val weekDays = remember {
        val list = mutableListOf<DayTabInfo>()
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        val dayFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val nameFormatter = SimpleDateFormat("EEE", Locale("ru"))
        val dayNumFormatter = SimpleDateFormat("d MMM", Locale("ru"))

        for (i in 0 until 6) { // Mon-Sat
            val dateStr = dayFormatter.format(cal.time)
            val dayName = nameFormatter.format(cal.time).uppercase()
            val dayNum = dayNumFormatter.format(cal.time)
            list.add(DayTabInfo(dayName = dayName, dayDateFormatted = dayNum, dateString = dateStr))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var selectedTabIndex by remember {
        val index = weekDays.indexOfFirst { it.dateString == todayStr }
        mutableIntStateOf(if (index >= 0) index else 0)
    }

    val currentTabDate = weekDays.getOrNull(selectedTabIndex)?.dateString ?: todayStr
    val classesForSelectedDay = remember(collegeClasses, currentTabDate) {
        collegeClasses.filter { it.date == currentTabDate }.sortedBy { it.collegePairNumber }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF8B5CF6),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Clickable group switcher in TopAppBar
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SigeonPrimary.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .bounceClick { showGroupSelectorSheet = true }
                                .testTag("top_bar_group_switcher")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Column {
                                    Text(
                                        text = groupName.ifBlank { "Выбрать группу" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SigeonPrimary
                                    )
                                    Text(
                                        text = "Нажмите для смены группы",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Сменить группу",
                                    tint = SigeonPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showConfigDialog = true },
                        modifier = Modifier.testTag("college_api_config_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Настройка API",
                            tint = SigeonPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // College Profile & API Status Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedProfile.collegeName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (apiUrl.contains("planovo.pro")) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Planovo",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Interactive Pill to switch group immediately
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .bounceClick { showGroupSelectorSheet = true }
                                    .background(SigeonPrimary.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = SigeonPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Группа: $groupName ▾",
                                    fontSize = 12.sp,
                                    color = SigeonPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // API sync button
                        Button(
                            onClick = {
                                viewModel.syncCollegeScheduleFromApi(apiUrl, groupName, selectedProfile.id)
                            },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SigeonPrimary),
                            modifier = Modifier
                                .bounceClick {
                                    if (!isSyncing) {
                                        viewModel.syncCollegeScheduleFromApi(apiUrl, groupName, selectedProfile.id)
                                    }
                                }
                                .testTag("refresh_api_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (isOnline) Icons.Default.CloudSync else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isOnline) "Обновить" else "Офлайн", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sync to Google Calendar action row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "В расписании: ${collegeClasses.size} пар",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = { viewModel.exportCollegeScheduleToGoogle() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("export_schedule_to_google_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = Color(0xFF4285F4),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "В Google Календарь",
                                fontSize = 11.sp,
                                color = Color(0xFF4285F4)
                            )
                        }
                    }
                }
            }

            // Weekday tabs (Mon, Tue, Wed, Thu, Fri, Sat)
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = SigeonPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                weekDays.forEachIndexed { index, tabInfo ->
                    val isSelected = selectedTabIndex == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = tabInfo.dayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = tabInfo.dayDateFormatted,
                                    fontSize = 10.sp,
                                    color = if (isSelected) SigeonPrimaryDark else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.testTag("day_tab_$index")
                    )
                }
            }

            // Sync loading banner with smooth spring animation
            AnimatedVisibility(
                visible = isSyncing,
                enter = fadeIn() + slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = fadeOut()
            ) {
                Surface(
                    color = SigeonPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = SigeonPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Загрузка расписания группы $groupName...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SigeonPrimary
                        )
                    }
                }
            }

            // Classes list for chosen day
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                if (classesForSelectedDay.isEmpty() && !isSyncing) {
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
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "На этот день пар нет (или выходной)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Все данные сохраняются локально и доступны без интернета",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(classesForSelectedDay, key = { "class_${it.id}" }) { event ->
                        EnhancedCollegeClassCard(
                            event = event,
                            onExportToGoogle = { viewModel.exportEventToGoogle(event) },
                            onEditClass = { editingClassEvent = event },
                            onDelete = { viewModel.deleteEvent(event) },
                            onAddNoteForClass = {
                                noteDialogClassDate = event.date
                                noteDialogSubject = event.collegeSubject
                            }
                        )
                    }
                }
            }
        }
    }

    // Group Selector Bottom Sheet (Instant Group Switcher)
    if (showGroupSelectorSheet) {
        GroupSelectorBottomSheet(
            currentGroupId = selectedPlanovoGroup.id,
            onDismiss = { showGroupSelectorSheet = false },
            onGroupSelected = { group ->
                viewModel.selectAndSyncPlanovoGroup(group)
                showGroupSelectorSheet = false
            },
            onCustomGroupSubmit = { customId, customCode ->
                viewModel.selectAndSyncCustomGroupId(customId, customCode)
                showGroupSelectorSheet = false
            }
        )
    }

    // Config Dialog
    if (showConfigDialog) {
        CollegeApiConfigDialog(
            currentProfile = selectedProfile,
            allProfiles = viewModel.fetcher.defaultProfiles,
            currentApiUrl = apiUrl,
            currentGroupName = groupName,
            onDismiss = { showConfigDialog = false },
            onConfirm = { profile, newApiUrl, newGroupName ->
                viewModel.selectProfile(profile)
                viewModel.setApiUrl(newApiUrl)
                viewModel.setGroupName(newGroupName)
                viewModel.syncCollegeScheduleFromApi(newApiUrl, newGroupName, profile.id)
                showConfigDialog = false
            }
        )
    }

    // Edit Class Dialog (Offline modification)
    editingClassEvent?.let { eventToEdit ->
        EditCollegeClassDialog(
            event = eventToEdit,
            onDismiss = { editingClassEvent = null },
            onConfirm = { updatedEvent ->
                viewModel.updateCollegeClass(updatedEvent)
                editingClassEvent = null
            }
        )
    }

    // Note dialog triggered from class
    if (noteDialogClassDate != null) {
        AddEditNoteDialog(
            initialDate = noteDialogClassDate,
            onDismiss = { noteDialogClassDate = null },
            onConfirm = { title, content, date, tag, color, isPinned, items ->
                viewModel.addNote(
                    title = if (title.isNotBlank()) title else "Д/З: $noteDialogSubject",
                    content = content,
                    date = date,
                    tag = "Д/З",
                    colorHex = "#6366F1",
                    isPinned = isPinned,
                    checklistItems = items
                )
                noteDialogClassDate = null
            }
        )
    }
}

@Composable
fun EnhancedCollegeClassCard(
    event: EventEntity,
    onExportToGoogle: () -> Unit,
    onEditClass: () -> Unit,
    onDelete: () -> Unit,
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
            .bounceClick(scaleDown = 0.98f) { onEditClass() }
            .testTag("college_class_card_${event.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLiveNow) SigeonPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLiveNow) 4.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Live status badge if class is going right now!
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
                        color = Color(0xFFEF4444)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pair Number Badge
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
                            fontSize = 15.sp
                        )
                        Text(
                            text = "пара",
                            fontSize = 9.sp,
                            color = if (isLiveNow) Color(0xFFEF4444) else SigeonPrimaryDark
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
                            color = if (isLiveNow) Color(0xFFEF4444) else SigeonPrimary
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

                        if (event.isCustomEdited) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "изменено",
                                    fontSize = 9.sp,
                                    color = Color(0xFFF59E0B),
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

                // Quick actions (Edit class, Add note to class / Export to Google)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditClass,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать пару",
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
        }
    }
}
