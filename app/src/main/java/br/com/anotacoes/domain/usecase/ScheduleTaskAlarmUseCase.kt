package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.port.TaskAlarmPort
import br.com.anotacoes.domain.repository.TaskRepository
import javax.inject.Inject

class ScheduleTaskAlarmUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val alarmScheduler: AlarmScheduler,
    private val alarmPort: TaskAlarmPort
) {
    suspend operator fun invoke(taskId: String) {
        val task = repository.getTaskById(taskId) ?: return

        val alarm = alarmScheduler.calculateAlarmForTask(task)

        // Cancel and reschedule main task alarm
        alarmPort.cancelAlarm(alarm.requestCode)
        alarmPort.scheduleAlarm(alarm)

        // Cancel and reschedule all advance reminder alarms
        val advanceAlarms = alarmScheduler.calculateAdvanceReminderAlarms(task)
        advanceAlarms.forEach { advAlarm ->
            alarmPort.cancelAdvanceReminder(advAlarm.requestCode)
            alarmPort.scheduleAdvanceReminder(advAlarm)
        }

        // Additionally, cancel any old advance reminder alarms that might no longer be relevant
        // This handles the case where task recurrence or reminder settings changed
        val allPossibleAlarms = getAllPossibleAdvanceReminderAlarms(task)
        val currentAlarmCodes = advanceAlarms.map { it.requestCode }.toSet()
        allPossibleAlarms.forEach { oldAlarm ->
            if (oldAlarm.requestCode !in currentAlarmCodes) {
                alarmPort.cancelAdvanceReminder(oldAlarm.requestCode)
            }
        }
    }

    private fun getAllPossibleAdvanceReminderAlarms(task: Task): List<AdvanceReminderAlarm> {
        // This function should return all possible advance reminder alarms that could have been scheduled
        // for this task, including past dates that may still have active alarms

        // Use the same logic as calculateAdvanceReminderAlarms to determine all possible reminder dates
        val allPossibleAlarms = mutableListOf<AdvanceReminderAlarm>()

        for (daysBeforeCount in task.advanceReminderDays) {
            // Generate all possible reminder dates for this daysBeforeCount
            val startDate = task.date.minusDays(daysBeforeCount.toLong())
            val endDate = task.date.plusDays(365) // Look ahead 365 days from the original task date

            var currentDate = startDate
            while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
                // Calculate which actual task occurrence this reminder would correspond to
                val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(currentDate, task.date.plusDays(365)).toInt()

                // Check if this date is a valid reminder date based on the recurrence pattern
                var isValidReminderDate = false

                when (task.recurrence.type) {
                    br.com.anotacoes.domain.model.RecurrenceType.Single -> {
                        // For single tasks, check if this date is in the range from startDate to (original_task_date - 1)
                        if (currentDate.isBefore(task.date) && currentDate >= startDate) {
                            isValidReminderDate = true
                        }
                    }
                    br.com.anotacoes.domain.model.RecurrenceType.Daily -> {
                        val correspondingTaskDate = currentDate.plusDays(daysRemaining.toLong())
                        if (correspondingTaskDate >= task.date) {
                            isValidReminderDate = true
                        }
                    }
                    br.com.anotacoes.domain.model.RecurrenceType.CustomWeekly -> {
                        val daysOfWeek = task.recurrence.daysOfWeek ?: continue
                        val correspondingTaskDate = currentDate.plusDays(daysRemaining.toLong())

                        val dayOfWeek = br.com.anotacoes.domain.model.DayOfWeek.valueOf(correspondingTaskDate.dayOfWeek.name)
                        if (daysOfWeek.contains(dayOfWeek) && correspondingTaskDate >= task.date) {
                            isValidReminderDate = true
                        }
                    }
                    br.com.anotacoes.domain.model.RecurrenceType.Monthly -> {
                        val dayOfMonth = task.recurrence.dayOfMonth ?: task.date.dayOfMonth
                        val correspondingTaskDate = currentDate.plusDays(daysRemaining.toLong())

                        if (correspondingTaskDate.dayOfMonth == dayOfMonth && correspondingTaskDate >= task.date) {
                            isValidReminderDate = true
                        }
                    }
                    br.com.anotacoes.domain.model.RecurrenceType.Yearly -> {
                        val month = task.recurrence.month ?: task.date.monthValue
                        val dayOfMonth = task.recurrence.dayOfMonth ?: task.date.dayOfMonth
                        val correspondingTaskDate = currentDate.plusDays(daysRemaining.toLong())

                        if (correspondingTaskDate.monthValue == month &&
                            correspondingTaskDate.dayOfMonth == dayOfMonth &&
                            correspondingTaskDate >= task.date) {
                            isValidReminderDate = true
                        }
                    }
                }

                if (isValidReminderDate) {
                    // Create unique request code for each reminder occurrence
                    val requestCode = "${task.id}_ADV_REMINDER_${currentDate}".hashCode()

                    allPossibleAlarms.add(AdvanceReminderAlarm(
                        taskId = task.id,
                        requestCode = requestCode,
                        triggerTimeMillis = 0L, // We only care about the requestCode
                        taskTitle = task.title,
                        daysRemaining = daysRemaining.coerceAtMost(daysBeforeCount) // Adjust based on actual days remaining
                    ))
                }

                currentDate = currentDate.plusDays(1)
            }
        }

        return allPossibleAlarms
    }
}
