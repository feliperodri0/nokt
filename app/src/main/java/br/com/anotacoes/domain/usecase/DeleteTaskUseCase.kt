package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.port.TaskAlarmPort
import br.com.anotacoes.domain.port.TaskNotificationPort
import br.com.anotacoes.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val notificationPort: TaskNotificationPort,
    private val alarmPort: TaskAlarmPort
) {
    suspend operator fun invoke(taskId: String) {
        repository.deleteTask(taskId)
        notificationPort.cancelNotification(taskId)
        alarmPort.cancelAlarm(taskId.hashCode())
    }
}
