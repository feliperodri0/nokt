package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.repository.TaskRepository
import javax.inject.Inject

class ToggleTaskNotificationUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val scheduleTaskAlarmUseCase: ScheduleTaskAlarmUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase
) {
    suspend operator fun invoke(taskId: String) {
        val task = repository.getTaskById(taskId) ?: return
        val updated = task.copy(showInNotification = !task.showInNotification)
        updateTaskUseCase(updated)
        scheduleTaskAlarmUseCase(taskId)
    }
}
