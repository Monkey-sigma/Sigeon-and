package com.example.ui

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.CollegeProfile
import com.example.data.api.CollegeScheduleFetcher
import com.example.data.local.AppDatabase
import com.example.data.local.EventEntity
import com.example.data.local.NoteEntity
import com.example.data.repository.CalendarRepository
import com.example.data.sync.DeviceCalendar
import com.example.data.sync.GoogleCalendarSyncManager
import com.example.widget.CollegeScheduleWidgetProvider
import com.example.widget.WidgetUpdateHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class EventFilter(val label: String) {
    ALL("Все"),
    COLLEGE("Пары"),
    NOTES("Заметки"),
    EVENTS("События")
}

data class CalendarMonth(
    val year: Int,
    val month: Int // 0-based
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val fetcher = CollegeScheduleFetcher()
    private val syncManager = GoogleCalendarSyncManager(application)
    val repository = CalendarRepository(db.calendarDao(), fetcher, syncManager)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Selected date
    private val _selectedDate = MutableStateFlow(dateFormat.format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Current calendar viewing month
    private val _currentMonth = MutableStateFlow(
        Calendar.getInstance().let { CalendarMonth(it.get(Calendar.YEAR), it.get(Calendar.MONTH)) }
    )
    val currentMonth: StateFlow<CalendarMonth> = _currentMonth.asStateFlow()

    // Active filter on Day agenda
    private val _activeFilter = MutableStateFlow(EventFilter.ALL)
    val activeFilter: StateFlow<EventFilter> = _activeFilter.asStateFlow()

    // Status & notifications
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Available calendars & permissions
    private val _hasCalendarPermission = MutableStateFlow(syncManager.hasCalendarPermissions())
    val hasCalendarPermission: StateFlow<Boolean> = _hasCalendarPermission.asStateFlow()

    private val _availableCalendars = MutableStateFlow<List<DeviceCalendar>>(emptyList())
    val availableCalendars: StateFlow<List<DeviceCalendar>> = _availableCalendars.asStateFlow()

    private val _selectedCalendarId = MutableStateFlow<Long?>(null)
    val selectedCalendarId: StateFlow<Long?> = _selectedCalendarId.asStateFlow()

    // College profile config
    private val _selectedProfile = MutableStateFlow(fetcher.defaultProfiles.first())
    val selectedProfile: StateFlow<CollegeProfile> = _selectedProfile.asStateFlow()

    private val _apiUrlInput = MutableStateFlow(fetcher.defaultProfiles.first().apiUrl)
    val apiUrlInput: StateFlow<String> = _apiUrlInput.asStateFlow()

    private val _groupNameInput = MutableStateFlow(fetcher.defaultProfiles.first().groupName)
    val groupNameInput: StateFlow<String> = _groupNameInput.asStateFlow()

    // Database reactive flows
    val allEvents: StateFlow<List<EventEntity>> = repository.allEvents.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val collegeSchedule: StateFlow<List<EventEntity>> = repository.getCollegeSchedule().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Events for selected date
    val eventsForSelectedDate: StateFlow<List<EventEntity>> = combine(allEvents, _selectedDate) { events, date ->
        events.filter { it.date == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notes for selected date
    val notesForSelectedDate: StateFlow<List<NoteEntity>> = combine(allNotes, _selectedDate) { notes, date ->
        notes.filter { it.date == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatically check if initial schedule exists, if not sync college schedule
        viewModelScope.launch {
            checkPermissionsAndLoadCalendars()
            // If database is empty, load initial schedule for smooth first experience
            repository.allEvents.collect { list ->
                if (list.isEmpty()) {
                    syncCollegeScheduleFromApi(
                        apiUrl = _selectedProfile.value.apiUrl,
                        groupName = _selectedProfile.value.groupName,
                        profileId = _selectedProfile.value.id
                    )
                }
            }
        }
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
        // Update current month if needed
        try {
            val parts = date.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1
            _currentMonth.value = CalendarMonth(year, month)
        } catch (_: Exception) {}
    }

    fun jumpToToday() {
        val todayStr = dateFormat.format(Date())
        selectDate(todayStr)
    }

    fun changeMonth(delta: Int) {
        val current = _currentMonth.value
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, current.year)
            set(Calendar.MONTH, current.month)
            add(Calendar.MONTH, delta)
        }
        _currentMonth.value = CalendarMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun setFilter(filter: EventFilter) {
        _activeFilter.value = filter
    }

    fun setApiUrl(url: String) {
        _apiUrlInput.value = url
    }

    fun setGroupName(group: String) {
        _groupNameInput.value = group
    }

    fun selectProfile(profile: CollegeProfile) {
        _selectedProfile.value = profile
        _apiUrlInput.value = profile.apiUrl
        _groupNameInput.value = profile.groupName
    }

    fun setSelectedCalendarId(id: Long?) {
        _selectedCalendarId.value = id
    }

    // CRUD Events
    fun addEvent(
        title: String,
        description: String,
        date: String,
        startTime: String,
        endTime: String,
        location: String,
        colorHex: String,
        type: String = "EVENT"
    ) {
        viewModelScope.launch {
            val event = EventEntity(
                title = title,
                description = description,
                date = date,
                startTime = startTime,
                endTime = endTime,
                location = location,
                colorHex = colorHex,
                type = type
            )
            val newId = repository.addEvent(event)
            _statusMessage.value = "Событие добавлено"
            WidgetUpdateHelper.updateAllWidgets(getApplication())

            // If Google Calendar permission is granted and user has calendar selected, optionally export
            if (_hasCalendarPermission.value && _selectedCalendarId.value != null) {
                repository.exportEventToGoogleCalendar(event.copy(id = newId), _selectedCalendarId.value)
            }
        }
    }

    fun deleteEvent(event: EventEntity) {
        viewModelScope.launch {
            repository.deleteEvent(event)
            _statusMessage.value = "Событие удалено"
            WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    // CRUD Notes
    fun addNote(
        title: String,
        content: String,
        date: String?,
        tag: String,
        colorHex: String,
        isPinned: Boolean = false
    ) {
        viewModelScope.launch {
            val note = NoteEntity(
                title = title,
                content = content,
                date = date,
                tag = tag,
                colorHex = colorHex,
                isPinned = isPinned
            )
            repository.addNote(note)
            _statusMessage.value = "Заметка сохранена"
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
            _statusMessage.value = "Заметка обновлена"
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            _statusMessage.value = "Заметка удалена"
        }
    }

    fun togglePinNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.togglePinNote(note.id, !note.isPinned)
        }
    }

    // College API Sync
    fun syncCollegeScheduleFromApi(
        apiUrl: String = _apiUrlInput.value,
        groupName: String = _groupNameInput.value,
        profileId: String? = _selectedProfile.value.id
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            _statusMessage.value = "Загрузка расписания колледжа по API..."
            val result = repository.syncCollegeSchedule(apiUrl, groupName, profileId)
            _isSyncing.value = false
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _statusMessage.value = "Расписание колледжа обновлено ($count пар)"
                WidgetUpdateHelper.updateAllWidgets(getApplication())
            } else {
                _statusMessage.value = "Ошибка: ${result.exceptionOrNull()?.message ?: "Сбой соединения"}"
            }
        }
    }

    fun pinScheduleWidget() {
        val app = getApplication<Application>()
        val appWidgetManager = AppWidgetManager.getInstance(app)
        val myProvider = ComponentName(app, CollegeScheduleWidgetProvider::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                appWidgetManager.requestPinAppWidget(myProvider, null, null)
                _statusMessage.value = "Запрос на добавление виджета отправлен на рабочий стол"
            } else {
                _statusMessage.value = "Добавьте виджет через долгое нажатие на рабочем столе"
            }
        } else {
            _statusMessage.value = "Добавьте виджет через меню виджетов рабочего стола"
        }
    }

    // Google Calendar Sync
    fun checkPermissionsAndLoadCalendars() {
        val granted = repository.hasCalendarPermissions()
        _hasCalendarPermission.value = granted
        if (granted) {
            viewModelScope.launch {
                val cals = repository.getAvailableGoogleCalendars()
                _availableCalendars.value = cals
                if (_selectedCalendarId.value == null && cals.isNotEmpty()) {
                    _selectedCalendarId.value = cals.firstOrNull { it.isGoogle }?.id ?: cals.first().id
                }
            }
        }
    }

    fun importFromGoogleCalendar() {
        viewModelScope.launch {
            _isSyncing.value = true
            _statusMessage.value = "Импорт событий из Google Календаря..."
            val count = repository.importGoogleCalendarEvents()
            _isSyncing.value = false
            _statusMessage.value = "Импортировано из Google: $count событий"
        }
    }

    fun exportCollegeScheduleToGoogle() {
        viewModelScope.launch {
            _isSyncing.value = true
            _statusMessage.value = "Синхронизация расписания в Google Календарь..."
            val result = repository.exportCollegeScheduleToGoogle(_selectedCalendarId.value)
            _isSyncing.value = false
            _statusMessage.value = result.message
        }
    }

    fun exportEventToGoogle(event: EventEntity) {
        viewModelScope.launch {
            val res = repository.exportEventToGoogleCalendar(event, _selectedCalendarId.value)
            if (res != null) {
                _statusMessage.value = "Событие добавлено в Google Календарь"
            } else {
                _statusMessage.value = "Не удалось экспортировать в Google Календарь"
            }
        }
    }

    fun dismissStatusMessage() {
        _statusMessage.value = null
    }
}
