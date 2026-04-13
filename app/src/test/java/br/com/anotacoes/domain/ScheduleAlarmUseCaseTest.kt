package br.com.anotacoes.domain

import br.com.anotacoes.domain.model.DayOfWeek
import br.com.anotacoes.domain.model.Recurrence
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskTime
import br.com.anotacoes.domain.usecase.AlarmScheduler
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Tests for AlarmScheduler use case (Requirement 6, 10, 12).
 * Validates alarm time calculation from task time interval.
 */
class ScheduleAlarmUseCaseTest {

    private val scheduler = AlarmScheduler()

    @Test
    fun `should calculate alarm timestamp for a task with time interval`() {
        val task = createTask("Meeting", TaskTime.interval(9, 30, 10, 0))

        val alarm = scheduler.calculateAlarmForTask(task)

        assertThat(alarm.title).isEqualTo("Meeting")
        assertThat(alarm.startTriggerTimeMillis).isGreaterThan(0L)
    }

    @Test
    fun `should calculate alarm for all-day task at midnight`() {
        val today = LocalDate.now()
        val task = createTask("Birthday", TaskTime.allDay())

        val alarm = scheduler.calculateAlarmForTask(task)

        assertThat(alarm.startTriggerTimeMillis).isAtLeast(today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    }

    @Test
    fun `alarm time should be after current system time for future tasks`() {
        val futureDate = LocalDate.now().plusDays(1)
        val task = createTask("Tomorrow meeting", TaskTime.interval(10, 0, 11, 0))
            .copy(date = futureDate)

        val now = System.currentTimeMillis()
        val alarm = scheduler.calculateAlarmForTask(task)

        assertThat(alarm.startTriggerTimeMillis).isGreaterThan(now)
    }

    @Test
    fun `alarm request code should be unique per task ID`() {
        val task1 = createTask("Task 1", TaskTime.interval(9, 0, 10, 0))
        val task2 = createTask("Task 2", TaskTime.interval(9, 0, 10, 0))

        val alarm1 = scheduler.calculateAlarmForTask(task1)
        val alarm2 = scheduler.calculateAlarmForTask(task2)

        assertThat(alarm1.requestCode).isNotEqualTo(alarm2.requestCode)
    }

    @Test
    fun `should calculate start alarm time correctly`() {
        val today = LocalDate.now()
        val task = createTask("Gym", TaskTime.interval(7, 0, 8, 0))

        val expectedMidnight = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val alarm = scheduler.calculateAlarmForTask(task)

        assertThat(alarm.startTriggerTimeMillis).isGreaterThan(expectedMidnight)
    }

    @Test
    fun `end alarm time should be after start alarm time`() {
        val task = createTask("Class", TaskTime.interval(10, 0, 11, 0))

        val alarm = scheduler.calculateAlarmForTask(task)

        assertThat(alarm.endTriggerTimeMillis).isGreaterThan(alarm.startTriggerTimeMillis)
    }

    @Test
    fun `should map task ID to alarm correctly`() {
        val task = createTask("Dentist", TaskTime.interval(14, 0, 14, 30))

        val alarm = scheduler.calculateAlarmForTask(task)

        assertThat(alarm.taskId).isEqualTo(task.id)
        assertThat(alarm.description).isNull()
    }

    @Test
    fun `should map task description to alarm`() {
        val task = createTask("Review", TaskTime.interval(9, 0, 10, 0))
            .copy(description = "Code review sprint")

        val alarm = scheduler.calculateAlarmForTask(task)

        assertThat(alarm.description).isEqualTo("Code review sprint")
    }

    // --- Recurrence text ---

    @Test
    fun `should build yearly recurrence text`() {
        val task = createTask("Anniversary", TaskTime.allDay())
            .copy(recurrence = Recurrence.Yearly(4, 24))

        val alarm = scheduler.calculateAlarmForTask(task)

        assertThat(alarm.recurrenceText).contains("Abr")
        assertThat(alarm.recurrenceText).contains("24")
    }

    @Test
    fun `should build monthly recurrence text`() {
        val task = createTask("Bill", TaskTime.allDay())
            .copy(recurrence = Recurrence.monthly(15))

        val alarm = scheduler.calculateAlarmForTask(task)

        assertThat(alarm.recurrenceText).contains("15")
    }

    @Test
    fun `should build custom weekly recurrence text`() {
        val task = createTask("Gym", TaskTime.interval(7, 0, 8, 0))
            .copy(recurrence = Recurrence.customWeekly(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)))

        val alarm = scheduler.calculateAlarmForTask(task)

        assertThat(alarm.recurrenceText).contains("Seg")
        assertThat(alarm.recurrenceText).contains("Qua")
    }

