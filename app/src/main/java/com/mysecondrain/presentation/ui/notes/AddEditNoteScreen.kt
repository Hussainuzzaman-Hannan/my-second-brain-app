package com.mysecondrain.presentation.ui.notes

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mysecondrain.domain.model.Note
import com.mysecondrain.domain.model.NoteType
import com.mysecondrain.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val noteColors = listOf(
    "#FFFFFF", "#FFF9C4", "#F8BBD0", "#B2EBF2", "#C8E6C9", "#E1BEE7", "#FFE0B2"
)

data class AddEditNoteUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val title: String = "",
    val content: String = "",
    val noteType: NoteType = NoteType.PERSONAL,
    val isPinned: Boolean = false,
    val colorHex: String = "#FFFFFF",
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false
) {
    val canSave: Boolean get() = title.isNotBlank()
}

@HiltViewModel
class AddEditNoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: -1L
    private val _uiState = MutableStateFlow(AddEditNoteUiState())
    val uiState: StateFlow<AddEditNoteUiState> = _uiState.asStateFlow()

    init {
        if (noteId > 0) loadNote()
        else _uiState.update { it.copy(isLoading = false) }
    }

    private fun loadNote() {
        viewModelScope.launch {
            noteRepository.getNoteById(noteId).firstOrNull()?.let { n ->
                _uiState.update {
                    it.copy(isLoading = false, isEditMode = true,
                        title = n.title, content = n.content,
                        noteType = n.noteType, isPinned = n.isPinned, colorHex = n.colorHex)
                }
            } ?: _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onTitleChange(v: String) = _uiState.update { it.copy(title = v) }
    fun onContentChange(v: String) = _uiState.update { it.copy(content = v) }
    fun onTypeChange(t: NoteType) = _uiState.update { it.copy(noteType = t) }
    fun onPinToggle() = _uiState.update { it.copy(isPinned = !it.isPinned) }
    fun onColorChange(hex: String) = _uiState.update { it.copy(colorHex = hex) }

    fun save() {
        val s = _uiState.value
        if (!s.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val note = Note(id = if (s.isEditMode) noteId else 0,
                title = s.title.trim(), content = s.content,
                noteType = s.noteType, isPinned = s.isPinned, colorHex = s.colorHex)
            try {
                if (s.isEditMode) noteRepository.updateNote(note)
                else noteRepository.addNote(note)
                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun delete() {
        if (noteId <= 0) return
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
            _uiState.update { it.copy(savedSuccessfully = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteScreen(
    noteId: Long,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddEditNoteViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedSuccessfully) { if (state.savedSuccessfully) onSaved() }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note") },
            text = { Text("Delete this note permanently?") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(); showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    val bgColor = try { Color(android.graphics.Color.parseColor(state.colorHex)) }
    catch (_: Exception) { Color.White }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Note" else "New Note", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = viewModel::onPinToggle) {
                        Icon(if (state.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            "Pin", tint = if (state.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (state.isEditMode) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = viewModel::save, enabled = state.canSave && !state.isSaving) {
                        if (state.isSaving) CircularProgressIndicator(Modifier.size(16.dp))
                        else Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor.copy(alpha = 0.3f))
            )
        },
        containerColor = bgColor.copy(alpha = 0.15f)
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Note type selector
                item {
                    Text("Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(NoteType.entries) { type ->
                            FilterChip(
                                selected = state.noteType == type,
                                onClick = { viewModel.onTypeChange(type) },
                                label = { Text(type.label) }
                            )
                        }
                    }
                }
                // Color picker
                item {
                    Text("Color", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        noteColors.forEach { hex ->
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.White }
                            Box(modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { viewModel.onColorChange(hex) }
                                .then(if (state.colorHex == hex) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                            )
                        }
                    }
                }
                // Title
                item {
                    TextField(value = state.title, onValueChange = viewModel::onTitleChange,
                        placeholder = { Text("Title", style = MaterialTheme.typography.headlineSmall) },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
                // Content
                item {
                    TextField(value = state.content, onValueChange = viewModel::onContentChange,
                        placeholder = { Text("Start writing your note here...") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                        minLines = 8,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
