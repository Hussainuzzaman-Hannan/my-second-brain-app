package com.mysecondrain.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.material3.ColorProviders
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.mysecondrain.MainActivity
import com.mysecondrain.domain.model.Priority
import com.mysecondrain.domain.model.Task
import com.mysecondrain.domain.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

// ─── Hilt Entry Point ─────────────────────────────────────────────────────────

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TaskWidgetEntryPoint {
    fun taskRepository(): TaskRepository
}

// ─── Widget ───────────────────────────────────────────────────────────────────

class TaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            TaskWidgetEntryPoint::class.java
        )
        val tasks = entryPoint.taskRepository()
            .getTodayTasks()
            .first()
            .filter { !it.isCompleted }
            .take(5)

        provideContent {
            TaskWidgetContent(
                tasks     = tasks,
                context   = context
            )
        }
    }
}

// ─── Widget Content ───────────────────────────────────────────────────────────

@Composable
private fun TaskWidgetContent(
    tasks: List<Task>,
    context: Context
) {
    val bgColor    = ColorProvider(Color(0xFFF5F5F5))
    val textColor  = ColorProvider(Color(0xFF1A1A1A))
    val subColor   = ColorProvider(Color(0xFF757575))
    val accentColor = ColorProvider(Color(0xFF3949AB))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {

            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = "📋 Today's Tasks",
                    style = TextStyle(
                        color    = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text  = "${tasks.size} pending",
                    style = TextStyle(
                        color    = subColor,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(GlanceModifier.height(8.dp))

            // Divider
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ColorProvider(Color(0xFFE0E0E0)))
            ) {}

            Spacer(GlanceModifier.height(8.dp))

            if (tasks.isEmpty()) {
                // Empty state
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "🎉 No pending tasks today!",
                        style = TextStyle(
                            color    = subColor,
                            fontSize = 13.sp
                        )
                    )
                }
            } else {
                // Task list
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    tasks.forEach { task ->
                        WidgetTaskItem(
                            task      = task,
                            textColor = textColor,
                            subColor  = subColor
                        )
                        Spacer(GlanceModifier.height(6.dp))
                    }
                }
            }
        }
    }
}

// ─── Widget Task Item ─────────────────────────────────────────────────────────

@Composable
private fun WidgetTaskItem(
    task: Task,
    textColor: ColorProvider,
    subColor: ColorProvider
) {
    val dotColor = when (task.priority) {
        Priority.LOW    -> ColorProvider(Color(0xFF2E7D32))
        Priority.MEDIUM -> ColorProvider(Color(0xFF1565C0))
        Priority.HIGH   -> ColorProvider(Color(0xFFE65100))
        Priority.URGENT -> ColorProvider(Color(0xFFB71C1C))
    }

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Priority dot
        Box(
            modifier = GlanceModifier
                .size(8.dp)
                .background(dotColor)
        ) {}

        Spacer(GlanceModifier.width(8.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text     = task.title,
                style    = TextStyle(
                    color    = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            task.dueDate?.let { date ->
                Text(
                    text  = date.toString(),
                    style = TextStyle(
                        color    = subColor,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}