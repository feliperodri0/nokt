package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.model.DayOfWeek
import br.com.anotacoes.domain.model.RecurrenceType
import br.com.anotacoes.domain.model.Reminder
import br.com.anotacoes.domain.model.ReminderStatus
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskStatus
import br.com.anotacoes.domain.repository.ReminderRepository
import br.com.anotacoes.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class GetRemindersForDateUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository
) {
    operator fun invoke(date: LocalDate): Flow<List<Reminder>> {
        return combine(
            taskRepository.getAllTasks(),
            reminderRepository.getAllStatuses()
        ) { tasks, statuses ->
            val reminders = mutableListOf<Reminder>()

            tasks.forEach { task ->
                if (task.status != TaskStatus.ACTIVE) return@forEach

                // For each task, check if it should appear as a reminder on the target date
                // This happens when the target date is one of the reminder days before a future task occurrence
                for (daysBeforeMax in task.advanceReminderDays) {
                    // For each max days before value, generate reminders for all days from 1 to daysBeforeMax
                    for (daysBefore in 1..daysBeforeMax) {
                        val expectedReminderDate = task.date.minusDays(daysBefore.toLong())

                        // Check if the current target date matches this specific reminder date
                        when (task.recurrence.type) {
                            RecurrenceType.Single -> {
                                // For single tasks, check if the current date matches the expected reminder date
                                if (date == expectedReminderDate && date.isBefore(task.date)) {
                                    val reminderId = "${task.id}_adv_${daysBefore}"
                                    val status = statuses[reminderId] ?: ReminderStatus.ACTIVE

                                    if (status != ReminderStatus.DELETED) {
                                        reminders.add(
                                            Reminder(
                                                id = reminderId,
                                                taskId = task.id,
                                                taskTitle = task.title,
                                                task = task,
                                                daysBeforeTask = daysBefore,
                                                reminderDate = date,
                                                status = status
                                            )
                                        )
                                    }
                                }
                            }

                            RecurrenceType.Daily -> {
                                // For daily tasks, check if the current date is 'daysBefore' before any future occurrence
                                // Starting from the first occurrence on or after the current date
                                var occurrenceDate = if (date <= task.date) task.date else date

                                while (occurrenceDate.isBefore(date.plusDays(2))) { // Check a small range
                                    val expectedReminderDateForOccurrence =
                                        occurrenceDate.minusDays(daysBefore.toLong())

                                    if (date == expectedReminderDateForOccurrence && occurrenceDate.isAfter(
                                            date.minusDays(daysBefore.toLong())
                                        )
                                    ) {
                                        val reminderId = "${task.id}_adv_${daysBefore}"
                                        val status = statuses[reminderId] ?: ReminderStatus.ACTIVE

                                        if (status != ReminderStatus.DELETED) {
                                            reminders.add(
                                                Reminder(
                                                    id = reminderId,
                                                    taskId = task.id,
                                                    taskTitle = task.title,
                                                    task = task,
                                                    daysBeforeTask = daysBefore,
                                                    reminderDate = date,
                                                    status = status
                                                )
                                            )
                                        }
                                    }
                                    occurrenceDate = occurrenceDate.plusDays(1)
                                }
                            }

                            RecurrenceType.CustomWeekly -> {
                                val daysOfWeek = task.recurrence.daysOfWeek ?: continue

                                // For weekly tasks, find future occurrences and check if any has a reminder today
                                var occurrenceDate = if (date <= task.date) task.date else date
                                var weeksChecked = 0
                                val maxWeeksToCheck = 4 // Limit search range

                                while (weeksChecked < maxWeeksToCheck) {
                                    val dayOfWeek = DayOfWeek.valueOf(occurrenceDate.dayOfWeek.name)
                                    if (daysOfWeek.contains(dayOfWeek) && occurrenceDate >= task.date) {
                                        val expectedReminderDateForOccurrence =
                                            occurrenceDate.minusDays(daysBefore.toLong())

                                        if (date == expectedReminderDateForOccurrence && occurrenceDate.isAfter(
                                                date.minusDays(daysBefore.toLong())
                                            )
                                        ) {
                                            val reminderId = "${task.id}_adv_${daysBefore}"
                                            val status =
                                                statuses[reminderId] ?: ReminderStatus.ACTIVE

                                            if (status != ReminderStatus.DELETED) {
                                                reminders.add(
                                                    Reminder(
                                                        id = reminderId,
                                                        taskId = task.id,
                                                        taskTitle = task.title,
                                                        task = task,
                                                        daysBeforeTask = daysBefore,
                                                        reminderDate = date,
                                                        status = status
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    occurrenceDate = occurrenceDate.plusDays(1)
                                    if (occurrenceDate.dayOfWeek == java.time.DayOfWeek.MONDAY) weeksChecked++
                                }
                            }

                            RecurrenceType.Monthly -> {
                                val dayOfMonth = task.recurrence.dayOfMonth ?: task.date.dayOfMonth

                                // For monthly tasks, find future occurrences and check if any has a reminder today
                                var occurrenceDate = if (date <= task.date) task.date else date
                                var monthsChecked = 0
                                val maxMonthsToCheck = 12 // Limit search range

                                while (monthsChecked < maxMonthsToCheck) {
                                    if (occurrenceDate.dayOfMonth == dayOfMonth && occurrenceDate >= task.date) {
                                        val expectedReminderDateForOccurrence =
                                            occurrenceDate.minusDays(daysBefore.toLong())

                                        if (date == expectedReminderDateForOccurrence && occurrenceDate.isAfter(
                                                date.minusDays(daysBefore.toLong())
                                            )
                                        ) {
                                            val reminderId = "${task.id}_adv_${daysBefore}"
                                            val status =
                                                statuses[reminderId] ?: ReminderStatus.ACTIVE

                                            if (status != ReminderStatus.DELETED) {
                                                reminders.add(
                                                    Reminder(
                                                        id = reminderId,
                                                        taskId = task.id,
                                                        taskTitle = task.title,
                                                        task = task,
                                                        daysBeforeTask = daysBefore,
                                                        reminderDate = date,
                                                        status = status
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    occurrenceDate = occurrenceDate.plusDays(1)
                                    if (occurrenceDate.dayOfMonth == 1) monthsChecked++ // New month
                                }
                            }

                            RecurrenceType.Yearly -> {
                                val month = task.recurrence.month ?: task.date.monthValue
                                val dayOfMonth = task.recurrence.dayOfMonth ?: task.date.dayOfMonth

                                // For yearly tasks, find future occurrences and check if any has a reminder today
                                var occurrenceDate = if (date <= task.date) task.date else date
                                var yearsChecked = 0
                                val maxYearsToCheck = 5 // Limit search range

                                while (yearsChecked < maxYearsToCheck) {
                                    if (occurrenceDate.monthValue == month &&
                                        occurrenceDate.dayOfMonth == dayOfMonth &&
                                        occurrenceDate >= task.date
                                    ) {
                                        val expectedReminderDateForOccurrence =
                                            occurrenceDate.minusDays(daysBefore.toLong())

                                        if (date == expectedReminderDateForOccurrence && occurrenceDate.isAfter(
                                                date.minusDays(daysBefore.toLong())
                                            )
                                        ) {
                                            val reminderId = "${task.id}_adv_${daysBefore}"
                                            val status =
                                                statuses[reminderId] ?: ReminderStatus.ACTIVE

                                            if (status != ReminderStatus.DELETED) {
                                                reminders.add(
                                                    Reminder(
                                                        id = reminderId,
                                                        taskId = task.id,
                                                        taskTitle = task.title,
                                                        task = task,
                                                        daysBeforeTask = daysBefore,
                                                        reminderDate = date,
                                                        status = status
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    occurrenceDate = occurrenceDate.plusDays(1)
                                    if (occurrenceDate.monthValue == 1 && occurrenceDate.dayOfMonth == 1) yearsChecked++ // New year
                                }
                            }
                        }
                    }
                }

                // Also add the main task if it occurs on the target date (not as a reminder)
                when (task.recurrence.type) {
                    RecurrenceType.Single -> {
                        if (task.date == date) {
                            // This is the main task, not a reminder - skipping here as it's handled elsewhere
                        }
                    }

                    RecurrenceType.Daily -> {
                        if (date >= task.date) { // Daily tasks occur every day from their start date
                            // This is the main task, not a reminder - skipping here as it's handled elsewhere
                        }
                    }

                    RecurrenceType.CustomWeekly -> {
                        val daysOfWeek = task.recurrence.daysOfWeek ?: emptyList()
                        val dayOfWeek = DayOfWeek.valueOf(date.dayOfWeek.name)
                        if (daysOfWeek.contains(dayOfWeek) && date >= task.date) {
                            // This is the main task, not a reminder - skipping here as it's handled elsewhere
                        }
                    }

                    RecurrenceType.Monthly -> {
                        val dayOfMonth = task.recurrence.dayOfMonth ?: task.date.dayOfMonth
                        if (date.dayOfMonth == dayOfMonth && date >= task.date) {
                            // This is the main task, not a reminder - skipping here as it's handled elsewhere
                        }
                    }

                    RecurrenceType.Yearly -> {
                        val month = task.recurrence.month ?: task.date.monthValue
                        val dayOfMonth = task.recurrence.dayOfMonth ?: task.date.dayOfMonth
                        if (date.monthValue == month && date.dayOfMonth == dayOfMonth && date >= task.date) {
                            // This is the main task, not a reminder - skipping here as it's handled elsewhere
                        }
                    }
                }
            }

            reminders
        }
    }
}