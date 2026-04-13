package br.com.anotacoes.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores only non-ACTIVE reminder statuses.
 * A reminder that has no entry here is implicitly ACTIVE.
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey
    val id: String,                  // "${taskId}_adv_${daysBeforeTask}"

    @ColumnInfo(name = "task_id")
    val taskId: String,

    @ColumnInfo(name = "status")
    val status: String               // ReminderStatus enum name
)
