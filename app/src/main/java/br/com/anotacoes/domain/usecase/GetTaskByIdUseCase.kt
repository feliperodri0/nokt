package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.repository.TaskRepository
import javax.inject.Inject

class GetTaskByIdUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: String): Task? {
        return repository.getTaskById(taskId)
    }
}
