package com.mysecondrain.presentation.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mysecondrain.domain.model.*
import com.mysecondrain.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val greeting: String = "",
    val todayTasks: List<Task> = emptyList(),
    val upcomingMeetings: List<Meeting> = emptyList(),
    val todayEvents: List<Event> = emptyList(),
    val pendingTaskCount: Int = 0,
    val completedTaskCount: Int = 0,
    val totalTaskCount: Int = 0,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val meetingRepository: MeetingRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            // Combine all data flows into a single state
            combine(
                taskRepository.getTodayTasks(),
                taskRepository.getPendingTaskCount(),
                taskRepository.getCompletedTaskCount(),
                taskRepository.getTotalTaskCount(),
                meetingRepository.getUpcomingMeetings(5)
            ) { todayTasks, pending, completed, total, meetings ->
                DashboardUiState(
                    isLoading         = false,
                    greeting          = buildGreeting(),
                    todayTasks        = todayTasks,
                    upcomingMeetings  = meetings,
                    pendingTaskCount  = pending,
                    completedTaskCount = completed,
                    totalTaskCount    = total
                )
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }.collect { state ->
                _uiState.value = state
            }
        }

        // Collect today's events separately
        viewModelScope.launch {
            val todayStart = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            eventRepository.getEventsForDay(todayStart).collect { events ->
                _uiState.update { it.copy(todayEvents = events) }
            }
        }
    }

    fun toggleTaskCompletion(taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(taskId, completed)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun buildGreeting(): String {
        val hour = java.time.LocalTime.now().hour
        return when {
            hour < 12 -> "শুভ সকাল"
            hour < 17 -> "শুভ অপরাহ্ন"
            else      -> "শুভ সন্ধ্যা"
        }
    }
}
