package com.mysecondrain.presentation.ui.voice

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mysecondrain.domain.model.*
import com.mysecondrain.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class VoiceUiState(
    val isListening: Boolean = false,
    val recognizedText: String = "",
    val parsedTitle: String = "",
    val isSaved: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class VoiceEntryViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    fun onSpeechResult(text: String) {
        val parsed = parseVoiceInput(text)
        _uiState.update {
            it.copy(
                recognizedText = text,
                parsedTitle    = parsed,
                isListening    = false
            )
        }
    }

    fun onListeningStart() {
        _uiState.update {
            it.copy(isListening = true, recognizedText = "", parsedTitle = "")
        }
    }

    fun onError(message: String) {
        _uiState.update { it.copy(isListening = false, errorMessage = message) }
    }

    fun saveAsTask() {
        val title = _uiState.value.parsedTitle.ifBlank { _uiState.value.recognizedText }
        if (title.isBlank()) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            taskRepository.addTask(
                Task(
                    title       = title,
                    description = "Added via voice",
                    priority    = Priority.MEDIUM,
                    dueDate     = LocalDate.now()
                )
            )
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    fun reset() {
        _uiState.update { VoiceUiState() }
    }

    private fun parseVoiceInput(text: String): String {
        var result = text
        listOf(
            "remind me to", "add task", "create task",
            "set reminder for", "don't forget to", "remember to"
        ).forEach { phrase ->
            result = result.replace(phrase, "", ignoreCase = true).trim()
        }
        return result.replaceFirstChar { it.uppercase() }
    }
}

// ─── Voice Entry Screen ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceEntryScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: VoiceEntryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) { if (state.isSaved) onSaved() }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val matches = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (!matches.isNullOrEmpty()) {
            viewModel.onSpeechResult(matches[0])
        } else {
            viewModel.onError("Could not recognize speech. Please try again.")
        }
    }

    fun startListening() {
        viewModel.onListeningStart()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your task or reminder...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechLauncher.launch(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Entry", fontWeight = FontWeight.SemiBold) },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            // Instruction text
            Text(
                text = when {
                    state.isListening -> "Listening..."
                    state.recognizedText.isNotBlank() -> "I heard you say:"
                    else -> "Tap the mic and speak your task or reminder"
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Mic button with pulse animation
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue  = if (state.isListening) 1.2f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(contentAlignment = Alignment.Center) {
                if (state.isListening) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    )
                }
                FloatingActionButton(
                    onClick = { startListening() },
                    modifier = Modifier.size(80.dp),
                    containerColor = if (state.isListening)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Outlined.Mic, "Speak",
                        modifier = Modifier.size(36.dp))
                }
            }

            // Recognized text display
            AnimatedVisibility(visible = state.recognizedText.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Recognized:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(state.recognizedText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Parsed task display
            AnimatedVisibility(visible = state.parsedTitle.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Task to be created:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(4.dp))
                        Text(state.parsedTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Error message
            state.errorMessage?.let { error ->
                Text(error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center)
            }

            Spacer(Modifier.weight(1f))

            // Action buttons
            AnimatedVisibility(visible = state.parsedTitle.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick   = viewModel::reset,
                        modifier  = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Refresh, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Try Again")
                    }
                    Button(
                        onClick  = viewModel::saveAsTask,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Save, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Save Task")
                    }
                }
            }

            // Examples
            if (state.recognizedText.isBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Try saying:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        listOf(
                            "\"Remind me to call Rahim tomorrow\"",
                            "\"Meeting with school committee on Friday\"",
                            "\"Submit exam papers by Monday\"",
                            "\"Buy groceries after school\""
                        ).forEach { example ->
                            Text("• $example",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}