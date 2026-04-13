package br.com.anotacoes.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,

    val title: String,

    @ColumnInfo(name = "task_date")
    val dateMillis: Long, // LocalDate stored as epoch millis of start of day

    @ColumnInfo(name = "is_all_day")
    val isAllDay: Boolean,

    @ColumnInfo(name = "start_hour")
    val startHour: Int?,

    @ColumnInfo(name = "start_minute")
    val startMinute: Int?,

    @ColumnInfo(name = "end_hour")
    val endHour: Int?,

    @ColumnInfo(name = "end_minute")
    val endMinute: Int?,

    @ColumnInfo(name = "recurrence_type")
    val recurrenceType: String, // "SINGLE", "DAILY", "CUSTOM_WEEKLY", "MONTHLY", "YEARLY"

    @ColumnInfo(name = "recurrence_date")
    val recurrenceDateMillis: Long?, // Stored only for Single recurrence

    @ColumnInfo(name = "recurrence_days")
    val recurrenceDaysJson: String, // JSON days for CustomWeekly; day number for Monthly; "MM-DD" for Yearly

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "attachments")
    val attachmentsJson: String?, // JSON: ["file1.jpg", "doc2.pdf"]

    @ColumnInfo(name = "checklist")
    val checklistJson: String, // JSON: [{"id":"...","title":"...","isChecked":false},...]

    @ColumnInfo(name = "status")
    val status: String = "ACTIVE", // TaskStatus enum name: ACTIVE, DISMISSED, COMPLETED

    @ColumnInfo(name = "show_in_notification")
    val showInNotification: Boolean = true,

    @ColumnInfo(name = "advance_reminder_days")
    val advanceReminderDaysJson: String = "[]" // JSON: [7, 3, 1]
)
