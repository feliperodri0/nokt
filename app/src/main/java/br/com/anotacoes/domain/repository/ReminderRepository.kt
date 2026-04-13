package br.com.anotacoes.domain.repository

import br.com.anotacoes.domain.model.ReminderStatus
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    /** Returns a reactive map of reminderId → status for all non-ACTIVE entries. */
    fun getAllStatuses(): Flow<Map<String, ReminderStatus>>

    /** Persists a status change. Removes entry when status is ACTIVE (default). */
    suspend fun setStatus(reminderId: String, taskId: String, status: ReminderStatus)

    /** Removes all reminder status entries for a task (called on task deletion). */
    suspend fun deleteAllForTask(taskId: String)
}
