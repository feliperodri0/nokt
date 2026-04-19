package br.com.anotacoes.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val alpha = if (isInactive) 0.48f else 1f
    val titleDecoration = if (task.status == TaskStatus.COMPLETED) {
        TextDecoration.LineThrough
    } else {
        TextDecoration.None
    }

    // Left bar color: primary = active, secondary = completed, muted/outline = dismissed
    val barColor = when (task.status) {
        TaskStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        TaskStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .shadow(elevation = if (isInactive) 0.dp else 2.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
    ) {
        // Left colored bar (5 dp wide, fills card height)
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(72.dp)
                .background(barColor)
        )

        // Content
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = titleDecoration
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Time chip
                val timeLabel = formatTaskTime(task.taskTime)
                val chipBg = if (task.taskTime.isAllDay) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
                val chipFg = if (task.taskTime.isAllDay) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimary
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(chipBg)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = chipFg
                    )
                }
                if (task.checklistItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    val done = task.checklistItems.count { it.isChecked }
                    val total = task.checklistItems.size
                    Text(
                        text = "Checklist: $done/$total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
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

            // Action buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onToggleNotification != null) {
                    IconButton(
                        onClick = onToggleNotification,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (task.showInNotification) Icons.Default.Notifications
                            else Icons.Default.NotificationsOff,
                            contentDescription = if (task.showInNotification) "Ocultar notificacao" else "Exibir notificacao",
                            tint = if (task.showInNotification)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (!isInactive) {
                    // ok button: success tinted circle
                    ActionCircleButton(
                        onClick = onComplete,
                        bgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        iconColor = MaterialTheme.colorScheme.primary,
                        icon = Icons.Default.CheckCircle,
                        description = "Concluir"
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    // dismiss button: warn tinted
                    ActionCircleButton(
                        onClick = onDismiss,
                        bgColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                        iconColor = MaterialTheme.colorScheme.tertiary,
                        icon = Icons.Default.Close,
                        description = "Dispensar"
                    )
                } else {
                    ActionCircleButton(
                        onClick = onReactivate,
                        bgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        iconColor = MaterialTheme.colorScheme.primary,
                        icon = Icons.Default.Refresh,
                        description = "Desfazer"
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                // delete: danger tinted
                ActionCircleButton(
                    onClick = onDelete,
                    bgColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                    iconColor = MaterialTheme.colorScheme.error,
                    icon = Icons.Default.Delete,
                    description = "Excluir"
                )
            }
        }
    }
}

@Composable
private fun ActionCircleButton(
    onClick: () -> Unit,
    bgColor: androidx.compose.ui.graphics.Color,
    iconColor: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun formatTaskTime(taskTime: TaskTime): String {
    return if (taskTime.isAllDay) {
        "Dia inteiro"
    } else {
        val start = taskTime.startTime
        val end = taskTime.endTime
        "${formatTime(start?.hour ?: 0, start?.minute ?: 0)} - ${formatTime(end?.hour ?: 0, end?.minute ?: 0)}"
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    return "%02d:%02d".format(hour, minute)
}
