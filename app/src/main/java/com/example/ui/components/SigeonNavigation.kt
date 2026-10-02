package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SigeonPrimary
import com.example.ui.theme.SigeonPrimaryLight

enum class NavDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    CALENDAR(
        route = "calendar",
        title = "Календарь",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        tag = "nav_calendar"
    ),
    COLLEGE(
        route = "college",
        title = "Расписание",
        selectedIcon = Icons.Filled.School,
        unselectedIcon = Icons.Outlined.School,
        tag = "nav_college"
    ),
    NOTES(
        route = "notes",
        title = "Заметки",
        selectedIcon = Icons.Filled.EditNote,
        unselectedIcon = Icons.Outlined.EditNote,
        tag = "nav_notes"
    ),
    SYNC(
        route = "sync",
        title = "Синхронизация",
        selectedIcon = Icons.Filled.Sync,
        unselectedIcon = Icons.Outlined.Sync,
        tag = "nav_sync"
    )
}

@Composable
fun SigeonBottomNavBar(
    currentDestination: NavDestination,
    onNavigate: (NavDestination) -> Unit,
    collegeClassCount: Int = 0,
    notesCount: Int = 0,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        windowInsets = WindowInsets.navigationBars,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavDestination.values().forEach { destination ->
            val isSelected = currentDestination == destination
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(destination) },
                icon = {
                    val icon = if (isSelected) destination.selectedIcon else destination.unselectedIcon
                    val badgeCount = when (destination) {
                        NavDestination.COLLEGE -> collegeClassCount
                        NavDestination.NOTES -> notesCount
                        else -> 0
                    }

                    if (badgeCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = SigeonPrimary) {
                                    Text("$badgeCount")
                                }
                            }
                        ) {
                            Icon(icon, contentDescription = destination.title)
                        }
                    } else {
                        Icon(icon, contentDescription = destination.title)
                    }
                },
                label = { Text(destination.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SigeonPrimary,
                    selectedTextColor = SigeonPrimary,
                    indicatorColor = SigeonPrimaryLight
                ),
                modifier = Modifier.testTag(destination.tag)
            )
        }
    }
}
