package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.model.DayOfWeek
import br.com.anotacoes.domain.model.RecurrenceType
import br.com.anotacoes.domain.model.Task
import java.time.ZoneId

data class UseScheduledAlarm(
    val taskId: String,
    val requestCode: Int,
    val startTriggerTimeMillis: Long,
    val endTriggerTimeMillis: Long,
    val title: String,
    val description: String?,
    val timeText: String,
    val recurrenceText: String,
    val showInNotification: Boolean = true
)

data class AdvanceReminderAlarm(
    val taskId: String,
    val requestCode: Int,
    val triggerTimeMillis: Long,
    val taskTitle: String,
    val daysRemaining: Int
)

class AlarmScheduler {

    fun calculateAlarmForTask(task: Task, startOffsetMinutes: Int = 0): UseScheduledAlarm {
        // For recurring tasks, we should calculate alarms for upcoming occurrences
        // Use the actual task date to ensure the main task alarm is scheduled strictly for the task date
        val effectiveDate = task.date // Changed to use task.date directly for main task alarm
        val startTrigger = calculateTriggerTimeForDate(task, effectiveDate, startOffsetMinutes)
        val endTrigger = calculateEndTriggerTimeForDate(task, effectiveDate, endOffsetMinutes = 0)
        val requestCode = "${task.id}_TASK".hashCode() // More specific request code

        return UseScheduledAlarm(
            taskId = task.id,
            requestCode = requestCode,
            startTriggerTimeMillis = startTrigger,
            endTriggerTimeMillis = endTrigger,
            title = task.title,
            description = task.description,
            timeText = buildTimeText(task),
            recurrenceText = buildRecurrenceText(task),
            showInNotification = task.showInNotification
        )
    }

    private fun getNextOccurrenceDate(task: Task): java.time.LocalDate {
        val today = java.time.LocalDate.now()

        when (task.recurrence.type) {
            RecurrenceType.Single -> return if (task.date.isBefore(today)) task.date else today
            RecurrenceType.Daily -> return today
            RecurrenceType.CustomWeekly -> {
                val daysOfWeek = task.recurrence.daysOfWeek ?: emptyList()
                var currentDate = today
                // Find the next occurrence from today onwards
                while (currentDate.isBefore(today.plusWeeks(1))) { // Check next week max
                    val dayOfWeek = DayOfWeek.valueOf(currentDate.dayOfWeek.name)
                    if (daysOfWeek.contains(dayOfWeek)) {
                        return currentDate
                    }
                    currentDate = currentDate.plusDays(1)
                }
                return today // fallback
            }
            RecurrenceType.Monthly -> {
                val dayOfMonth = task.recurrence.dayOfMonth ?: task.date.dayOfMonth
                // If the day of month has passed this month, go to next month
                if (today.dayOfMonth <= dayOfMonth) {
                    return java.time.LocalDate.of(today.year, today.month, dayOfMonth.coerceAtMost(today.lengthOfMonth()))
                } else {
                    val nextMonth = if (today.monthValue == 12) java.time.LocalDate.of(today.year + 1, 1, 1)
                                   else java.time.LocalDate.of(today.year, today.monthValue + 1, 1)
                    return java.time.LocalDate.of(nextMonth.year, nextMonth.month, dayOfMonth.coerceAtMost(nextMonth.lengthOfMonth()))
                }
            }
            RecurrenceType.Yearly -> {
                val month = task.recurrence.month ?: task.date.monthValue
                val dayOfMonth = task.recurrence.dayOfMonth ?: task.date.dayOfMonth
                val thisYearDate = java.time.LocalDate.of(today.year, month, dayOfMonth.coerceAtMost(java.time.LocalDate.of(today.year, month, 1).lengthOfMonth()))

                return if (thisYearDate.isBefore(today)) {
                    java.time.LocalDate.of(today.year + 1, month, dayOfMonth.coerceAtMost(java.time.LocalDate.of(today.year + 1, month, 1).lengthOfMonth()))
                } else {
                    thisYearDate
                }
            }
        }
    }

