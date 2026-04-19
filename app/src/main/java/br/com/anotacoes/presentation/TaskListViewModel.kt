package br.com.anotacoes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.anotacoes.domain.model.DayOfWeek
import br.com.anotacoes.domain.model.RecurrenceType
import br.com.anotacoes.domain.model.Reminder
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.usecase.CompleteReminderUseCase
import br.com.anotacoes.domain.usecase.CompleteTaskUseCase
import br.com.anotacoes.domain.usecase.DeleteReminderUseCase
import br.com.anotacoes.domain.usecase.DeleteTaskUseCase
import br.com.anotacoes.domain.usecase.DismissReminderUseCase
import br.com.anotacoes.domain.usecase.DismissTaskUseCase
import br.com.anotacoes.domain.usecase.GetAllTasksUseCase
import br.com.anotacoes.domain.usecase.GetRemindersForDateUseCase
import br.com.anotacoes.domain.usecase.GetTasksForDateUseCase
import br.com.anotacoes.domain.usecase.ReactivateReminderUseCase
import br.com.anotacoes.domain.usecase.ReactivateTaskUseCase
import br.com.anotacoes.domain.usecase.ToggleTaskNotificationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val navigateToForm: Boolean = false,
    val datesWithTasks: Set<LocalDate> = emptySet(),
    val selectedReminder: Reminder? = null,
    val reminderPendingDelete: Reminder? = null,
    val taskPendingDelete: Task? = null
)

