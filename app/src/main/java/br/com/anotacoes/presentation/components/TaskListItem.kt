package br.com.anotacoes.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskStatus
import br.com.anotacoes.domain.model.TaskTime

@Composable
fun TaskListItem(
    task: Task,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onReactivate: () -> Unit,
    onToggleNotification: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isInactive = task.status.isInactive
    val alpha = if (isInactive) 0.45f else 1f
    val titleDecoration = if (task.status == TaskStatus.COMPLETED) {
        TextDecoration.LineThrough
    } else {
        TextDecoration.None
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .alpha(alpha)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isInactive) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isInactive)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = titleDecoration
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatTaskTime(task.taskTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!task.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (task.checklistItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    val done = task.checklistItems.count { it.isChecked }
                    val total = task.checklistItems.size
                    Text(
                        text = "Checklist: $done/$total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                if (isInactive) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (task.status == TaskStatus.COMPLETED) "Concluida" else "Dispensada",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Row {
                if (onToggleNotification != null) {
                    IconButton(onClick = onToggleNotification) {
                        Icon(
                            if (task.showInNotification) Icons.Default.Notifications
                            else Icons.Default.NotificationsOff,
                            contentDescription = if (task.showInNotification) "Ocultar notificação" else "Exibir notificação",
                            tint = if (task.showInNotification)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline
                        )
                    }
                }
                if (!isInactive) {
                    IconButton(onClick = onComplete) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Concluir",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dispensar",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    IconButton(onClick = onReactivate) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Desfazer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun formatTaskTime(taskTime: TaskTime): String {
    return if (taskTime.isAllDay) {
        "O dia todo"
    } else {
        val start = taskTime.startTime
        val end = taskTime.endTime
        "${formatTime(start?.hour ?: 0, start?.minute ?: 0)} - ${formatTime(end?.hour ?: 0, end?.minute ?: 0)}"
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    return "%02d:%02d".format(hour, minute)
}
