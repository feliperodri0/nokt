package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

data class AllScheduledAlarms(
    val taskAlarms: List<UseScheduledAlarm>,
    val advanceAlarms: List<AdvanceReminderAlarm>
)

class ScheduleAllAlarmsUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(start: LocalDate, end: LocalDate): List<UseScheduledAlarm> {
        return repository.getTasksForDateRange(start, end)
            .map { taskList -> taskList.map { task -> alarmScheduler.calculateAlarmForTask(task) } }
            .first()
    }

    suspend fun invokeWithAdvanceReminders(start: LocalDate, end: LocalDate): AllScheduledAlarms {
        val tasks = repository.getTasksForDateRange(start, end).first()
        val taskAlarms = tasks.map { alarmScheduler.calculateAlarmForTask(it) }
        val advanceAlarms = tasks.flatMap { alarmScheduler.calculateAdvanceReminderAlarms(it) }
        return AllScheduledAlarms(taskAlarms, advanceAlarms)
    }
}
