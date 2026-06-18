package com.mysecondrain.presentation.ui.events

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mysecondrain.domain.model.Event
import com.mysecondrain.domain.model.EventType
import com.mysecondrain.domain.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.inject.Inject

data class AddEditEventUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val title: String = "",
    val description: String = "",
    val personName: String = "",
    val eventType: EventType = EventType.BIRTHDAY,
    val eventDate: LocalDate = LocalDate.now(),
    val isYearlyRecurring: Boolean = true,
    val reminderDaysBefore: Int = 1,
    val colorHex: String = "#FF6B6B",
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false
) { val canSave: Boolean get() = title.isNotBlank() }

@HiltViewModel
class AddEditEventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val eventId: Long = savedStateHandle.get<Long>("eventId") ?: -1L
    private val _uiState = MutableStateFlow(AddEditEventUiState())
    val uiState: StateFlow<AddEditEventUiState> = _uiState.asStateFlow()

    init {
        if (eventId > 0) loadEvent()
        else _uiState.update { it.copy(isLoading = false) }
    }

    private fun loadEvent() {
        viewModelScope.launch {
            eventRepository.getEventById(eventId).firstOrNull()?.let { e ->
                _uiState.update {
                    it.copy(isLoading = false, isEditMode = true, title = e.title,
                        description = e.description, personName = e.personName,
                        eventType = e.eventType, eventDate = e.eventDate,
                        isYearlyRecurring = e.isYearlyRecurring,
                        reminderDaysBefore = e.reminderDaysBefore, colorHex = e.colorHex)
                }
            } ?: _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onTitleChange(v: String) = _uiState.update { it.copy(title = v) }
    fun onDescriptionChange(v: String) = _uiState.update { it.copy(description = v) }
    fun onPersonChange(v: String) = _uiState.update { it.copy(personName = v) }
    fun onTypeChange(t: EventType) = _uiState.update { it.copy(eventType = t) }
    fun onDateChange(d: LocalDate) = _uiState.update { it.copy(eventDate = d) }
    fun onRecurringChange(r: Boolean) = _uiState.update { it.copy(isYearlyRecurring = r) }
    fun onReminderDaysChange(d: Int) = _uiState.update { it.copy(reminderDaysBefore = d) }

    fun save() {
        val s = _uiState.value
        if (!s.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val event = Event(id = if (s.isEditMode) eventId else 0, title = s.title.trim(),
                description = s.description.trim(), personName = s.personName.trim(),
                eventType = s.eventType, eventDate = s.eventDate,
                isYearlyRecurring = s.isYearlyRecurring,
                reminderDaysBefore = s.reminderDaysBefore, colorHex = s.colorHex)
            try {
                if (s.isEditMode) eventRepository.updateEvent(event)
                else eventRepository.addEvent(event)
                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun delete() {
        if (eventId <= 0) return
        viewModelScope.launch {
            eventRepository.deleteEvent(eventId)
            _uiState.update { it.copy(savedSuccessfully = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEventScreen(
    eventId: Long,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddEditEventViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedSuccessfully) { if (state.savedSuccessfully) onSaved() }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Event") },
            text = { Text("Delete this event permanently?") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(); showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Event" else "New Event", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    if (state.isEditMode) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = viewModel::save, enabled = state.canSave && !state.isSaving) {
                        if (state.isSaving) CircularProgressIndicator(Modifier.size(16.dp))
                        else Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Event Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(EventType.entries) { type ->
                            val icon = when (type) {
                                EventType.BIRTHDAY -> Icons.Outlined.Cake
                                EventType.ANNIVERSARY -> Icons.Outlined.Favorite
                                EventType.SCHOOL_EVENT -> Icons.Outlined.School
                                EventType.FAMILY_EVENT -> Icons.Outlined.FamilyRestroom
                                EventType.CLASS -> Icons.Outlined.MenuBook
                                EventType.OTHER -> Icons.Outlined.Event
                            }
                            FilterChip(
                                selected = state.eventType == type,
                                onClick = { viewModel.onTypeChange(type) },
                                label = { Text(type.label) },
                                leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(value = state.title, onValueChange = viewModel::onTitleChange,
                        label = { Text("Event Title *") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, leadingIcon = { Icon(Icons.Outlined.Event, null) })
                }
                item {
                    OutlinedTextField(value = state.personName, onValueChange = viewModel::onPersonChange,
                        label = { Text("Person Name (if applicable)") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, leadingIcon = { Icon(Icons.Outlined.Person, null) })
                }
                item {
                    OutlinedTextField(value = state.description, onValueChange = viewModel::onDescriptionChange,
                        label = { Text("Description") }, modifier = Modifier.fillMaxWidth(),
                        minLines = 2, maxLines = 4, leadingIcon = { Icon(Icons.Outlined.Notes, null) })
                }
                // Date picker
                item {
                    val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy")
                    OutlinedCard(modifier = Modifier.fillMaxWidth().clickable {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(context, { _, y, m, d ->
                            viewModel.onDateChange(LocalDate.of(y, m + 1, d))
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }, shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarToday, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(state.eventDate.format(fmt))
                            }
                        }
                    }
                }
                // Recurring
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Repeat, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Yearly Recurring", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("Get reminded every year on this date", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = state.isYearlyRecurring, onCheckedChange = viewModel::onRecurringChange)
                        }
                    }
                }
                // Reminder days
                item {
                    Text("Remind me", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "On day", 1 to "1 day", 3 to "3 days", 7 to "1 week").forEach { (days, label) ->
                            FilterChip(
                                selected = state.reminderDaysBefore == days,
                                onClick = { viewModel.onReminderDaysChange(days) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(60.dp)) }
            }
        }
    }
}
