package com.mysecondrain.presentation.ui.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mysecondrain.domain.model.Debt
import com.mysecondrain.domain.model.DebtPayment
import com.mysecondrain.domain.model.DebtStatus
import com.mysecondrain.domain.model.DebtType
import com.mysecondrain.domain.repository.DebtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─── UiState ──────────────────────────────────────────────────────────────────

data class DebtDetailUiState(
    val isLoading: Boolean = true,
    val debt: Debt? = null,
    val payments: List<DebtPayment> = emptyList(),
    val deleted: Boolean = false
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class DebtDetailViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val debtId: Long = savedStateHandle.get<Long>("debtId") ?: -1L

    private val _uiState = MutableStateFlow(DebtDetailUiState())
    val uiState: StateFlow<DebtDetailUiState> = _uiState.asStateFlow()

    init {
        if (debtId > 0) {
            viewModelScope.launch {
                combine(
                    debtRepository.getDebtById(debtId),
                    debtRepository.getPaymentsForDebt(debtId)
                ) { debt, payments -> debt to payments }
                    .collect { (debt, payments) ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                debt      = debt,
                                payments  = payments
                            )
                        }
                    }
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun addPayment(amount: Double, note: String) {
        if (debtId <= 0 || amount <= 0) return
        viewModelScope.launch { debtRepository.addPayment(debtId, amount, note) }
    }

    fun delete() {
        if (debtId <= 0) return
        viewModelScope.launch {
            debtRepository.deleteDebt(debtId)
            _uiState.update { it.copy(deleted = true) }
        }
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailScreen(
    debtId: Long,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: DebtDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }

    val debt = state.debt
    val accentColor = if (debt?.debtType == DebtType.OWES_ME)
        Color(0xFF2E7D32) else Color(0xFFC62828)

    if (showDeleteDialog && debt != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("মুছে ফেলবেন?") },
            text  = { Text("\"${debt.personName}\" এর এই হিসাবটি স্থায়ীভাবে মুছে যাবে।") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.delete(); showDeleteDialog = false },
                    colors  = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("মুছুন") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("বাতিল") }
            }
        )
    }

    if (showPaymentDialog && debt != null) {
        AddPaymentDialog(
            remainingAmount = debt.remainingAmount,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, note ->
                viewModel.addPayment(amount, note)
                showPaymentDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("হিসাবের বিস্তারিত", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (debt != null) {
                        IconButton(onClick = { onEdit(debt.id) }) {
                            Icon(Icons.Outlined.Edit, "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Outlined.Delete, "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            debt == null -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "হিসাবটি খুঁজে পাওয়া যায়নি",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ── Person header ──────────────────────────────────────────
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    debt.personName.take(1).uppercase(),
                                    style      = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color      = accentColor
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    debt.personName,
                                    style      = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        debt.debtType.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = accentColor
                                    )
                                    StatusBadge(status = debt.status)
                                }
                            }
                        }
                    }

                    if (debt.reason.isNotBlank()) {
                        item {
                            Text(
                                debt.reason,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // ── Amount summary ─────────────────────────────────────────
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = CardDefaults.cardColors(
                                containerColor = accentColor.copy(alpha = 0.08f)
                            )
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "মোট পরিমাণ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            formatTaka(debt.totalAmount),
                                            style      = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column {
                                        Text(
                                            "পরিশোধিত",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            formatTaka(debt.paidAmount),
                                            style      = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color      = Color(0xFF2E7D32)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "বাকি",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            formatTaka(debt.remainingAmount),
                                            style      = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color      = accentColor
                                        )
                                    }
                                }

                                if (!debt.isFullyPaid) {
                                    Spacer(Modifier.height(14.dp))
                                    Button(
                                        onClick  = { showPaymentDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors   = ButtonDefaults.buttonColors(containerColor = accentColor)
                                    ) {
                                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            if (debt.debtType == DebtType.OWES_ME)
                                                "পরিশোধ পেয়েছি যোগ করুন"
                                            else
                                                "পরিশোধ করেছি যোগ করুন"
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.height(10.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.CheckCircle, null,
                                            tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "সম্পূর্ণ পরিশোধিত",
                                            style      = MaterialTheme.typography.bodySmall,
                                            color      = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Dates ───────────────────────────────────────────────────
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "হিসাবের তারিখ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    debt.debtDate.format(dateFormatter),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (debt.dueDate != null) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "শেষ তারিখ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        debt.dueDate.format(dateFormatter),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    if (debt.notes.isNotBlank()) {
                        item {
                            HorizontalDivider()
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "নোট",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                debt.notes,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // ── Payment history ─────────────────────────────────────────
                    item {
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "পরিশোধের ইতিহাস",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (state.payments.isEmpty()) {
                        item {
                            Text(
                                "এখনো কোনো পরিশোধ যোগ করা হয়নি",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        items(state.payments, key = { it.id }) { payment ->
                            PaymentHistoryItem(payment = payment, accentColor = accentColor)
                        }
                    }

                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
}