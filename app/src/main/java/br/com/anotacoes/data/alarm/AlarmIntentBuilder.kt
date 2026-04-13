package br.com.anotacoes.data.alarm

import br.com.anotacoes.domain.usecase.AdvanceReminderAlarm
import br.com.anotacoes.domain.usecase.UseScheduledAlarm
import br.com.anotacoes.receiver.TaskAlarmHandler

data class AlarmIntentData(
    val action: String,
    val requestCode: Int,
    val triggerTimeMillis: Long,
    val extras: Map<String, String?>
)

class AlarmIntentBuilder {

    fun buildStartIntentData(alarm: UseScheduledAlarm): AlarmIntentData {
        return AlarmIntentData(
            action = TaskAlarmHandler.ACTION_TASK_START,
            requestCode = alarm.requestCode,
            triggerTimeMillis = alarm.startTriggerTimeMillis,
            extras = mapOf(
                TaskAlarmHandler.EXTRA_TASK_ID to alarm.taskId,
                TaskAlarmHandler.EXTRA_TASK_TITLE to alarm.title,
                TaskAlarmHandler.EXTRA_TASK_DESCRIPTION to alarm.description,
                TaskAlarmHandler.EXTRA_TASK_TIME to alarm.timeText,
                TaskAlarmHandler.EXTRA_TASK_RECURRENCE to alarm.recurrenceText,
                TaskAlarmHandler.EXTRA_SHOW_IN_NOTIFICATION to alarm.showInNotification.toString()
            )
        )
    }

    fun buildEndIntentData(alarm: UseScheduledAlarm): AlarmIntentData {
        return AlarmIntentData(
            action = TaskAlarmHandler.ACTION_TASK_END,
            requestCode = alarm.requestCode + END_REQUEST_CODE_OFFSET,
            triggerTimeMillis = alarm.endTriggerTimeMillis,
            extras = mapOf(
                TaskAlarmHandler.EXTRA_TASK_ID to alarm.taskId,
                TaskAlarmHandler.EXTRA_TASK_TITLE to alarm.title,
                TaskAlarmHandler.EXTRA_TASK_DESCRIPTION to alarm.description,
                TaskAlarmHandler.EXTRA_TASK_TIME to alarm.timeText,
                TaskAlarmHandler.EXTRA_TASK_RECURRENCE to alarm.recurrenceText,
                TaskAlarmHandler.EXTRA_SHOW_IN_NOTIFICATION to alarm.showInNotification.toString()
            )
        )
    }

    fun buildAdvanceReminderIntentData(alarm: AdvanceReminderAlarm): AlarmIntentData {
        return AlarmIntentData(
            action = TaskAlarmHandler.ACTION_ADVANCE_REMINDER,
            requestCode = alarm.requestCode,
            triggerTimeMillis = alarm.triggerTimeMillis,
            extras = mapOf(
                TaskAlarmHandler.EXTRA_TASK_ID to alarm.taskId,
                TaskAlarmHandler.EXTRA_TASK_TITLE to alarm.taskTitle,
                TaskAlarmHandler.EXTRA_DAYS_REMAINING to alarm.daysRemaining.toString()
            )
        )
    }

    companion object {
        const val END_REQUEST_CODE_OFFSET = 100000
    }
}
