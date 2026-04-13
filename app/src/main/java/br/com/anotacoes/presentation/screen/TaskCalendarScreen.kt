package br.com.anotacoes.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.anotacoes.presentation.TaskListIntent
import br.com.anotacoes.presentation.TaskListViewModel
import br.com.anotacoes.presentation.components.ReminderListItem
import br.com.anotacoes.presentation.components.TaskListItem
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MIN_YEAR_MONTH = YearMonth.now().minusYears(5)
private val MAX_YEAR_MONTH = YearMonth.now().plusYears(5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCalendarScreen(
    viewModel: TaskListViewModel,
    onNavigateToForm: (selectedDate: String) -> Unit,
    onNavigateToEditTask: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentMonth by remember { mutableStateOf(YearMonth.from(uiState.selectedDate)) }

    // Load indicators when month changes
    LaunchedEffect(currentMonth) {
        viewModel.onIntent(TaskListIntent.LoadMonthIndicators(currentMonth))
    }

    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", Locale("pt", "BR"))
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("pt", "BR"))

    // Confirmation dialog for deleting a task
    uiState.taskPendingDelete?.let {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(TaskListIntent.CancelDeleteTask) },
            title = { Text("Excluir tarefa") },
            text = { Text("Deseja mesmo excluir esta tarefa? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(TaskListIntent.ConfirmDeleteTask) }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(TaskListIntent.CancelDeleteTask) }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendario") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToForm(uiState.selectedDate.toString()) }) {
                Icon(Icons.Default.Add, contentDescription = "Nova tarefa")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // ── Month navigation header ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentMonth > MIN_YEAR_MONTH) {
                            currentMonth = currentMonth.minusMonths(1)
                        }
                    },
                    enabled = currentMonth > MIN_YEAR_MONTH
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
                }

                Text(
                    text = currentMonth.format(monthFormatter)
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(
                    onClick = {
                        if (currentMonth < MAX_YEAR_MONTH) {
                            currentMonth = currentMonth.plusMonths(1)
                        }
                    },
                    enabled = currentMonth < MAX_YEAR_MONTH
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Proximo mes")
                }
            }

            // ── Weekday header row ────────────────────────────────────────────
            val dayNames = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sab")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                dayNames.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Month grid ───────────────────────────────────────────────────
            MonthGrid(
                yearMonth = currentMonth,
                selectedDate = uiState.selectedDate,
                today = LocalDate.now(),
                datesWithTasks = uiState.datesWithTasks,
                onDateSelected = { date ->
                    viewModel.onIntent(TaskListIntent.SelectDate(date))
                }
            )

            Divider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // ── Selected date label ──────────────────────────────────────────
            Text(
                text = uiState.selectedDate.format(dateFormatter)
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            // ── Lembretes e tarefas para a data selecionada ──────────────────
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.tasks.isEmpty() && uiState.reminders.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhuma tarefa ou lembrete para este dia",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (uiState.reminders.isNotEmpty()) {
                            item {
                                Text(
                                    text = "LEMBRETES",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(
                                        start = 16.dp, end = 16.dp,
                                        top = 12.dp, bottom = 4.dp
                                    )
                                )
                            }
                            items(uiState.reminders, key = { "reminder_${it.id}" }) { reminder ->
                                ReminderListItem(
                                    reminder = reminder,
                                    onClick = {
                                        viewModel.onIntent(TaskListIntent.SelectReminderDetail(reminder))
                                    },
                                    onDismiss = {
                                        viewModel.onIntent(TaskListIntent.DismissReminder(reminder.id, reminder.taskId))
                                    },
                                    onComplete = {
                                        viewModel.onIntent(TaskListIntent.CompleteReminder(reminder.id, reminder.taskId))
                                    },
                                    onDelete = {
                                        viewModel.onIntent(TaskListIntent.RequestDeleteReminder(reminder))
                                    },
                                    onReactivate = {
                                        viewModel.onIntent(TaskListIntent.ReactivateReminder(reminder.id, reminder.taskId))
                                    }
                                )
                            }
                        }

                        if (uiState.tasks.isNotEmpty()) {
                            item {
                                Text(
                                    text = "TAREFAS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(
                                        start = 16.dp, end = 16.dp,
                                        top = 12.dp, bottom = 4.dp
                                    )
                                )
                            }
                            items(uiState.tasks, key = { it.id }) { task ->
                                TaskListItem(
                                    task = task,
                                    onClick = { onNavigateToEditTask(task.id) },
                                    onDelete = { viewModel.onIntent(TaskListIntent.RequestDeleteTask(task)) },
                                    onDismiss = { viewModel.onIntent(TaskListIntent.DismissTask(task.id)) },
                                    onComplete = { viewModel.onIntent(TaskListIntent.CompleteTask(task.id)) },
                                    onReactivate = { viewModel.onIntent(TaskListIntent.ReactivateTask(task.id)) }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom sheet de detalhe do lembrete
            uiState.selectedReminder?.let { reminder ->
                ReminderDetailBottomSheet(
                    reminder = reminder,
                    onDismissSheet = { viewModel.onIntent(TaskListIntent.DismissReminderDetail) },
                    onNavigateToTask = { taskId ->
                        viewModel.onIntent(TaskListIntent.DismissReminderDetail)
                        onNavigateToEditTask(taskId)
                    },
                    onDismissReminder = {
                        viewModel.onIntent(TaskListIntent.DismissReminder(reminder.id, reminder.taskId))
                    },
                    onCompleteReminder = {
                        viewModel.onIntent(TaskListIntent.CompleteReminder(reminder.id, reminder.taskId))
                    },
                    onRequestDeleteReminder = {
                        viewModel.onIntent(TaskListIntent.RequestDeleteReminder(reminder))
                    },
                    onReactivateReminder = {
                        viewModel.onIntent(TaskListIntent.ReactivateReminder(reminder.id, reminder.taskId))
                    },
                    onNotifyReminder = { }
                )
            }

            // Diálogo de confirmação para deletar lembrete
            uiState.reminderPendingDelete?.let {
                AlertDialog(
                    onDismissRequest = { viewModel.onIntent(TaskListIntent.CancelDeleteReminder) },
                    title = { Text("Excluir lembrete") },
                    text = { Text("Deseja mesmo excluir este lembrete? Esta ação não pode ser desfeita.") },
                    confirmButton = {
                        TextButton(onClick = { viewModel.onIntent(TaskListIntent.ConfirmDeleteReminder) }) {
                            Text("Excluir", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.onIntent(TaskListIntent.CancelDeleteReminder) }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    datesWithTasks: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit
) {
    // Sunday = 7 in ISO, but we want Sunday = 0 for the grid (first column)
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    // Sunday=0, Monday=1, ..., Saturday=6
    val firstDayOffset = firstDayOfMonth.dayOfWeek.value % 7
    val totalCells = firstDayOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - firstDayOffset + 1

                    if (dayNumber in 1..daysInMonth) {
                        val date = yearMonth.atDay(dayNumber)
                        DayCell(
                            dayNumber = dayNumber,
                            isSelected = date == selectedDate,
                            isToday = date == today,
                            hasTask = datesWithTasks.contains(date),
                            onClick = { onDateSelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasTask: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val circleColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderMod = if (isToday && !isSelected) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(circleColor)
            .then(borderMod)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayNumber.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontSize = 13.sp,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
        // Task indicator dot
        if (hasTask) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary
                    )
            )
        } else {
            Spacer(modifier = Modifier.size(4.dp))
        }
    }
}
