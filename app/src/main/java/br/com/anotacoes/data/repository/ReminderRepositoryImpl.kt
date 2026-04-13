package br.com.anotacoes.data.repository

import br.com.anotacoes.data.db.ReminderDao
import br.com.anotacoes.data.db.ReminderEntity
import br.com.anotacoes.domain.model.ReminderStatus
import br.com.anotacoes.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReminderRepositoryImpl @Inject constructor(
    private val dao: ReminderDao
) : ReminderRepository {

    override fun getAllStatuses(): Flow<Map<String, ReminderStatus>> {
        return dao.observeAll().map { entities ->
            entities.associate { entity ->
                entity.id to ReminderStatus.valueOf(entity.status)
            }
        }
    }

    override suspend fun setStatus(reminderId: String, taskId: String, status: ReminderStatus) {
        if (status == ReminderStatus.ACTIVE) {
            dao.deleteById(reminderId)
        } else {
            dao.upsert(ReminderEntity(id = reminderId, taskId = taskId, status = status.name))
        }
    }

    override suspend fun deleteAllForTask(taskId: String) {
        dao.deleteAllForTask(taskId)
    }
}
