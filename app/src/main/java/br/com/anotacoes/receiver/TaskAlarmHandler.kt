package br.com.anotacoes.receiver

enum class TaskAlarmAction { START, END, ADVANCE_REMINDER }

data class TaskAlarmData(
    val taskId: String,
    val title: String,
    val description: String?,
    val timeText: String?,
    val recurrenceText: String?,
    val showInNotification: Boolean = true
)

data class AdvanceReminderData(
    val taskId: String,
    val taskTitle: String,
    val daysRemaining: Int
)

sealed class TaskAlarmCommand {
    data class ShowNotification(
        val taskId: String,
        val title: String,
        val description: String?,
        val timeText: String?,
        val recurrenceText: String?,
        val showInNotification: Boolean = true
    ) : TaskAlarmCommand()

    data class RemoveNotification(
        val taskId: String
    ) : TaskAlarmCommand()

    data class ShowAdvanceReminder(
        val taskId: String,
        val taskTitle: String,
        val daysRemaining: Int
    ) : TaskAlarmCommand()
}

class TaskAlarmHandler {

    companion object {
        const val ACTION_TASK_START = "br.com.anotacoes.ACTION_TASK_START"
        const val ACTION_TASK_END = "br.com.anotacoes.ACTION_TASK_END"
        const val ACTION_ADVANCE_REMINDER = "br.com.anotacoes.ACTION_ADVANCE_REMINDER"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
        const val EXTRA_TASK_TIME = "extra_task_time"
        const val EXTRA_TASK_RECURRENCE = "extra_task_recurrence"
        const val EXTRA_SHOW_IN_NOTIFICATION = "extra_show_in_notification"
        const val EXTRA_DAYS_REMAINING = "extra_days_remaining"
    }

    fun parseAction(action: String?): TaskAlarmAction? {
        return when (action) {
            ACTION_TASK_START -> TaskAlarmAction.START
            ACTION_TASK_END -> TaskAlarmAction.END
            ACTION_ADVANCE_REMINDER -> TaskAlarmAction.ADVANCE_REMINDER
            else -> null
        }
    }

    fun extractTaskData(extras: Map<String, String?>): TaskAlarmData? {
        val taskId = extras[EXTRA_TASK_ID]
        val title = extras[EXTRA_TASK_TITLE]

        if (taskId.isNullOrBlank() || title.isNullOrBlank()) return null

        return TaskAlarmData(
            taskId = taskId,
            title = title,
            description = extras[EXTRA_TASK_DESCRIPTION],
            timeText = extras[EXTRA_TASK_TIME],
            recurrenceText = extras[EXTRA_TASK_RECURRENCE],
            showInNotification = extras[EXTRA_SHOW_IN_NOTIFICATION]?.toBooleanStrictOrNull() ?: true
        )
    }

    fun extractAdvanceReminderData(extras: Map<String, String?>): AdvanceReminderData? {
        val taskId = extras[EXTRA_TASK_ID]
        val title = extras[EXTRA_TASK_TITLE]
        val daysStr = extras[EXTRA_DAYS_REMAINING]

        if (taskId.isNullOrBlank() || title.isNullOrBlank()) return null

        return AdvanceReminderData(
            taskId = taskId,
            taskTitle = title,
            daysRemaining = daysStr?.toIntOrNull() ?: 1
        )
    }

    fun resolveCommand(action: TaskAlarmAction, data: TaskAlarmData, extras: Map<String, String?> = emptyMap()): TaskAlarmCommand {
        return when (action) {
            TaskAlarmAction.START -> TaskAlarmCommand.ShowNotification(
                taskId = data.taskId,
                title = data.title,
                description = data.description,
                timeText = data.timeText,
                recurrenceText = data.recurrenceText,
                showInNotification = data.showInNotification
            )
            TaskAlarmAction.END -> TaskAlarmCommand.RemoveNotification(
                taskId = data.taskId
            )
            TaskAlarmAction.ADVANCE_REMINDER -> {
                val reminderData = extractAdvanceReminderData(extras) ?: return TaskAlarmCommand.RemoveNotification(data.taskId)
                resolveAdvanceCommand(reminderData)
            }
        }
    }

    fun resolveAdvanceCommand(data: AdvanceReminderData): TaskAlarmCommand {
        return TaskAlarmCommand.ShowAdvanceReminder(
            taskId = data.taskId,
            taskTitle = data.taskTitle,
            daysRemaining = data.daysRemaining
        )
    }
}
