package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.model.TaskStatus
import br.com.anotacoes.domain.port.TaskAlarmPort
import br.com.anotacoes.domain.port.TaskNotificationPort
import br.com.anotacoes.domain.repository.TaskRepository
import javax.inject.Inject

class CompleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val notificationPort: TaskNotificationPort,
    private val alarmPort: TaskAlarmPort
) {
    suspend operator fun invoke(taskId: String) {
        repository.setTaskStatus(taskId, TaskStatus.COMPLETED)
        notificationPort.cancelNotification(taskId)
        alarmPort.cancelAlarm(taskId.hashCode())
    }
}
