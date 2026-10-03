package com.example.ui.college

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.local.EventEntity
import com.example.ui.theme.SigeonPrimary
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.launch

@Composable
fun EditCollegeClassDialog(
    event: EventEntity,
    onDismiss: () -> Unit,
    onConfirm: (updatedEvent: EventEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var subject by remember { mutableStateOf(event.collegeSubject.ifBlank { event.title }) }
    var teacher by remember { mutableStateOf(event.collegeTeacher) }
    var room by remember { mutableStateOf(event.collegeRoom.ifBlank { event.location }) }
    var startTime by remember { mutableStateOf(event.startTime) }
    var endTime by remember { mutableStateOf(event.endTime) }
    var lessonType by remember { mutableStateOf(event.collegeLessonType.ifBlank { "Пара" }) }
    var pairNumber by remember { mutableStateOf(event.collegePairNumber.toString()) }
    var attachedImageUri by remember { mutableStateOf(event.imageUri) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val localPath = ImageStorageHelper.saveImageToInternalStorage(context, uri)
                attachedImageUri = localPath
            }
        }
    }

    val lessonTypes = listOf("Лекция", "Практика", "Лабораторная", "Семинар", "Зачет", "Экзамен")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = SigeonPrimary
                )
                Text(
                    text = "Редактировать пару (Офлайн)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Название предмета / дисциплины", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                        Icon(Icons.Default.School, contentDescription = null, tint = SigeonPrimary)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("edit_subject_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    maxLines = 1
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Кабинет / Ауд.", maxLines = 1) },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                        },
                        modifier = Modifier.weight(1f).testTag("edit_room_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        maxLines = 1
                    )

                    OutlinedTextField(
                        value = pairNumber,
                        onValueChange = { pairNumber = it },
                        label = { Text("Номер пары", maxLines = 1) },
                        modifier = Modifier.weight(1f).testTag("edit_pair_number_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        maxLines = 1
                    )
                }

                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("ФИО Преподавателя", maxLines = 1) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("edit_teacher_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    maxLines = 1
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Начало", maxLines = 1) },
                        leadingIcon = {
                            Icon(Icons.Default.AccessTime, contentDescription = null)
                        },
                        modifier = Modifier.weight(1f).testTag("edit_start_time_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        maxLines = 1
                    )

                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Конец", maxLines = 1) },
                        leadingIcon = {
                            Icon(Icons.Default.AccessTime, contentDescription = null)
                        },
                        modifier = Modifier.weight(1f).testTag("edit_end_time_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        maxLines = 1
                    )
                }

                Text(
                    text = "Тип занятия:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    lessonTypes.take(3).forEach { type ->
                        val isSelected = lessonType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { lessonType = type },
                            label = { Text(type, fontSize = 11.sp, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SigeonPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = SigeonPrimary
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    lessonTypes.drop(3).forEach { type ->
                        val isSelected = lessonType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { lessonType = type },
                            label = { Text(type, fontSize = 11.sp, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SigeonPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = SigeonPrimary
                            )
                        )
                    }
                }

                // Custom Picture / Attachment for College Class
                Text(
                    text = "Свое фото / Картинка для пары:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                if (attachedImageUri != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = attachedImageUri,
                                contentDescription = "Фото предмета",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AddPhotoAlternate,
                                            contentDescription = "Заменить фото",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    IconButton(
                                        onClick = { attachedImageUri = null },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Удалить фото",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Загрузить свое фото для пары", fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pairNum = pairNumber.toIntOrNull() ?: event.collegePairNumber
                    val updated = event.copy(
                        title = "$pairNum. ${subject.trim()}",
                        collegeSubject = subject.trim(),
                        collegeTeacher = teacher.trim(),
                        collegeRoom = room.trim(),
                        location = room.trim(),
                        startTime = startTime.trim(),
                        endTime = endTime.trim(),
                        collegeLessonType = lessonType,
                        collegePairNumber = pairNum,
                        imageUri = attachedImageUri,
                        description = "Кабинет: ${room.trim()}, Преподаватель: ${teacher.trim()}"
                    )
                    onConfirm(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SigeonPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_edit_class_button")
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
