package com.mysecondrain.presentation.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mysecondrain.domain.model.*
import com.mysecondrain.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─── Data Class ───────────────────────────────────────────────────────────────

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val tasks: List<Task> = emptyList(),
    val meetings: List<Meeting> = emptyList(),
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = true
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val meetingRepository: MeetingRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                taskRepository.getAllTasks(),
                meetingRepository.getAllMeetings(),
                eventRepository.getAllEvents()
            ) { tasks, meetings, events ->
                _uiState.value.copy(
                    tasks     = tasks,
                    meetings  = meetings,
                    events    = events,
                    isLoading = false
                )
            }.collect { _uiState.value = it }
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun previousMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.minusMonths(1)) }
    }

    fun nextMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.plusMonths(1)) }
    }

    fun tasksForDate(date: LocalDate): List<Task> =
        _uiState.value.tasks.filter { task ->
            task.dueDate == date
        }

    fun meetingsForDate(date: LocalDate): List<Meeting> =
        _uiState.value.meetings.filter { meeting ->
            meeting.dateTime.year == date.year &&
                    meeting.dateTime.monthValue == date.month.value &&
                    meeting.dateTime.dayOfMonth == date.dayOfMonth
        }

    // Normal (non-class) events — birthday, anniversary ইত্যাদি
    fun eventsForDate(date: LocalDate): List<Event> =
        _uiState.value.events.filter { event ->
            event.eventType != EventType.CLASS &&
                    event.eventDate.dayOfMonth == date.dayOfMonth &&
                    event.eventDate.month == date.month
        }

    // Class — direct date match + weekly recurring মিলিয়ে দেখায়
    fun classesForDate(date: LocalDate): List<Event> {
        val weekDay = toWeekDay(date)

        val directClasses = _uiState.value.events.filter { event ->
            event.eventType == EventType.CLASS &&
                    !event.isWeeklyRecurring &&
                    event.eventDate.dayOfMonth == date.dayOfMonth &&
                    event.eventDate.month == date.month &&
                    event.eventDate.year == date.year
        }

        val weeklyClasses = _uiState.value.events.filter { event ->
            event.eventType == EventType.CLASS &&
                    event.isWeeklyRecurring &&
                    event.weeklyDay == weekDay &&
                    event.eventDate <= date   // শুরুর তারিখের পর থেকেই দেখাবে
        }

        return (directClasses + weeklyClasses)
            .distinctBy { it.id }
            .sortedBy { it.startTime }
    }

    fun hasActivity(date: LocalDate): Boolean =
        tasksForDate(date).isNotEmpty() ||
                meetingsForDate(date).isNotEmpty() ||
                eventsForDate(date).isNotEmpty() ||
                classesForDate(date).isNotEmpty()

    private fun toWeekDay(date: LocalDate): WeekDay = when (date.dayOfWeek) {
        java.time.DayOfWeek.SUNDAY    -> WeekDay.SUNDAY
        java.time.DayOfWeek.MONDAY    -> WeekDay.MONDAY
        java.time.DayOfWeek.TUESDAY   -> WeekDay.TUESDAY
        java.time.DayOfWeek.WEDNESDAY -> WeekDay.WEDNESDAY
        java.time.DayOfWeek.THURSDAY  -> WeekDay.THURSDAY
        java.time.DayOfWeek.FRIDAY    -> WeekDay.FRIDAY
        java.time.DayOfWeek.SATURDAY  -> WeekDay.SATURDAY
    }
}

