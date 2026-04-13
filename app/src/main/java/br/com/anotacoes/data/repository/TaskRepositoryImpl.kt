package br.com.anotacoes.data.repository

import br.com.anotacoes.data.db.TaskDao
import br.com.anotacoes.data.db.converter.TypeConverters
import br.com.anotacoes.data.db.toDomain
import br.com.anotacoes.data.db.toEntity
import br.com.anotacoes.domain.model.DayOfWeek
import br.com.anotacoes.domain.model.RecurrenceType
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskStatus
import br.com.anotacoes.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val converters: TypeConverters
) : TaskRepository {

    override suspend fun insertTask(task: Task) {
        taskDao.insertTask(task.toEntity(converters))
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity(converters))
    }

    override suspend fun deleteTask(taskId: String) {
        taskDao.deleteTask(taskId)
    }

    override fun getTasksForDate(date: LocalDate): Flow<List<Task>> {
        val dateMillis = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val dayOfWeek = DayOfWeek.valueOf(date.dayOfWeek.name)
        return taskDao.getTasksForDateWithRecurrence(dateMillis)
            .map { entities ->
                entities.map { it.toDomain(converters) }
                    .filter { task ->
                        when (task.recurrence.type) {
                            RecurrenceType.Single -> true
                            RecurrenceType.Daily -> true
                            RecurrenceType.CustomWeekly ->
                                task.recurrence.daysOfWeek?.contains(dayOfWeek) == true
                            RecurrenceType.Monthly ->
                                task.recurrence.dayOfMonth == date.dayOfMonth
                            RecurrenceType.Yearly ->
                                task.recurrence.month == date.monthValue &&
                                task.recurrence.dayOfMonth == date.dayOfMonth
                        }
                    }
            }
    }

    override fun getTasksForDateRange(start: LocalDate, end: LocalDate): Flow<List<Task>> {
        val startMillis = start.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val endMillis = end.atTime(23, 59, 59).atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        return taskDao.getTasksForDateRange(startMillis, endMillis)
            .map { entities -> entities.map { it.toDomain(converters) } }
    }

    override suspend fun getTaskById(taskId: String): Task? {
        return taskDao.getTaskById(taskId)?.toDomain(converters)
    }

    override fun getActiveTasksForDate(date: LocalDate, hour: Int, minute: Int): Flow<List<Task>> {
        val dateMillis = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        return taskDao.getActiveTasksForDate(dateMillis, hour, minute)
            .map { entities -> entities.map { it.toDomain(converters) } }
    }

    override suspend fun setTaskStatus(taskId: String, status: TaskStatus) {
        taskDao.updateStatus(taskId, status.name)
    }

    override suspend fun dismissTask(taskId: String) {
        taskDao.updateStatus(taskId, TaskStatus.DISMISSED.name)
    }

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks()
            .map { entities -> entities.map { it.toDomain(converters) } }
    }
}