sealed class TaskListIntent {
    data class SelectDate(val date: LocalDate) : TaskListIntent()
    data class DeleteTask(val taskId: String) : TaskListIntent()
    data class DismissTask(val taskId: String) : TaskListIntent()
    data object NavigateToCreateTask : TaskListIntent()
    data object NavigationHandled : TaskListIntent()
    data object ReloadTasks : TaskListIntent()
    data class LoadMonthIndicators(val yearMonth: YearMonth) : TaskListIntent()
    data class CompleteTask(val taskId: String) : TaskListIntent()
    data class ToggleNotification(val taskId: String) : TaskListIntent()
    data class DismissReminder(val reminderId: String, val taskId: String) : TaskListIntent()
    data class CompleteReminder(val reminderId: String, val taskId: String) : TaskListIntent()
    data class DeleteReminder(val reminderId: String, val taskId: String) : TaskListIntent()
    data class SelectReminderDetail(val reminder: Reminder) : TaskListIntent()
    data object DismissReminderDetail : TaskListIntent()
    data class ReactivateReminder(val reminderId: String, val taskId: String) : TaskListIntent()
    data class RequestDeleteReminder(val reminder: Reminder) : TaskListIntent()
    data object ConfirmDeleteReminder : TaskListIntent()
    data object CancelDeleteReminder : TaskListIntent()
    data class ReactivateTask(val taskId: String) : TaskListIntent()
    data class RequestDeleteTask(val task: Task) : TaskListIntent()
    data object ConfirmDeleteTask : TaskListIntent()
    data object CancelDeleteTask : TaskListIntent()
}

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val getTasksForDateUseCase: GetTasksForDateUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val dismissTaskUseCase: DismissTaskUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val getAllTasksUseCase: GetAllTasksUseCase,
    private val toggleTaskNotificationUseCase: ToggleTaskNotificationUseCase,
    private val getRemindersForDateUseCase: GetRemindersForDateUseCase,
    private val dismissReminderUseCase: DismissReminderUseCase,
    private val completeReminderUseCase: CompleteReminderUseCase,
    private val deleteReminderUseCase: DeleteReminderUseCase,
    private val reactivateReminderUseCase: ReactivateReminderUseCase,
    private val reactivateTaskUseCase: ReactivateTaskUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var monthJob: Job? = null
    private var reminderJob: Job? = null

    init {
        loadTasksForDate(LocalDate.now())
        loadRemindersForDate(LocalDate.now())
        loadMonthIndicators(YearMonth.now())
    }

    fun onIntent(intent: TaskListIntent) {
        when (intent) {
            is TaskListIntent.SelectDate -> {
                loadTasksForDate(intent.date)
                loadRemindersForDate(intent.date)
            }
            is TaskListIntent.DeleteTask -> deleteTask(intent.taskId)
            is TaskListIntent.DismissTask -> dismissTask(intent.taskId)
            is TaskListIntent.NavigateToCreateTask -> {
                _uiState.value = _uiState.value.copy(navigateToForm = true)
            }
            is TaskListIntent.NavigationHandled -> {
                _uiState.value = _uiState.value.copy(navigateToForm = false)
            }
            is TaskListIntent.ReloadTasks -> {
                loadTasksForDate(_uiState.value.selectedDate)
                loadRemindersForDate(_uiState.value.selectedDate)
            }
            is TaskListIntent.LoadMonthIndicators -> loadMonthIndicators(intent.yearMonth)
            is TaskListIntent.CompleteTask -> completeTask(intent.taskId)
            is TaskListIntent.ToggleNotification -> toggleNotification(intent.taskId)
            is TaskListIntent.DismissReminder -> dismissReminder(intent.reminderId, intent.taskId)
            is TaskListIntent.CompleteReminder -> completeReminder(intent.reminderId, intent.taskId)
            is TaskListIntent.DeleteReminder -> deleteReminder(intent.reminderId, intent.taskId)
            is TaskListIntent.SelectReminderDetail -> {
                _uiState.value = _uiState.value.copy(selectedReminder = intent.reminder)
            }
            is TaskListIntent.DismissReminderDetail -> {
                _uiState.value = _uiState.value.copy(selectedReminder = null)
            }
            is TaskListIntent.ReactivateReminder -> reactivateReminder(intent.reminderId, intent.taskId)
            is TaskListIntent.RequestDeleteReminder -> {
                _uiState.value = _uiState.value.copy(reminderPendingDelete = intent.reminder)
            }
            is TaskListIntent.ConfirmDeleteReminder -> {
                _uiState.value.reminderPendingDelete?.let { reminder ->
                    deleteReminder(reminder.id, reminder.taskId)
                }
                _uiState.value = _uiState.value.copy(reminderPendingDelete = null)
            }
            is TaskListIntent.CancelDeleteReminder -> {
                _uiState.value = _uiState.value.copy(reminderPendingDelete = null)
            }
            is TaskListIntent.ReactivateTask -> reactivateTask(intent.taskId)
            is TaskListIntent.RequestDeleteTask -> {
                _uiState.value = _uiState.value.copy(taskPendingDelete = intent.task)
            }
            is TaskListIntent.ConfirmDeleteTask -> {
                _uiState.value.taskPendingDelete?.let { task ->
                    deleteTask(task.id)
                }
                _uiState.value = _uiState.value.copy(taskPendingDelete = null)
            }
            is TaskListIntent.CancelDeleteTask -> {
                _uiState.value = _uiState.value.copy(taskPendingDelete = null)
            }
        }
    }

    private fun loadRemindersForDate(date: LocalDate) {
        reminderJob?.cancel()
        reminderJob = viewModelScope.launch {
            try {
                getRemindersForDateUseCase(date).collect { reminders ->
                    _uiState.value = _uiState.value.copy(reminders = reminders)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Reminder errors are non-critical; silently ignore
            }
        }
    }

    private fun loadTasksForDate(date: LocalDate) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, selectedDate = date)
            try {
                getTasksForDateUseCase(date).collect { tasks ->
                    _uiState.value = _uiState.value.copy(
                        tasks = tasks,
                        isLoading = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    private fun loadMonthIndicators(yearMonth: YearMonth) {
        monthJob?.cancel()
        monthJob = viewModelScope.launch {
            try {
                getAllTasksUseCase().collect { tasks ->
                    val datesWithTasks = tasks
                        .flatMap { task ->
                            expandTaskDatesToMonth(task, yearMonth.minusMonths(1)) +
                                    expandTaskDatesToMonth(task, yearMonth) +
                                    expandTaskDatesToMonth(task, yearMonth.plusMonths(1))
                        }
                        .toSet()
                    _uiState.value = _uiState.value.copy(datesWithTasks = datesWithTasks)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
            }
        }
    }

    private fun expandTaskDatesToMonth(task: Task, yearMonth: YearMonth): List<LocalDate> {
        val taskDates = when (task.recurrence.type) {
            RecurrenceType.Single -> {
                if (YearMonth.from(task.date) == yearMonth) listOf(task.date) else emptyList()
            }
            RecurrenceType.Daily -> {
                (1..yearMonth.lengthOfMonth()).map { yearMonth.atDay(it) }
            }
            RecurrenceType.CustomWeekly -> {
                val weekDays = task.recurrence.daysOfWeek ?: emptyList()
                (1..yearMonth.lengthOfMonth())
                    .map { yearMonth.atDay(it) }
                    .filter { date ->
                        val dayOfWeek = DayOfWeek.valueOf(date.dayOfWeek.name)
                        weekDays.contains(dayOfWeek)
                    }
            }
            RecurrenceType.Monthly -> {
                val dom = task.recurrence.dayOfMonth ?: return emptyList()
                if (dom <= yearMonth.lengthOfMonth()) listOf(yearMonth.atDay(dom)) else emptyList()
            }
            RecurrenceType.Yearly -> {
                val month = task.recurrence.month ?: return emptyList()
                val day = task.recurrence.dayOfMonth ?: return emptyList()
                if (yearMonth.monthValue == month && day <= yearMonth.lengthOfMonth()) {
                    listOf(yearMonth.atDay(day))
                } else {
                    emptyList()
                }
            }
        }

        if (task.advanceReminderDays.isEmpty()) return taskDates

        // For each task date in this month, include all reminder dates (1..maxDays days before)
        val maxDays = task.advanceReminderDays.max()
        val reminderDates = taskDates.flatMap { taskDate ->
            (1..maxDays).map { daysBeforeCount -> taskDate.minusDays(daysBeforeCount.toLong()) }
        }

        return taskDates + reminderDates
    }

    private fun completeTask(taskId: String) {
        viewModelScope.launch {
            completeTaskUseCase(taskId)
        }
    }

    private fun toggleNotification(taskId: String) {
        viewModelScope.launch {
            toggleTaskNotificationUseCase(taskId)
        }
    }

    private fun deleteTask(taskId: String) {
        viewModelScope.launch {
            deleteTaskUseCase(taskId)
            loadTasksForDate(_uiState.value.selectedDate)
        }
    }

    private fun dismissTask(taskId: String) {
        viewModelScope.launch {
            dismissTaskUseCase(taskId)
            loadTasksForDate(_uiState.value.selectedDate)
        }
    }

    private fun dismissReminder(reminderId: String, taskId: String) {
        viewModelScope.launch {
            try { dismissReminderUseCase(reminderId, taskId) } catch (_: Exception) {}
        }
    }

    private fun completeReminder(reminderId: String, taskId: String) {
        viewModelScope.launch {
            try { completeReminderUseCase(reminderId, taskId) } catch (_: Exception) {}
        }
    }

    private fun deleteReminder(reminderId: String, taskId: String) {
        viewModelScope.launch {
            try { deleteReminderUseCase(reminderId, taskId) } catch (_: Exception) {}
        }
    }

    private fun reactivateReminder(reminderId: String, taskId: String) {
        viewModelScope.launch {
            try { reactivateReminderUseCase(reminderId, taskId) } catch (_: Exception) {}
        }
    }

    private fun reactivateTask(taskId: String) {
        viewModelScope.launch {
            reactivateTaskUseCase(taskId)
        }
    }
}
