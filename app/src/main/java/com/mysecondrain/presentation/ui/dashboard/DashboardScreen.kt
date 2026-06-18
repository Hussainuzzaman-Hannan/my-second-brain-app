package com.mysecondrain.presentation.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mysecondrain.domain.model.*
import com.mysecondrain.presentation.theme.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTasks: () -> Unit,
    onNavigateToMeetings: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onAddTask: () -> Unit,
    onTaskClick: (Long) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())

    Scaffold(
        topBar = {
            DashboardTopBar(
                greeting     = uiState.greeting,
                dateString   = today.format(dayFormatter),
                onSearchClick = onSearchClick
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTask,
                icon    = { Icon(Icons.Filled.Add, contentDescription = "Add task") },
                text    = { Text("Quick Add") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ── Stats Row ────────────────────────────────────────────────
                item {
                    StatsRow(
                        pending   = uiState.pendingTaskCount,
                        completed = uiState.completedTaskCount,
                        total     = uiState.totalTaskCount,
                        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                // ── Quick Actions ─────────────────────────────────────────────
                item {
                    SectionHeader(title = "Quick Access", modifier = Modifier.padding(horizontal = 16.dp))
                    QuickActionsRow(
                        onTasks    = onNavigateToTasks,
                        onMeetings = onNavigateToMeetings,
                        onCalendar = onNavigateToCalendar,
                        modifier   = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ── Today's Tasks ─────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        SectionHeader(title = "Today's Tasks")
                        TextButton(onClick = onNavigateToTasks) {
                            Text("See all", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (uiState.todayTasks.isEmpty()) {
                    item { EmptyStateCard(message = "No tasks for today 🎉", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
                } else {
                    items(uiState.todayTasks, key = { it.id }) { task ->
                        TaskCard(
                            task      = task,
                            onToggle  = { viewModel.toggleTaskCompletion(task.id, !task.isCompleted) },
                            onClick   = { onTaskClick(task.id) },
                            modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }

                // ── Upcoming Meetings ─────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        SectionHeader(title = "Upcoming Meetings")
                        TextButton(onClick = onNavigateToMeetings) {
                            Text("See all", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (uiState.upcomingMeetings.isEmpty()) {
                    item { EmptyStateCard(message = "No upcoming meetings", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
                } else {
                    items(uiState.upcomingMeetings.take(3), key = { "meeting_${it.id}" }) { meeting ->
                        MeetingCard(
                            meeting  = meeting,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                // ── Today's Events ────────────────────────────────────────────
                if (uiState.todayEvents.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        SectionHeader(title = "Today's Events", modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    items(uiState.todayEvents, key = { "event_${it.id}" }) { event ->
                        EventCard(
                            event    = event,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                // ── Today's Classes ───────────────────────────────────────────────────────────
                if (uiState.todayClasses.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            SectionHeader(title = "Today's Classes")
                        }
                    }
                    items(uiState.todayClasses, key = { "class_${it.id}" }) { classEvent ->
                        ClassCard(
                            event    = classEvent,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                item { Spacer(Modifier.height(88.dp)) } // FAB clearance
            }
        }
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    greeting: String,
    dateString: String,
    onSearchClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text  = "$greeting 👋",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text  = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Outlined.Search, contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = { /* notifications */ }) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurface)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

// ─── Stats Row ────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(
    pending: Int,
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            value = pending.toString(),
            label = "Pending",
            color = MaterialTheme.colorScheme.errorContainer,
            textColor = MaterialTheme.colorScheme.error,
            icon  = Icons.Outlined.PendingActions,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = completed.toString(),
            label = "Done",
            color = Color(0xFFE8F5E9),
            textColor = Color(0xFF2E7D32),
            icon  = Icons.Outlined.TaskAlt,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = total.toString(),
            label = "Total",
            color = MaterialTheme.colorScheme.primaryContainer,
            textColor = MaterialTheme.colorScheme.primary,
            icon  = Icons.Outlined.FormatListBulleted,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    color: Color,
    textColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = color),
        shape    = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector       = icon,
                contentDescription = null,
                tint              = textColor,
                modifier          = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = value,
                style = MaterialTheme.typography.headlineMedium,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.75f)
            )
        }
    }
}

// ─── Quick Actions ────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsRow(
    onTasks: () -> Unit,
    onMeetings: () -> Unit,
    onCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            QuickActionChip(
                label    = "All Tasks",
                icon     = Icons.Outlined.CheckCircle,
                color    = MaterialTheme.colorScheme.primary,
                onClick  = onTasks
            )
        }
        item {
            QuickActionChip(
                label   = "Meetings",
                icon    = Icons.Outlined.Groups,
                color   = Color(0xFFE65100),
                onClick = onMeetings
            )
        }
        item {
            QuickActionChip(
                label   = "Calendar",
                icon    = Icons.Outlined.CalendarMonth,
                color   = Color(0xFF6A1B9A),
                onClick = onCalendar
            )
        }
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label   = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        },
        shape  = RoundedCornerShape(20.dp),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.10f),
            labelColor     = color,
        )
    )
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color    = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(bottom = 4.dp)
    )
}

// ─── Task Card ────────────────────────────────────────────────────────────────

@Composable
private fun TaskCard(
    task: Task,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityColor = when (task.priority) {
        Priority.LOW    -> PriorityLow
        Priority.MEDIUM -> PriorityMedium
        Priority.HIGH   -> PriorityHigh
        Priority.URGENT -> PriorityUrgent
    }

    Card(
        modifier  = modifier.fillMaxWidth(),
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (task.isCompleted) Color.LightGray else priorityColor)
            )

            Spacer(Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = task.title,
                    style    = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color    = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurfaceVariant
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Category chip
                    task.category?.let { cat ->
                        val catColor = try { Color(android.graphics.Color.parseColor(cat.colorHex)) }
                        catch (_: Exception) { MaterialTheme.colorScheme.primary }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = catColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text     = cat.name,
                                style    = MaterialTheme.typography.labelSmall,
                                color    = catColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    // Due time
                    task.dueDateTime?.let { dt ->
                        Icon(Icons.Outlined.Schedule, contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text  = dt.format(DateTimeFormatter.ofPattern("hh:mm a")),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Priority label
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = priorityColor.copy(alpha = if (task.isCompleted) 0.05f else 0.12f)
                    ) {
                        Text(
                            text     = task.priority.label,
                            style    = MaterialTheme.typography.labelSmall,
                            color    = if (task.isCompleted) Color.LightGray else priorityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Checkbox
            Checkbox(
                checked         = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors          = CheckboxDefaults.colors(
                    checkedColor   = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

// ─── Meeting Card ─────────────────────────────────────────────────────────────

@Composable
private fun MeetingCard(
    meeting: Meeting,
    modifier: Modifier = Modifier
) {
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon bubble
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE65100).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Groups, contentDescription = null,
                    tint = Color(0xFFE65100), modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = meeting.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                if (meeting.personName.isNotBlank()) {
                    Text(
                        text  = "with ${meeting.personName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (meeting.location.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text  = meeting.location,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text  = meeting.dateTime.format(timeFormatter),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFE65100),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text  = meeting.dateTime.format(dateFormatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Event Card ───────────────────────────────────────────────────────────────

@Composable
private fun EventCard(
    event: Event,
    modifier: Modifier = Modifier
) {
    val eventColor = try { Color(android.graphics.Color.parseColor(event.colorHex)) }
    catch (_: Exception) { Color(0xFFFF6B6B) }

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = eventColor.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (event.eventType) {
                EventType.BIRTHDAY     -> Icons.Outlined.Cake
                EventType.ANNIVERSARY  -> Icons.Outlined.Favorite
                EventType.SCHOOL_EVENT -> Icons.Outlined.School
                EventType.CLASS        -> Icons.Outlined.MenuBook
                else                   -> Icons.Outlined.Event
            }
            Icon(icon, contentDescription = null, tint = eventColor,
                modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text  = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = eventColor
                )
                if (event.personName.isNotBlank()) {
                    Text(
                        text  = event.personName,
                        style = MaterialTheme.typography.bodySmall,
                        color = eventColor.copy(alpha = 0.75f)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = eventColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text     = event.eventType.label,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = eventColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ─── Class Card ───────────────────────────────────────────────────────────────

@Composable
private fun ClassCard(
    event: Event,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = Color(0xFF1565C0).copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon bubble
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1565C0).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.MenuBook, null,
                    tint     = Color(0xFF1565C0),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = event.title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color      = Color(0xFF1565C0),
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                if (event.personName.isNotBlank()) {
                    Text(
                        text  = event.personName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1565C0).copy(alpha = 0.7f)
                    )
                }
                if (event.description.isNotBlank()) {
                    Text(
                        text  = event.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1565C0).copy(alpha = 0.12f)
            ) {
                Text(
                    text     = "Class",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = Color(0xFF1565C0),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyStateCard(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
