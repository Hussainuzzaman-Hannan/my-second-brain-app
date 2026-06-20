package com.mysecondrain.presentation.ui.debts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mysecondrain.R
import com.mysecondrain.domain.model.DebtPayment
import com.mysecondrain.domain.model.DebtStatus
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── Helper ───────────────────────────────────────────────────────────────────

fun formatTaka(amount: Double): String {
    return "৳" + String.format(Locale.US, "%,.0f", amount)
}

// ─── Status Badge ─────────────────────────────────────────────────────────────

@Composable
fun StatusBadge(status: DebtStatus) {
    val color = when (status) {
        DebtStatus.PENDING        -> Color(0xFFE65100)
        DebtStatus.PARTIALLY_PAID -> Color(0xFF1565C0)
        DebtStatus.PAID           -> Color(0xFF2E7D32)
    }
    val label = stringResource(
        when (status) {
            DebtStatus.PENDING        -> R.string.debt_status_pending
            DebtStatus.PARTIALLY_PAID -> R.string.debt_status_partial
            DebtStatus.PAID           -> R.string.debt_status_paid
        }
    )
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            label,
            style    = MaterialTheme.typography.labelSmall,
            color    = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}

// ─── Payment History Item ─────────────────────────────────────────────────────

@Composable
fun PaymentHistoryItem(payment: DebtPayment, accentColor: Color) {
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
fun AddPaymentDialog(
    remainingAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note   by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.debt_payment_dialog_title), fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.debt_payment_remaining, formatTaka(remainingAmount)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                    label = { Text(stringResource(R.string.debt_payment_amount)) },
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
                    label = { Text(stringResource(R.string.debt_payment_note)) },
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
            ) { Text(stringResource(R.string.debt_payment_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.debt_cancel)) }
        }
    )
}