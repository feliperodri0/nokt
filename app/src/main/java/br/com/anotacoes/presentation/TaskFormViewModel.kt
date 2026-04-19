package br.com.anotacoes.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.anotacoes.domain.model.ChecklistItem
import br.com.anotacoes.domain.model.DayOfWeek
import br.com.anotacoes.domain.model.Recurrence
import br.com.anotacoes.domain.model.RecurrenceType
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskStatus
import br.com.anotacoes.domain.model.TaskTime
import br.com.anotacoes.domain.usecase.CompleteTaskUseCase
import br.com.anotacoes.domain.usecase.CreateTaskUseCase
import br.com.anotacoes.domain.usecase.DeleteTaskUseCase
import br.com.anotacoes.domain.usecase.DismissTaskUseCase
import br.com.anotacoes.domain.usecase.GetTaskByIdUseCase
import br.com.anotacoes.domain.usecase.ReactivateTaskUseCase
import br.com.anotacoes.domain.usecase.ScheduleTaskAlarmUseCase
import br.com.anotacoes.domain.usecase.ToggleTaskNotificationUseCase
import br.com.anotacoes.domain.usecase.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

enum class RecurrenceTypeOption {
    SINGLE, DAILY, CUSTOM_WEEKLY, MONTHLY, YEARLY
}

sealed class TaskFormPendingUndo {
    data class TaskStatusUndo(val taskId: String, val wasCompleted: Boolean) : TaskFormPendingUndo()
}

data class TaskFormUiState(
    val editingTaskId: String? = null,
    val title: String = "",
    val date: LocalDate = LocalDate.now(),
    val isAllDay: Boolean = true,
    val startHour: Int = 8,
    val startMinute: Int = 0,
    val endHour: Int = 9,
    val endMinute: Int = 0,
    val recurrenceType: RecurrenceTypeOption = RecurrenceTypeOption.SINGLE,
    val selectedDays: List<DayOfWeek> = emptyList(),
    val selectedDayOfMonth: Int = LocalDate.now().dayOfMonth,
    val selectedYearlyMonth: Int = LocalDate.now().monthValue,
    val selectedYearlyDay: Int = LocalDate.now().dayOfMonth,
    val description: String = "",
    val checklistItems: List<ChecklistItem> = emptyList(),
    val showInNotification: Boolean = true,
    // Advance reminders
    val advanceReminderDays: List<Int> = emptyList(),
    // Attachments
    val attachments: List<String> = emptyList(),
    val isSaved: Boolean = false,
    val validationError: String? = null,
    val isLoading: Boolean = false,
    // Status management
    val currentTaskStatus: TaskStatus = TaskStatus.ACTIVE,
    val showDeleteTaskConfirmation: Boolean = false,
    val isTaskDeleted: Boolean = false,
    val pendingTaskUndo: TaskFormPendingUndo? = null
) {
    val isDateFieldEnabled: Boolean
        get() = recurrenceType == RecurrenceTypeOption.SINGLE
}

sealed class TaskFormIntent {
    data class UpdateTitle(val title: String) : TaskFormIntent()
    data class UpdateDate(val date: LocalDate) : TaskFormIntent()
    data class ToggleAllDay(val isAllDay: Boolean) : TaskFormIntent()
    data class UpdateStartTime(val hour: Int, val minute: Int) : TaskFormIntent()
    data class UpdateEndTime(val hour: Int, val minute: Int) : TaskFormIntent()
    data class UpdateRecurrenceType(val type: RecurrenceTypeOption) : TaskFormIntent()
    data class ToggleDayOfWeek(val day: DayOfWeek) : TaskFormIntent()
    data class UpdateDayOfMonth(val day: Int) : TaskFormIntent()
    data class UpdateYearlyMonth(val month: Int) : TaskFormIntent()
    data class UpdateYearlyDay(val day: Int) : TaskFormIntent()
    data class UpdateDescription(val description: String) : TaskFormIntent()
    data class AddChecklistItem(val title: String) : TaskFormIntent()
    data class RemoveChecklistItem(val itemId: String) : TaskFormIntent()
    data class ToggleChecklistItem(val itemId: String) : TaskFormIntent()
    data class ToggleShowInNotification(val show: Boolean) : TaskFormIntent()
    // Advance reminders
    data class ToggleAdvanceReminderDay(val days: Int) : TaskFormIntent()
    data class AddCustomAdvanceReminderDay(val days: Int) : TaskFormIntent()
    // Attachments
    data class AddAttachment(val uri: Uri) : TaskFormIntent()
    data class RemoveAttachment(val path: String) : TaskFormIntent()
    data object Save : TaskFormIntent()
    // Status actions (only for existing tasks)
    data object CompleteTask : TaskFormIntent()
    data object DismissTask : TaskFormIntent()
    data object RequestDeleteTask : TaskFormIntent()
    data object ConfirmDeleteTask : TaskFormIntent()
    data object CancelDeleteTask : TaskFormIntent()
    data object ToggleNotification : TaskFormIntent()
    data object UndoStatusChange : TaskFormIntent()
    data object ClearUndo : TaskFormIntent()
}

