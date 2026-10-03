package com.example.ui.college

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.CollegeProfile
import com.example.ui.theme.SigeonPrimary

@Composable
fun CollegeApiConfigDialog(
    currentProfile: CollegeProfile = CollegeProfile(
        id = "custom",
        collegeName = "Пользовательский",
        groupName = "",
        apiUrl = "",
        description = ""
    ),
    allProfiles: List<CollegeProfile> = emptyList(),
    currentApiUrl: String,
    currentGroupName: String,
    profiles: List<CollegeProfile> = allProfiles,
    onDismiss: () -> Unit,
    onSave: (apiUrl: String, groupName: String) -> Unit = { _, _ -> },
    onSelectProfile: (CollegeProfile) -> Unit = {},
    onConfirm: (profile: CollegeProfile, apiUrl: String, groupName: String) -> Unit = { p, u, g -> }
) {
    val profilesList = if (allProfiles.isNotEmpty()) allProfiles else profiles
    var selectedProfile by remember {
        mutableStateOf(profilesList.firstOrNull { it.id == currentProfile.id } ?: profilesList.firstOrNull() ?: currentProfile)
    }
    var apiUrl by remember { mutableStateOf(currentApiUrl) }
    var groupName by remember { mutableStateOf(currentGroupName) }
    var planovoGroupIdInput by remember {
        mutableStateOf(
            if (currentApiUrl.contains("/groups/")) currentApiUrl.substringAfter("/groups/").substringBefore("/") else "41"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = SigeonPrimary
                )
                Text(
                    text = "Синхронизация Planovo & API",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Выберите учебное заведение или укажите ссылку на Planovo (.ics) / API:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Profiles list (Planovo, КИТ, etc.)
                profilesList.forEach { profile ->
                    val isSelected = selectedProfile.id == profile.id
                    val isPlanovo = profile.apiUrl.contains("planovo.pro")
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedProfile = profile
                                apiUrl = profile.apiUrl
                                groupName = profile.groupName
                            }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) SigeonPrimary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        color = if (isSelected) SigeonPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SigeonPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Box(modifier = Modifier.size(20.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile.collegeName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    if (isPlanovo) {
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "iCal .ICS",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981),
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Группа: ${profile.groupName}",
                                    fontSize = 12.sp,
                                    color = SigeonPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = profile.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Quick Planovo ID Generator
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Быстрая настройка Planovo по ID группы:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = planovoGroupIdInput,
                                onValueChange = {
                                    planovoGroupIdInput = it
                                    if (it.isNotBlank()) {
                                        apiUrl = "https://planovo.pro/api/v1/public/groups/${it.trim()}/calendar.ics"
                                    }
                                },
                                label = { Text("ID группы Planovo (число)", maxLines = 1, softWrap = false) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Full URL & Group Inputs
                Text(
                    text = "Параметры ссылки и группы:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Название группы", maxLines = 1, softWrap = false) },
                    modifier = Modifier.fillMaxWidth().testTag("api_group_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    maxLines = 1
                )

                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it },
                    label = { Text("URL расписания (Planovo .ics или JSON)", maxLines = 1, softWrap = false) },
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null, tint = SigeonPrimary)
                    },
                    supportingText = {
                        Text("Поддерживаются: Planovo calendar.ics, 1C:Колледж, Modeus", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("api_url_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    maxLines = 1
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(apiUrl.trim(), groupName.trim())
                    onSelectProfile(selectedProfile)
                    onConfirm(selectedProfile, apiUrl.trim(), groupName.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = SigeonPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("apply_api_config_button")
            ) {
                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Синхронизировать", maxLines = 1, softWrap = false)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", maxLines = 1, softWrap = false)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