// ─── Calendar Screen ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onTaskClick: (Long) -> Unit,
    onAddTask: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = onAddTask,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Outlined.Add, "Add Task")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                MonthHeader(
                    currentMonth = state.currentMonth,
                    onPrevious   = viewModel::previousMonth,
                    onNext       = viewModel::nextMonth
                )
            }

            item { DayOfWeekRow() }

            item {
                CalendarGrid(
                    currentMonth = state.currentMonth,
                    selectedDate = state.selectedDate,
                    hasActivity  = { viewModel.hasActivity(it) },
                    onDateSelect = viewModel::selectDate
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Text(
                    text = state.selectedDate.format(
                        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
                    ),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Classes (sorted by time)
            val dayClasses = viewModel.classesForDate(state.selectedDate)
            if (dayClasses.isNotEmpty()) {
                item {
                    DaySectionHeader(
                        title = "Classes",
                        icon  = Icons.Outlined.MenuBook,
                        color = Color(0xFF1565C0)
                    )
                }
                items(dayClasses) { classEvent ->
                    DayClassItem(event = classEvent)
                }
            }

            // Tasks
            val dayTasks = viewModel.tasksForDate(state.selectedDate)
            if (dayTasks.isNotEmpty()) {
                item {
                    DaySectionHeader(
                        title = "Tasks",
                        icon  = Icons.Outlined.CheckCircle,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(dayTasks) { task ->
                    DayTaskItem(task = task, onClick = { onTaskClick(task.id) })
                }
            }

            // Meetings
            val dayMeetings = viewModel.meetingsForDate(state.selectedDate)
            if (dayMeetings.isNotEmpty()) {
                item {
                    DaySectionHeader(
                        title = "Meetings",
                        icon  = Icons.Outlined.Groups,
                        color = Color(0xFFE65100)
                    )
                }
                items(dayMeetings) { meeting ->
                    DayMeetingItem(meeting = meeting)
                }
            }

            // Events
            val dayEvents = viewModel.eventsForDate(state.selectedDate)
            if (dayEvents.isNotEmpty()) {
                item {
                    DaySectionHeader(
                        title = "Events",
                        icon  = Icons.Outlined.Cake,
                        color = Color(0xFFE91E63)
                    )
                }
                items(dayEvents) { event ->
                    DayEventItem(event = event)
                }
            }

            // Empty state
            if (dayTasks.isEmpty() && dayMeetings.isEmpty() &&
                dayEvents.isEmpty() && dayClasses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.EventAvailable, null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No activities this day",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Month Header ─────────────────────────────────────────────────────────────

@Composable
private fun MonthHeader(
    currentMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Outlined.ChevronLeft, "Previous month")
        }
        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Outlined.ChevronRight, "Next month")
        }
    }
}

// ─── Day of Week Row ──────────────────────────────────────────────────────────

@Composable
private fun DayOfWeekRow() {
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        days.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (day == "Sun" || day == "Sat")
                    MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(Modifier.height(4.dp))
}

// ─── Calendar Grid ────────────────────────────────────────────────────────────

@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    hasActivity: (LocalDate) -> Boolean,
    onDateSelect: (LocalDate) -> Unit
) {
    val today        = LocalDate.now()
    val firstDay     = currentMonth.atDay(1)
    val firstDayOfWeek = firstDay.dayOfWeek.value % 7
    val daysInMonth  = currentMonth.lengthOfMonth()
    val totalCells   = firstDayOfWeek + daysInMonth
    val rows         = (totalCells + 6) / 7

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val dayIndex = row * 7 + col - firstDayOfWeek + 1
                    if (dayIndex < 1 || dayIndex > daysInMonth) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val date       = currentMonth.atDay(dayIndex)
                        val isSelected = date == selectedDate
                        val isToday    = date == today
                        val isWeekend  = col == 0 || col == 6
                        val hasAct     = hasActivity(date)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday    -> MaterialTheme.colorScheme.primaryContainer
                                        else       -> Color.Transparent
                                    }
                                )
                                .clickable { onDateSelect(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayIndex.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isToday || isSelected)
                                        FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday    -> MaterialTheme.colorScheme.primary
                                        isWeekend  -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                        else       -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (hasAct) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected)
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.primary
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun DaySectionHeader(
    title: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── Day Item Cards ───────────────────────────────────────────────────────────

@Composable
private fun DayClassItem(event: Event) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape  = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1565C0).copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.MenuBook, null,
                tint     = Color(0xFF1565C0),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    event.title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color      = Color(0xFF1565C0),
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                if (event.personName.isNotBlank()) {
                    Text(
                        event.personName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF1565C0).copy(alpha = 0.7f)
                    )
                }
                if (event.description.isNotBlank()) {
                    Text(
                        event.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (event.startTime.isNotBlank()) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        event.startTime,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color(0xFF1565C0)
                    )
                    if (event.endTime.isNotBlank()) {
                        Text(
                            "- ${event.endTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1565C0).copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayTaskItem(task: Task, onClick: () -> Unit) {
    val priorityColor = when (task.priority) {
        Priority.LOW    -> Color(0xFF2E7D32)
        Priority.MEDIUM -> Color(0xFF1565C0)
        Priority.HIGH   -> Color(0xFFE65100)
        Priority.URGENT -> Color(0xFFB71C1C)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clickable { onClick() },
        shape  = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(priorityColor)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                task.dueDateTime?.let { dt ->
                    Text(
                        dt.format(DateTimeFormatter.ofPattern("hh:mm a")),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (task.isCompleted) {
                Icon(
                    Icons.Outlined.CheckCircle, null,
                    tint     = Color(0xFF2E7D32),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DayMeetingItem(meeting: Meeting) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape  = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE65100).copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Groups, null,
                tint     = Color(0xFFE65100),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    meeting.title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                if (meeting.personName.isNotBlank()) {
                    Text(
                        "with ${meeting.personName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                meeting.dateTime.format(DateTimeFormatter.ofPattern("hh:mm a")),
                style      = MaterialTheme.typography.labelMedium,
                color      = Color(0xFFE65100),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DayEventItem(event: Event) {
    val color = try {
        Color(android.graphics.Color.parseColor(event.colorHex))
    } catch (_: Exception) {
        Color(0xFFE91E63)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape  = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Cake, null,
                tint     = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    event.title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                if (event.personName.isNotBlank()) {
                    Text(
                        event.personName,
                        style = MaterialTheme.typography.labelSmall,
                        color = color.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}