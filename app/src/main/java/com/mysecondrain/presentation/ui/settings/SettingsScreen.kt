package com.mysecondrain.presentation.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// ─── UiState ──────────────────────────────────────────────────────────────────

data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderHour: Int = 8,
    val dailyReminderMinute: Int = 0,
    val notificationsEnabled: Boolean = true,
    val appVersion: String = "1.0.0",
    val selectedLanguage: String = "English"   // ← যোগ করা হয়েছে
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            selectedLanguage = prefs.getString("language", "English") ?: "English"
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun toggleDarkMode() =
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }

    fun toggleDailyReminder() =
        _uiState.update { it.copy(dailyReminderEnabled = !it.dailyReminderEnabled) }

    fun toggleNotifications() =
        _uiState.update { it.copy(notificationsEnabled = !it.notificationsEnabled) }

    fun setReminderTime(hour: Int, minute: Int) =
        _uiState.update { it.copy(dailyReminderHour = hour, dailyReminderMinute = minute) }

    fun setLanguage(language: String) {
        prefs.edit().putString("language", language).apply()
        _uiState.update { it.copy(selectedLanguage = language) }
    }
}

// ─── Settings Screen ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    isDarkMode: Boolean,
    onToggleDark: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Appearance
            item { SettingsSectionHeader("Appearance") }
            item {
                SettingsToggleItem(
                    icon     = Icons.Outlined.DarkMode,
                    title    = "Dark Mode",
                    subtitle = if (isDarkMode) "Dark theme is on"
                    else "Light theme is on",
                    checked  = isDarkMode,
                    onToggle = { onToggleDark() },
                    color    = Color(0xFF1565C0)
                )
            }

            // Language
            item { SettingsSectionHeader("ভাষা / Language") }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "App Language",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("English", "বাংলা").forEach { lang ->
                                FilterChip(
                                    selected = state.selectedLanguage == lang,
                                    onClick  = { viewModel.setLanguage(lang) },
                                    label    = { Text(lang) }
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "ভাষা পরিবর্তনের পর app restart করুন",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Notifications
            item { SettingsSectionHeader("Notifications") }
            item {
                SettingsToggleItem(
                    icon     = Icons.Outlined.Notifications,
                    title    = "Enable Notifications",
                    subtitle = "Show reminders and alerts",
                    checked  = state.notificationsEnabled,
                    onToggle = viewModel::toggleNotifications,
                    color    = Color(0xFF6A1B9A)
                )
            }
            item {
                SettingsToggleItem(
                    icon     = Icons.Outlined.WbSunny,
                    title    = "Daily Summary",
                    subtitle = "Morning reminder of today's tasks",
                    checked  = state.dailyReminderEnabled,
                    onToggle = viewModel::toggleDailyReminder,
                    color    = Color(0xFFE65100)
                )
            }
            if (state.dailyReminderEnabled) {
                item {
                    SettingsInfoItem(
                        icon     = Icons.Outlined.Schedule,
                        title    = "Reminder Time",
                        subtitle = String.format(
                            "%02d:%02d %s",
                            if (state.dailyReminderHour % 12 == 0) 12
                            else state.dailyReminderHour % 12,
                            state.dailyReminderMinute,
                            if (state.dailyReminderHour < 12) "AM" else "PM"
                        ),
                        color    = Color(0xFFE65100)
                    )
                }
            }

            // Data
            item { SettingsSectionHeader("Data") }
            item {
                SettingsActionItem(
                    icon     = Icons.Outlined.Backup,
                    title    = "Backup & Restore",
                    subtitle = "Export or import your data",
                    color    = Color(0xFF2E7D32),
                    onClick  = {}
                )
            }

            // About
            item { SettingsSectionHeader("About") }
            item {
                SettingsInfoItem(
                    icon     = Icons.Outlined.Info,
                    title    = "Version",
                    subtitle = state.appVersion,
                    color    = MaterialTheme.colorScheme.primary
                )
            }
            item {
                SettingsInfoItem(
                    icon     = Icons.Outlined.Person,
                    title    = "My Second Brain",
                    subtitle = "Your personal life assistant",
                    color    = MaterialTheme.colorScheme.primary
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ─── Settings Components ──────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text       = title,
        style      = MaterialTheme.typography.labelLarge,
        color      = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier   = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                Text(subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick  = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                Text(subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsInfoItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                Text(subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}