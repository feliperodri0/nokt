package br.com.anotacoes.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.presentation.TaskListIntent
import br.com.anotacoes.presentation.TaskListViewModel
import br.com.anotacoes.presentation.components.ReminderListItem
import br.com.anotacoes.presentation.components.TaskListItem
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Calendario",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToForm(uiState.selectedDate.toString()) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = androidx.compose.foundation.shape.CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova tarefa")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Calendar section with pinch-to-zoom ───────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(uiState.isCalendarExpanded) {
                            detectTransformGestures { _, _, zoom, _ ->
                                if (zoom > 1.03f && !uiState.isCalendarExpanded) {
                                    viewModel.onIntent(TaskListIntent.ToggleCalendarExpanded(true))
                                } else if (zoom < 0.97f && uiState.isCalendarExpanded) {
                                    viewModel.onIntent(TaskListIntent.ToggleCalendarExpanded(false))
                                }
                            }
                        }
                ) {
                    // ── Month navigation header ──────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
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
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "Mes anterior",
                                tint = if (currentMonth > MIN_YEAR_MONTH)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                        }

                        Text(
                            text = currentMonth.format(monthFormatter)
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = {
                                if (currentMonth < MAX_YEAR_MONTH) {
                                    currentMonth = currentMonth.plusMonths(1)
                                }
                            },
                            enabled = currentMonth < MAX_YEAR_MONTH
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Proximo mes",
                                tint = if (currentMonth < MAX_YEAR_MONTH)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // ── Weekday header row ────────────────────────────────────
                    val dayNames = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sab")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        dayNames.forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.outline,
                                letterSpacing = 1.4.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ── Month grid ───────────────────────────────────────────
                    MonthGrid(
                        yearMonth = currentMonth,
                        selectedDate = uiState.selectedDate,
                        today = LocalDate.now(),
                        datesWithTasks = uiState.datesWithTasks,
                        tasksByDate = uiState.tasksByDate,
                        isExpanded = uiState.isCalendarExpanded,
                        onDateSelected = { date ->
                            viewModel.onIntent(TaskListIntent.SelectDate(date))
                        }
                    )
                }

                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // ── Selected date label ──────────────────────────────────────
                Text(
                    text = uiState.selectedDate.format(dateFormatter)
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // ── Lembretes e tarefas para a data selecionada ──────────────
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
                                    CalendarSectionHeader("Lembretes")
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
                                    CalendarSectionHeader("Tarefas")
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

            // ── Onboarding overlay ───────────────────────────────────────────
            if (uiState.showExpandOnboarding) {
                CalendarZoomOnboardingOverlay(
                    onDismiss = { viewModel.onIntent(TaskListIntent.DismissExpandOnboarding) }
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
    tasksByDate: Map<LocalDate, List<Task>>,
    isExpanded: Boolean,
    onDateSelected: (LocalDate) -> Unit
) {
    // Sunday = 7 in ISO, but we want Sunday = 0 for the grid (first column)
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOffset = firstDayOfMonth.dayOfWeek.value % 7
    val totalCells = firstDayOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    AnimatedContent(
        targetState = isExpanded,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
        label = "calendarExpand"
    ) { expanded ->
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
                                tasks = tasksByDate[date] ?: emptyList(),
                                isExpanded = expanded,
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
}

@Composable
private fun DayCell(
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasTask: Boolean,
    tasks: List<Task>,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isExpanded) {
        // ── Expanded layout: rectangle with coloured task chips ──────────────
        val bgColor = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.background
        }
        val numColor = when {
            isToday || isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onBackground
        }
        val borderMod = when {
            isSelected -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            isToday -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            else -> Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
        }

        Column(
            modifier = modifier
                .animateContentSize()
                .aspectRatio(0.55f)
                .padding(2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .then(borderMod)
                .clickable(onClick = onClick)
                .padding(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = numColor,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            val visibleTasks = tasks.take(3)
            val overflow = tasks.size - visibleTasks.size
            // Cycle through pri / sec / ter chip colors
            visibleTasks.forEachIndexed { index, task ->
                val (chipBg, chipFg) = when (index % 3) {
                    0 -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                    1 -> Pair(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
                    else -> Pair(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(chipBg)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.title,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = chipFg,
                        lineHeight = 10.sp
                    )
                }
            }
            if (overflow > 0) {
                Text(
                    text = "+$overflow",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
    } else {
        // ── Compact layout: circle with secondary dot ────────────────────────
        val circleColor = when {
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surface
        }
        val textColor = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            isToday -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onBackground
        }
        val borderMod = if (isToday && !isSelected) {
            Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
        } else {
            Modifier
        }

        Column(
            modifier = modifier
                .animateContentSize()
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
                fontSize = 12.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            // Dot indicator: secondary color per design spec
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            hasTask && isSelected -> MaterialTheme.colorScheme.onPrimary
                            hasTask -> MaterialTheme.colorScheme.secondary
                            else -> androidx.compose.ui.graphics.Color.Transparent
                        }
                    )
            )
        }
    }
}

@Composable
private fun CalendarSectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 6.dp, start = 18.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.secondary)
        )
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        )
    }
}

@Composable
private fun CalendarZoomOnboardingOverlay(onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "onboardingPulse")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.64f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ZoomOutMap,
                contentDescription = null,
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer(scaleX = iconScale, scaleY = iconScale),
                tint = Color.White
            )
            Text(
                text = "Expanda o calendario",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Use o gesto de pinca (abrir os dedos) sobre o calendario para visualizar suas tarefas no grid expandido.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 26.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Entendi",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
