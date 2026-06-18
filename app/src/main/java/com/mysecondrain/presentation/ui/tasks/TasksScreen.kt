package com.mysecondrain.presentation.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mysecondrain.domain.model.*
import com.mysecondrain.domain.repository.CategoryRepository
import com.mysecondrain.domain.repository.TaskRepository
import com.mysecondrain.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedFilter: String = "ALL",
    val isLoading: Boolean = true
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _filter = MutableStateFlow("ALL")
    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                _filter.flatMapLatest { filter ->
                    when (filter) {
                        "PENDING"   -> taskRepository.getPendingTasks()
                        "COMPLETED" -> taskRepository.getCompletedTasks()
                        else        -> taskRepository.getAllTasks()
                    }
                },
                categoryRepository.getAllCategories()
            ) { tasks, categories ->
                TasksUiState(
                    tasks          = tasks,
                    categories     = categories,
                    selectedFilter = _filter.value,
                    isLoading      = false
                )
            }.collect { _uiState.value = it }
        }
    }

    fun setFilter(filter: String) { _filter.value = filter }

    fun toggleComplete(id: Long, completed: Boolean) {
        viewModelScope.launch { taskRepository.toggleTaskCompletion(id, completed) }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch { taskRepository.deleteTask(id) }
    }
}

// ─── Tasks Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onAddTask: () -> Unit,
    onTaskClick: (Long) -> Unit,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filters = listOf("ALL", "PENDING", "COMPLETED")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Tasks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTask,
                icon    = { Icon(Icons.Filled.Add, "Add task") },
                text    = { Text("New Task") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = uiState.selectedFilter == filter,
                        onClick  = { viewModel.setFilter(filter) },
                        label    = { Text(filter.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.tasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.TaskAlt, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(16.dp))
                        Text("No tasks yet", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tap + to add your first task", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(uiState.tasks, key = { it.id }) { task ->
                        TaskListItem(
                            task     = task,
                            onToggle = { viewModel.toggleComplete(task.id, !task.isCompleted) },
                            onClick  = { onTaskClick(task.id) },
                            onDelete = { viewModel.deleteTask(task.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListItem(
    task: Task,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (task.priority) {
        Priority.LOW    -> PriorityLow
        Priority.MEDIUM -> PriorityMedium
        Priority.HIGH   -> PriorityHigh
        Priority.URGENT -> PriorityUrgent
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title  = { Text("Delete task?") },
            text   = { Text("\"${task.title}\" will be removed.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        onClick   = onClick,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority bar
            Box(
                modifier = Modifier
                    .width(4.dp).height(44.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (task.isCompleted) Color.LightGray else priorityColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text     = task.description,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    task.category?.let { cat ->
                        val catColor = try { Color(android.graphics.Color.parseColor(cat.colorHex)) }
                                       catch (_: Exception) { MaterialTheme.colorScheme.primary }
                        Surface(shape = RoundedCornerShape(4.dp), color = catColor.copy(alpha = 0.12f)) {
                            Text(cat.name, style = MaterialTheme.typography.labelSmall,
                                color = catColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    task.dueDate?.let {
                        Icon(Icons.Outlined.CalendarToday, null,
                            modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(it.format(DateTimeFormatter.ofPattern("d MMM")),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(shape = RoundedCornerShape(4.dp),
                        color = priorityColor.copy(alpha = if (task.isCompleted) 0.05f else 0.12f)) {
                        Text(task.priority.label, style = MaterialTheme.typography.labelSmall,
                            color = if (task.isCompleted) Color.LightGray else priorityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            Checkbox(
                checked         = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor   = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Outlined.Delete, "Delete",
                    tint     = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}
