package br.com.anotacoes.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.anotacoes.domain.model.Reminder
import br.com.anotacoes.domain.model.ReminderStatus
import br.com.anotacoes.domain.model.TaskTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDetailBottomSheet(
    reminder: Reminder,
    onDismissSheet: () -> Unit,
    onNavigateToTask: (String) -> Unit,
    onDismissReminder: () -> Unit,
    onCompleteReminder: () -> Unit,
    onRequestDeleteReminder: () -> Unit,
    onReactivateReminder: () -> Unit,
    onNotifyReminder: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismissSheet,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Lembrete",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    )
                }
                Text(
                    text = when (reminder.daysBeforeTask) {
                        1 -> "Tarefa é amanhã"
                        else -> "Tarefa em ${reminder.daysBeforeTask} dias"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // ── Tarefa associada ──────────────────────────────────────────────
            Text(
                text = "Para a tarefa",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.outline,
                    letterSpacing = 0.8.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onNavigateToTask(reminder.taskId) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.taskTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = formatTaskTime(reminder.task.taskTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Ver tarefa",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // ── Ações de status ───────────────────────────────────────────────
            Text(
                text = "Ações",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.outline,
                    letterSpacing = 0.8.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (reminder.status == ReminderStatus.ACTIVE) {
                ReminderActionRow(
                    icon = Icons.Default.Notifications,
                    label = "Notificar",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onNotifyReminder()
                        onDismissSheet()
                    }
                )
                ReminderActionRow(
                    icon = Icons.Default.CheckCircle,
                    label = "Concluir",
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        onCompleteReminder()
                        onDismissSheet()
                    }
                )
                ReminderActionRow(
                    icon = Icons.Default.Close,
                    label = "Dispensar",
                    color = MaterialTheme.colorScheme.outline,
                    onClick = {
                        onDismissReminder()
                        onDismissSheet()
                    }
                )
            } else {
                ReminderActionRow(
                    icon = Icons.Default.Refresh,
                    label = "Desfazer",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onReactivateReminder()
                        onDismissSheet()
                    }
                )
            }

            ReminderActionRow(
                icon = Icons.Default.Delete,
                label = "Excluir lembrete",
                color = MaterialTheme.colorScheme.error,
                onClick = {
                    onRequestDeleteReminder()
                    onDismissSheet()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReminderActionRow(
    icon: ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = color
            )
        )
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

private fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)
