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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
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
import br.com.anotacoes.domain.model.Reminder
import br.com.anotacoes.domain.model.ReminderStatus

@Composable
fun ReminderListItem(
    reminder: Reminder,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onReactivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isInactive = reminder.isInactive
    val alpha = if (isInactive) 0.48f else 1f
    val titleDecoration = if (reminder.status == ReminderStatus.COMPLETED) {
        TextDecoration.LineThrough
    } else {
        TextDecoration.None
    }

    // Left bar: secondary for active, muted/outline for inactive
    val barColor = if (isInactive) MaterialTheme.colorScheme.outline
                   else MaterialTheme.colorScheme.secondary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .shadow(elevation = if (isInactive) 0.dp else 2.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
    ) {
        // Left colored bar
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(72.dp)
                .background(barColor)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // "Lembrete" badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Lembrete",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reminder.taskTitle,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = titleDecoration
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = when (reminder.daysBeforeTask) {
                        1 -> "Falta 1 dia para a tarefa"
                        0 -> "Hoje e o dia da tarefa!"
                        else -> "Faltam ${reminder.daysBeforeTask} dias para a tarefa"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isInactive) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (reminder.status == ReminderStatus.COMPLETED) "Concluido" else "Dispensado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isInactive) {
                    ReminderActionCircle(
                        onClick = onComplete,
                        bgColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                        iconColor = MaterialTheme.colorScheme.secondary,
                        icon = Icons.Default.CheckCircle,
                        description = "Concluir lembrete"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ReminderActionCircle(
                        onClick = onDismiss,
                        bgColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                        iconColor = MaterialTheme.colorScheme.tertiary,
                        icon = Icons.Default.Close,
                        description = "Dispensar lembrete"
                    )
                } else {
                    ReminderActionCircle(
                        onClick = onReactivate,
                        bgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        iconColor = MaterialTheme.colorScheme.primary,
                        icon = Icons.Default.Refresh,
                        description = "Desfazer"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                ReminderActionCircle(
                    onClick = onDelete,
                    bgColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                    iconColor = MaterialTheme.colorScheme.error,
                    icon = Icons.Default.Delete,
                    description = "Excluir lembrete"
                )
            }
        }
    }
}

@Composable
private fun ReminderActionCircle(
    onClick: () -> Unit,
    bgColor: androidx.compose.ui.graphics.Color,
    iconColor: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
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