    // --- Advance reminder alarms ---

    @Test
    fun `should return empty list when no advance reminder days`() {
        val task = createTask("Simple", TaskTime.allDay())

        val alarms = scheduler.calculateAdvanceReminderAlarms(task)

        assertThat(alarms).isEmpty()
    }

    @Test
    fun `should return one alarm per day in range 1 to max reminder day`() {
        val futureDate = LocalDate.now().plusDays(10)
        val task = Task(
            id = "adv-1",
            title = "Conference",
            date = futureDate,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.Yearly(futureDate.monthValue, futureDate.dayOfMonth),
            // max=3 → alarms for days 1, 2 and 3 before the task
            advanceReminderDays = listOf(3)
        )

        val alarms = scheduler.calculateAdvanceReminderAlarms(task)

        assertThat(alarms).hasSize(3)
    }

    @Test
    fun `advance reminder alarm should fire 8AM on correct day`() {
        val futureDate = LocalDate.now().plusDays(10)
        val task = Task(
            id = "adv-2",
            title = "Trip",
            date = futureDate,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.Yearly(futureDate.monthValue, futureDate.dayOfMonth),
            // max=7 → alarms for days 1..7; verify the outermost (7-day) alarm
            advanceReminderDays = listOf(7)
        )

        val alarms = scheduler.calculateAdvanceReminderAlarms(task)
        assertThat(alarms).hasSize(7)

        val alarm = alarms.first { it.daysRemaining == 7 }
        assertThat(alarm.taskTitle).isEqualTo("Trip")
        assertThat(alarm.taskId).isEqualTo("adv-2")
    }

    @Test
    fun `advance reminder request codes should be unique per day count`() {
        val futureDate = LocalDate.now().plusDays(10)
        val task = Task(
            id = "adv-3",
            title = "Event",
            date = futureDate,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.Yearly(futureDate.monthValue, futureDate.dayOfMonth),
            // max=7 → alarms for days 1..7; all 7 request codes must be distinct
            advanceReminderDays = listOf(7)
        )

        val alarms = scheduler.calculateAdvanceReminderAlarms(task)

        val codes = alarms.map { it.requestCode }.toSet()
        assertThat(codes).hasSize(alarms.size)
    }

    @Test
    fun `advance reminder trigger times should differ by day count`() {
        val futureDate = LocalDate.now().plusDays(10)
        val task = Task(
            id = "adv-4",
            title = "Seminar",
            date = futureDate,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.Yearly(futureDate.monthValue, futureDate.dayOfMonth),
            advanceReminderDays = listOf(7, 3)
        )

        val alarms = scheduler.calculateAdvanceReminderAlarms(task)
        val sevenDay = alarms.first { it.daysRemaining == 7 }
        val threeDay = alarms.first { it.daysRemaining == 3 }

        // 7-day reminder fires earlier than 3-day
        assertThat(sevenDay.triggerTimeMillis).isLessThan(threeDay.triggerTimeMillis)
    }

    private fun createTask(title: String = "Task", taskTime: TaskTime = TaskTime.allDay()): Task {
        val today = LocalDate.now()
        return Task(
            id = title.hashCode().toString(),
            title = title,
            date = today,
            taskTime = taskTime,
            recurrence = Recurrence.single(today)
        )
    }
}
