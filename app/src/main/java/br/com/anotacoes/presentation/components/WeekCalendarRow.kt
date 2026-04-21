package br.com.anotacoes.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Faixa horizontal de dias navegáveis.
 *
 * - Destacado diferente: hoje tem borda, dia selecionado tem fundo preenchido.
 * - Quando selectedDate != today, exibe chip "Ir para hoje" no canto superior direito.
 * - Scroll animado para o dia selecionado sempre que ele mudar.
 * - Ponto indicador abaixo do número quando o dia possui tarefas ou lembretes.
 */
@Composable
fun WeekCalendarRow(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    datesWithTasks: Set<LocalDate> = emptySet(),
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    // Janela de ±45 dias a partir de hoje (91 itens)
    val days = remember { (-45L..45L).map { today.plusDays(it) } }
    val listState = rememberLazyListState()

    // Scroll animado para o dia selecionado sempre que ele mudar
    LaunchedEffect(selectedDate) {
        val index = days.indexOfFirst { it == selectedDate }
        if (index >= 0) {
            listState.animateScrollToItem(
                index = maxOf(0, index - 3) // deixa 3 itens à esquerda para contexto
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // ── "Ir para hoje" — aparece apenas quando o dia selecionado não é hoje ──
        AnimatedVisibility(
            visible = selectedDate != today,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 14.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onDateSelected(today) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Hoje",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // ── Faixa de dias ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(days, key = { it.toEpochDay() }) { date ->
                    DayItem(
                        date = date,
                        isSelected = date == selectedDate,
                        isToday = date == today,
                        hasEvent = datesWithTasks.contains(date),
                        onClick = { onDateSelected(date) }
                    )
                }
            }
            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )
        }
    }
}

@Composable
private fun DayItem(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasEvent: Boolean,
    onClick: () -> Unit
) {
    val circleColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val dayLabelColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        else -> MaterialTheme.colorScheme.outline
    }

    // Borda para hoje quando não está selecionado
    val borderModifier = if (isToday && !isSelected) {
        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Nome do dia da semana
        Text(
            text = date.dayOfWeek
                .getDisplayName(TextStyle.SHORT, Locale("pt", "BR"))
                .replaceFirstChar { it.titlecase() }
                .take(3),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = dayLabelColor
            ),
            modifier = Modifier.padding(top = 2.dp)
        )

        // Círculo com número do dia
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(circleColor)
                .then(borderModifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                ),
                textAlign = TextAlign.Center
            )
        }

        // Ponto indicador de tarefa/lembrete
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(
                    if (hasEvent) {
                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.secondary
                    } else Color.Transparent
                )
        )

        Spacer(modifier = Modifier.height(4.dp))
    }
}
