package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.model.TaskStatus
import br.com.anotacoes.domain.repository.TaskRepository
import javax.inject.Inject

class ReactivateTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val scheduleTaskAlarmUseCase: ScheduleTaskAlarmUseCase
) {
    suspend operator fun invoke(taskId: String) {
        repository.setTaskStatus(taskId, TaskStatus.ACTIVE)
        scheduleTaskAlarmUseCase(taskId)
    }
}
