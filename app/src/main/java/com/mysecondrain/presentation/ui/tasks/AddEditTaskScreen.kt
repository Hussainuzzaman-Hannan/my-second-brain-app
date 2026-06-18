package com.mysecondrain.presentation.ui.tasks

import com.mysecondrain.notification.ReminderScheduler
import java.time.ZoneId
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class AddEditTaskUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val title: String = "",
    val description: String = "",
    val selectedCategory: Category? = null,
    val priority: Priority = Priority.MEDIUM,
    val dueDate: LocalDate? = null,
    val dueTime: LocalDateTime? = null,
    val reminderOffset: ReminderOffset = ReminderOffset.NONE,
    val categories: List<Category> = emptyList(),
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null
) {
    val canSave: Boolean get() = title.isNotBlank()
}

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    private val reminderScheduler: ReminderScheduler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: -1L

    private val _uiState = MutableStateFlow(AddEditTaskUiState())
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        if (taskId > 0) loadTask(taskId)
        else _uiState.update { it.copy(isLoading = false) }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    private fun loadTask(id: Long) {
        viewModelScope.launch {
            taskRepository.getTaskById(id).firstOrNull()?.let { task ->
                _uiState.update {
                    it.copy(
                        isLoading        = false,
                        isEditMode       = true,
                        title            = task.title,
                        description      = task.description,
                        selectedCategory = task.category,
                        priority         = task.priority,
                        dueDate          = task.dueDate,
                        dueTime          = task.dueDateTime,
                        reminderOffset   = task.reminderOffset
                    )
                }
            } ?: _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onTitleChange(value: String) =
        _uiState.update { it.copy(title = value) }

    fun onDescriptionChange(value: String) =
        _uiState.update { it.copy(description = value) }

    fun onCategorySelected(category: Category?) =
        _uiState.update { it.copy(selectedCategory = category) }

    fun onPrioritySelected(priority: Priority) =
        _uiState.update { it.copy(priority = priority) }

    fun onDueDateSelected(date: LocalDate?) =
        _uiState.update { it.copy(dueDate = date) }

    fun onDueTimeSelected(time: LocalDateTime?) =
        _uiState.update { it.copy(dueTime = time) }

    fun onReminderSelected(offset: ReminderOffset) =
        _uiState.update { it.copy(reminderOffset = offset) }

    fun saveTask() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val task = Task(
                id             = if (state.isEditMode) taskId else 0,
                title          = state.title.trim(),
                description    = state.description.trim(),
                category       = state.selectedCategory,
                priority       = state.priority,
                dueDate        = state.dueDate,
                dueDateTime    = state.dueTime,
                reminderOffset = state.reminderOffset
            )
            try {
                val savedId = if (state.isEditMode) {
                    taskRepository.updateTask(task)
                    taskId
                } else {
                    taskRepository.addTask(task)
                }

                // Auto-schedule reminder
                scheduleReminder(savedId, task)

                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    private fun scheduleReminder(taskId: Long, task: Task) {
        // Cancel existing reminder first
        reminderScheduler.cancelReminder(taskId.toInt())

        if (task.reminderOffset == ReminderOffset.NONE) return

        val dueMillis = task.dueDateTime
            ?.atZone(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli() ?: return

        val offsetMillis = when (task.reminderOffset) {
            ReminderOffset.MIN_15 -> 15 * 60 * 1000L
            ReminderOffset.MIN_30 -> 30 * 60 * 1000L
            ReminderOffset.HR_1   -> 60 * 60 * 1000L
            ReminderOffset.DAY_1  -> 24 * 60 * 60 * 1000L
            else                  -> return
        }

        val triggerMillis = dueMillis - offsetMillis
        if (triggerMillis <= System.currentTimeMillis()) return

        reminderScheduler.scheduleReminder(
            requestCode   = taskId.toInt(),
            triggerMillis = triggerMillis,
            title         = "Task Reminder ⏰",
            message       = task.title,
            type          = com.mysecondrain.notification.ReminderReceiver.TYPE_TASK
        )
    }

    fun deleteTask() {
        if (taskId <= 0) return
        viewModelScope.launch {
            reminderScheduler.cancelReminder(taskId.toInt())
            taskRepository.deleteTask(taskId)
            _uiState.update { it.copy(savedSuccessfully = true) }
        }
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    taskId: Long,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddEditTaskViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showReminderSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onSaved()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete this task? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteTask(); showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showCategorySheet) {
        CategoryBottomSheet(
            categories = state.categories,
            selected = state.selectedCategory,
            onSelect = { viewModel.onCategorySelected(it); showCategorySheet = false },
            onDismiss = { showCategorySheet = false }
        )
    }

    if (showReminderSheet) {
        ReminderBottomSheet(
            selected = state.reminderOffset,
            onSelect = { viewModel.onReminderSelected(it); showReminderSheet = false },
            onDismiss = { showReminderSheet = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Task" else "New Task", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (state.isEditMode) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(
                        onClick = viewModel::saveTask,
                        enabled = state.canSave && !state.isSaving
                    ) {
                        if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp))
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
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                item {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::onTitleChange,
                        label = { Text("Title *") },
                        placeholder = { Text("What needs to be done?") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Title, null) }
                    )
                }

                // Description
                item {
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text("Description") },
                        placeholder = { Text("Add details (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        leadingIcon = { Icon(Icons.Outlined.Notes, null) }
                    )
                }

                // Priority
                item {
                    Text("Priority", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Priority.entries) { p ->
                            val color = when (p) {
                                Priority.LOW -> PriorityLow
                                Priority.MEDIUM -> PriorityMedium
                                Priority.HIGH -> PriorityHigh
                                Priority.URGENT -> PriorityUrgent
                            }
                            FilterChip(
                                selected = state.priority == p,
                                onClick = { viewModel.onPrioritySelected(p) },
                                label = { Text(p.label) },
                                leadingIcon = if (state.priority == p) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.15f),
                                    selectedLabelColor = color,
                                    selectedLeadingIconColor = color
                                )
                            )
                        }
                    }
                }

                // Category
                item {
                    ClickableField(
                        label = "Category",
                        value = state.selectedCategory?.name ?: "Select category",
                        icon = Icons.Outlined.Category,
                        onClick = { showCategorySheet = true }
                    )
                }

                // Due Date
                item {
                    val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
                    ClickableField(
                        label = "Due Date",
                        value = state.dueDate?.format(dateFormatter) ?: "Set due date",
                        icon = Icons.Outlined.CalendarToday,
                        onClick = {
                            val cal = Calendar.getInstance()
                            state.dueDate?.let {
                                cal.set(it.year, it.monthValue - 1, it.dayOfMonth)
                            }
                            DatePickerDialog(context, { _, y, m, d ->
                                viewModel.onDueDateSelected(LocalDate.of(y, m + 1, d))
                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        trailingContent = if (state.dueDate != null) {
                            {
                                IconButton(onClick = { viewModel.onDueDateSelected(null) }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        } else null
                    )
                }

                // Due Time
                item {
                    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
                    ClickableField(
                        label = "Due Time",
                        value = state.dueTime?.format(timeFormatter) ?: "Set due time",
                        icon = Icons.Outlined.Schedule,
                        onClick = {
                            val cal = Calendar.getInstance()
                            TimePickerDialog(context, { _, h, m ->
                                val base = state.dueDate?.atTime(h, m) ?: LocalDateTime.now().withHour(h).withMinute(m)
                                viewModel.onDueTimeSelected(base)
                            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                        },
                        trailingContent = if (state.dueTime != null) {
                            {
                                IconButton(onClick = { viewModel.onDueTimeSelected(null) }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        } else null
                    )
                }

                // Reminder
                item {
                    ClickableField(
                        label = "Reminder",
                        value = state.reminderOffset.label,
                        icon = Icons.Outlined.NotificationsActive,
                        onClick = { showReminderSheet = true }
                    )
                }

                item { Spacer(Modifier.height(60.dp)) }
            }
        }
    }
}

@Composable
private fun ClickableField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium)
            }
            trailingContent?.invoke() ?: Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryBottomSheet(
    categories: List<Category>,
    selected: Category?,
    onSelect: (Category?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text("Choose Category", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            ListItem(
                headlineContent = { Text("No Category") },
                leadingContent = { Icon(Icons.Outlined.FolderOff, null) },
                modifier = Modifier.clickable { onSelect(null) },
                trailingContent = if (selected == null) { { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) } } else null
            )
            HorizontalDivider()
            categories.forEach { cat ->
                val catColor = try { Color(android.graphics.Color.parseColor(cat.colorHex)) }
                catch (_: Exception) { MaterialTheme.colorScheme.primary }
                ListItem(
                    headlineContent = { Text(cat.name) },
                    leadingContent = {
                        Box(modifier = Modifier.size(32.dp).background(catColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Circle, null, tint = catColor, modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.clickable { onSelect(cat) },
                    trailingContent = if (selected?.id == cat.id) {
                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderBottomSheet(
    selected: ReminderOffset,
    onSelect: (ReminderOffset) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text("Set Reminder", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            ReminderOffset.entries.forEach { offset ->
                ListItem(
                    headlineContent = { Text(offset.label) },
                    leadingContent = { Icon(Icons.Outlined.Alarm, null) },
                    modifier = Modifier.clickable { onSelect(offset) },
                    trailingContent = if (selected == offset) {
                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null
                )
            }
        }
    }
}
