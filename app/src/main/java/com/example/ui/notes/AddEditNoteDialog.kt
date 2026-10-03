package com.example.ui.notes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ChecklistItem
import com.example.data.local.NoteEntity
import com.example.ui.theme.SigeonPrimary
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditNoteDialog(
    initialDate: String? = null,
    existingNote: NoteEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        content: String,
        date: String?,
        tag: String,
        colorHex: String,
        isPinned: Boolean,
        checklistItems: List<ChecklistItem>,
        imageUri: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf(existingNote?.title ?: "") }
    var content by remember { mutableStateOf(existingNote?.content ?: "") }
    var attachToDate by remember { mutableStateOf(existingNote?.date != null || initialDate != null) }
    var date by remember { mutableStateOf(existingNote?.date ?: initialDate ?: "") }
    var isPinned by remember { mutableStateOf(existingNote?.isPinned ?: false) }
    var attachedImageUri by remember { mutableStateOf(existingNote?.imageUri) }

    val tags = listOf("Учеба", "Д/З", "Экзамен", "Личное", "Важное")
    var selectedTag by remember { mutableStateOf(existingNote?.tag ?: "Учеба") }

    val colors = listOf(
        "#6366F1", // Indigo
        "#10B981", // Emerald
        "#F59E0B", // Amber
        "#EC4899", // Pink
        "#8B5CF6", // Violet
        "#06B6D4"  // Cyan
    )
    var selectedColor by remember { mutableStateOf(existingNote?.colorHex ?: colors[0]) }
    var titleError by remember { mutableStateOf(false) }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val savedPath = ImageStorageHelper.saveImageToInternalStorage(context, uri)
                if (savedPath != null) {
                    attachedImageUri = savedPath
                }
            }
        }
    }

    // Checklist items state
    val checklistItems = remember {
        mutableStateListOf<ChecklistItem>().apply {
            if (existingNote != null) {
                addAll(existingNote.getChecklistItems())
            }
        }
    }
    var newChecklistText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingNote != null) "Редактировать заметку" else "Новая заметка",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = { isPinned = !isPinned }) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Закрепить",
                        tint = if (isPinned) SigeonPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("Заголовок заметки *", maxLines = 1) },
                    isError = titleError,
                    supportingText = if (titleError) { { Text("Введите заголовок", maxLines = 1) } } else null,
                    modifier = Modifier.fillMaxWidth().testTag("note_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Описание / текст...", maxLines = 1) },
                    modifier = Modifier.fillMaxWidth().testTag("note_content_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 6
                )

                // Attached image section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Прикрепленное фото:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (attachedImageUri != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = attachedImageUri,
                                    contentDescription = "Прикрепленное изображение",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                ) {
                                    IconButton(
                                        onClick = { attachedImageUri = null },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Удалить фото",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("add_note_photo_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Прикрепить картинку / фото", maxLines = 1)
                        }
                    }
                }

                // Checklist Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = null,
                        tint = SigeonPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Чек-лист / Задачи (${checklistItems.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                // Checklist items list
                checklistItems.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.isDone,
                            onCheckedChange = { isChecked ->
                                checklistItems[index] = item.copy(isDone = isChecked)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = SigeonPrimary)
                        )
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = { checklistItems.removeAt(index) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Удалить пункт",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Add item field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newChecklistText,
                        onValueChange = { newChecklistText = it },
                        placeholder = { Text("Новый пункт задания...", maxLines = 1) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (newChecklistText.isNotBlank()) {
                                checklistItems.add(ChecklistItem(text = newChecklistText.trim()))
                                newChecklistText = ""
                            }
                        },
                        enabled = newChecklistText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = SigeonPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }

                // Category Tag Selector
                Text(
                    text = "Категория:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tags.forEach { tag ->
                        val isSelected = selectedTag == tag
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTag = tag },
                            label = { Text(tag, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SigeonPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = SigeonPrimary
                            )
                        )
                    }
                }

                // Date attachment toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = SigeonPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Привязать к дате",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }
                    Switch(
                        checked = attachToDate,
                        onCheckedChange = { attachToDate = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = SigeonPrimary)
                    )
                }

                if (attachToDate) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Дата (ГГГГ-ММ-ДД)", maxLines = 1) },
                        modifier = Modifier.fillMaxWidth().testTag("note_date_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Color picker
                Text(
                    text = "Цвет карточки:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEach { hex ->
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { selectedColor = hex }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    onConfirm(
                        title.trim(),
                        content.trim(),
                        if (attachToDate && date.isNotBlank()) date.trim() else null,
                        selectedTag,
                        selectedColor,
                        isPinned,
                        checklistItems.toList(),
                        attachedImageUri
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SigeonPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_note_button")
            ) {
                Text("Сохранить", maxLines = 1)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", maxLines = 1)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
