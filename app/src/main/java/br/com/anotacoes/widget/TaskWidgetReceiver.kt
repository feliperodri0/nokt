package br.com.anotacoes.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.usecase.GetTodaysActiveTasksUseCase
import br.com.anotacoes.receiver.TaskDismissHandler
import br.com.anotacoes.receiver.TaskDismissReceiver
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun getTodaysActiveTasksUseCase(): GetTodaysActiveTasksUseCase
}

val taskIdKey = ActionParameters.Key<String>("task_id")

class TaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = TaskWidget()
}

class TaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val useCase = entryPoint.getTodaysActiveTasksUseCase()

        val now = LocalTime.now()
        val tasks = useCase(LocalDate.now(), now.hour, now.minute).first()

        provideContent {
            GlanceTheme {
                WidgetContent(context = context, tasks = tasks)
            }
        }
    }
}

@Composable
private fun WidgetContent(context: Context, tasks: List<Task>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F).copy(alpha = 0.85f))
            .padding(8.dp)
    ) {
        Text(
            text = "Tarefas Ativas",
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(Color.White),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.padding(bottom = 6.dp)
        )

        if (tasks.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma tarefa no momento",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color(0xFFCAC4D0)),
                        fontSize = 12.sp
                    )
                )
            }
        } else {
            tasks.forEach { task ->
                WidgetTaskRow(task = task)
            }
        }
    }
}

@Composable
private fun WidgetTaskRow(task: Task) {
    val timeStr = if (task.taskTime.isAllDay) "O dia todo" else {
        val s = task.taskTime.startTime
        val e = task.taskTime.endTime
        "%02d:%02d - %02d:%02d".format(s?.hour ?: 0, s?.minute ?: 0, e?.hour ?: 0, e?.minute ?: 0)
    }

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.title,
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = "  [X]",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color(0xFFFFB4AB)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.clickable(
                    actionRunCallback<DismissTaskFromWidgetAction>(
                        actionParametersOf(taskIdKey to task.id)
                    )
                )
            )
        }
        Text(
            text = timeStr,
            style = TextStyle(
                color = androidx.glance.unit.ColorProvider(Color(0xFFCAC4D0)),
                fontSize = 11.sp
            )
        )
    }
}

class DismissTaskFromWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[taskIdKey] ?: return

        // Send dismiss broadcast (reuses existing receiver)
        val dismissIntent = Intent(context, TaskDismissReceiver::class.java).apply {
            action = TaskDismissHandler.ACTION_DISMISS_TASK
            putExtra(TaskDismissHandler.EXTRA_TASK_ID, taskId)
        }
        context.sendBroadcast(dismissIntent)

        // Refresh widget
        GlanceAppWidgetManager(context)
            .getGlanceIds(TaskWidget::class.java)
            .forEach { id -> TaskWidget().update(context, id) }
    }
}
