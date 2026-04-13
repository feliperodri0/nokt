package br.com.anotacoes.receiver

import br.com.anotacoes.domain.usecase.CompleteTaskUseCase
import javax.inject.Inject

class TaskCompleteHandler @Inject constructor(
    private val completeTaskUseCase: CompleteTaskUseCase
) {
    companion object {
        const val ACTION_COMPLETE_TASK = "br.com.anotacoes.ACTION_COMPLETE_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    fun isValidAction(action: String?): Boolean = action == ACTION_COMPLETE_TASK

    fun extractTaskId(extras: Map<String, String?>): String? {
        val taskId = extras[EXTRA_TASK_ID]
        return if (taskId.isNullOrBlank()) null else taskId
    }

    suspend fun complete(taskId: String) {
        completeTaskUseCase(taskId)
    }
}
