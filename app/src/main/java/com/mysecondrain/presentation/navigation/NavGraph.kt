package com.mysecondrain.presentation.navigation

import com.mysecondrain.presentation.ui.settings.AboutScreen
import com.mysecondrain.presentation.ui.settings.CategoriesScreen
import com.mysecondrain.presentation.ui.settings.BackupScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mysecondrain.presentation.ui.calendar.CalendarScreen
import com.mysecondrain.presentation.ui.dashboard.DashboardScreen
import com.mysecondrain.presentation.ui.debts.AddEditDebtScreen
import com.mysecondrain.presentation.ui.debts.DebtDetailScreen
import com.mysecondrain.presentation.ui.debts.DebtsScreen
import com.mysecondrain.presentation.ui.events.AddEditEventScreen
import com.mysecondrain.presentation.ui.events.EventsScreen
import com.mysecondrain.presentation.ui.meetings.AddEditMeetingScreen
import com.mysecondrain.presentation.ui.meetings.MeetingsScreen
import com.mysecondrain.presentation.ui.notes.AddEditNoteScreen
import com.mysecondrain.presentation.ui.notes.NotesScreen
import com.mysecondrain.presentation.ui.search.SearchScreen
import com.mysecondrain.presentation.ui.settings.MoreScreen
import com.mysecondrain.presentation.ui.settings.SettingsScreen
import com.mysecondrain.presentation.ui.statistics.StatisticsScreen
import com.mysecondrain.presentation.ui.tasks.AddEditTaskScreen
import com.mysecondrain.presentation.ui.tasks.TasksScreen
import com.mysecondrain.presentation.ui.voice.VoiceEntryScreen

private val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(300))
}

private val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(300))
}

private val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300))
}

