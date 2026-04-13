package br.com.anotacoes.receiver

import br.com.anotacoes.domain.usecase.AlarmScheduler
import br.com.anotacoes.domain.usecase.UseScheduledAlarm
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class BootTaskRescheduler @Inject constructor(
    private val repository: br.com.anotacoes.domain.repository.TaskRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend fun rescheduleAll(): List<UseScheduledAlarm> {
        val today = LocalDate.now()
        val start = today
        val end = today.plusYears(5)

        return repository.getTasksForDateRange(start, end)
            .map { taskList -> taskList.map { task -> alarmScheduler.calculateAlarmForTask(task) } }
            .first()
    }
}
