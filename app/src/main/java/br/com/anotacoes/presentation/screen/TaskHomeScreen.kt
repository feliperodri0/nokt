package br.com.anotacoes.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.anotacoes.presentation.TaskListIntent
import br.com.anotacoes.presentation.TaskListViewModel
import br.com.anotacoes.presentation.components.ReminderListItem
import br.com.anotacoes.presentation.components.TaskListItem
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import br.com.anotacoes.presentation.components.WeekCalendarRow

@Composable
fun TaskHomeScreen(
    viewModel: TaskListViewModel,
    onNavigateToForm: () -> Unit,
    onNavigateToEditTask: (String) -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNotifyReminder: ((taskId: String, taskTitle: String, daysRemaining: Int) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Show reminder detail bottom sheet when a reminder is selected
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
            onNotifyReminder = {
                onNotifyReminder?.invoke(reminder.taskId, reminder.taskTitle, reminder.daysBeforeTask)
            }
        )
    }

    // Confirmation dialog for deleting a reminder
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

    val today = remember { LocalDate.now() }

    // Recarrega indicadores do mês sempre que a data selecionada mudar de mês
    val selectedMonth = YearMonth.from(uiState.selectedDate)
    LaunchedEffect(selectedMonth) {
        viewModel.onIntent(TaskListIntent.LoadMonthIndicators(selectedMonth))
    }

    val topBarDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale("pt", "BR"))
    val topBarTitle = uiState.selectedDate.format(topBarDateFormatter).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 44.dp, bottomEnd = 44.dp),
                modifier = Modifier.width(280.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(top = 54.dp, bottom = 22.dp, start = 22.dp, end = 22.dp)
                ) {
                    Text(
                        text = "Nokt",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Text(
                        text = "Organize seu dia",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    DrawerCustomItem(
                        icon = "🏠",
                        label = "Home",
                        isSelected = true,
                        onClick = {
                            if (drawerState.currentValue != DrawerValue.Closed) {
                                scope.launch { drawerState.close() }
                            }
                        }
                    )
                    DrawerCustomItem(
                        icon = "📅",
                        label = "Calendário",
                        isSelected = false,
                        onClick = {
                            if (drawerState.currentValue != DrawerValue.Closed) {
                                scope.launch {
                                    drawerState.close()
                                    onNavigateToCalendar()
                                }
                            } else {
                                scope.launch {
                                    onNavigateToCalendar()
                                }
                            }
                        }
                    )
                    DrawerCustomItem(
                        icon = "⚙️",
                        label = "Configurações",
                        isSelected = false,
                        onClick = {
                            if (drawerState.currentValue != DrawerValue.Closed) {
                                scope.launch {
                                    drawerState.close()
                                    onNavigateToSettings()
                                }
                            } else {
                                scope.launch {
                                    onNavigateToSettings()
                                }
                            }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CustomTopBar(
                    title = topBarTitle,
                    onMenuClick = {
                        if (drawerState.currentValue != DrawerValue.Open) {
                            scope.launch { drawerState.open() }
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToForm,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nova tarefa")
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                WeekCalendarRow(
                    selectedDate = uiState.selectedDate,
                    datesWithTasks = uiState.datesWithTasks,
                    onDateSelected = { viewModel.onIntent(TaskListIntent.SelectDate(it)) }
                )

                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    uiState.error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.error ?: "Erro desconhecido",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    uiState.tasks.isEmpty() && uiState.reminders.isEmpty() -> {
                        EmptyStateSection()
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            if (uiState.reminders.isNotEmpty()) {
                                item {
                                    ListHeaderSection(title = "Lembretes")
                                }
                                items(uiState.reminders, key = { "reminder_${it.id}" }) { reminder ->
                                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
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
                            }

                            if (uiState.tasks.isNotEmpty()) {
                                item {
                                    val taskSectionTitle = if (uiState.selectedDate == today) {
                                        "Tarefas de hoje"
                                    } else {
                                        val fmt = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("pt", "BR"))
                                        "Tarefas de ${uiState.selectedDate.format(fmt)}"
                                    }
                                    ListHeaderSection(title = taskSectionTitle)
                                }
                                items(uiState.tasks, key = { it.id }) { task ->
                                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                                        TaskListItem(
                                            task = task,
                                            onClick = { onNavigateToEditTask(task.id) },
                                            onDelete = { viewModel.onIntent(TaskListIntent.RequestDeleteTask(task)) },
                                            onDismiss = { viewModel.onIntent(TaskListIntent.DismissTask(task.id)) },
                                            onComplete = { viewModel.onIntent(TaskListIntent.CompleteTask(task.id)) },
                                            onReactivate = { viewModel.onIntent(TaskListIntent.ReactivateTask(task.id)) },
                                            onToggleNotification = { viewModel.onIntent(TaskListIntent.ToggleNotification(task.id)) }
                                        )
                                    }
                                }
                            }

                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomTopBar(title: String, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(elevation = 6.dp, spotColor = Color.Black.copy(alpha = 0.07f))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .width(36.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onMenuClick
                ),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(modifier = Modifier.size(width = 20.dp, height = 2.dp).clip(RoundedCornerShape(1.dp)).background(MaterialTheme.colorScheme.onSurface))
            Box(modifier = Modifier.size(width = 20.dp, height = 2.dp).clip(RoundedCornerShape(1.dp)).background(MaterialTheme.colorScheme.onSurface))
            Box(modifier = Modifier.size(width = 20.dp, height = 2.dp).clip(RoundedCornerShape(1.dp)).background(MaterialTheme.colorScheme.onSurface))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

//        Box(
//            modifier = Modifier
//                .size(34.dp)
//                .clip(CircleShape)
//                .background(MaterialTheme.colorScheme.primary),
//            contentAlignment = Alignment.Center
//        ) {
//            Text(
//                text = "F",
//                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
//                color = MaterialTheme.colorScheme.onPrimary
//            )
//        }
    }
}

@Composable
private fun ListHeaderSection(title: String) {
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
private fun EmptyStateSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(430.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "📋", fontSize = 46.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Sem tarefas para hoje",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Toque em + para adicionar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun DrawerCustomItem(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = icon, fontSize = 18.sp)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
            )
        )
    }
}