private val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(300))
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues,
    isDarkMode: Boolean,
    onToggleDark: () -> Unit
) {
    NavHost(
        navController      = navController,
        startDestination   = Screen.Dashboard.route,
        modifier           = Modifier.padding(paddingValues),
        enterTransition    = enterTransition,
        exitTransition     = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition  = popExitTransition
    ) {
        // ── Dashboard ─────────────────────────────────────────────────────────
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToTasks    = { navController.navigate(Screen.Tasks.route) },
                onNavigateToMeetings = { navController.navigate(Screen.Meetings.route) },
                onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                onAddTask            = { navController.navigate(Screen.AddEditTask.createRoute()) },
                onTaskClick          = { id -> navController.navigate(Screen.AddEditTask.createRoute(id)) },
                onSearchClick        = { navController.navigate(Screen.Search.route) }
            )
        }

        // ── Tasks ─────────────────────────────────────────────────────────────
        composable(Screen.Tasks.route) {
            TasksScreen(
                onAddTask   = { navController.navigate(Screen.AddEditTask.createRoute()) },
                onTaskClick = { id -> navController.navigate(Screen.AddEditTask.createRoute(id)) }
            )
        }
        composable(
            route     = Screen.AddEditTask.route,
            arguments = listOf(
                navArgument("taskId") {
                    defaultValue = -1L
                    type         = NavType.LongType
                }
            )
        ) { backStack ->
            val taskId = backStack.arguments?.getLong("taskId") ?: -1L
            AddEditTaskScreen(
                taskId  = taskId,
                onSaved = { navController.popBackStack() },
                onBack  = { navController.popBackStack() }
            )
        }
        composable(
            route     = Screen.TaskDetail.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.LongType }
            )
        ) { backStack ->
            val taskId = backStack.arguments?.getLong("taskId") ?: return@composable
            AddEditTaskScreen(
                taskId  = taskId,
                onSaved = { navController.popBackStack() },
                onBack  = { navController.popBackStack() }
            )
        }

        // ── Calendar ──────────────────────────────────────────────────────────
        composable(Screen.Calendar.route) {
            CalendarScreen(
                onAddTask   = { navController.navigate(Screen.AddEditTask.createRoute()) },
                onTaskClick = { id -> navController.navigate(Screen.AddEditTask.createRoute(id)) }
            )
        }

        // ── Notes ─────────────────────────────────────────────────────────────
        composable(Screen.Notes.route) {
            NotesScreen(
                onAddNote   = { navController.navigate(Screen.AddEditNote.createRoute()) },
                onNoteClick = { id -> navController.navigate(Screen.AddEditNote.createRoute(id)) }
            )
        }
        composable(
            route     = Screen.AddEditNote.route,
            arguments = listOf(
                navArgument("noteId") {
                    defaultValue = -1L
                    type         = NavType.LongType
                }
            )
        ) { backStack ->
            val noteId = backStack.arguments?.getLong("noteId") ?: -1L
            AddEditNoteScreen(
                noteId  = noteId,
                onSaved = { navController.popBackStack() },
                onBack  = { navController.popBackStack() }
            )
        }

        // ── More / Hub ────────────────────────────────────────────────────────
        composable(Screen.More.route) {
            MoreScreen(
                onMeetings   = { navController.navigate(Screen.Meetings.route) },
                onEvents     = { navController.navigate(Screen.Events.route) },
                onStatistics = { navController.navigate(Screen.Statistics.route) },
                onVoice      = { navController.navigate(Screen.Voice.route) },
                onCategories = { navController.navigate(Screen.Categories.route) },
                onBackup     = { navController.navigate(Screen.Backup.route) },
                onSettings   = { navController.navigate(Screen.Settings.route) },
                onDebts      = { navController.navigate(Screen.Debts.route) },
                isDarkMode   = isDarkMode
            )
        }

        // ── Meetings ──────────────────────────────────────────────────────────
        composable(Screen.Meetings.route) {
            MeetingsScreen(
                onAddMeeting   = { navController.navigate(Screen.AddEditMeeting.createRoute()) },
                onMeetingClick = { id -> navController.navigate(Screen.AddEditMeeting.createRoute(id)) },
                onBack         = { navController.popBackStack() }
            )
        }
        composable(
            route     = Screen.AddEditMeeting.route,
            arguments = listOf(
                navArgument("meetingId") {
                    defaultValue = -1L
                    type         = NavType.LongType
                }
            )
        ) { backStack ->
            val meetingId = backStack.arguments?.getLong("meetingId") ?: -1L
            AddEditMeetingScreen(
                meetingId = meetingId,
                onSaved   = { navController.popBackStack() },
                onBack    = { navController.popBackStack() }
            )
        }

        // ── Events ────────────────────────────────────────────────────────────
        composable(Screen.Events.route) {
            EventsScreen(
                onAddEvent   = { navController.navigate(Screen.AddEditEvent.createRoute()) },
                onEventClick = { id -> navController.navigate(Screen.AddEditEvent.createRoute(id)) },
                onBack       = { navController.popBackStack() }
            )
        }
        composable(
            route     = Screen.AddEditEvent.route,
            arguments = listOf(
                navArgument("eventId") {
                    defaultValue = -1L
                    type         = NavType.LongType
                }
            )
        ) { backStack ->
            val eventId = backStack.arguments?.getLong("eventId") ?: -1L
            AddEditEventScreen(
                eventId = eventId,
                onSaved = { navController.popBackStack() },
                onBack  = { navController.popBackStack() }
            )
        }

        // ── Search ────────────────────────────────────────────────────────────
        composable(Screen.Search.route) {
            SearchScreen(
                onTaskClick = { id -> navController.navigate(Screen.AddEditTask.createRoute(id)) },
                onNoteClick = { id -> navController.navigate(Screen.AddEditNote.createRoute(id)) },
                onDebtClick = { id -> navController.navigate(Screen.DebtDetail.createRoute(id)) }
            )
        }

        // ── Statistics ────────────────────────────────────────────────────────
        composable(Screen.Statistics.route) {
            StatisticsScreen()
        }

        // ── Settings ──────────────────────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack        = { navController.popBackStack() },
                onAboutClick  = { navController.navigate(Screen.About.route) },
                onBackupClick = { navController.navigate(Screen.Backup.route) },   // ← নতুন
                isDarkMode    = isDarkMode,
                onToggleDark  = onToggleDark
            )
        }

        // ── About ─────────────────────────────────────────────────────────────
        composable(Screen.About.route) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Voice Entry ───────────────────────────────────────────────────────
        composable(Screen.Voice.route) {
            VoiceEntryScreen(
                onBack  = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // ── Categories ────────────────────────────────────────────────────────
        composable(Screen.Categories.route) {
            CategoriesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Backup ────────────────────────────────────────────────────────────
        composable(Screen.Backup.route) {
            BackupScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Debts ─────────────────────────────────────────────────────────────
        composable(Screen.Debts.route) {
            DebtsScreen(
                onAddDebt   = { navController.navigate(Screen.AddEditDebt.createRoute()) },
                onDebtClick = { id -> navController.navigate(Screen.DebtDetail.createRoute(id)) },
                onBack      = { navController.popBackStack() }
            )
        }
        composable(
            route     = Screen.AddEditDebt.route,
            arguments = listOf(
                navArgument("debtId") {
                    defaultValue = -1L
                    type         = NavType.LongType
                }
            )
        ) { backStack ->
            val debtId = backStack.arguments?.getLong("debtId") ?: -1L
            AddEditDebtScreen(
                debtId  = debtId,
                onSaved = { navController.popBackStack() },
                onBack  = { navController.popBackStack() }
            )
        }
        composable(
            route     = Screen.DebtDetail.route,
            arguments = listOf(
                navArgument("debtId") { type = NavType.LongType }
            )
        ) { backStack ->
            val debtId = backStack.arguments?.getLong("debtId") ?: return@composable
            DebtDetailScreen(
                debtId    = debtId,
                onEdit    = { id -> navController.navigate(Screen.AddEditDebt.createRoute(id)) },
                onDeleted = { navController.popBackStack() },
                onBack    = { navController.popBackStack() }
            )
        }
    }
}