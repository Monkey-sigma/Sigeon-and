package com.example.ui.college

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.data.api.PlanovoGroup
import com.example.data.api.PlanovoGroupsCatalog
import com.example.ui.components.bounceClick
import com.example.ui.theme.SigeonPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSelectorBottomSheet(
    currentGroupId: Int,
    onDismiss: () -> Unit,
    onGroupSelected: (PlanovoGroup) -> Unit,
    onCustomGroupSubmit: (groupId: Int, groupCode: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedCourseFilter by remember { mutableStateOf<Int?>(null) } // null = All
    var showCustomIdInput by remember { mutableStateOf(false) }
    var customIdText by remember { mutableStateOf("") }

    val allGroups = remember { PlanovoGroupsCatalog.bundledGroups }

    val filteredGroups = remember(allGroups, searchQuery, selectedCourseFilter) {
        allGroups.filter { group ->
            val matchesQuery = searchQuery.isBlank() ||
                group.code.contains(searchQuery, ignoreCase = true) ||
                group.id.toString() == searchQuery.trim() ||
                group.direction.contains(searchQuery, ignoreCase = true)

            val matchesCourse = selectedCourseFilter == null || group.course == selectedCourseFilter
            matchesQuery && matchesCourse
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SigeonPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = SigeonPrimary
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Выбор учебной группы",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Planovo • Расписание загрузится сразу",
                            fontSize = 12.sp,
                            color = SigeonPrimary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${filteredGroups.size} групп",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск группы (ои31, 41, э21...)") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = SigeonPrimary)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("group_search_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Course filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCourseFilter == null,
                        onClick = { selectedCourseFilter = null },
                        label = { Text("Все курсы") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SigeonPrimary.copy(alpha = 0.18f),
                            selectedLabelColor = SigeonPrimary
                        )
                    )
                }
                listOf(1, 2, 3, 4).forEach { course ->
                    item {
                        FilterChip(
                            selected = selectedCourseFilter == course,
                            onClick = {
                                selectedCourseFilter = if (selectedCourseFilter == course) null else course
                            },
                            label = { Text("$course курс") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SigeonPrimary.copy(alpha = 0.18f),
                                selectedLabelColor = SigeonPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Custom ID accordion toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showCustomIdInput = !showCustomIdInput }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showCustomIdInput) "− Скрыть ручной ввод ID" else "+ Ввести другой ID группы Planovo вручную",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SigeonPrimary
                )
            }

            AnimatedVisibility(visible = showCustomIdInput) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customIdText,
                        onValueChange = { customIdText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("ID группы (число)") },
                        placeholder = { Text("41") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val id = customIdText.toIntOrNull()
                            if (id != null) {
                                onCustomGroupSubmit(id, "Группа #$id")
                            }
                        },
                        enabled = customIdText.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SigeonPrimary)
                    ) {
                        Text("Применить")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Groups List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredGroups, key = { it.id }) { group ->
                    val isSelected = group.id == currentGroupId
                    GroupItemRow(
                        group = group,
                        isSelected = isSelected,
                        onClick = { onGroupSelected(group) }
                    )
                }
            }
        }
    }
}

@Composable
fun GroupItemRow(
    group: PlanovoGroup,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .bounceClick(scaleDown = 0.96f) { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) SigeonPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) SigeonPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = group.code,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SigeonPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${group.course} курс",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SigeonPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "ID: ${group.id}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = group.direction,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Выбрано",
                    tint = SigeonPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
