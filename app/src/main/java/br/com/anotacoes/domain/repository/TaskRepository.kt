package br.com.anotacoes.domain.repository
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    suspend fun insertTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(taskId: String)
    suspend fun setTaskStatus(taskId: String, status: TaskStatus)
    fun getTasksForDate(date: LocalDate): Flow<List<Task>>
    fun getTasksForDateRange(start: LocalDate, end: LocalDate): Flow<List<Task>>
    suspend fun getTaskById(taskId: String): Task?
    fun getActiveTasksForDate(date: LocalDate, hour: Int, minute: Int): Flow<List<Task>>
    suspend fun dismissTask(taskId: String)
    fun getAllTasks(): Flow<List<Task>>
}
