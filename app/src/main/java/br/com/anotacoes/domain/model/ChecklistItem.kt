package br.com.anotacoes.domain.model

import java.util.UUID

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isChecked: Boolean = false
) {
    init {
        require(title.isNotBlank()) { "Checklist item title must not be empty" }
    }

    fun toggle(): ChecklistItem = copy(isChecked = !isChecked)
}
