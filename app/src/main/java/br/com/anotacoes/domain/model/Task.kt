package br.com.anotacoes.domain.model

import java.time.LocalDate
import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: LocalDate,
    val taskTime: TaskTime,
    val recurrence: Recurrence,
    val description: String? = null,
    val attachments: List<String> = emptyList(),
    val checklistItems: List<ChecklistItem> = emptyList(),
    val status: TaskStatus = TaskStatus.ACTIVE,
    val showInNotification: Boolean = true,
    val advanceReminderDays: List<Int> = emptyList()
) {
    init {
        require(title.isNotBlank()) { "Task title must not be empty" }
    }
}
