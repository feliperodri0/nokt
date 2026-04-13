package br.com.anotacoes.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.com.anotacoes.service.TaskLockScreenService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TaskCompleteReceiver : BroadcastReceiver() {

    @Inject
    lateinit var handler: TaskCompleteHandler

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (!handler.isValidAction(intent.action)) return

        val extras = mapOf(
            TaskCompleteHandler.EXTRA_TASK_ID to intent.getStringExtra(TaskCompleteHandler.EXTRA_TASK_ID)
        )

        val taskId = handler.extractTaskId(extras) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handler.complete(taskId)

                val serviceIntent = Intent(context, TaskLockScreenService::class.java).apply {
                    action = TaskLockScreenService.ACTION_REMOVE
                    putExtra(TaskAlarmHandler.EXTRA_TASK_ID, taskId)
                }
                context.startService(serviceIntent)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
