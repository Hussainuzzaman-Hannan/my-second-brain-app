package com.mysecondrain.presentation.components

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    actionLabel: String = "",
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon, null,
            modifier = Modifier.size(72.dp),
            tint     = MaterialTheme.colorScheme.outline
        )
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.Center,
            color      = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (subtitle.isNotBlank()) {
            Text(
                text      = subtitle,
                style     = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.outline
            )
        }
        if (actionLabel.isNotBlank() && onAction != null) {
            Spacer(Modifier.height(4.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

// ─── Error State ──────────────────────────────────────────────────────────────

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Outlined.ErrorOutline, null,
            modifier = Modifier.size(64.dp),
            tint     = MaterialTheme.colorScheme.error
        )
        Text(
            text       = "Something went wrong",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.error
        )
        Text(
            text      = message,
            style     = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onRetry != null) {
            OutlinedButton(onClick = onRetry) {
                Icon(Icons.Outlined.Refresh, null,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Try Again")
            }
        }
    }
}

// ─── Loading State ────────────────────────────────────────────────────────────

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

// ─── Confirmation Dialog ──────────────────────────────────────────────────────

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirm",
    dismissLabel: String = "Cancel",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Text(message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors  = if (isDestructive)
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                else ButtonDefaults.textButtonColors()
            ) {
                Text(confirmLabel, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel)
            }
        }
    )
}

// ─── Info Banner ──────────────────────────────────────────────────────────────

@Composable
fun InfoBanner(
    message: String,
    icon: ImageVector = Icons.Outlined.Info,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.10f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null,
                tint     = color,
                modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text  = message,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

// ─── Priority Badge ───────────────────────────────────────────────────────────

@Composable
fun PriorityBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(4.dp),
        color    = color.copy(alpha = 0.12f)
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelSmall,
            color    = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}