package br.com.anotacoes.data.notification

import android.content.Context
import android.content.Intent
import br.com.anotacoes.domain.port.TaskNotificationPort
import br.com.anotacoes.receiver.TaskAlarmHandler
import br.com.anotacoes.service.TaskLockScreenService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TaskNotificationManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TaskNotificationPort {

    override fun cancelNotification(taskId: String) {
        val intent = Intent(context, TaskLockScreenService::class.java).apply {
            action = TaskLockScreenService.ACTION_REMOVE
            putExtra(TaskAlarmHandler.EXTRA_TASK_ID, taskId)
        }
        context.startService(intent)
    }
}
