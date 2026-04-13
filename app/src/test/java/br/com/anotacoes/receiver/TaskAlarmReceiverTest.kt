package br.com.anotacoes.receiver

import android.content.Context
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

/**
 * TDD tests for TaskAlarmReceiver (Requirement 6 - Lock Screen).
 * Validates that the receiver correctly delegates to TaskAlarmHandler
 * and produces the right command from Intent data.
 */
class TaskAlarmReceiverTest {

    private lateinit var handler: TaskAlarmHandler

    @Before
    fun setup() {
        handler = TaskAlarmHandler()
    }

    @Test
    fun `should produce ShowNotification command from START intent`() {
        val extras = mapOf(
            TaskAlarmHandler.EXTRA_TASK_ID to "task-1",
            TaskAlarmHandler.EXTRA_TASK_TITLE to "Meeting",
            TaskAlarmHandler.EXTRA_TASK_DESCRIPTION to "Room A"
        )

        val action = handler.parseAction(TaskAlarmHandler.ACTION_TASK_START)
        val data = handler.extractTaskData(extras)

        assertThat(action).isNotNull()
        assertThat(data).isNotNull()

        val command = handler.resolveCommand(action!!, data!!)

        assertThat(command).isEqualTo(
            TaskAlarmCommand.ShowNotification("task-1", "Meeting", "Room A", null, null)
        )
    }

    @Test
    fun `should produce RemoveNotification command from END intent`() {
        val extras = mapOf(
            TaskAlarmHandler.EXTRA_TASK_ID to "task-2",
            TaskAlarmHandler.EXTRA_TASK_TITLE to "Lunch"
        )

        val action = handler.parseAction(TaskAlarmHandler.ACTION_TASK_END)
        val data = handler.extractTaskData(extras)

        assertThat(action).isNotNull()
        assertThat(data).isNotNull()

        val command = handler.resolveCommand(action!!, data!!)

        assertThat(command).isEqualTo(
            TaskAlarmCommand.RemoveNotification("task-2")
        )
    }

    @Test
    fun `should not produce command for invalid action`() {
        val action = handler.parseAction("invalid.ACTION")

        assertThat(action).isNull()
    }

    @Test
    fun `should not produce command when extras are incomplete`() {
        val extras = mapOf(
            TaskAlarmHandler.EXTRA_TASK_ID to "task-3"
            // missing title
        )

        val data = handler.extractTaskData(extras)

        assertThat(data).isNull()
    }

    @Test
    fun `full flow - START with complete data produces ShowNotification`() {
        val action = handler.parseAction(TaskAlarmHandler.ACTION_TASK_START)
        val data = handler.extractTaskData(
            mapOf(
                TaskAlarmHandler.EXTRA_TASK_ID to "t1",
                TaskAlarmHandler.EXTRA_TASK_TITLE to "Gym",
                TaskAlarmHandler.EXTRA_TASK_DESCRIPTION to null
            )
        )

        assertThat(action).isEqualTo(TaskAlarmAction.START)
        assertThat(data).isNotNull()

        val command = handler.resolveCommand(action!!, data!!)
        assertThat(command).isInstanceOf(TaskAlarmCommand.ShowNotification::class.java)
        assertThat((command as TaskAlarmCommand.ShowNotification).description).isNull()
    }

    @Test
    fun `full flow - END with complete data produces RemoveNotification`() {
        val action = handler.parseAction(TaskAlarmHandler.ACTION_TASK_END)
        val data = handler.extractTaskData(
            mapOf(
                TaskAlarmHandler.EXTRA_TASK_ID to "t2",
                TaskAlarmHandler.EXTRA_TASK_TITLE to "Review"
            )
        )

        assertThat(action).isEqualTo(TaskAlarmAction.END)
        assertThat(data).isNotNull()

        val command = handler.resolveCommand(action!!, data!!)
        assertThat(command).isInstanceOf(TaskAlarmCommand.RemoveNotification::class.java)
    }
}