@HiltViewModel
class TaskFormViewModel @Inject constructor(
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val scheduleTaskAlarmUseCase: ScheduleTaskAlarmUseCase,
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    @ApplicationContext private val context: Context,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val dismissTaskUseCase: DismissTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val toggleTaskNotificationUseCase: ToggleTaskNotificationUseCase,
    private val reactivateTaskUseCase: ReactivateTaskUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskFormUiState())
    val uiState: StateFlow<TaskFormUiState> = _uiState.asStateFlow()

    fun setInitialDate(date: LocalDate) {
        if (_uiState.value.editingTaskId == null) {
            _uiState.value = _uiState.value.copy(date = date)
        }
    }

    fun loadTask(taskId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val task = getTaskByIdUseCase(taskId)
            if (task != null) {
                _uiState.value = TaskFormUiState(
                    editingTaskId = task.id,
                    title = task.title,
                    date = task.date,
                    isAllDay = task.taskTime.isAllDay,
                    startHour = task.taskTime.startTime?.hour ?: 8,
                    startMinute = task.taskTime.startTime?.minute ?: 0,
                    endHour = task.taskTime.endTime?.hour ?: 9,
                    endMinute = task.taskTime.endTime?.minute ?: 0,
                    recurrenceType = when (task.recurrence.type) {
                        RecurrenceType.Single -> RecurrenceTypeOption.SINGLE
                        RecurrenceType.Daily -> RecurrenceTypeOption.DAILY
                        RecurrenceType.CustomWeekly -> RecurrenceTypeOption.CUSTOM_WEEKLY
                        RecurrenceType.Monthly -> RecurrenceTypeOption.MONTHLY
                        RecurrenceType.Yearly -> RecurrenceTypeOption.YEARLY
                    },
                    selectedDays = task.recurrence.daysOfWeek ?: emptyList(),
                    selectedDayOfMonth = task.recurrence.dayOfMonth ?: task.date.dayOfMonth,
                    selectedYearlyMonth = (task.recurrence as? Recurrence.Yearly)?.yearMonth ?: task.date.monthValue,
                    selectedYearlyDay = (task.recurrence as? Recurrence.Yearly)?.yearDay ?: task.date.dayOfMonth,
                    description = task.description ?: "",
                    checklistItems = task.checklistItems,
                    showInNotification = task.showInNotification,
                    advanceReminderDays = task.advanceReminderDays,
                    attachments = task.attachments,
                    currentTaskStatus = task.status,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onIntent(intent: TaskFormIntent) {
        when (intent) {
            is TaskFormIntent.UpdateTitle -> {
                _uiState.value = _uiState.value.copy(title = intent.title, validationError = null)
            }
            is TaskFormIntent.UpdateDate -> {
                _uiState.value = _uiState.value.copy(date = intent.date)
            }
            is TaskFormIntent.ToggleAllDay -> {
                _uiState.value = _uiState.value.copy(isAllDay = intent.isAllDay)
            }
            is TaskFormIntent.UpdateStartTime -> {
                _uiState.value = _uiState.value.copy(
                    startHour = intent.hour,
                    startMinute = intent.minute
                )
            }
            is TaskFormIntent.UpdateEndTime -> {
                _uiState.value = _uiState.value.copy(
                    endHour = intent.hour,
                    endMinute = intent.minute
                )
            }
            is TaskFormIntent.UpdateRecurrenceType -> {
                val newState = when (intent.type) {
                    RecurrenceTypeOption.MONTHLY -> _uiState.value.copy(
                        recurrenceType = intent.type,
                        selectedDayOfMonth = _uiState.value.date.dayOfMonth
                    )
                    RecurrenceTypeOption.YEARLY -> _uiState.value.copy(
                        recurrenceType = intent.type,
                        selectedYearlyDay = _uiState.value.date.dayOfMonth,
                        selectedYearlyMonth = _uiState.value.date.monthValue
                    )
                    else -> _uiState.value.copy(recurrenceType = intent.type)
                }
                _uiState.value = newState
            }
            is TaskFormIntent.ToggleDayOfWeek -> {
                val currentDays = _uiState.value.selectedDays.toMutableList()
                if (currentDays.contains(intent.day)) currentDays.remove(intent.day)
                else currentDays.add(intent.day)
                _uiState.value = _uiState.value.copy(selectedDays = currentDays)
            }
            is TaskFormIntent.UpdateDayOfMonth -> {
                _uiState.value = _uiState.value.copy(selectedDayOfMonth = intent.day)
            }
            is TaskFormIntent.UpdateYearlyMonth -> {
                _uiState.value = _uiState.value.copy(selectedYearlyMonth = intent.month)
            }
            is TaskFormIntent.UpdateYearlyDay -> {
                _uiState.value = _uiState.value.copy(selectedYearlyDay = intent.day)
            }
            is TaskFormIntent.UpdateDescription -> {
                _uiState.value = _uiState.value.copy(description = intent.description)
            }
            is TaskFormIntent.AddChecklistItem -> {
                if (intent.title.isNotBlank()) {
                    val item = ChecklistItem(title = intent.title.trim())
                    _uiState.value = _uiState.value.copy(
                        checklistItems = _uiState.value.checklistItems + item
                    )
                }
            }
            is TaskFormIntent.RemoveChecklistItem -> {
                _uiState.value = _uiState.value.copy(
                    checklistItems = _uiState.value.checklistItems.filter { it.id != intent.itemId }
                )
            }
            is TaskFormIntent.ToggleChecklistItem -> {
                _uiState.value = _uiState.value.copy(
                    checklistItems = _uiState.value.checklistItems.map {
                        if (it.id == intent.itemId) it.toggle() else it
                    }
                )
            }
            is TaskFormIntent.ToggleShowInNotification -> {
                _uiState.value = _uiState.value.copy(showInNotification = intent.show)
            }
            is TaskFormIntent.ToggleAdvanceReminderDay -> {
                val alreadySelected = _uiState.value.advanceReminderDays.contains(intent.days)
                _uiState.value = _uiState.value.copy(
                    advanceReminderDays = if (alreadySelected) emptyList() else listOf(intent.days)
                )
            }
            is TaskFormIntent.AddCustomAdvanceReminderDay -> {
                if (intent.days in 1..60) {
                    _uiState.value = _uiState.value.copy(advanceReminderDays = listOf(intent.days))
                }
            }
            is TaskFormIntent.AddAttachment -> addAttachment(intent.uri)
            is TaskFormIntent.RemoveAttachment -> {
                val updated = _uiState.value.attachments.filter { it != intent.path }
                _uiState.value = _uiState.value.copy(attachments = updated)
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching {
                        // Validar que o caminho é seguro antes de tentar deletar
                        val file = File(intent.path)
                        // Verificar se o arquivo está dentro do diretório de anexos do app
                        val attachmentsDir = File(context.filesDir, "attachments")
                        if (file.canonicalPath.startsWith(attachmentsDir.canonicalPath) && file.exists()) {
                            file.delete()
                        }
                    }
                }
            }
            is TaskFormIntent.Save -> save()
            is TaskFormIntent.CompleteTask -> completeTask()
            is TaskFormIntent.DismissTask -> dismissTask()
            is TaskFormIntent.RequestDeleteTask -> {
                _uiState.value = _uiState.value.copy(showDeleteTaskConfirmation = true)
            }
            is TaskFormIntent.ConfirmDeleteTask -> confirmDeleteTask()
            is TaskFormIntent.CancelDeleteTask -> {
                _uiState.value = _uiState.value.copy(showDeleteTaskConfirmation = false)
            }
            is TaskFormIntent.ToggleNotification -> toggleNotification()
            is TaskFormIntent.UndoStatusChange -> undoStatusChange()
            is TaskFormIntent.ClearUndo -> {
                _uiState.value = _uiState.value.copy(pendingTaskUndo = null)
            }
        }
    }

    private fun completeTask() {
        val taskId = _uiState.value.editingTaskId ?: return
        viewModelScope.launch {
            completeTaskUseCase(taskId)
            _uiState.value = _uiState.value.copy(
                currentTaskStatus = TaskStatus.COMPLETED,
                pendingTaskUndo = TaskFormPendingUndo.TaskStatusUndo(taskId, wasCompleted = true)
            )
        }
    }

    private fun dismissTask() {
        val taskId = _uiState.value.editingTaskId ?: return
        viewModelScope.launch {
            dismissTaskUseCase(taskId)
            _uiState.value = _uiState.value.copy(
                currentTaskStatus = TaskStatus.DISMISSED,
                pendingTaskUndo = TaskFormPendingUndo.TaskStatusUndo(taskId, wasCompleted = false)
            )
        }
    }

    private fun confirmDeleteTask() {
        val taskId = _uiState.value.editingTaskId ?: return
        viewModelScope.launch {
            deleteTaskUseCase(taskId)
            _uiState.value = _uiState.value.copy(
                isTaskDeleted = true,
                showDeleteTaskConfirmation = false
            )
        }
    }

    private fun toggleNotification() {
        val taskId = _uiState.value.editingTaskId ?: return
        viewModelScope.launch {
            toggleTaskNotificationUseCase(taskId)
        }
    }

    private fun undoStatusChange() {
        val undo = _uiState.value.pendingTaskUndo ?: return
        when (undo) {
            is TaskFormPendingUndo.TaskStatusUndo -> {
                viewModelScope.launch {
                    reactivateTaskUseCase(undo.taskId)
                    _uiState.value = _uiState.value.copy(
                        currentTaskStatus = TaskStatus.ACTIVE,
                        pendingTaskUndo = null
                    )
                }
            }
        }
    }

    private fun addAttachment(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val copiedPath = copyUriToInternalStorage(uri) ?: return@launch
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    attachments = _uiState.value.attachments + copiedPath
                )
            }
        }
    }

    private fun copyUriToInternalStorage(uri: Uri): String? {
        return try {
            val attachmentsDir = File(context.filesDir, "attachments").also { it.mkdirs() }
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val subDir = if (mimeType.startsWith("image/")) {
                File(attachmentsDir, "images").also { it.mkdirs() }
            } else {
                File(attachmentsDir, "docs").also { it.mkdirs() }
            }

            val extension = when {
                mimeType.contains("jpeg") || mimeType.contains("jpg") -> ".jpg"
                mimeType.contains("png") -> ".png"
                mimeType.contains("pdf") -> ".pdf"
                mimeType.contains("doc") -> ".doc"
                else -> ""
            }
            val fileName = "${UUID.randomUUID()}$extension"
            val destFile = File(subDir, fileName)

            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
            inputStream.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun save() {
        val state = _uiState.value

        val error = validate(state)
        if (error != null) {
            _uiState.value = state.copy(validationError = error)
            return
        }

        viewModelScope.launch {
            val taskTime = if (state.isAllDay) {
                TaskTime.allDay()
            } else {
                TaskTime.interval(state.startHour, state.startMinute, state.endHour, state.endMinute)
            }

            val recurrence = when (state.recurrenceType) {
                RecurrenceTypeOption.SINGLE -> Recurrence.single(state.date)
                RecurrenceTypeOption.DAILY -> Recurrence.daily()
                RecurrenceTypeOption.CUSTOM_WEEKLY -> Recurrence.customWeekly(state.selectedDays)
                RecurrenceTypeOption.MONTHLY -> Recurrence.monthly(state.selectedDayOfMonth)
                RecurrenceTypeOption.YEARLY -> Recurrence.yearly(state.selectedYearlyMonth, state.selectedYearlyDay)
            }

            val task = Task(
                id = state.editingTaskId ?: UUID.randomUUID().toString(),
                title = state.title.trim(),
                date = state.date,
                taskTime = taskTime,
                recurrence = recurrence,
                description = state.description.ifBlank { null },
                checklistItems = state.checklistItems,
                showInNotification = state.showInNotification,
                advanceReminderDays = state.advanceReminderDays,
                attachments = state.attachments,
                status = state.currentTaskStatus
            )

            if (state.editingTaskId != null) {
                updateTaskUseCase(task)
            } else {
                createTaskUseCase(task)
            }
            scheduleTaskAlarmUseCase(task.id)
            _uiState.value = _uiState.value.copy(isSaved = true, validationError = null)
        }
    }

    private fun validate(state: TaskFormUiState): String? {
        if (state.title.isBlank()) return "Titulo e obrigatorio"

        if (!state.isAllDay) {
            val startTotal = state.startHour * 60 + state.startMinute
            val endTotal = state.endHour * 60 + state.endMinute
            if (endTotal <= startTotal) return "Horario de termino deve ser apos o inicio"
        }

        if (state.recurrenceType == RecurrenceTypeOption.CUSTOM_WEEKLY && state.selectedDays.isEmpty()) {
            return "Selecione pelo menos um dia da semana"
        }

        if (state.recurrenceType == RecurrenceTypeOption.MONTHLY &&
            state.selectedDayOfMonth !in 1..31) {
            return "Dia do mês deve estar entre 1 e 31"
        }

        if (state.recurrenceType == RecurrenceTypeOption.YEARLY) {
            if (state.selectedYearlyMonth !in 1..12) return "Mês deve estar entre 1 e 12"
            if (state.selectedYearlyDay !in 1..31) return "Dia deve estar entre 1 e 31"
        }

        if (state.recurrenceType == RecurrenceTypeOption.SINGLE && state.editingTaskId == null) {
            if (state.date.isBefore(LocalDate.now())) {
                return "Data deve ser hoje ou no futuro"
            }
        }

        return null
    }
}
