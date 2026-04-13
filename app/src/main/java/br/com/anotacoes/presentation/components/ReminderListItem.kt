package br.com.anotacoes.presentation.components

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.draw.clip
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
    val alpha = if (isInactive) 0.45f else 1f
    val titleDecoration = if (reminder.status == ReminderStatus.COMPLETED) {
        TextDecoration.LineThrough
    } else {
        TextDecoration.None
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isInactive) 0.dp else 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isInactive)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge "Lembrete"
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lembrete",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.taskTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = titleDecoration
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (reminder.daysBeforeTask) {
                        1 -> "Falta 1 dia para a tarefa"
                        0 -> "Hoje é o dia da tarefa!"
                        else -> "Faltam ${reminder.daysBeforeTask} dias para a tarefa"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isInactive) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (reminder.status == ReminderStatus.COMPLETED) "Concluído" else "Dispensado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Row {
                if (!isInactive) {
                    IconButton(onClick = onComplete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Concluir lembrete",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dispensar lembrete",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(onClick = onReactivate, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Desfazer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Excluir lembrete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
