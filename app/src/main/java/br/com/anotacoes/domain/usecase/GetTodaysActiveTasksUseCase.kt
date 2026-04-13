package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class GetTodaysActiveTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(date: LocalDate, hour: Int, minute: Int): Flow<List<Task>> {
        return repository.getActiveTasksForDate(date, hour, minute)
    }
}
