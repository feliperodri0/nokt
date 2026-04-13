package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.repository.TaskRepository
import javax.inject.Inject

class UpdateTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(task: Task) {
        repository.updateTask(task)
    }
}
