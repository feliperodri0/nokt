package br.com.anotacoes.service

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * TDD tests for LockScreenNotificationState (Requirement 6 - Lock Screen).
 * Manages the state of active task notifications shown on the lock screen.
 * Pure Kotlin logic - no Android dependencies.
 */
class LockScreenNotificationStateTest {

    private lateinit var state: LockScreenNotificationState

    @Before
    fun setup() {
        state = LockScreenNotificationState()
    }

    // --- Adding notifications ---

    @Test
    fun `should add task notification`() {
        state.addNotification("task-1", "Meeting", "Room A")

        assertThat(state.hasActiveNotifications()).isTrue()
        assertThat(state.getActiveCount()).isEqualTo(1)
    }

    @Test
    fun `should add multiple task notifications`() {
        state.addNotification("task-1", "Meeting", null)
        state.addNotification("task-2", "Lunch", "Cafeteria")

        assertThat(state.getActiveCount()).isEqualTo(2)
    }

    @Test
    fun `should replace notification for same task id`() {
        state.addNotification("task-1", "Meeting v1", null)
        state.addNotification("task-1", "Meeting v2", "Updated")

        assertThat(state.getActiveCount()).isEqualTo(1)
        val info = state.getNotificationInfo("task-1")
        assertThat(info?.title).isEqualTo("Meeting v2")
        assertThat(info?.description).isEqualTo("Updated")
    }

    // --- Removing notifications ---

    @Test
    fun `should remove task notification`() {
        state.addNotification("task-1", "Meeting", null)

        state.removeNotification("task-1")

        assertThat(state.hasActiveNotifications()).isFalse()
        assertThat(state.getActiveCount()).isEqualTo(0)
    }

    @Test
    fun `should handle removing non-existent notification`() {
        state.removeNotification("non-existent")

        assertThat(state.hasActiveNotifications()).isFalse()
    }

    @Test
    fun `should only remove specified notification`() {
        state.addNotification("task-1", "Meeting", null)
        state.addNotification("task-2", "Lunch", null)

        state.removeNotification("task-1")

        assertThat(state.getActiveCount()).isEqualTo(1)
        assertThat(state.getNotificationInfo("task-2")).isNotNull()
        assertThat(state.getNotificationInfo("task-1")).isNull()
    }

    // --- Querying state ---

    @Test
    fun `should return false for hasActiveNotifications when empty`() {
        assertThat(state.hasActiveNotifications()).isFalse()
    }

    @Test
    fun `should return notification info by task id`() {
        state.addNotification("task-1", "Gym", "Leg day")

        val info = state.getNotificationInfo("task-1")

        assertThat(info).isNotNull()
        assertThat(info!!.taskId).isEqualTo("task-1")
        assertThat(info.title).isEqualTo("Gym")
        assertThat(info.description).isEqualTo("Leg day")
    }

    @Test
    fun `should return null for unknown task id`() {
        val info = state.getNotificationInfo("unknown")

        assertThat(info).isNull()
    }

    // --- Notification ID generation ---

    @Test
    fun `should generate stable notification id from task id`() {
        val id1 = state.getNotificationId("task-1")
        val id2 = state.getNotificationId("task-1")

        assertThat(id1).isEqualTo(id2)
    }

    @Test
    fun `should generate different notification ids for different tasks`() {
        val id1 = state.getNotificationId("task-1")
        val id2 = state.getNotificationId("task-2")

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun `notification id should be positive`() {
        val id = state.getNotificationId("any-task")

        assertThat(id).isGreaterThan(0)
    }

    // --- Clear all ---

    @Test
    fun `should clear all notifications`() {
        state.addNotification("task-1", "A", null)
        state.addNotification("task-2", "B", null)
        state.addNotification("task-3", "C", null)

        val clearedIds = state.clearAll()

        assertThat(state.hasActiveNotifications()).isFalse()
        assertThat(clearedIds).hasSize(3)
    }

    @Test
    fun `clearAll should return task ids that were cleared`() {
        state.addNotification("task-1", "A", null)
        state.addNotification("task-2", "B", null)

        val clearedIds = state.clearAll()

        assertThat(clearedIds).containsExactly("task-1", "task-2")
    }

    @Test
    fun `clearAll on empty state should return empty list`() {
        val clearedIds = state.clearAll()

        assertThat(clearedIds).isEmpty()
    }

    // --- Summary for foreground notification ---

    @Test
    fun `should build summary text with single active task`() {
        state.addNotification("task-1", "Meeting", null)

        val summary = state.buildSummaryText()

        assertThat(summary).contains("1")
    }

    @Test
    fun `should build summary text with multiple active tasks`() {
        state.addNotification("task-1", "Meeting", null)
        state.addNotification("task-2", "Lunch", null)
        state.addNotification("task-3", "Gym", null)

        val summary = state.buildSummaryText()

        assertThat(summary).contains("3")
    }

    @Test
    fun `should build empty summary when no active tasks`() {
        val summary = state.buildSummaryText()

        assertThat(summary).contains("0")
    }
}
