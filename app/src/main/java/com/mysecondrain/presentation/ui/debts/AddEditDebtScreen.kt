package com.mysecondrain.presentation.ui.debts

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.mysecondrain.domain.model.DebtType
import com.mysecondrain.domain.repository.DebtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.inject.Inject

// ─── UiState ──────────────────────────────────────────────────────────────────

data class AddEditDebtUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val personName: String = "",
    val debtType: DebtType = DebtType.OWES_ME,
    val totalAmount: String = "",
    val reason: String = "",
    val debtDate: LocalDate = LocalDate.now(),
    val dueDate: LocalDate? = null,
    val notes: String = "",
    val paidAmount: Double = 0.0,
    val payments: List<DebtPayment> = emptyList(),
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false
) {
    val canSave: Boolean
        get() = personName.isNotBlank() &&
                totalAmount.toDoubleOrNull() != null &&
                (totalAmount.toDoubleOrNull() ?: 0.0) > 0
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class AddEditDebtViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val debtId: Long = savedStateHandle.get<Long>("debtId") ?: -1L
    private val _uiState = MutableStateFlow(AddEditDebtUiState())
    val uiState: StateFlow<AddEditDebtUiState> = _uiState.asStateFlow()

    init {
        if (debtId > 0) loadDebt()
        else _uiState.update { it.copy(isLoading = false) }
    }

    private fun loadDebt() {
        viewModelScope.launch {
            debtRepository.getDebtById(debtId).firstOrNull()?.let { d ->
                _uiState.update {
                    it.copy(
                        isLoading   = false,
                        isEditMode  = true,
                        personName  = d.personName,
                        debtType    = d.debtType,
                        totalAmount = d.totalAmount.toInt().toString(),
                        reason      = d.reason,
                        debtDate    = d.debtDate,
                        dueDate     = d.dueDate,
                        notes       = d.notes,
                        paidAmount  = d.paidAmount
                    )
                }
            } ?: _uiState.update { it.copy(isLoading = false) }
        }

        viewModelScope.launch {
            debtRepository.getPaymentsForDebt(debtId).collect { payments ->
                _uiState.update { it.copy(payments = payments) }
            }
        }
    }

    fun onPersonNameChange(v: String)   = _uiState.update { it.copy(personName = v) }
    fun onDebtTypeChange(t: DebtType)   = _uiState.update { it.copy(debtType = t) }
    fun onAmountChange(v: String) {
        if (v.isEmpty() || v.toDoubleOrNull() != null) {
            _uiState.update { it.copy(totalAmount = v) }
        }
    }
    fun onReasonChange(v: String)       = _uiState.update { it.copy(reason = v) }
    fun onDebtDateChange(d: LocalDate)  = _uiState.update { it.copy(debtDate = d) }
    fun onDueDateChange(d: LocalDate?)  = _uiState.update { it.copy(dueDate = d) }
    fun onNotesChange(v: String)        = _uiState.update { it.copy(notes = v) }

    fun save() {
        val s = _uiState.value
        if (!s.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val debt = Debt(
                id          = if (s.isEditMode) debtId else 0,
                personName  = s.personName.trim(),
                debtType    = s.debtType,
                totalAmount = s.totalAmount.toDouble(),
                paidAmount  = s.paidAmount,
                reason      = s.reason.trim(),
                debtDate    = s.debtDate,
                dueDate     = s.dueDate,
                notes       = s.notes.trim()
            )
            try {
                if (s.isEditMode) debtRepository.updateDebt(debt)
                else debtRepository.addDebt(debt)
                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun addPayment(amount: Double, note: String) {
        if (debtId <= 0 || amount <= 0) return
        viewModelScope.launch {
            debtRepository.addPayment(debtId, amount, note)
            debtRepository.getDebtById(debtId).firstOrNull()?.let { d ->
                _uiState.update { it.copy(paidAmount = d.paidAmount) }
            }
        }
    }

    fun delete() {
        if (debtId <= 0) return
        viewModelScope.launch {
            debtRepository.deleteDebt(debtId)
            _uiState.update { it.copy(savedSuccessfully = true) }
        }
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDebtScreen(
    debtId: Long,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddEditDebtViewModel = hiltViewModel()
) {
    val state   by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onSaved()
    }

    val accentColor = if (state.debtType == DebtType.OWES_ME)
        Color(0xFF2E7D32) else Color(0xFFC62828)

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("মুছে ফেলবেন?") },
            text  = { Text("এই হিসাবটি স্থায়ীভাবে মুছে যাবে।") },
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

    if (showPaymentDialog) {
        AddPaymentDialog(
            remainingAmount = state.totalAmount.toDoubleOrNull()?.minus(state.paidAmount) ?: 0.0,
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
                title = {
                    Text(
                        if (state.isEditMode) "হিসাব সম্পাদনা" else "নতুন হিসাব",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (state.isEditMode) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(
                        onClick = viewModel::save,
                        enabled = state.canSave && !state.isSaving
                    ) {
                        if (state.isSaving)
                            CircularProgressIndicator(Modifier.size(16.dp))
                        else
                            Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Debt Type selector
                item {
                    Text("ধরন",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.debtType == DebtType.OWES_ME,
                            onClick  = { viewModel.onDebtTypeChange(DebtType.OWES_ME) },
                            label    = { Text("আমি পাই") },
                            leadingIcon = {
                                Icon(Icons.Outlined.TrendingUp, null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                selectedLabelColor     = Color(0xFF2E7D32),
                                selectedLeadingIconColor = Color(0xFF2E7D32)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = state.debtType == DebtType.I_OWE,
                            onClick  = { viewModel.onDebtTypeChange(DebtType.I_OWE) },
                            label    = { Text("আমি দেই") },
                            leadingIcon = {
                                Icon(Icons.Outlined.TrendingDown, null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFC62828).copy(alpha = 0.15f),
                                selectedLabelColor     = Color(0xFFC62828),
                                selectedLeadingIconColor = Color(0xFFC62828)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Person Name
                item {
                    OutlinedTextField(
                        value         = state.personName,
                        onValueChange = viewModel::onPersonNameChange,
                        label         = { Text("ব্যক্তির নাম *") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        leadingIcon   = { Icon(Icons.Outlined.Person, null) }
                    )
                }

                // Amount
                item {
                    OutlinedTextField(
                        value         = state.totalAmount,
                        onValueChange = viewModel::onAmountChange,
                        label         = { Text("টাকার পরিমাণ *") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        leadingIcon   = {
                            Text("৳", style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 12.dp))
                        }
                    )
                }

                // Reason
                item {
                    OutlinedTextField(
                        value         = state.reason,
                        onValueChange = viewModel::onReasonChange,
                        label         = { Text("কারণ (যেমন: হাত খরচ, ধার)") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        leadingIcon   = { Icon(Icons.Outlined.Notes, null) }
                    )
                }

                // Debt Date
                item {
                    val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy")
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context, { _, y, m, d ->
                                        viewModel.onDebtDateChange(LocalDate.of(y, m + 1, d))
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.CalendarToday, null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("লেনদেনের তারিখ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(state.debtDate.format(fmt))
                            }
                        }
                    }
                }

                // Due Date (optional)
                item {
                    val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy")
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context, { _, y, m, d ->
                                        viewModel.onDueDateChange(LocalDate.of(y, m + 1, d))
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.EventBusy, null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("ফেরতের তারিখ (ঐচ্ছিক)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(state.dueDate?.format(fmt) ?: "নির্ধারণ করুন")
                            }
                            if (state.dueDate != null) {
                                IconButton(onClick = { viewModel.onDueDateChange(null) }) {
                                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Notes
                item {
                    OutlinedTextField(
                        value         = state.notes,
                        onValueChange = viewModel::onNotesChange,
                        label         = { Text("নোট") },
                        modifier      = Modifier.fillMaxWidth(),
                        minLines      = 2,
                        maxLines      = 4,
                        leadingIcon   = { Icon(Icons.Outlined.StickyNote2, null) }
                    )
                }

                // ── Payment History (শুধু edit mode এ দেখাবে) ──────────────────
                if (state.isEditMode) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                    }

                    val totalAmt = state.totalAmount.toDoubleOrNull() ?: 0.0
                    val remaining = totalAmt - state.paidAmount

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
                                        Text("পরিশোধিত",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatTaka(state.paidAmount),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("বাকি",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatTaka(remaining),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor)
                                    }
                                }
                                if (remaining > 0) {
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick  = { showPaymentDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors   = ButtonDefaults.buttonColors(containerColor = accentColor)
                                    ) {
                                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            if (state.debtType == DebtType.OWES_ME)
                                                "পরিশোধ পেয়েছি যোগ করুন"
                                            else
                                                "পরিশোধ করেছি যোগ করুন"
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.CheckCircle, null,
                                            tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("সম্পূর্ণ পরিশোধিত",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }

                    if (state.payments.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text("পরিশোধের ইতিহাস",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                        }
                        items(state.payments, key = { it.id }) { payment ->
                            PaymentHistoryItem(payment = payment, accentColor = accentColor)
                        }
                    }
                }

                item { Spacer(Modifier.height(60.dp)) }
            }
        }
    }
}

// ─── Payment History Item ─────────────────────────────────────────────────────

@Composable
private fun PaymentHistoryItem(payment: DebtPayment, accentColor: Color) {
    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Receipt, null,
                tint = accentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(formatTaka(payment.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor)
                if (payment.note.isNotBlank()) {
                    Text(payment.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(payment.paymentDate.format(fmt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
        }
    }
}

// ─── Add Payment Dialog ───────────────────────────────────────────────────────

@Composable
private fun AddPaymentDialog(
    remainingAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note   by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("পরিশোধ যোগ করুন", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "বাকি আছে: ${formatTaka(remainingAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                    label = { Text("পরিমাণ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    leadingIcon = { Text("৳") }
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("নোট (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    amount.toDoubleOrNull()?.let { amt ->
                        if (amt > 0) onConfirm(amt, note)
                    }
                },
                enabled = amount.toDoubleOrNull()?.let { it > 0 } ?: false
            ) { Text("যোগ করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}