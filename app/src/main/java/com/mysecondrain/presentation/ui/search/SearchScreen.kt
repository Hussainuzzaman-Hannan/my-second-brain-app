package com.mysecondrain.presentation.ui.search

import com.mysecondrain.presentation.ui.debts.formatTaka
import com.mysecondrain.domain.repository.DebtRepository
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mysecondrain.domain.model.*
import com.mysecondrain.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class SearchResult(
    val id: Long,
    val title: String,
    val subtitle: String,
    val type: String,
    val color: Color,
    val icon: ImageVector
)

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val meetingRepository: MeetingRepository,
    private val noteRepository: NoteRepository,
    private val eventRepository: EventRepository,
    private val debtRepository: DebtRepository    // ← নতুন
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _query
                .debounce(300)
                .distinctUntilChanged()
                .collect { q ->
                    if (q.isBlank()) {
                        _uiState.update { it.copy(query = q, results = emptyList(), isSearching = false) }
                    } else {
                        _uiState.update { it.copy(isSearching = true) }
                        val results = mutableListOf<SearchResult>()

                        taskRepository.searchTasks(q).first().forEach { task ->
                            results.add(SearchResult(
                                id       = task.id,
                                title    = task.title,
                                subtitle = task.description.ifBlank { task.priority.label },
                                type     = "Task",
                                color    = Color(0xFF1565C0),
                                icon     = Icons.Outlined.CheckCircle
                            ))
                        }

                        meetingRepository.searchMeetings(q).first().forEach { m ->
                            results.add(SearchResult(
                                id       = m.id,
                                title    = m.title,
                                subtitle = if (m.personName.isNotBlank()) "with ${m.personName}" else m.location,
                                type     = "Meeting",
                                color    = Color(0xFFE65100),
                                icon     = Icons.Outlined.Groups
                            ))
                        }

                        noteRepository.searchNotes(q).first().forEach { note ->
                            results.add(SearchResult(
                                id       = note.id,
                                title    = note.title,
                                subtitle = note.content.take(60),
                                type     = "Note",
                                color    = Color(0xFF6A1B9A),
                                icon     = Icons.Outlined.StickyNote2
                            ))
                        }

                        eventRepository.searchEvents(q).first().forEach { event ->
                            results.add(SearchResult(
                                id       = event.id,
                                title    = event.title,
                                subtitle = event.personName.ifBlank { event.eventType.label },
                                type     = "Event",
                                color    = Color(0xFFE91E63),
                                icon     = Icons.Outlined.Cake
                            ))
                        }

                        debtRepository.searchDebts(q).first().forEach { debt ->
                            results.add(SearchResult(
                                id       = debt.id,
                                title    = debt.personName,
                                subtitle = debt.reason.ifBlank {
                                    if (debt.debtType.name == "OWES_ME")
                                        "পাবো: ${formatTaka(debt.totalAmount)}"
                                    else
                                        "দিবো: ${formatTaka(debt.totalAmount)}"
                                },
                                type     = "Debt",
                                color    = if (debt.debtType.name == "OWES_ME")
                                    Color(0xFF2E7D32) else Color(0xFFC62828),
                                icon     = Icons.Outlined.AccountBalance
                            ))
                        }

                        _uiState.update {
                            it.copy(query = q, results = results, isSearching = false)
                        }
                    }
                }
        }
    }

    fun onQueryChange(q: String) {
        _query.value = q
        _uiState.update { it.copy(query = q) }
    }

    fun clearQuery() {
        _query.value = ""
        _uiState.update { SearchUiState() }
    }
}

// ─── Search Screen ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onTaskClick: (Long) -> Unit,
    onNoteClick: (Long) -> Unit,
    onDebtClick: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("Search tasks, notes, meetings...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                },
                navigationIcon = {
                    Icon(Icons.Outlined.Search, null,
                        modifier = Modifier.padding(start = 16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                actions = {
                    if (state.query.isNotBlank()) {
                        IconButton(onClick = viewModel::clearQuery) {
                            Icon(Icons.Outlined.Clear, "Clear")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        when {
            state.isSearching -> {
                Box(Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.query.isBlank() -> {
                Box(Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Search, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(16.dp))
                        Text("Search everything",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tasks · Meetings · Notes · Events",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            state.results.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.SearchOff, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(16.dp))
                        Text("No results for \"${state.query}\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("${state.results.size} results",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(state.results, key = { "${it.type}_${it.id}" }) { result ->
                        SearchResultItem(
                            result  = result,
                            onClick = {
                                when (result.type) {
                                    "Task" -> onTaskClick(result.id)
                                    "Note" -> onNoteClick(result.id)
                                    "Debt" -> onDebtClick(result.id)
                                    else   -> {}
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultItem(result: SearchResult, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        onClick   = onClick,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(result.icon, null,
                    tint = result.color,
                    modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(result.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (result.subtitle.isNotBlank()) {
                    Text(result.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = result.color.copy(alpha = 0.12f)
            ) {
                Text(result.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = result.color,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}