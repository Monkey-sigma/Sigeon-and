package com.example.ui.college

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.EventEntity
import com.example.ui.theme.SigeonPrimary

@Composable
fun EditCollegeClassDialog(
    event: EventEntity,
    onDismiss: () -> Unit,
    onConfirm: (updatedEvent: EventEntity) -> Unit
) {
    var subject by remember { mutableStateOf(event.collegeSubject.ifBlank { event.title }) }
    var teacher by remember { mutableStateOf(event.collegeTeacher) }
    var room by remember { mutableStateOf(event.collegeRoom.ifBlank { event.location }) }
    var startTime by remember { mutableStateOf(event.startTime) }
    var endTime by remember { mutableStateOf(event.endTime) }
    var lessonType by remember { mutableStateOf(event.collegeLessonType.ifBlank { "Пара" }) }
    var pairNumber by remember { mutableStateOf(event.collegePairNumber.toString()) }

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
                    fontSize = 18.sp
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
                    label = { Text("Название предмета / дисциплины") },
                    leadingIcon = {
                        Icon(Icons.Default.School, contentDescription = null, tint = SigeonPrimary)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("edit_subject_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Кабинет / Ауд.") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                        },
                        modifier = Modifier.weight(1f).testTag("edit_room_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = pairNumber,
                        onValueChange = { pairNumber = it },
                        label = { Text("Номер пары") },
                        modifier = Modifier.weight(1f).testTag("edit_pair_number_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("ФИО Преподавателя") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("edit_teacher_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Начало") },
                        leadingIcon = {
                            Icon(Icons.Default.AccessTime, contentDescription = null)
                        },
                        modifier = Modifier.weight(1f).testTag("edit_start_time_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Конец") },
                        leadingIcon = {
                            Icon(Icons.Default.AccessTime, contentDescription = null)
                        },
                        modifier = Modifier.weight(1f).testTag("edit_end_time_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Text(
                    text = "Тип занятия:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            label = { Text(type, fontSize = 11.sp) },
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
                            label = { Text(type, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SigeonPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = SigeonPrimary
                            )
                        )
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
                        description = "Кабинет: ${room.trim()}, Преподаватель: ${teacher.trim()}"
                    )
                    onConfirm(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SigeonPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_edit_class_button")
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
