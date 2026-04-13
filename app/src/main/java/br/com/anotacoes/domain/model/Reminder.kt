package br.com.anotacoes.domain.model

import java.time.LocalDate

data class Reminder(
    val id: String,               // "${taskId}_adv_${reminderDate}"
    val taskId: String,
    val taskTitle: String,
    val task: Task,
    val daysBeforeTask: Int,      // Quantos dias faltam para a tarefa
    val reminderDate: LocalDate,  // Data específica do lembrete
    val status: ReminderStatus = ReminderStatus.ACTIVE
) {
    val isInactive: Boolean get() = status == ReminderStatus.DISMISSED || status == ReminderStatus.COMPLETED
}
