package br.com.anotacoes.service

data class NotificationInfo(
    val taskId: String,
    val title: String,
    val description: String?
)

class LockScreenNotificationState {

    private val activeNotifications = mutableMapOf<String, NotificationInfo>()

    fun addNotification(taskId: String, title: String, description: String?) {
        activeNotifications[taskId] = NotificationInfo(taskId, title, description)
    }

    fun removeNotification(taskId: String) {
        activeNotifications.remove(taskId)
    }

    fun hasActiveNotifications(): Boolean = activeNotifications.isNotEmpty()

    fun getActiveCount(): Int = activeNotifications.size

    fun getNotificationInfo(taskId: String): NotificationInfo? = activeNotifications[taskId]

    fun getNotificationId(taskId: String): Int {
        return (taskId.hashCode() and Int.MAX_VALUE).coerceAtLeast(1)
    }

    fun clearAll(): List<String> {
        val ids = activeNotifications.keys.toList()
        activeNotifications.clear()
        return ids
    }

    fun buildSummaryText(): String {
        val count = activeNotifications.size
        return "$count tarefa(s) ativa(s)"
    }
}
