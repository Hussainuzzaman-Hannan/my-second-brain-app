package com.mysecondrain.presentation.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mysecondrain.R

// ─── Feature model ──────────────────────────────────────────────────────────────

private data class AboutFeature(
    val icon: ImageVector,
    val color: Color,
    val titleRes: Int,
    val descRes: Int,
    val howToRes: Int
)

private val aboutFeatures = listOf(
    AboutFeature(Icons.Outlined.Home,               Color(0xFF1565C0), R.string.about_feature_dashboard_title,  R.string.about_feature_dashboard_desc,  R.string.about_feature_dashboard_howto),
    AboutFeature(Icons.Outlined.CheckCircle,         Color(0xFF2E7D32), R.string.about_feature_tasks_title,      R.string.about_feature_tasks_desc,      R.string.about_feature_tasks_howto),
    AboutFeature(Icons.Outlined.CalendarMonth,       Color(0xFF6A1B9A), R.string.about_feature_calendar_title,   R.string.about_feature_calendar_desc,   R.string.about_feature_calendar_howto),
    AboutFeature(Icons.Outlined.StickyNote2,         Color(0xFFF57F17), R.string.about_feature_notes_title,      R.string.about_feature_notes_desc,      R.string.about_feature_notes_howto),
    AboutFeature(Icons.Outlined.Groups,              Color(0xFFE65100), R.string.about_feature_meetings_title,   R.string.about_feature_meetings_desc,   R.string.about_feature_meetings_howto),
    AboutFeature(Icons.Outlined.Cake,                Color(0xFFE91E63), R.string.about_feature_events_title,     R.string.about_feature_events_desc,     R.string.about_feature_events_howto),
    AboutFeature(Icons.Outlined.AccountBalanceWallet,Color(0xFF00695C), R.string.about_feature_debts_title,      R.string.about_feature_debts_desc,      R.string.about_feature_debts_howto),
    AboutFeature(Icons.Outlined.Mic,                 Color(0xFFAD1457), R.string.about_feature_voice_title,      R.string.about_feature_voice_desc,      R.string.about_feature_voice_howto),
    AboutFeature(Icons.Outlined.BarChart,            Color(0xFF1565C0), R.string.about_feature_statistics_title, R.string.about_feature_statistics_desc, R.string.about_feature_statistics_howto),
    AboutFeature(Icons.Outlined.Category,            Color(0xFF00838F), R.string.about_feature_categories_title, R.string.about_feature_categories_desc, R.string.about_feature_categories_howto),
    AboutFeature(Icons.Outlined.Backup,              Color(0xFF4E342E), R.string.about_feature_backup_title,     R.string.about_feature_backup_desc,     R.string.about_feature_backup_howto),
    AboutFeature(Icons.Outlined.Widgets,             Color(0xFF3949AB), R.string.about_feature_widget_title,     R.string.about_feature_widget_desc,     R.string.about_feature_widget_howto),
    AboutFeature(Icons.Outlined.NotificationsActive, Color(0xFF6A1B9A), R.string.about_feature_reminders_title,  R.string.about_feature_reminders_desc,  R.string.about_feature_reminders_howto)
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title), fontWeight = FontWeight.SemiBold) },
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Psychology, null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.app_name),
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.about_tagline),
                        style     = MaterialTheme.typography.bodySmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier  = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // ── Intro ─────────────────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                    )
                ) {
                    Text(
                        stringResource(R.string.about_intro),
                        style    = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // ── Features section header ──────────────────────────────────────
            item {
                Text(
                    stringResource(R.string.about_section_features),
                    style      = MaterialTheme.typography.labelLarge,
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            }

            // ── Feature cards ────────────────────────────────────────────────
            items(aboutFeatures) { feature ->
                AboutFeatureCard(feature)
            }

            // ── Tips section ──────────────────────────────────────────────────
            item {
                Text(
                    stringResource(R.string.about_section_tips),
                    style      = MaterialTheme.typography.labelLarge,
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(top = 12.dp, bottom = 2.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AboutTipRow(stringResource(R.string.about_tip_1))
                        Spacer(Modifier.height(10.dp))
                        AboutTipRow(stringResource(R.string.about_tip_2))
                        Spacer(Modifier.height(10.dp))
                        AboutTipRow(stringResource(R.string.about_tip_3))
                    }
                }
            }

            // ── Footer ────────────────────────────────────────────────────────
            item {
                Text(
                    stringResource(R.string.about_made_for),
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.outline,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }

            // ── Developer info ───────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                DeveloperInfoCard()
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

// ─── Developer info card ───────────────────────────────────────────────────────

@Composable
private fun DeveloperInfoCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val phoneNumber = "8801719074004"           // country code + number, no symbols
    val email       = "mdhussainuzzamanhannan@gmail.com"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.about_developer_title),
            style      = MaterialTheme.typography.labelLarge,
            color      = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.about_developer_names),
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign  = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(12.dp),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                DeveloperContactRow(
                    icon    = Icons.Outlined.Chat,
                    label   = stringResource(R.string.about_developer_whatsapp),
                    value   = "+$phoneNumber",
                    color   = Color(0xFF2E7D32),
                    onClick = {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://wa.me/$phoneNumber")
                            )
                            context.startActivity(intent)
                        } catch (_: Exception) { }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                DeveloperContactRow(
                    icon    = Icons.Outlined.Send,
                    label   = stringResource(R.string.about_developer_telegram),
                    value   = "+$phoneNumber",
                    color   = Color(0xFF1565C0),
                    onClick = {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://t.me/+$phoneNumber")
                            )
                            context.startActivity(intent)
                        } catch (_: Exception) { }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                DeveloperContactRow(
                    icon    = Icons.Outlined.MailOutline,
                    label   = stringResource(R.string.about_developer_email),
                    value   = email,
                    color   = Color(0xFFC62828),
                    onClick = {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_SENDTO,
                                android.net.Uri.parse("mailto:$email")
                            )
                            context.startActivity(intent)
                        } catch (_: Exception) { }
                    }
                )
            }
        }
    }
}

@Composable
private fun DeveloperContactRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Icon(
            Icons.Outlined.ChevronRight, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Feature card (expandable) ─────────────────────────────────────────────────

@Composable
private fun AboutFeatureCard(feature: AboutFeature) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron_rotation")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick  = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(feature.color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(feature.icon, null, tint = feature.color, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(feature.titleRes),
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(feature.descRes),
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 2
                    )
                }
                Icon(
                    Icons.Outlined.ExpandMore, null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(rotation)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    Row {
                        Icon(
                            Icons.Outlined.Lightbulb, null,
                            tint     = feature.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(feature.howToRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ─── Tip row ────────────────────────────────────────────────────────────────────

@Composable
private fun AboutTipRow(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Outlined.CheckCircle, null,
            tint     = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text  = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}