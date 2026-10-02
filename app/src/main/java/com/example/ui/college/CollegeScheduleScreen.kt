package com.example.ui.college

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.calendar.CollegeClassCard
import com.example.ui.notes.AddEditNoteDialog
import com.example.ui.theme.SigeonPrimary
import com.example.ui.theme.SigeonPrimaryDark
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollegeScheduleScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val collegeClasses by viewModel.collegeSchedule.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val apiUrl by viewModel.apiUrlInput.collectAsState()
    val groupName by viewModel.groupNameInput.collectAsState()

    var showConfigDialog by remember { mutableStateOf(false) }
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
        collegeClasses.filter { it.date == currentTabDate }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Расписание колледжа",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${selectedProfile.groupName} • API Синхр.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                            Text(
                                text = selectedProfile.collegeName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Группа: $groupName",
                                fontSize = 12.sp,
                                color = SigeonPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // API sync button
                        Button(
                            onClick = {
                                viewModel.syncCollegeScheduleFromApi(apiUrl, groupName, selectedProfile.id)
                            },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SigeonPrimary),
                            modifier = Modifier.testTag("refresh_api_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Обновить API", fontSize = 12.sp)
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
                            text = "Всего в расписании: ${collegeClasses.size} пар",
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

            // Classes list for chosen day
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
            ) {
                if (classesForSelectedDay.isEmpty()) {
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
                                    text = "Нажмите «Обновить API» чтобы загрузить свежие пары из базы колледжа",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(classesForSelectedDay, key = { "class_${it.id}" }) { event ->
                        CollegeClassCard(
                            event = event,
                            onExportToGoogle = { viewModel.exportEventToGoogle(event) },
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

    // Note dialog triggered from class
    if (noteDialogClassDate != null) {
        AddEditNoteDialog(
            initialDate = noteDialogClassDate,
            onDismiss = { noteDialogClassDate = null },
            onConfirm = { title, content, date, tag, color, isPinned ->
                viewModel.addNote(
                    title = if (title.isNotBlank()) title else "Д/З: $noteDialogSubject",
                    content = content,
                    date = date,
                    tag = "Д/З",
                    colorHex = "#6366F1",
                    isPinned = isPinned
                )
                noteDialogClassDate = null
            }
        )
    }
}

data class DayTabInfo(
    val dayName: String,
    val dayDateFormatted: String,
    val dateString: String
)
