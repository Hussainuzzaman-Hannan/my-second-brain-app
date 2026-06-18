package com.mysecondrain.presentation.ui.meetings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.mysecondrain.domain.model.Meeting
import com.mysecondrain.domain.repository.MeetingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class AddEditMeetingUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val title: String = "",
    val personName: String = "",
    val location: String = "",
    val notes: String = "",
    val meetingDate: LocalDate = LocalDate.now(),
    val meetingTime: LocalDateTime = LocalDateTime.now().plusHours(1),
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null
) {
    val canSave: Boolean get() = title.isNotBlank()
}

@HiltViewModel
class AddEditMeetingViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val meetingId: Long = savedStateHandle.get<Long>("meetingId") ?: -1L
    private val _uiState = MutableStateFlow(AddEditMeetingUiState())
    val uiState: StateFlow<AddEditMeetingUiState> = _uiState.asStateFlow()

    init {
        if (meetingId > 0) loadMeeting(meetingId)
        else _uiState.update { it.copy(isLoading = false) }
    }

    private fun loadMeeting(id: Long) {
        viewModelScope.launch {
            meetingRepository.getMeetingById(id).firstOrNull()?.let { m ->
                _uiState.update {
                    it.copy(
                        isLoading = false, isEditMode = true,
                        title = m.title, personName = m.personName,
                        location = m.location, notes = m.notes,
                        meetingDate = m.dateTime.toLocalDate(),
                        meetingTime = m.dateTime
                    )
                }
            } ?: _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onTitleChange(v: String) = _uiState.update { it.copy(title = v) }
    fun onPersonChange(v: String) = _uiState.update { it.copy(personName = v) }
    fun onLocationChange(v: String) = _uiState.update { it.copy(location = v) }
    fun onNotesChange(v: String) = _uiState.update { it.copy(notes = v) }
    fun onDateChange(d: LocalDate) = _uiState.update { it.copy(meetingDate = d) }
    fun onTimeChange(t: LocalDateTime) = _uiState.update { it.copy(meetingTime = t) }

    fun save() {
        val s = _uiState.value
        if (!s.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val dt = LocalDateTime.of(s.meetingDate, s.meetingTime.toLocalTime())
            val meeting = Meeting(
                id = if (s.isEditMode) meetingId else 0,
                title = s.title.trim(), personName = s.personName.trim(),
                location = s.location.trim(), notes = s.notes.trim(), dateTime = dt
            )
            try {
                if (s.isEditMode) meetingRepository.updateMeeting(meeting)
                else meetingRepository.addMeeting(meeting)
                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun delete() {
        if (meetingId <= 0) return
        viewModelScope.launch {
            meetingRepository.deleteMeeting(meetingId)
            _uiState.update { it.copy(savedSuccessfully = true) }
        }
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMeetingScreen(
    meetingId: Long,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddEditMeetingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedSuccessfully) { if (state.savedSuccessfully) onSaved() }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Meeting") },
            text = { Text("Delete this meeting permanently?") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(); showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Meeting" else "New Meeting", fontWeight = FontWeight.SemiBold) },
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
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
            val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(value = state.title, onValueChange = viewModel::onTitleChange,
                        label = { Text("Meeting Title *") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, leadingIcon = { Icon(Icons.Outlined.Groups, null) })
                }
                item {
                    OutlinedTextField(value = state.personName, onValueChange = viewModel::onPersonChange,
                        label = { Text("Person / With") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, leadingIcon = { Icon(Icons.Outlined.Person, null) })
                }
                item {
                    OutlinedTextField(value = state.location, onValueChange = viewModel::onLocationChange,
                        label = { Text("Location") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, leadingIcon = { Icon(Icons.Outlined.LocationOn, null) })
                }
                // Date picker
                item {
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
                                Text("Meeting Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(state.meetingDate.format(dateFormatter))
                            }
                        }
                    }
                }
                // Time picker
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth().clickable {
                        val cal = Calendar.getInstance()
                        TimePickerDialog(context, { _, h, m ->
                            viewModel.onTimeChange(LocalDateTime.of(state.meetingDate, java.time.LocalTime.of(h, m)))
                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                    }, shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Meeting Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(state.meetingTime.format(timeFormatter))
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(value = state.notes, onValueChange = viewModel::onNotesChange,
                        label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(),
                        minLines = 3, maxLines = 6, leadingIcon = { Icon(Icons.Outlined.Notes, null) })
                }
                item { Spacer(Modifier.height(60.dp)) }
            }
        }
    }
}
