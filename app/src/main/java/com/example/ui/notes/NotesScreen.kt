package com.example.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.local.NoteEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.SigeonPrimary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allNotes by viewModel.allNotes.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf("Все") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var fullscreenImageUri by remember { mutableStateOf<String?>(null) }

    val tags = listOf("Все", "Учеба", "Пара", "Д/З", "Экзамен", "Личное", "Важное")

    val filteredNotes = remember(allNotes, searchQuery, selectedTagFilter) {
        allNotes.filter { note ->
            val matchesQuery = searchQuery.isBlank() ||
                    note.title.contains(searchQuery, ignoreCase = true) ||
                    note.content.contains(searchQuery, ignoreCase = true) ||
                    note.collegeSubject.contains(searchQuery, ignoreCase = true)
            val matchesTag = when (selectedTagFilter) {
                "Все" -> true
                "Пара" -> note.collegePairNumber > 0 || note.collegeSubject.isNotBlank()
                else -> note.tag == selectedTagFilter
            }
            matchesQuery && matchesTag
        }
    }

    val pinnedNotes = remember(filteredNotes) { filteredNotes.filter { it.isPinned } }
    val regularNotes = remember(filteredNotes) { filteredNotes.filter { !it.isPinned } }

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
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Заметки & Задания",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SigeonPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_note_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Добавить заметку")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск заметок или предметов...", maxLines = 1, softWrap = false) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить поиск")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("notes_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    maxLines = 1
                )
            }

            // Category Filter Chips
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    tags.forEach { tag ->
                        val isSelected = selectedTagFilter == tag
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTagFilter = tag },
                            label = { Text(tag, maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SigeonPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = SigeonPrimary
                            )
                        )
                    }
                }
            }

            // Empty State
            if (filteredNotes.isEmpty()) {
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
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Заметок пока нет",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Нажмите +, чтобы добавить заметку к паре или задаче",
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

            // Pinned Notes
            if (pinnedNotes.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = SigeonPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Закрепленные",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SigeonPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                items(pinnedNotes, key = { "pinned_${it.id}" }) { note ->
                    NoteCardItem(
                        note = note,
                        onPinToggle = { viewModel.togglePinNote(note) },
                        onEdit = { editingNote = note },
                        onDelete = { viewModel.deleteNote(note) },
                        onImageClick = { fullscreenImageUri = it },
                        onToggleChecklistItem = { itemId, isDone ->
                            viewModel.toggleChecklistItem(note, itemId, isDone)
                        }
                    )
                }
            }

            // Regular Notes
            if (regularNotes.isNotEmpty()) {
                if (pinnedNotes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Все заметки",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                        )
                    }
                }

                items(regularNotes, key = { "note_${it.id}" }) { note ->
                    NoteCardItem(
                        note = note,
                        onPinToggle = { viewModel.togglePinNote(note) },
                        onEdit = { editingNote = note },
                        onDelete = { viewModel.deleteNote(note) },
                        onImageClick = { fullscreenImageUri = it },
                        onToggleChecklistItem = { itemId, isDone ->
                            viewModel.toggleChecklistItem(note, itemId, isDone)
                        }
                    )
                }
            }
        }
    }

    // Fullscreen Image Viewer Dialog
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
                        contentDescription = "Фото заметки",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().height(350.dp).clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        }
    }

    // Add Note Dialog
    if (showAddDialog) {
        AddEditNoteDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, content, date, tag, color, isPinned, items, imageUri, slotId, pairNum, subj ->
                viewModel.addNote(
                    title = title,
                    content = content,
                    date = date,
                    tag = tag,
                    colorHex = color,
                    isPinned = isPinned,
                    checklistItems = items,
                    imageUri = imageUri,
                    classSlotId = slotId,
                    collegePairNumber = pairNum,
                    collegeSubject = subj
                )
                showAddDialog = false
            }
        )
    }

    // Edit Note Dialog
    if (editingNote != null) {
        AddEditNoteDialog(
            existingNote = editingNote,
            onDismiss = { editingNote = null },
            onConfirm = { title, content, date, tag, color, isPinned, items, imageUri, slotId, pairNum, subj ->
                viewModel.updateNote(
                    editingNote!!.copy(
                        title = title,
                        content = content,
                        date = date,
                        tag = tag,
                        colorHex = color,
                        isPinned = isPinned,
                        checklistJson = NoteEntity.serializeChecklist(items),
                        imageUri = imageUri,
                        classSlotId = slotId,
                        collegePairNumber = pairNum,
                        collegeSubject = subj
                    )
                )
                editingNote = null
            }
        )
    }
}

@Composable
fun NoteCardItem(
    note: NoteEntity,
    onPinToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onImageClick: (String) -> Unit,
    onToggleChecklistItem: (itemId: String, isDone: Boolean) -> Unit
) {
    val checklist = remember(note.checklistJson) { note.getChecklistItems() }
    val completedCount = checklist.count { it.isDone }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEdit() }
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category Tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(android.graphics.Color.parseColor(note.colorHex)).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = note.tag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(android.graphics.Color.parseColor(note.colorHex)),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    // Class Slot Association Pill
                    if (note.collegePairNumber > 0 || note.collegeSubject.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SigeonPrimary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = SigeonPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (note.collegePairNumber > 0) "Пара №${note.collegePairNumber}" else note.collegeSubject,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SigeonPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (checklist.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (completedCount == checklist.size) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "$completedCount/${checklist.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                color = if (completedCount == checklist.size) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    IconButton(onClick = onPinToggle, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Закрепить",
                            tint = if (note.isPinned) SigeonPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            }

            // Display attached image thumbnail if available
            note.imageUri?.let { imgPath ->
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
                        contentDescription = "Фото к заметке",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Interactive Checklists rendered directly on Card
            if (checklist.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    checklist.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onToggleChecklistItem(item.id, !item.isDone) }
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = item.isDone,
                                onCheckedChange = { onToggleChecklistItem(item.id, it) },
                                colors = CheckboxDefaults.colors(checkedColor = SigeonPrimary),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.text,
                                fontSize = 13.sp,
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

            if (note.date != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = SigeonPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Дата: ${note.date}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SigeonPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
