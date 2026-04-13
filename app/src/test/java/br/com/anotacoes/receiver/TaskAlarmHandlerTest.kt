package br.com.anotacoes.receiver

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * TDD tests for TaskAlarmHandler (Requirement 6 - Lock Screen).
 * Validates intent data extraction and action routing for alarm broadcasts.
 */
class TaskAlarmHandlerTest {

    private lateinit var handler: TaskAlarmHandler

    @Before
    fun setup() {
        handler = TaskAlarmHandler()
    }

    // --- Action parsing ---

    @Test
    fun `should parse START action`() {
        val result = handler.parseAction(TaskAlarmHandler.ACTION_TASK_START)

        assertThat(result).isEqualTo(TaskAlarmAction.START)
    }

    @Test
    fun `should parse END action`() {
        val result = handler.parseAction(TaskAlarmHandler.ACTION_TASK_END)

        assertThat(result).isEqualTo(TaskAlarmAction.END)
    }

    @Test
    fun `should return null for unknown action`() {
        val result = handler.parseAction("com.unknown.ACTION")

        assertThat(result).isNull()
    }

    @Test
    fun `should return null for null action`() {
        val result = handler.parseAction(null)

        assertThat(result).isNull()
    }

    // --- Intent data extraction ---

    @Test
    fun `should extract task data from valid extras`() {
        val extras = mapOf(
            TaskAlarmHandler.EXTRA_TASK_ID to "task-123",
            TaskAlarmHandler.EXTRA_TASK_TITLE to "Meeting",
            TaskAlarmHandler.EXTRA_TASK_DESCRIPTION to "Sprint review"
        )

        val data = handler.extractTaskData(extras)

        assertThat(data).isNotNull()
        assertThat(data!!.taskId).isEqualTo("task-123")
        assertThat(data.title).isEqualTo("Meeting")
        assertThat(data.description).isEqualTo("Sprint review")
    }

    @Test
    fun `should extract task data with null description`() {
        val extras = mapOf(
            TaskAlarmHandler.EXTRA_TASK_ID to "task-456",
            TaskAlarmHandler.EXTRA_TASK_TITLE to "Quick task"
        )

        val data = handler.extractTaskData(extras)

        assertThat(data).isNotNull()
        assertThat(data!!.taskId).isEqualTo("task-456")
        assertThat(data.title).isEqualTo("Quick task")
        assertThat(data.description).isNull()
    }

    @Test
    fun `should return null when task id is missing`() {
        val extras = mapOf(
            TaskAlarmHandler.EXTRA_TASK_TITLE to "No ID task"
        )

        val data = handler.extractTaskData(extras)

        assertThat(data).isNull()
    }

    @Test
    fun `should return null when title is missing`() {
        val extras = mapOf(
            TaskAlarmHandler.EXTRA_TASK_ID to "task-789"
        )

        val data = handler.extractTaskData(extras)

        assertThat(data).isNull()
    }

    @Test
    fun `should return null when extras map is empty`() {
        val data = handler.extractTaskData(emptyMap())

        assertThat(data).isNull()
    }

    @Test
    fun `should return null when task id is blank`() {
        val extras = mapOf(
            TaskAlarmHandler.EXTRA_TASK_ID to "  ",
            TaskAlarmHandler.EXTRA_TASK_TITLE to "Some task"
        )

        val data = handler.extractTaskData(extras)

        assertThat(data).isNull()
    }

    @Test
    fun `should return null when title is blank`() {
        val extras = mapOf(
            TaskAlarmHandler.EXTRA_TASK_ID to "task-1",
            TaskAlarmHandler.EXTRA_TASK_TITLE to ""
        )

        val data = handler.extractTaskData(extras)

        assertThat(data).isNull()
    }

    // --- Resolve command ---

    @Test
    fun `should resolve START action with valid data to ShowNotification command`() {
        val data = TaskAlarmData("task-1", "Meeting", "Room 3", "09:00 - 10:00", "Unica vez")

        val command = handler.resolveCommand(TaskAlarmAction.START, data)

        assertThat(command).isEqualTo(
            TaskAlarmCommand.ShowNotification("task-1", "Meeting", "Room 3", "09:00 - 10:00", "Unica vez")
        )
    }

    @Test
    fun `should resolve END action with valid data to RemoveNotification command`() {
        val data = TaskAlarmData("task-1", "Meeting", null, null, null)

        val command = handler.resolveCommand(TaskAlarmAction.END, data)

        assertThat(command).isEqualTo(TaskAlarmCommand.RemoveNotification("task-1"))
    }

    // --- ADVANCE_REMINDER action ---

    @Test
    fun `should parse ADVANCE_REMINDER action`() {
        val result = handler.parseAction(TaskAlarmHandler.ACTION_ADVANCE_REMINDER)

        assertThat(result).isEqualTo(TaskAlarmAction.ADVANCE_REMINDER)
    }

    // --- extractAdvanceReminderData ---

    @Test
    fun `should extract advance reminder data from valid extras`() {
        val extras = mapOf(
            TaskAlarmHandler.EXTRA_TASK_ID to "task-123",
            TaskAlarmHandler.EXTRA_TASK_TITLE to "Conference",
            TaskAlarmHandler.EXTRA_DAYS_REMAINING to "7"
        )

        val data = handler.extractAdvanceReminderData(extras)

        assertThat(data).isNotNull()
        assertThat(data!!.taskId).isEqualTo("task-123")
        assertThat(data.taskTitle).isEqualTo("Conference")
        assertThat(data.daysRemaining).isEqualTo(7)
    }

    @Test
    fun `should default daysRemaining to 1 when missing`() {
        val extras = mapOf(
            TaskAlarmHandler.EXTRA_TASK_ID to "task-456",
            TaskAlarmHandler.EXTRA_TASK_TITLE to "Trip"
        )

        val data = handler.extractAdvanceReminderData(extras)

        assertThat(data).isNotNull()
        assertThat(data!!.daysRemaining).isEqualTo(1)
    }

    @Test
    fun `advance reminder data should return null when task id missing`() {
        val extras = mapOf(
            TaskAlarmHandler.EXTRA_TASK_TITLE to "Meeting"
        )

        val data = handler.extractAdvanceReminderData(extras)

        assertThat(data).isNull()
    }

    @Test
    fun `advance reminder data should return null when title missing`() {
        val extras = mapOf(
            TaskAlarmHandler.EXTRA_TASK_ID to "task-1"
        )

        val data = handler.extractAdvanceReminderData(extras)

        assertThat(data).isNull()
    }

    @Test
    fun `should resolve ADVANCE_REMINDER action via ShowAdvanceReminder command`() {
        val advData = AdvanceReminderData("task-1", "Conference", 7)

        val command = handler.resolveAdvanceCommand(advData)

        assertThat(command).isEqualTo(
            TaskAlarmCommand.ShowAdvanceReminder("task-1", "Conference", 7)
        )
    }
}
