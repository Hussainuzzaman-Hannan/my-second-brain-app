package com.mysecondrain.presentation.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class BackupUiState(
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val lastBackupTime: String = "Never",
    val message: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun backup(targetUri: Uri) {
        _uiState.update { it.copy(isBackingUp = true, message = null) }
        viewModelScope.launch {
            try {
                val dbFile = context.getDatabasePath("my_second_brain.db")
                if (!dbFile.exists()) {
                    _uiState.update {
                        it.copy(isBackingUp = false,
                            message = "Database not found", isSuccess = false)
                    }
                    return@launch
                }
                context.contentResolver.openOutputStream(targetUri)?.use { output ->
                    dbFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                } ?: throw java.io.IOException("Could not open destination file")

                val timestamp = SimpleDateFormat(
                    "yyyy-MM-dd_HH-mm", Locale.getDefault()
                ).format(Date())

                _uiState.update {
                    it.copy(
                        isBackingUp    = false,
                        lastBackupTime = timestamp,
                        message        = "Backup saved successfully",
                        isSuccess      = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isBackingUp = false,
                        message = "Backup failed: ${e.message}", isSuccess = false)
                }
            }
        }
    }

    fun restore(uri: Uri) {
        _uiState.update { it.copy(isRestoring = true, message = null) }
        viewModelScope.launch {
            try {
                val dbFile = context.getDatabasePath("my_second_brain.db")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dbFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        message     = "Restore successful! Please restart the app.",
                        isSuccess   = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isRestoring = false,
                        message = "Restore failed: ${e.message}", isSuccess = false)
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

// ─── Backup Screen ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { viewModel.backup(it) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.restore(it) }
    }

    state.message?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(4000)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore", fontWeight = FontWeight.SemiBold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Info, null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Your data is stored locally on this device. " +
                                "Regular backups are recommended.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Backup card
            BackupActionCard(
                icon      = Icons.Outlined.Backup,
                title     = "Backup Data",
                subtitle  = "Choose where to save your data",
                buttonText = if (state.isBackingUp) "Backing up..." else "Create Backup",
                color     = Color(0xFF2E7D32),
                isLoading = state.isBackingUp,
                onClick   = {
                    val timestamp = SimpleDateFormat(
                        "yyyy-MM-dd_HH-mm", Locale.getDefault()
                    ).format(Date())
                    backupLauncher.launch("MySecondBrain_backup_$timestamp.db")
                }
            )

            // Restore card
            BackupActionCard(
                icon       = Icons.Outlined.Restore,
                title      = "Restore Data",
                subtitle   = "Import data from a backup file",
                buttonText = if (state.isRestoring) "Restoring..." else "Choose Backup File",
                color      = Color(0xFF1565C0),
                isLoading  = state.isRestoring,
                onClick    = {
                    restoreLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                }
            )

            // Message
            state.message?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = if (state.isSuccess)
                            Color(0xFF2E7D32).copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (state.isSuccess) Icons.Outlined.CheckCircle
                            else Icons.Outlined.Error,
                            null,
                            tint = if (state.isSuccess) Color(0xFF2E7D32)
                            else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.isSuccess) Color(0xFF2E7D32)
                            else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    color: Color,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick  = onClick,
                enabled  = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = color)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color    = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(buttonText)
            }
        }
    }
}