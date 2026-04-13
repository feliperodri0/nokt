package br.com.anotacoes.receiver

import br.com.anotacoes.domain.usecase.DismissTaskUseCase
import javax.inject.Inject

class TaskDismissHandler @Inject constructor(
    private val dismissTaskUseCase: DismissTaskUseCase
) {

    companion object {
        const val ACTION_DISMISS_TASK = "br.com.anotacoes.ACTION_DISMISS_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    fun isValidAction(action: String?): Boolean {
        return action == ACTION_DISMISS_TASK
    }

    fun extractTaskId(extras: Map<String, String?>): String? {
        val taskId = extras[EXTRA_TASK_ID]
        return if (taskId.isNullOrBlank()) null else taskId
    }

    suspend fun dismiss(taskId: String) {
        dismissTaskUseCase(taskId)
    }
}
