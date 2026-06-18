package com.mysecondrain.presentation.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mysecondrain.domain.model.Event
import com.mysecondrain.domain.model.EventType
import com.mysecondrain.domain.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repo: EventRepository
) : ViewModel() {
    val events = repo.getAllEvents()
    fun delete(id: Long) { viewModelScope.launch { repo.deleteEvent(id) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    onAddEvent: () -> Unit,
    onEventClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: EventsViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsStateWithLifecycle(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events & Birthdays", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = onAddEvent,
                icon           = { Icon(Icons.Filled.Add, null) },
                text           = { Text("New Event") },
                containerColor = Color(0xFFE91E63),
                contentColor   = Color.White
            )
        }
    ) { padding ->
        if (events.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Cake, null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(16.dp))
                    Text("No events yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Add birthdays, anniversaries & more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    EventListItem(
                        event    = event,
                        onClick  = { onEventClick(event.id) },
                        onDelete = { viewModel.delete(event.id) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventListItem(
    event: Event,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("d MMMM")
    val eventColor = try {
        Color(android.graphics.Color.parseColor(event.colorHex))
    } catch (_: Exception) {
        Color(0xFFE91E63)
    }
    val icon: ImageVector = when (event.eventType) {
        EventType.BIRTHDAY     -> Icons.Outlined.Cake
        EventType.ANNIVERSARY  -> Icons.Outlined.Favorite
        EventType.SCHOOL_EVENT -> Icons.Outlined.School
        EventType.CLASS        -> Icons.Outlined.MenuBook
        EventType.FAMILY_EVENT -> Icons.Outlined.Group
        else                   -> Icons.Outlined.Event
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete event?") },
            text  = { Text("\"${event.title}\" will be removed.") },
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
            containerColor = eventColor.copy(alpha = 0.07f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(eventColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null,
                    tint = eventColor,
                    modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = eventColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                if (event.personName.isNotBlank()) {
                    Text(event.personName,
                        style = MaterialTheme.typography.bodySmall,
                        color = eventColor.copy(alpha = 0.7f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = eventColor.copy(alpha = 0.12f)
                    ) {
                        Text(event.eventType.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = eventColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    if (event.isYearlyRecurring) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text("Yearly",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(event.eventDate.format(dateFormatter),
                    style = MaterialTheme.typography.labelMedium,
                    color = eventColor,
                    fontWeight = FontWeight.SemiBold)
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Outlined.Delete, null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}