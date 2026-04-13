package br.com.anotacoes.domain.port

interface TaskNotificationPort {
    fun cancelNotification(taskId: String)
}
