package com.mysecondrain.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

// ─── Data Class ───────────────────────────────────────────────────────────────

data class MoreMenuItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

// ─── More Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onMeetings: () -> Unit,
    onEvents: () -> Unit,
    onStatistics: () -> Unit,
    onVoice: () -> Unit,
    onCategories: () -> Unit,
    onBackup: () -> Unit,
    onSettings: () -> Unit
) {
    val menuItems = listOf(
        MoreMenuItem("Meetings",    Icons.Outlined.Groups,    Color(0xFFE65100), onMeetings),
        MoreMenuItem("Events",      Icons.Outlined.Cake,      Color(0xFFE91E63), onEvents),
        MoreMenuItem("Statistics",  Icons.Outlined.BarChart,  Color(0xFF1565C0), onStatistics),
        MoreMenuItem("Voice Entry", Icons.Outlined.Mic,       Color(0xFF2E7D32), onVoice),
        MoreMenuItem("Categories",  Icons.Outlined.Category,  Color(0xFF00838F), onCategories),
        MoreMenuItem("Backup",      Icons.Outlined.Backup,    Color(0xFF4E342E), onBackup),
        MoreMenuItem("Settings",    Icons.Outlined.Settings,  Color(0xFF6A1B9A), onSettings)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More", fontWeight = FontWeight.SemiBold) },
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
                .padding(16.dp)
        ) {
            Text(
                text = "Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(menuItems) { item ->
                    MoreMenuCard(item = item)
                }
            }
        }
    }
}

// ─── Menu Card ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreMenuCard(item: MoreMenuItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        onClick  = item.onClick,
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = item.color.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement   = Arrangement.Center,
            horizontalAlignment   = Alignment.Start
        ) {
            Icon(
                item.icon, null,
                tint     = item.color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text       = item.title,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = item.color
            )
        }
    }
}