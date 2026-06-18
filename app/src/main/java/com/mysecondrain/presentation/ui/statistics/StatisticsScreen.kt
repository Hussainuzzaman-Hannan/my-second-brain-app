package com.mysecondrain.presentation.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mysecondrain.domain.model.*
import com.mysecondrain.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class StatisticsUiState(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val pendingTasks: Int = 0,
    val urgentTasks: Int = 0,
    val totalMeetings: Int = 0,
    val totalNotes: Int = 0,
    val totalEvents: Int = 0,
    val tasksByCategory: Map<String, Int> = emptyMap(),
    val tasksByPriority: Map<String, Int> = emptyMap(),
    val completionRate: Float = 0f,
    val isLoading: Boolean = true
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val meetingRepository: MeetingRepository,
    private val noteRepository: NoteRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                taskRepository.getAllTasks(),
                meetingRepository.getAllMeetings(),
                noteRepository.getAllNotes(),
                eventRepository.getAllEvents()
            ) { tasks, meetings, notes, events ->
                val completed = tasks.count { it.isCompleted }
                val pending   = tasks.count { !it.isCompleted }
                val urgent    = tasks.count { it.priority == Priority.URGENT && !it.isCompleted }

                val byCategory = tasks
                    .groupBy { it.category?.name ?: "Uncategorized" }
                    .mapValues { it.value.size }

                val byPriority = tasks
                    .groupBy { it.priority.label }
                    .mapValues { it.value.size }

                val rate = if (tasks.isEmpty()) 0f
                else (completed.toFloat() / tasks.size) * 100f

                StatisticsUiState(
                    totalTasks      = tasks.size,
                    completedTasks  = completed,
                    pendingTasks    = pending,
                    urgentTasks     = urgent,
                    totalMeetings   = meetings.size,
                    totalNotes      = notes.size,
                    totalEvents     = events.size,
                    tasksByCategory = byCategory,
                    tasksByPriority = byPriority,
                    completionRate  = rate,
                    isLoading       = false
                )
            }.collect { _uiState.value = it }
        }
    }
}

// ─── Statistics Screen ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overview cards
                item {
                    Text("Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard("Total Tasks", state.totalTasks.toString(),
                            Icons.Outlined.FormatListBulleted,
                            MaterialTheme.colorScheme.primary,
                            Modifier.weight(1f))
                        StatCard("Completed", state.completedTasks.toString(),
                            Icons.Outlined.CheckCircle,
                            Color(0xFF2E7D32),
                            Modifier.weight(1f))
                        StatCard("Pending", state.pendingTasks.toString(),
                            Icons.Outlined.PendingActions,
                            Color(0xFFE65100),
                            Modifier.weight(1f))
                    }
                }

                // Second row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard("Meetings", state.totalMeetings.toString(),
                            Icons.Outlined.Groups,
                            Color(0xFFE65100),
                            Modifier.weight(1f))
                        StatCard("Notes", state.totalNotes.toString(),
                            Icons.Outlined.StickyNote2,
                            Color(0xFF6A1B9A),
                            Modifier.weight(1f))
                        StatCard("Events", state.totalEvents.toString(),
                            Icons.Outlined.Cake,
                            Color(0xFFE91E63),
                            Modifier.weight(1f))
                    }
                }

                // Completion rate
                item {
                    CompletionRateCard(rate = state.completionRate)
                }

                // Priority breakdown
                if (state.tasksByPriority.isNotEmpty()) {
                    item {
                        BreakdownCard(
                            title = "Tasks by Priority",
                            data  = state.tasksByPriority,
                            colorMap = mapOf(
                                "Low"    to Color(0xFF2E7D32),
                                "Medium" to Color(0xFF1565C0),
                                "High"   to Color(0xFFE65100),
                                "Urgent" to Color(0xFFB71C1C)
                            )
                        )
                    }
                }

                // Category breakdown
                if (state.tasksByCategory.isNotEmpty()) {
                    item {
                        BreakdownCard(
                            title = "Tasks by Category",
                            data  = state.tasksByCategory,
                            colorMap = emptyMap()
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ─── Stat Card ────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color)
            Text(label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── Completion Rate Card ─────────────────────────────────────────────────────

@Composable
private fun CompletionRateCard(rate: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Completion Rate",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Text("${rate.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { rate / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color            = when {
                    rate >= 75f -> Color(0xFF2E7D32)
                    rate >= 50f -> Color(0xFF1565C0)
                    rate >= 25f -> Color(0xFFE65100)
                    else        -> Color(0xFFB71C1C)
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    rate >= 75f -> "Excellent! Keep it up 🎉"
                    rate >= 50f -> "Good progress 👍"
                    rate >= 25f -> "Keep going, you can do it 💪"
                    else        -> "Let's start completing tasks 🚀"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Breakdown Card ───────────────────────────────────────────────────────────

@Composable
private fun BreakdownCard(
    title: String,
    data: Map<String, Int>,
    colorMap: Map<String, Color>
) {
    val total = data.values.sum().coerceAtLeast(1)
    val defaultColors = listOf(
        Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFFE65100),
        Color(0xFF6A1B9A), Color(0xFFE91E63), Color(0xFF00838F)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            data.entries.sortedByDescending { it.value }
                .forEachIndexed { index, (key, count) ->
                    val color = colorMap[key] ?: defaultColors[index % defaultColors.size]
                    val fraction = count.toFloat() / total
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(key,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall)
                        Text("$count",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = color)
                        Spacer(Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .width(80.dp)
                                .height(6.dp)
                                .clip(CircleShape),
                            color      = color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
        }
    }
}