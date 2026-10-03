package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.calendar.CalendarScreen
import com.example.ui.college.CollegeScheduleScreen
import com.example.ui.components.NavDestination
import com.example.ui.components.SigeonBottomNavBar
import com.example.ui.notes.NotesScreen
import com.example.ui.settings.SyncSettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SigeonApp()
            }
        }
    }
}

@Composable
fun SigeonApp(viewModel: MainViewModel = viewModel()) {
    var currentDestination by remember { mutableStateOf(NavDestination.COLLEGE) } // Immediately show schedule
    val snackbarHostState = remember { SnackbarHostState() }
    val isOnline by viewModel.isOnline.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val collegeClasses by viewModel.collegeSchedule.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()

    // Handle system back navigation to return to College schedule tab
    BackHandler(enabled = currentDestination != NavDestination.COLLEGE) {
        currentDestination = NavDestination.COLLEGE
    }

    // Show status messages / feedback toasts
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.dismissStatusMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            SigeonBottomNavBar(
                currentDestination = currentDestination,
                onNavigate = { currentDestination = it },
                collegeClassCount = collegeClasses.size,
                notesCount = allNotes.size
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Animated Offline Banner
            AnimatedVisibility(
                visible = !isOnline,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Офлайн-режим • Расписание и заметки доступны локально",
                            color = Color.White,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                AnimatedContent(
                    targetState = currentDestination,
                    transitionSpec = {
                        val targetIndex = targetState.ordinal
                        val initialIndex = initialState.ordinal
                        val offsetSpec = spring<androidx.compose.ui.unit.IntOffset>(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                        val floatSpec = spring<Float>(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                        if (targetIndex >= initialIndex) {
                            (slideInHorizontally(offsetSpec) { width -> width / 4 } + fadeIn(floatSpec))
                                .togetherWith(slideOutHorizontally(offsetSpec) { width -> -width / 4 } + fadeOut(floatSpec))
                        } else {
                            (slideInHorizontally(offsetSpec) { width -> -width / 4 } + fadeIn(floatSpec))
                                .togetherWith(slideOutHorizontally(offsetSpec) { width -> width / 4 } + fadeOut(floatSpec))
                        }
                    },
                    label = "tab_animated_content"
                ) { destination ->
                    when (destination) {
                        NavDestination.CALENDAR -> CalendarScreen(viewModel = viewModel)
                        NavDestination.COLLEGE -> CollegeScheduleScreen(viewModel = viewModel)
                        NavDestination.NOTES -> NotesScreen(viewModel = viewModel)
                        NavDestination.SYNC -> SyncSettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
