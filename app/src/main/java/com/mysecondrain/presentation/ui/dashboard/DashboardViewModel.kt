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
    val todayClasses: List<Event> = emptyList(),
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
            combine(
                taskRepository.getTodayTasks(),
                taskRepository.getPendingTaskCount(),
                taskRepository.getCompletedTaskCount(),
                taskRepository.getTotalTaskCount(),
                meetingRepository.getUpcomingMeetings(5)
            ) { todayTasks, pending, completed, total, meetings ->
                DashboardUiState(
                    isLoading          = false,
                    greeting           = buildGreeting(),
                    todayTasks         = todayTasks,
                    upcomingMeetings   = meetings,
                    pendingTaskCount   = pending,
                    completedTaskCount = completed,
                    totalTaskCount     = total
                )
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }.collect { state ->
                // আগের todayEvents/todayClasses preserve করো, বাকি সব আপডেট করো
                _uiState.update { current ->
                    state.copy(
                        todayEvents  = current.todayEvents,
                        todayClasses = current.todayClasses
                    )
                }
            }
        }

        // Today's events & weekly recurring classes
        viewModelScope.launch {
            val today = LocalDate.now()
            val todayStart = today
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val todayWeekDay = toWeekDay(today)

            combine(
                eventRepository.getEventsForDay(todayStart),
                eventRepository.getAllEvents()
            ) { dayEvents, allEvents ->

                // আজকের তারিখে সরাসরি পড়া one-time class (weekly recurring নয়)
                val directClasses = dayEvents.filter {
                    it.eventType == EventType.CLASS && !it.isWeeklyRecurring
                }

                // Weekly recurring class — শুধু আজকের weekday এর সাথে মিললে দেখাবে
                val weeklyClasses = allEvents.filter { event ->
                    event.eventType == EventType.CLASS &&
                            event.isWeeklyRecurring &&
                            event.weeklyDay == todayWeekDay &&
                            event.eventDate <= today
                }

                val allTodayClasses = (directClasses + weeklyClasses)
                    .distinctBy { it.id }
                    .sortedBy { it.startTime }

                val otherEvents = dayEvents.filter { it.eventType != EventType.CLASS }

                Pair(otherEvents, allTodayClasses)
            }.collect { (otherEvents, classes) ->
                _uiState.update {
                    it.copy(
                        todayEvents  = otherEvents,
                        todayClasses = classes
                    )
                }
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

    // java.time.DayOfWeek কে আমাদের WeekDay enum এ রূপান্তর করে
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