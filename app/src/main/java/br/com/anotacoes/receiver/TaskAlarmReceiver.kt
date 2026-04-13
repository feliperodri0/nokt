package br.com.anotacoes.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.com.anotacoes.service.TaskLockScreenService

class TaskAlarmReceiver : BroadcastReceiver() {

    private val handler = TaskAlarmHandler()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = handler.parseAction(intent.action) ?: return

        val extras = mapOf(
            TaskAlarmHandler.EXTRA_TASK_ID to intent.getStringExtra(TaskAlarmHandler.EXTRA_TASK_ID),
            TaskAlarmHandler.EXTRA_TASK_TITLE to intent.getStringExtra(TaskAlarmHandler.EXTRA_TASK_TITLE),
            TaskAlarmHandler.EXTRA_TASK_DESCRIPTION to intent.getStringExtra(TaskAlarmHandler.EXTRA_TASK_DESCRIPTION),
            TaskAlarmHandler.EXTRA_TASK_TIME to intent.getStringExtra(TaskAlarmHandler.EXTRA_TASK_TIME),
            TaskAlarmHandler.EXTRA_TASK_RECURRENCE to intent.getStringExtra(TaskAlarmHandler.EXTRA_TASK_RECURRENCE),
            TaskAlarmHandler.EXTRA_SHOW_IN_NOTIFICATION to intent.getStringExtra(TaskAlarmHandler.EXTRA_SHOW_IN_NOTIFICATION),
            TaskAlarmHandler.EXTRA_DAYS_REMAINING to intent.getStringExtra(TaskAlarmHandler.EXTRA_DAYS_REMAINING)
        )

        when (action) {
            TaskAlarmAction.ADVANCE_REMINDER -> {
                val reminderData = handler.extractAdvanceReminderData(extras) ?: return
                val serviceIntent = Intent(context, TaskLockScreenService::class.java).apply {
                    this.action = TaskLockScreenService.ACTION_ADVANCE_REMINDER
                    putExtra(TaskAlarmHandler.EXTRA_TASK_ID, reminderData.taskId)
                    putExtra(TaskAlarmHandler.EXTRA_TASK_TITLE, reminderData.taskTitle)
                    putExtra(TaskAlarmHandler.EXTRA_DAYS_REMAINING, reminderData.daysRemaining.toString())
                }
                context.startService(serviceIntent)
            }
            else -> {
                val data = handler.extractTaskData(extras) ?: return
                val command = handler.resolveCommand(action, data)

                when (command) {
                    is TaskAlarmCommand.ShowNotification -> {
                        val serviceIntent = Intent(context, TaskLockScreenService::class.java).apply {
                            this.action = TaskLockScreenService.ACTION_SHOW
                            putExtra(TaskAlarmHandler.EXTRA_TASK_ID, command.taskId)
                            putExtra(TaskAlarmHandler.EXTRA_TASK_TITLE, command.title)
                            putExtra(TaskAlarmHandler.EXTRA_TASK_DESCRIPTION, command.description)
                            putExtra(TaskAlarmHandler.EXTRA_TASK_TIME, command.timeText)
                            putExtra(TaskAlarmHandler.EXTRA_TASK_RECURRENCE, command.recurrenceText)
                            putExtra(TaskAlarmHandler.EXTRA_SHOW_IN_NOTIFICATION, command.showInNotification)
                        }
                        context.startForegroundService(serviceIntent)
                    }
                    is TaskAlarmCommand.RemoveNotification -> {
                        val serviceIntent = Intent(context, TaskLockScreenService::class.java).apply {
                            this.action = TaskLockScreenService.ACTION_REMOVE
                            putExtra(TaskAlarmHandler.EXTRA_TASK_ID, command.taskId)
                        }
                        context.startService(serviceIntent)
                    }
                    is TaskAlarmCommand.ShowAdvanceReminder -> { /* handled above */ }
                }
            }
        }
    }
}
