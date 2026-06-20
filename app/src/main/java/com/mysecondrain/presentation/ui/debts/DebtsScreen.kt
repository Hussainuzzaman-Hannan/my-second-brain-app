package com.mysecondrain.presentation.ui.debts

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mysecondrain.domain.model.Debt
import com.mysecondrain.domain.model.DebtStatus
import com.mysecondrain.domain.model.DebtType
import com.mysecondrain.domain.repository.DebtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

private val owedToMeColor = Color(0xFF2E7D32)   // সবুজ — আমি পাই
private val iOweColor     = Color(0xFFC62828)   // লাল — আমি দেই

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class DebtsUiState(
    val debtsOwedToMe: List<Debt> = emptyList(),
    val debtsIOwe: List<Debt> = emptyList(),
    val totalOwedToMe: Double = 0.0,
    val totalIOwe: Double = 0.0,
    val isLoading: Boolean = true
)

@HiltViewModel
class DebtsViewModel @Inject constructor(
    private val repo: DebtRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebtsUiState())
    val uiState: StateFlow<DebtsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repo.getDebtsOwedToMe(),
                repo.getDebtsIOwe(),
                repo.getTotalOwedToMe(),
                repo.getTotalIOwe()
            ) { owedToMe, iOwe, totalOwedToMe, totalIOwe ->
                DebtsUiState(
                    debtsOwedToMe = owedToMe,
                    debtsIOwe     = iOwe,
                    totalOwedToMe = totalOwedToMe,
                    totalIOwe     = totalIOwe,
                    isLoading     = false
                )
            }.collect { _uiState.value = it }
        }
    }

    fun deleteDebt(id: Long) {
        viewModelScope.launch { repo.deleteDebt(id) }
    }
}

// ─── Debts Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    onAddDebt: () -> Unit,
    onDebtClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: DebtsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) } // 0 = আমি পাই, 1 = আমি দেই

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("হিসাব / পাওনা-দেনা", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = onAddDebt,
                icon           = { Icon(Icons.Filled.Add, null) },
                text           = { Text("নতুন হিসাব") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary cards
            SummaryRow(
                totalOwedToMe = state.totalOwedToMe,
                totalIOwe     = state.totalIOwe,
                modifier      = Modifier.padding(16.dp)
            )

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    text     = {
                        Text("আমি পাই (${state.debtsOwedToMe.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal)
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    text     = {
                        Text("আমি দেই (${state.debtsIOwe.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal)
                    }
                )
            }

            val list = if (selectedTab == 0) state.debtsOwedToMe else state.debtsIOwe
            val accentColor = if (selectedTab == 0) owedToMeColor else iOweColor

            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (selectedTab == 0) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint     = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (selectedTab == 0) "কেউ আপনার কাছে কোনো টাকা পাওনা নেই"
                            else "আপনি কারো কাছে কোনো টাকা পাওনা নেই",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("নতুন হিসাব যোগ করতে + চাপুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(list, key = { it.id }) { debt ->
                        DebtListItem(
                            debt        = debt,
                            accentColor = accentColor,
                            onClick     = { onDebtClick(debt.id) },
                            onDelete    = { viewModel.deleteDebt(debt.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ─── Summary Row ──────────────────────────────────────────────────────────────

@Composable
private fun SummaryRow(
    totalOwedToMe: Double,
    totalIOwe: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(
            label    = "আমি পাবো",
            amount   = totalOwedToMe,
            color    = owedToMeColor,
            icon     = Icons.Outlined.TrendingUp,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label    = "আমি দিবো",
            amount   = totalIOwe,
            color    = iOweColor,
            icon     = Icons.Outlined.TrendingDown,
            modifier = Modifier.weight(1f)
        )
    }

    val netBalance = totalOwedToMe - totalIOwe
    Spacer(Modifier.height(12.dp))
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (netBalance >= 0)
                owedToMeColor.copy(alpha = 0.08f)
            else
                iOweColor.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "নিট হিসাব",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                formatTaka(kotlin.math.abs(netBalance)) +
                        if (netBalance >= 0) " পাবেন" else " দিতে হবে",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = if (netBalance >= 0) owedToMeColor else iOweColor
            )
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                formatTaka(amount),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = color
            )
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f))
        }
    }
}

// ─── Debt List Item ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtListItem(
    debt: Debt,
    accentColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("মুছে ফেলবেন?") },
            text  = { Text("\"${debt.personName}\" এর এই হিসাবটি মুছে যাবে।") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("মুছুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("বাতিল") }
            }
        )
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        onClick   = onClick,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    debt.personName.take(1).uppercase(),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = accentColor
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    debt.personName,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                if (debt.reason.isNotBlank()) {
                    Text(
                        debt.reason,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        debt.debtDate.format(dateFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    StatusBadge(status = debt.status)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatTaka(debt.remainingAmount),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = accentColor
                )
                if (debt.paidAmount > 0) {
                    Text(
                        "মোট ${formatTaka(debt.totalAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(
                    onClick  = { showDeleteDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Outlined.Delete, null,
                        modifier = Modifier.size(14.dp),
                        tint     = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: DebtStatus) {
    val (color, _) = when (status) {
        DebtStatus.PENDING        -> Color(0xFFE65100) to "বাকি"
        DebtStatus.PARTIALLY_PAID -> Color(0xFF1565C0) to "আংশিক"
        DebtStatus.PAID           -> Color(0xFF2E7D32) to "পরিশোধ"
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            status.label,
            style    = MaterialTheme.typography.labelSmall,
            color    = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}

// ─── Helper ───────────────────────────────────────────────────────────────────

fun formatTaka(amount: Double): String {
    return "৳" + String.format(Locale.US, "%,.0f", amount)
}