package com.example.ui.college

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.local.EventEntity
import com.example.ui.MainViewModel
import com.example.ui.components.LiveLessonProgressBar
import com.example.ui.components.PulsatingLiveBadge
import com.example.ui.components.bounceClick
import com.example.ui.notes.AddEditNoteDialog
import com.example.ui.theme.SigeonPrimary
import com.example.ui.theme.SigeonPrimaryDark
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayTabInfo(
    val dayName: String,
    val dayDateFormatted: String,
    val dateString: String
)

data class GroupedCollegePair(
    val pairNumber: Int,
    val startTime: String,
    val endTime: String,
    val primarySubjectName: String,
    val primaryLessonType: String,
    val lessons: List<EventEntity>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollegeScheduleScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val collegeClasses by viewModel.collegeSchedule.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val selectedPlanovoGroup by viewModel.selectedPlanovoGroup.collectAsState()
    val customCoverUri by viewModel.customCoverImageUri.collectAsState()
    val apiUrl by viewModel.apiUrlInput.collectAsState()
    val groupName by viewModel.groupNameInput.collectAsState()

    var showConfigDialog by remember { mutableStateOf(false) }
    var showGroupSelectorSheet by remember { mutableStateOf(false) }
    var editingClassEvent by remember { mutableStateOf<EventEntity?>(null) }
    var noteDialogClassDate by remember { mutableStateOf<String?>(null) }
    var noteDialogSubject by remember { mutableStateOf("") }
    var fullscreenImageUri by remember { mutableStateOf<String?>(null) }

    // Cover image picker
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val localPath = ImageStorageHelper.saveImageToInternalStorage(context, uri)
                viewModel.setCustomCoverImage(localPath)
            }
        }
    }

    // Weekday tabs (dates from current week Monday to Saturday)
    val weekDays = remember {
        val list = mutableListOf<DayTabInfo>()
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        val dayFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val nameFormatter = SimpleDateFormat("EEE", Locale.forLanguageTag("ru"))
        val dayNumFormatter = SimpleDateFormat("d MMM", Locale.forLanguageTag("ru"))

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

    // Group duplicate or parallel lessons at the same time/pair slot into a single window
    val groupedClassesForSelectedDay = remember(collegeClasses, currentTabDate) {
        val dayLessons = collegeClasses.filter { it.date == currentTabDate }
        if (dayLessons.isEmpty()) emptyList()
        else {
            val groups = dayLessons.groupBy { lesson ->
                if (lesson.collegePairNumber > 0) "pair_${lesson.collegePairNumber}"
                else "time_${lesson.startTime}_${lesson.endTime}"
            }

            groups.map { (_, pairLessons) ->
                val first = pairLessons.first()
                val pairNum = if (first.collegePairNumber > 0) first.collegePairNumber else 0
                val startTime = pairLessons.minOfOrNull { it.startTime }?.ifBlank { first.startTime } ?: first.startTime
                val endTime = pairLessons.maxOfOrNull { it.endTime }?.ifBlank { first.endTime } ?: first.endTime

                val subjectNames = pairLessons.map { it.collegeSubject.ifBlank { it.title } }.filter { it.isNotBlank() }.distinct()
                val primarySubject = when {
                    subjectNames.isEmpty() -> "Пара №$pairNum"
                    subjectNames.size == 1 -> subjectNames.first()
                    subjectNames.all {
                        it.contains("английский", ignoreCase = true) ||
                        it.contains("иностранный", ignoreCase = true) ||
                        it.contains("немецкий", ignoreCase = true)
                    } -> "Иностранный язык"
                    else -> subjectNames.joinToString(" / ")
                }

                val primaryType = pairLessons.firstOrNull { it.collegeLessonType.isNotBlank() }?.collegeLessonType ?: ""

                GroupedCollegePair(
                    pairNumber = pairNum,
                    startTime = startTime,
                    endTime = endTime,
                    primarySubjectName = primarySubject,
                    primaryLessonType = primaryType,
                    lessons = pairLessons.sortedBy { it.id }
                )
            }.sortedWith(compareBy({ it.pairNumber }, { it.startTime }))
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
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
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .bounceClick { showGroupSelectorSheet = true }
                                .testTag("top_bar_group_switcher")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = groupName.ifBlank { "Выбрать группу" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SigeonPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "Нажмите для смены группы",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
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
            // Custom Cover Banner Header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
            ) {
                Column {
                    // Banner Image container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        if (customCoverUri != null) {
                            AsyncImage(
                                model = customCoverUri,
                                contentDescription = "Пользовательская обложка",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { fullscreenImageUri = customCoverUri }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF4F46E5),
                                                Color(0xFF7C3AED),
                                                Color(0xFF2563EB)
                                            )
                                        )
                                    )
                            )
                        }

                        // Gradient overlay for contrast
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                    )
                                )
                        )

                        // Cover Photo Buttons
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (customCoverUri != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.setCustomCoverImage(null) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Сбросить обложку",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.clickable {
                                    coverPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (customCoverUri == null) "Своя картинка" else "Изменить",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        // College Group Label on Banner
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "${selectedProfile.collegeName} • ${selectedPlanovoGroup.code}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                            Text(
                                text = "Актуальное расписание занятий",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }

                    // Schedule Status Sub-bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.CloudSync else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isOnline) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isOnline) "Онлайн-синхронизация" else "Офлайн-режим",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF4285F4).copy(alpha = 0.12f),
                            modifier = Modifier.clickable { viewModel.exportCollegeScheduleToGoogle() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
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
                                    color = Color(0xFF4285F4),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
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
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = tabInfo.dayDateFormatted,
                                    fontSize = 10.sp,
                                    color = if (isSelected) SigeonPrimaryDark else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        },
                        modifier = Modifier.testTag("day_tab_$index")
                    )
                }
            }

            // Sync loading banner
            AnimatedVisibility(
                visible = isSyncing,
                enter = fadeIn() + slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = fadeOut()
            ) {
                Surface(
                    color = SigeonPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = SigeonPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Синхронизация расписания Planovo...",
                            fontSize = 11.sp,
                            color = SigeonPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Schedule List for Selected Day
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (groupedClassesForSelectedDay.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
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
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Пар на этот день нет 🎉",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Отдыхайте или выберите другой день недели",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = groupedClassesForSelectedDay,
                        key = { "${it.pairNumber}_${it.startTime}_${it.lessons.firstOrNull()?.id ?: 0}" }
                    ) { groupedPair ->
                        EnhancedGroupedCollegeClassCard(
                            groupedPair = groupedPair,
                            onEditClick = { lesson -> editingClassEvent = lesson },
                            onImageClick = { imgUri -> fullscreenImageUri = imgUri },
                            onAddNoteClick = { lesson ->
                                noteDialogClassDate = lesson.date
                                noteDialogSubject = lesson.collegeSubject.ifBlank { lesson.title }
                            }
                        )
                    }
                }
            }
        }
    }

    // Group Selector Bottom Sheet
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

    // API Configuration Dialog
    if (showConfigDialog) {
        CollegeApiConfigDialog(
            currentApiUrl = apiUrl,
            currentGroupName = groupName,
            profiles = viewModel.fetcher.defaultProfiles,
            onDismiss = { showConfigDialog = false },
            onSave = { newUrl, newGroup ->
                viewModel.setApiUrl(newUrl)
                viewModel.setGroupName(newGroup)
                viewModel.syncCollegeScheduleFromApi(newUrl, newGroup)
                showConfigDialog = false
            },
            onSelectProfile = { profile ->
                viewModel.selectProfile(profile)
                viewModel.syncCollegeScheduleFromApi(profile.apiUrl, profile.groupName, profile.id)
                showConfigDialog = false
            }
        )
    }

    // Edit Class Dialog
    editingClassEvent?.let { event ->
        EditCollegeClassDialog(
            event = event,
            onDismiss = { editingClassEvent = null },
            onConfirm = { updated ->
                viewModel.updateCollegeClass(updated)
                editingClassEvent = null
            }
        )
    }

    // Add Note for Class Dialog
    noteDialogClassDate?.let { date ->
        AddEditNoteDialog(
            initialDate = date,
            onDismiss = {
                noteDialogClassDate = null
                noteDialogSubject = ""
            },
            onConfirm = { title, content, noteDate, tag, color, isPinned, items, imageUri ->
                viewModel.addNote(
                    title = if (title.isNotBlank()) title else "Заметка: $noteDialogSubject",
                    content = content,
                    date = noteDate,
                    tag = tag,
                    colorHex = color,
                    isPinned = isPinned,
                    checklistItems = items,
                    imageUri = imageUri
                )
                noteDialogClassDate = null
                noteDialogSubject = ""
            }
        )
    }

    // Fullscreen Image Viewer Dialog
    fullscreenImageUri?.let { uri ->
        Dialog(onDismissRequest = { fullscreenImageUri = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Просмотр фото",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                    )

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                    ) {
                        IconButton(
                            onClick = { fullscreenImageUri = null },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedGroupedCollegeClassCard(
    groupedPair: GroupedCollegePair,
    onEditClick: (EventEntity) -> Unit,
    onImageClick: (String) -> Unit = {},
    onAddNoteClick: (EventEntity) -> Unit
) {
    val currentTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val firstLesson = groupedPair.lessons.first()

    val isLiveNow = remember(groupedPair, currentTime, todayDate) {
        if (firstLesson.date != todayDate) false
        else {
            try {
                val curMin = timeToMinutes(currentTime)
                val startMin = timeToMinutes(groupedPair.startTime)
                val endMin = timeToMinutes(groupedPair.endTime)
                curMin in startMin..endMin
            } catch (e: Exception) {
                false
            }
        }
    }

    val typeColor = when (groupedPair.primaryLessonType.lowercase()) {
        "лекция" -> Color(0xFF3B82F6)
        "практика" -> Color(0xFF10B981)
        "лабораторная", "лабораторная работа" -> Color(0xFF8B5CF6)
        "экзамен", "зачет" -> Color(0xFFEF4444)
        else -> SigeonPrimary
    }

    // Moving ambient glow animation for currently active pair
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glow_offset"
    )

    val activeGradient = if (isLiveNow) {
        Brush.linearGradient(
            colors = listOf(
                SigeonPrimary.copy(alpha = 0.12f),
                Color(0xFF8B5CF6).copy(alpha = 0.18f),
                Color(0xFFEC4899).copy(alpha = 0.12f),
                SigeonPrimary.copy(alpha = 0.12f)
            ),
            start = Offset(animatedOffset, 0f),
            end = Offset(animatedOffset + 500f, 500f)
        )
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .bounceClick(scaleDown = 0.98f) {
                if (groupedPair.lessons.isNotEmpty()) {
                    onEditClick(groupedPair.lessons.first())
                }
            }
            .testTag("college_class_card_${firstLesson.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLiveNow) Color.Transparent else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLiveNow) 4.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (activeGradient != null) Modifier.background(activeGradient) else Modifier
                )
                .padding(14.dp)
        ) {
            Column {
                // Header Row: Pair Number + Time + Live Badge + Grouped Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = typeColor,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (groupedPair.pairNumber > 0) "${groupedPair.pairNumber}" else "•",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Text(
                            text = "${groupedPair.startTime} – ${groupedPair.endTime}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isLiveNow) {
                            PulsatingLiveBadge()
                        }

                        // Display Grouped Pill if multiple parallel/subgroup lessons
                        if (groupedPair.lessons.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF8B5CF6).copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${groupedPair.lessons.size} подгруппы",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8B5CF6),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        if (groupedPair.primaryLessonType.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = typeColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = groupedPair.primaryLessonType,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = typeColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Subject Title
                Text(
                    text = groupedPair.primarySubjectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )

                // Live Lesson Progress Bar if active
                if (isLiveNow) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LiveLessonProgressBar(
                        startTimeStr = groupedPair.startTime,
                        endTimeStr = groupedPair.endTime,
                        currentTimeStr = currentTime
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subgroups / Lessons details
                if (groupedPair.lessons.size == 1) {
                    // Single lesson details inside card
                    val lesson = groupedPair.lessons.first()

                    if (!lesson.imageUri.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onImageClick(lesson.imageUri) }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = lesson.imageUri,
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
                                            text = "Фото",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (lesson.collegeRoom.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = SigeonPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = lesson.collegeRoom,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SigeonPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            if (lesson.collegeTeacher.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = lesson.collegeTeacher,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onAddNoteClick(lesson) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventNote,
                                    contentDescription = "Заметка к паре",
                                    tint = SigeonPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = { onEditClick(lesson) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Редактировать пару",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Multiple subgroups combined into one window card!
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        groupedPair.lessons.forEachIndexed { index, subgroupLesson ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = SigeonPrimary.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "${index + 1}-я подгруппа",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SigeonPrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }

                                            if (subgroupLesson.collegeSubject.isNotBlank() &&
                                                subgroupLesson.collegeSubject != groupedPair.primarySubjectName
                                            ) {
                                                Text(
                                                    text = subgroupLesson.collegeSubject,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onAddNoteClick(subgroupLesson) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.EventNote,
                                                    contentDescription = "Заметка к подгруппе",
                                                    tint = SigeonPrimary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { onEditClick(subgroupLesson) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Редактировать подгруппу",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (subgroupLesson.collegeRoom.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    tint = SigeonPrimary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = subgroupLesson.collegeRoom,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SigeonPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        if (subgroupLesson.collegeTeacher.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = subgroupLesson.collegeTeacher,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }

                                    if (!subgroupLesson.imageUri.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(60.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onImageClick(subgroupLesson.imageUri) }
                                        ) {
                                            AsyncImage(
                                                model = subgroupLesson.imageUri,
                                                contentDescription = "Фото подгруппы",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun timeToMinutes(timeStr: String): Int {
    return try {
        val parts = timeStr.trim().split(":")
        parts[0].toInt() * 60 + parts[1].toInt()
    } catch (e: Exception) {
        0
    }
}
