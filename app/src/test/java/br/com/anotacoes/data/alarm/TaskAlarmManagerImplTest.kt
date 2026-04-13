package br.com.anotacoes.data.alarm

import br.com.anotacoes.domain.usecase.AdvanceReminderAlarm
import br.com.anotacoes.domain.usecase.UseScheduledAlarm
import br.com.anotacoes.receiver.TaskAlarmHandler
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * TDD tests for TaskAlarmManagerImpl (Requirement 6, 10 - Lock Screen + Permissions).
 * Tests the intent building logic for scheduling alarms.
 * Actual AlarmManager calls require instrumentation tests.
 */
class TaskAlarmManagerImplTest {

    private lateinit var intentBuilder: AlarmIntentBuilder

    @Before
    fun setup() {
        intentBuilder = AlarmIntentBuilder()
    }

    @Test
    fun `should build start alarm intent data with correct action`() {
        val alarm = createAlarm("task-1", "Meeting", "Sprint review")

        val intentData = intentBuilder.buildStartIntentData(alarm)

        assertThat(intentData.action).isEqualTo(TaskAlarmHandler.ACTION_TASK_START)
    }

    @Test
    fun `should build start alarm intent data with task extras`() {
        val alarm = createAlarm("task-1", "Meeting", "Sprint review")

        val intentData = intentBuilder.buildStartIntentData(alarm)

        assertThat(intentData.extras[TaskAlarmHandler.EXTRA_TASK_ID]).isEqualTo("task-1")
        assertThat(intentData.extras[TaskAlarmHandler.EXTRA_TASK_TITLE]).isEqualTo("Meeting")
        assertThat(intentData.extras[TaskAlarmHandler.EXTRA_TASK_DESCRIPTION]).isEqualTo("Sprint review")
    }

    @Test
    fun `should build end alarm intent data with correct action`() {
        val alarm = createAlarm("task-1", "Meeting", null)

        val intentData = intentBuilder.buildEndIntentData(alarm)

        assertThat(intentData.action).isEqualTo(TaskAlarmHandler.ACTION_TASK_END)
    }

    @Test
    fun `should build end alarm intent data with task id`() {
        val alarm = createAlarm("task-1", "Meeting", null)

        val intentData = intentBuilder.buildEndIntentData(alarm)

        assertThat(intentData.extras[TaskAlarmHandler.EXTRA_TASK_ID]).isEqualTo("task-1")
        assertThat(intentData.extras[TaskAlarmHandler.EXTRA_TASK_TITLE]).isEqualTo("Meeting")
    }

    @Test
    fun `should handle null description in intent data`() {
        val alarm = createAlarm("task-2", "Quick", null)

        val intentData = intentBuilder.buildStartIntentData(alarm)

        assertThat(intentData.extras[TaskAlarmHandler.EXTRA_TASK_DESCRIPTION]).isNull()
    }

    @Test
    fun `start request code should use alarm requestCode`() {
        val alarm = createAlarm("task-1", "Meeting", null)

        val intentData = intentBuilder.buildStartIntentData(alarm)

        assertThat(intentData.requestCode).isEqualTo(alarm.requestCode)
    }

    @Test
    fun `end request code should differ from start request code`() {
        val alarm = createAlarm("task-1", "Meeting", null)

        val startData = intentBuilder.buildStartIntentData(alarm)
        val endData = intentBuilder.buildEndIntentData(alarm)

        assertThat(startData.requestCode).isNotEqualTo(endData.requestCode)
    }

    @Test
    fun `should use correct trigger times`() {
        val alarm = createAlarm("task-1", "Meeting", null)

        val startData = intentBuilder.buildStartIntentData(alarm)
        val endData = intentBuilder.buildEndIntentData(alarm)

        assertThat(startData.triggerTimeMillis).isEqualTo(alarm.startTriggerTimeMillis)
        assertThat(endData.triggerTimeMillis).isEqualTo(alarm.endTriggerTimeMillis)
    }

    // --- buildAdvanceReminderIntentData ---

    @Test
    fun `should build advance reminder intent with correct action`() {
        val alarm = createAdvanceAlarm("task-1", "Meeting", 7)

        val intentData = intentBuilder.buildAdvanceReminderIntentData(alarm)

        assertThat(intentData.action).isEqualTo(TaskAlarmHandler.ACTION_ADVANCE_REMINDER)
    }

    @Test
    fun `should build advance reminder intent with task extras`() {
        val alarm = createAdvanceAlarm("task-1", "Conference", 3)

        val intentData = intentBuilder.buildAdvanceReminderIntentData(alarm)

        assertThat(intentData.extras[TaskAlarmHandler.EXTRA_TASK_ID]).isEqualTo("task-1")
        assertThat(intentData.extras[TaskAlarmHandler.EXTRA_TASK_TITLE]).isEqualTo("Conference")
        assertThat(intentData.extras[TaskAlarmHandler.EXTRA_DAYS_REMAINING]).isEqualTo("3")
    }

    @Test
    fun `should use alarm request code for advance reminder`() {
        val alarm = createAdvanceAlarm("task-1", "Trip", 5)

        val intentData = intentBuilder.buildAdvanceReminderIntentData(alarm)

        assertThat(intentData.requestCode).isEqualTo(alarm.requestCode)
    }

    @Test
    fun `advance reminder trigger time should match alarm trigger time`() {
        val alarm = createAdvanceAlarm("task-1", "Event", 2)

        val intentData = intentBuilder.buildAdvanceReminderIntentData(alarm)

        assertThat(intentData.triggerTimeMillis).isEqualTo(alarm.triggerTimeMillis)
    }

    @Test
    fun `advance reminder request code should differ from main alarm request code`() {
        val mainAlarm = createAlarm("task-1", "Meeting", null)
        val advAlarm = createAdvanceAlarm("task-1", "Meeting", 7)

        val mainData = intentBuilder.buildStartIntentData(mainAlarm)
        val advData = intentBuilder.buildAdvanceReminderIntentData(advAlarm)

        assertThat(mainData.requestCode).isNotEqualTo(advData.requestCode)
    }

    private fun createAdvanceAlarm(taskId: String, title: String, daysRemaining: Int): AdvanceReminderAlarm {
        return AdvanceReminderAlarm(
            taskId = taskId,
            requestCode = "${taskId}_adv_${daysRemaining}".hashCode(),
            triggerTimeMillis = System.currentTimeMillis() + (daysRemaining * 86400000L),
            taskTitle = title,
            daysRemaining = daysRemaining
        )
    }

    private fun createAlarm(
        taskId: String,
        title: String,
        description: String?
    ): UseScheduledAlarm {
        return UseScheduledAlarm(
            taskId = taskId,
            requestCode = taskId.hashCode(),
            startTriggerTimeMillis = System.currentTimeMillis() + 60000,
            endTriggerTimeMillis = System.currentTimeMillis() + 120000,
            title = title,
            description = description,
            timeText = "09:00 - 10:00",
            recurrenceText = "Unica vez"
        )
    }
}
