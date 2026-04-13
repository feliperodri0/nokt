package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.model.ReminderStatus
import br.com.anotacoes.domain.repository.ReminderRepository
import javax.inject.Inject

class DeleteReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(reminderId: String, taskId: String) {
        reminderRepository.setStatus(reminderId, taskId, ReminderStatus.DELETED)
    }
}
