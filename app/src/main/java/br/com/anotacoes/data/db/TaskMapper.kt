package br.com.anotacoes.data.db

import br.com.anotacoes.data.db.converter.TypeConverters
import br.com.anotacoes.domain.model.ChecklistItem
import br.com.anotacoes.domain.model.DayOfWeek
import br.com.anotacoes.domain.model.Recurrence
import br.com.anotacoes.domain.model.RecurrenceType
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskStatus
import br.com.anotacoes.domain.model.TaskTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val UTC = ZoneId.of("UTC")

fun Task.toEntity(converters: TypeConverters): TaskEntity {
    val isAllDay = taskTime.isAllDay
    val startHour = if (isAllDay) null else taskTime.startTime?.hour
    val startMinute = if (isAllDay) null else taskTime.startTime?.minute
    val endHour = if (isAllDay) null else taskTime.endTime?.hour
    val endMinute = if (isAllDay) null else taskTime.endTime?.minute

    val recurrenceType = when (recurrence.type) {
        RecurrenceType.Single -> "SINGLE"
        RecurrenceType.Daily -> "DAILY"
        RecurrenceType.CustomWeekly -> "CUSTOM_WEEKLY"
        RecurrenceType.Monthly -> "MONTHLY"
        RecurrenceType.Yearly -> "YEARLY"
    }

    val recurrenceDateMillis = when (recurrence) {
        is Recurrence.Single -> recurrence.date.toEpochMillis()
        else -> null
    }

    val recurrenceDays = when (recurrence) {
        is Recurrence.CustomWeekly -> converters.fromDayOfWeekList(recurrence.daysOfWeek ?: emptyList())
        is Recurrence.Monthly -> recurrence.dayOfMonth.toString()
        is Recurrence.Yearly -> "${recurrence.yearMonth}-${recurrence.yearDay}"
        else -> "[]"
    }

    return TaskEntity(
        id = id,
        title = title,
        dateMillis = date.toEpochMillis(),
        isAllDay = isAllDay,
        startHour = startHour,
        startMinute = startMinute,
        endHour = endHour,
        endMinute = endMinute,
        recurrenceType = recurrenceType,
        recurrenceDateMillis = recurrenceDateMillis,
        recurrenceDaysJson = recurrenceDays,
        description = description,
        attachmentsJson = if (attachments.isEmpty()) "[]" else converters.fromStringList(attachments),
        checklistJson = if (checklistItems.isEmpty()) "[]" else converters.fromChecklistItemList(checklistItems),
        status = status.name,
        showInNotification = showInNotification,
        advanceReminderDaysJson = converters.fromIntList(advanceReminderDays)
    )
}

fun TaskEntity.toDomain(converters: TypeConverters): Task {
    val taskTime = if (isAllDay) {
        TaskTime.allDay()
    } else {
        TaskTime.interval(
            startHour ?: 0, startMinute ?: 0,
            endHour ?: 0, endMinute ?: 0
        )
    }

    val recurrence = when (recurrenceType) {
        "SINGLE" -> {
            val date = recurrenceDateMillis?.let {
                Instant.ofEpochMilli(it).atZone(UTC).toLocalDate()
            } ?: LocalDate.now()
            Recurrence.Single(date)
        }
        "DAILY" -> Recurrence.daily()
        "CUSTOM_WEEKLY" -> {
            val days = converters.toDayOfWeekList(recurrenceDaysJson)
            Recurrence.customWeekly(days)
        }
        "MONTHLY" -> {
            val day = recurrenceDaysJson.toIntOrNull() ?: 1
            Recurrence.monthly(day)
        }
        "YEARLY" -> {
            val parts = recurrenceDaysJson.split("-")
            val month = parts.getOrNull(0)?.toIntOrNull() ?: 1
            val day = parts.getOrNull(1)?.toIntOrNull() ?: 1
            Recurrence.yearly(month, day)
        }
        else -> throw IllegalArgumentException("Unknown recurrence type: $recurrenceType")
    }

    val checklistItems = if (checklistJson == "[]") {
        emptyList()
    } else {
        converters.toChecklistItemList(checklistJson)
    }

    val attachments = if (attachmentsJson == null || attachmentsJson == "[]") {
        emptyList()
    } else {
        converters.toStringList(attachmentsJson)
    }

    val date = Instant.ofEpochMilli(dateMillis).atZone(UTC).toLocalDate()

    val taskStatus = try {
        TaskStatus.valueOf(status)
    } catch (e: IllegalArgumentException) {
        TaskStatus.ACTIVE
    }

    val advanceReminderDays = converters.toIntList(advanceReminderDaysJson)

    return Task(
        id = id,
        title = title,
        date = date,
        taskTime = taskTime,
        recurrence = recurrence,
        description = description,
        attachments = attachments,
        checklistItems = checklistItems,
        status = taskStatus,
        showInNotification = showInNotification,
        advanceReminderDays = advanceReminderDays
    )
}

private fun LocalDate.toEpochMillis(): Long {
    return this.atStartOfDay(UTC).toInstant().toEpochMilli()
}
