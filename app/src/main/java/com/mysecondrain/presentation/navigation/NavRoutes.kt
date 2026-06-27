package com.mysecondrain.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Splash     : Screen("splash")
    object Dashboard  : Screen("dashboard")
    object Tasks      : Screen("tasks")
    object Calendar   : Screen("calendar")
    object Notes      : Screen("notes")
    object More       : Screen("more")
    object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: Long = -1) = "task_detail/$taskId"
    }
    object AddEditTask : Screen("add_edit_task?taskId={taskId}") {
        fun createRoute(taskId: Long = -1) = "add_edit_task?taskId=$taskId"
    }
    object Meetings   : Screen("meetings")
    object AddEditMeeting : Screen("add_edit_meeting?meetingId={meetingId}") {
        fun createRoute(meetingId: Long = -1) = "add_edit_meeting?meetingId=$meetingId"
    }
    object Events     : Screen("events")
    object AddEditEvent : Screen("add_edit_event?eventId={eventId}") {
        fun createRoute(eventId: Long = -1) = "add_edit_event?eventId=$eventId"
    }
    object NoteDetail : Screen("note_detail/{noteId}") {
        fun createRoute(noteId: Long) = "note_detail/$noteId"
    }
    object AddEditNote : Screen("add_edit_note?noteId={noteId}") {
        fun createRoute(noteId: Long = -1) = "add_edit_note?noteId=$noteId"
    }
    object Search     : Screen("search")
    object Statistics : Screen("statistics")
    object Settings   : Screen("settings")

    object About      : Screen("about")
    object Voice      : Screen("voice")
    object Categories : Screen("categories")
    object Backup     : Screen("backup")

    // ── Debt / Loan Tracker ──────────────────────────────────────────────────
    object Debts      : Screen("debts")
    object AddEditDebt : Screen("add_edit_debt?debtId={debtId}") {
        fun createRoute(debtId: Long = -1) = "add_edit_debt?debtId=$debtId"
    }
    object DebtDetail : Screen("debt_detail/{debtId}") {
        fun createRoute(debtId: Long) = "debt_detail/$debtId"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Home",     Icons.Filled.Home,          Icons.Outlined.Home),
    BottomNavItem(Screen.Tasks,     "Tasks",    Icons.Filled.CheckCircle,   Icons.Outlined.CheckCircle),
    BottomNavItem(Screen.Calendar,  "Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem(Screen.Notes,     "Notes",    Icons.Filled.StickyNote2,   Icons.Outlined.StickyNote2),
    BottomNavItem(Screen.More,      "More",     Icons.Filled.GridView,      Icons.Outlined.GridView)
)