package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.model.Recurrence
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskTime
import br.com.anotacoes.domain.port.TaskAlarmPort
import br.com.anotacoes.domain.repository.TaskRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * TDD tests for ScheduleTaskAlarmUseCase (Requirement 6 - Lock Screen).
 * This use case schedules start and end alarms for a single task
 * using AlarmScheduler to calculate times and TaskAlarmPort to schedule on the OS.
 */
class ScheduleTaskAlarmUseCaseTest {

    private lateinit var repository: TaskRepository
    private lateinit var alarmPort: TaskAlarmPort
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var useCase: ScheduleTaskAlarmUseCase

    @Before
    fun setup() {
        repository = mockk()
        alarmPort = mockk(relaxed = true)
        alarmScheduler = AlarmScheduler()
        useCase = ScheduleTaskAlarmUseCase(repository, alarmScheduler, alarmPort)
    }

    @Test
    fun `should schedule alarm when task exists`() = runTest {
        val task = createTask("1", "Meeting", TaskTime.interval(9, 0, 10, 0))
        coEvery { repository.getTaskById("1") } returns task

        useCase("1")

        verify(exactly = 1) { alarmPort.scheduleAlarm(any()) }
    }

    @Test
    fun `should not schedule alarm when task does not exist`() = runTest {
        coEvery { repository.getTaskById("999") } returns null

        useCase("999")

        verify(exactly = 0) { alarmPort.scheduleAlarm(any()) }
    }

    @Test
    fun `should pass correct alarm data to port`() = runTest {
        val task = createTask("t1", "Gym", TaskTime.interval(7, 0, 8, 0))
        coEvery { repository.getTaskById("t1") } returns task

        val alarmSlot = slot<UseScheduledAlarm>()
        every { alarmPort.scheduleAlarm(capture(alarmSlot)) } returns Unit

        useCase("t1")

        val scheduledAlarm = alarmSlot.captured
        assertThat(scheduledAlarm.taskId).isEqualTo("t1")
        assertThat(scheduledAlarm.title).isEqualTo("Gym")
        assertThat(scheduledAlarm.startTriggerTimeMillis).isGreaterThan(0L)
        assertThat(scheduledAlarm.endTriggerTimeMillis).isGreaterThan(scheduledAlarm.startTriggerTimeMillis)
    }

    @Test
    fun `should schedule alarm for all-day task`() = runTest {
        val task = createTask("ad1", "Birthday", TaskTime.allDay())
        coEvery { repository.getTaskById("ad1") } returns task

        val alarmSlot = slot<UseScheduledAlarm>()
        every { alarmPort.scheduleAlarm(capture(alarmSlot)) } returns Unit

        useCase("ad1")

        val scheduledAlarm = alarmSlot.captured
        assertThat(scheduledAlarm.taskId).isEqualTo("ad1")
        assertThat(scheduledAlarm.title).isEqualTo("Birthday")
    }

    @Test
    fun `should cancel existing alarm before scheduling new one`() = runTest {
        val task = createTask("c1", "Updated Task", TaskTime.interval(14, 0, 15, 0))
        coEvery { repository.getTaskById("c1") } returns task

        useCase("c1")

        val expectedRequestCode = "c1".hashCode()
        verify(ordering = io.mockk.Ordering.ORDERED) {
            alarmPort.cancelAlarm(expectedRequestCode)
            alarmPort.scheduleAlarm(any())
        }
    }

    @Test
    fun `should use task id hashCode as request code`() = runTest {
        val task = createTask("unique-id", "Task", TaskTime.interval(10, 0, 11, 0))
        coEvery { repository.getTaskById("unique-id") } returns task

        val alarmSlot = slot<UseScheduledAlarm>()
        every { alarmPort.scheduleAlarm(capture(alarmSlot)) } returns Unit

        useCase("unique-id")

        assertThat(alarmSlot.captured.requestCode).isEqualTo("unique-id".hashCode())
    }

    @Test
    fun `should include task description in alarm`() = runTest {
        val task = createTask("d1", "Review", TaskTime.interval(9, 0, 10, 0))
            .copy(description = "Sprint review meeting")
        coEvery { repository.getTaskById("d1") } returns task

        val alarmSlot = slot<UseScheduledAlarm>()
        every { alarmPort.scheduleAlarm(capture(alarmSlot)) } returns Unit

        useCase("d1")

        assertThat(alarmSlot.captured.description).isEqualTo("Sprint review meeting")
    }

    @Test
    fun `should handle task with null description`() = runTest {
        val task = createTask("n1", "Quick task", TaskTime.interval(8, 0, 9, 0))
        coEvery { repository.getTaskById("n1") } returns task

        val alarmSlot = slot<UseScheduledAlarm>()
        every { alarmPort.scheduleAlarm(capture(alarmSlot)) } returns Unit

        useCase("n1")

        assertThat(alarmSlot.captured.description).isNull()
    }

    // --- Advance reminder alarms ---

    @Test
    fun `should schedule advance reminder alarms when task has advance days`() = runTest {
        val futureDate = LocalDate.now().plusDays(10)
        val task = Task(
            id = "adv-1",
            title = "Conference",
            date = futureDate,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.single(futureDate),
            // max=7 → alarms for days 1..7 = 7 alarms
            advanceReminderDays = listOf(7)
        )
        coEvery { repository.getTaskById("adv-1") } returns task

        useCase("adv-1")

        verify(exactly = 7) { alarmPort.scheduleAdvanceReminder(any()) }
    }

    @Test
    fun `should cancel advance reminder alarms before rescheduling`() = runTest {
        val futureDate = LocalDate.now().plusDays(10)
        val task = Task(
            id = "adv-2",
            title = "Trip",
            date = futureDate,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.single(futureDate),
            // max=3 → 3 alarms (days 1, 2, 3): each is cancelled then scheduled
            advanceReminderDays = listOf(3)
        )
        coEvery { repository.getTaskById("adv-2") } returns task

        useCase("adv-2")

        verify(exactly = 3) { alarmPort.cancelAdvanceReminder(any()) }
        verify(exactly = 3) { alarmPort.scheduleAdvanceReminder(any()) }
    }

    @Test
    fun `should not schedule advance reminders when task has no advance days`() = runTest {
        val task = createTask("simple", "Simple Task", TaskTime.allDay())
        coEvery { repository.getTaskById("simple") } returns task

        useCase("simple")

        verify(exactly = 0) { alarmPort.scheduleAdvanceReminder(any()) }
        verify(exactly = 0) { alarmPort.cancelAdvanceReminder(any()) }
    }

    private fun createTask(id: String, title: String, taskTime: TaskTime): Task {
        val today = LocalDate.now()
        return Task(
            id = id,
            title = title,
            date = today,
            taskTime = taskTime,
            recurrence = Recurrence.single(today)
        )
    }
}
