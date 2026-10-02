package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    var currentDestination by remember { mutableStateOf(NavDestination.CALENDAR) }
    val snackbarHostState = remember { SnackbarHostState() }
    val statusMessage by viewModel.statusMessage.collectAsState()
    val collegeClasses by viewModel.collegeSchedule.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = currentDestination,
                label = "screen_crossfade"
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