    private fun calculateTriggerTimeForDate(task: Task, date: java.time.LocalDate, offsetMinutes: Int): Long {
        val midnight = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return if (task.taskTime.isAllDay) {
            midnight
        } else {
            val hour = task.taskTime.startTime?.hour ?: 0
            val minute = task.taskTime.startTime?.minute ?: 0
            midnight + (hour * 3600000L) + ((minute + offsetMinutes) * 60000L)
        }
    }

    private fun calculateEndTriggerTimeForDate(task: Task, date: java.time.LocalDate, endOffsetMinutes: Int): Long {
        val midnight = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return if (task.taskTime.isAllDay) {
            midnight + (23 * 3600000L + 59 * 60000L)
        } else {
            val hour = task.taskTime.endTime?.hour ?: 0
            val minute = task.taskTime.endTime?.minute ?: 0
            midnight + (hour * 3600000L) + ((minute + endOffsetMinutes) * 60000L)
        }
    }

    fun calculateAdvanceReminderAlarms(task: Task): List<AdvanceReminderAlarm> {
        if (task.advanceReminderDays.isEmpty()) return emptyList()

        val reminderAlarms = mutableListOf<AdvanceReminderAlarm>()

        // For each possible reminder day in the range, calculate when the reminder should fire
        for (daysBeforeCount in task.advanceReminderDays) {
            // Generate alarms for each day from (task.date - daysBeforeCount) to (task.date - 1)
            val startDate = task.date.minusDays(daysBeforeCount.toLong())
            val endDate = task.date.minusDays(1) // One day before the task

            // Determine the actual reminder dates based on the recurrence pattern
            val reminderDates = mutableListOf<java.time.LocalDate>()

            when (task.recurrence.type) {
                RecurrenceType.Single -> {
                    // For single tasks, add all dates in the range
                    var currentDate = startDate
                    while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
                        reminderDates.add(currentDate)
                        currentDate = currentDate.plusDays(1)
                    }
                }
                RecurrenceType.Daily -> {
                    // For daily tasks, add all dates in the range that have corresponding future occurrences
                    var currentDate = startDate
                    while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
                        val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(currentDate, task.date).toInt()
                        val correspondingTaskDate = currentDate.plusDays(daysRemaining.toLong())

                        if (correspondingTaskDate >= task.date) {
                            reminderDates.add(currentDate)
                        }
                        currentDate = currentDate.plusDays(1)
                    }
                }
                RecurrenceType.CustomWeekly -> {
                    val daysOfWeek = task.recurrence.daysOfWeek ?: continue

                    // For weekly tasks, add dates that correspond to future occurrences
                    var currentDate = startDate
                    while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
                        val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(currentDate, task.date).toInt()
                        val correspondingTaskDate = currentDate.plusDays(daysRemaining.toLong())

                        val dayOfWeek = DayOfWeek.valueOf(correspondingTaskDate.dayOfWeek.name)
                        if (daysOfWeek.contains(dayOfWeek) && correspondingTaskDate >= task.date) {
                            reminderDates.add(currentDate)
                        }
                        currentDate = currentDate.plusDays(1)
                    }
                }
                RecurrenceType.Monthly -> {
                    val dayOfMonth = task.recurrence.dayOfMonth ?: task.date.dayOfMonth

                    // For monthly tasks, add dates that correspond to future occurrences
                    var currentDate = startDate
                    while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
                        val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(currentDate, task.date).toInt()
                        val correspondingTaskDate = currentDate.plusDays(daysRemaining.toLong())

                        if (correspondingTaskDate.dayOfMonth == dayOfMonth && correspondingTaskDate >= task.date) {
                            reminderDates.add(currentDate)
                        }
                        currentDate = currentDate.plusDays(1)
                    }
                }
                RecurrenceType.Yearly -> {
                    val month = task.recurrence.month ?: task.date.monthValue
                    val dayOfMonth = task.recurrence.dayOfMonth ?: task.date.dayOfMonth

                    // For yearly tasks, add dates that correspond to future occurrences
                    var currentDate = startDate
                    while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
                        val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(currentDate, task.date).toInt()
                        val correspondingTaskDate = currentDate.plusDays(daysRemaining.toLong())

                        if (correspondingTaskDate.monthValue == month &&
                            correspondingTaskDate.dayOfMonth == dayOfMonth &&
                            correspondingTaskDate >= task.date) {
                            reminderDates.add(currentDate)
                        }
                        currentDate = currentDate.plusDays(1)
                    }
                }
            }

            // Now create alarms for each calculated reminder date
            for (reminderDate in reminderDates) {
                // Only add reminder if it's today or in the future
                if (reminderDate.isAfter(java.time.LocalDate.now().minusDays(1))) {
                    // Fire at 8:00 AM on the reminder day
                    val midnight = reminderDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val triggerMillis = midnight + (8 * 3600000L) // 8:00 AM

                    // Calculate days remaining for this specific reminder date
                    val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(reminderDate, task.date).toInt()

                    // Create unique request code for each reminder occurrence - make it more specific
                    val requestCode = "${task.id}_ADV_REMINDER_${reminderDate}".hashCode()

                    reminderAlarms.add(AdvanceReminderAlarm(
                        taskId = task.id,
                        requestCode = requestCode,
                        triggerTimeMillis = triggerMillis,
                        taskTitle = task.title,
                        daysRemaining = daysRemaining
                    ))
                }
            }
        }

        return reminderAlarms
    }

    private fun getUpcomingTaskOccurrences(task: Task, daysToLookAhead: Int): List<java.time.LocalDate> {
        val occurrences = mutableListOf<java.time.LocalDate>()
        val startDate = task.date
        val endDate = startDate.plusDays(daysToLookAhead.toLong())

        when (task.recurrence.type) {
            RecurrenceType.Single -> {
                if (startDate.isBefore(endDate) || startDate.isEqual(endDate)) {
                    occurrences.add(startDate)
                }
            }
            RecurrenceType.Daily -> {
                var currentDate = startDate
                while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
                    occurrences.add(currentDate)
                    currentDate = currentDate.plusDays(1)
                }
            }
            RecurrenceType.CustomWeekly -> {
                val daysOfWeek = task.recurrence.daysOfWeek ?: emptyList()
                var currentDate = startDate
                while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
                    val dayOfWeek = DayOfWeek.valueOf(currentDate.dayOfWeek.name)
                    if (daysOfWeek.contains(dayOfWeek)) {
                        occurrences.add(currentDate)
                    }
                    currentDate = currentDate.plusDays(1)
                }
            }
            RecurrenceType.Monthly -> {
                val dayOfMonth = task.recurrence.dayOfMonth ?: task.date.dayOfMonth
                var currentDate = startDate

                // Adjust startDate to the first occurrence on or after the start date
                if (currentDate.dayOfMonth > dayOfMonth) {
                    // If the current day is after the target day, move to next month
                    currentDate = currentDate.plusMonths(1)
                }

                // Set to the target day of month, adjusting for month length (e.g. Jan 31 -> Feb 28)
                currentDate = java.time.LocalDate.of(
                    currentDate.year,
                    currentDate.month,
                    dayOfMonth.coerceAtMost(currentDate.lengthOfMonth())
                )

                // Add occurrences for each month until endDate
                while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
                    occurrences.add(currentDate)

                    // Move to the same day next month, adjusting for month length
                    val nextMonth = currentDate.plusMonths(1)
                    currentDate = java.time.LocalDate.of(
                        nextMonth.year,
                        nextMonth.month,
                        dayOfMonth.coerceAtMost(nextMonth.lengthOfMonth())
                    )
                }
            }
            RecurrenceType.Yearly -> {
                val month = task.recurrence.month ?: task.date.monthValue
                val dayOfMonth = task.recurrence.dayOfMonth ?: task.date.dayOfMonth
                var currentDate = startDate

                // Start from the first occurrence on or after the start date
                if (currentDate.monthValue > month || (currentDate.monthValue == month && currentDate.dayOfMonth > dayOfMonth)) {
                    currentDate = java.time.LocalDate.of(currentDate.year + 1, month, dayOfMonth.coerceAtMost(java.time.LocalDate.of(currentDate.year + 1, month, 1).lengthOfMonth()))
                } else {
                    currentDate = java.time.LocalDate.of(currentDate.year, month, dayOfMonth.coerceAtMost(currentDate.lengthOfMonth()))
                }

                while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
                    occurrences.add(currentDate)
                    currentDate = java.time.LocalDate.of(currentDate.year + 1, month, dayOfMonth.coerceAtMost(java.time.LocalDate.of(currentDate.year + 1, month, 1).lengthOfMonth()))
                }
            }
        }

        return occurrences
    }

    private fun buildTimeText(task: Task): String {
        return if (task.taskTime.isAllDay) {
            "O dia todo"
        } else {
            val start = task.taskTime.startTime
            val end = task.taskTime.endTime
            val s = "%02d:%02d".format(start?.hour ?: 0, start?.minute ?: 0)
            val e = "%02d:%02d".format(end?.hour ?: 0, end?.minute ?: 0)
            "$s - $e"
        }
    }

    private fun buildRecurrenceText(task: Task): String {
        return when (task.recurrence.type) {
            RecurrenceType.Single -> "Unica vez"
            RecurrenceType.Daily -> "Todos os dias"
            RecurrenceType.CustomWeekly -> {
                val days = task.recurrence.daysOfWeek ?: emptyList()
                days.joinToString(", ") { it.toShortName() }
            }
            RecurrenceType.Monthly -> {
                val day = task.recurrence.dayOfMonth ?: 1
                "Todo dia $day do mês"
            }
            RecurrenceType.Yearly -> {
                val month = task.recurrence.month ?: 1
                val day = task.recurrence.dayOfMonth ?: 1
                "Todo ano em ${monthName(month)}/$day"
            }
        }
    }

    private fun monthName(month: Int): String = when (month) {
        1 -> "Jan"; 2 -> "Fev"; 3 -> "Mar"; 4 -> "Abr"
        5 -> "Mai"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Ago"
        9 -> "Set"; 10 -> "Out"; 11 -> "Nov"; 12 -> "Dez"
        else -> "$month"
    }

    private fun DayOfWeek.toShortName(): String = when (this) {
        DayOfWeek.MONDAY -> "Seg"
        DayOfWeek.TUESDAY -> "Ter"
        DayOfWeek.WEDNESDAY -> "Qua"
        DayOfWeek.THURSDAY -> "Qui"
        DayOfWeek.FRIDAY -> "Sex"
        DayOfWeek.SATURDAY -> "Sab"
        DayOfWeek.SUNDAY -> "Dom"
    }

    private fun calculateTriggerTime(task: Task, offsetMinutes: Int): Long {
        val midnight = task.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return if (task.taskTime.isAllDay) {
            midnight
        } else {
            val hour = task.taskTime.startTime?.hour ?: 0
            val minute = task.taskTime.startTime?.minute ?: 0
            midnight + (hour * 3600000L) + ((minute + offsetMinutes) * 60000L)
        }
    }

    private fun calculateEndTriggerTime(task: Task, endOffsetMinutes: Int): Long {
        val midnight = task.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return if (task.taskTime.isAllDay) {
            midnight + (23 * 3600000L + 59 * 60000L)
        } else {
            val hour = task.taskTime.endTime?.hour ?: 0
            val minute = task.taskTime.endTime?.minute ?: 0
            midnight + (hour * 3600000L) + ((minute + endOffsetMinutes) * 60000L)
        }
    }
}
