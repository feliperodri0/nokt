package br.com.anotacoes.domain.model

enum class TaskStatus {
    ACTIVE,
    DISMISSED,
    COMPLETED;

    val isInactive: Boolean get() = this == DISMISSED || this == COMPLETED
}
