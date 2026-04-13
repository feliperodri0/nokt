package br.com.anotacoes.receiver

import br.com.anotacoes.domain.model.Recurrence
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskTime
import br.com.anotacoes.domain.repository.TaskRepository
import br.com.anotacoes.domain.usecase.AlarmScheduler
import br.com.anotacoes.domain.usecase.UseScheduledAlarm
import io.mockk.every
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for boot reschedule logic (Requirement 12).
 * When device reboots, all future tasks must have their alarms re-scheduled.
 */
class BootCompletedReceiverTest {

    private lateinit var repository: TaskRepository
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var bootRescheduler: BootTaskRescheduler

    @Before
    fun setup() {
        repository = mockk()
        alarmScheduler = AlarmScheduler()
        bootRescheduler = BootTaskRescheduler(repository, alarmScheduler)
    }

    @Test
    fun `reschedule should query tasks from today to 5 years in future`() = runTest {
        every { repository.getTasksForDateRange(any(), any()) } returns flowOf(emptyList())

        bootRescheduler.rescheduleAll()

        val today = LocalDate.now()
        coVerify { repository.getTasksForDateRange(today, today.plusYears(5)) }
    }

    @Test
    fun `reschedule should compute alarms for all tasks`() = runTest {
        val today = LocalDate.now()
        val tasks = listOf(
            createTask("1", "Meeting", TaskTime.interval(9, 0, 10, 0)),
            createTask("2", "Gym", TaskTime.interval(18, 0, 19, 0))
        )
        every { repository.getTasksForDateRange(any(), any()) } returns flowOf(tasks)

        val alarms = bootRescheduler.rescheduleAll()

        assert(alarms.size == 2)
    }

    @Test
    fun `reschedule should handle empty task list`() = runTest {
        every { repository.getTasksForDateRange(any(), any()) } returns flowOf(emptyList())

        val alarms = bootRescheduler.rescheduleAll()

        assert(alarms.isEmpty())
    }

    @Test
    fun `reschedule should produce valid alarm times for all tasks`() = runTest {
        val tasks = listOf(
            createTask("1", "Lunch", TaskTime.interval(12, 0, 13, 0)),
            createTask("2", "All Day", TaskTime.allDay())
        )
        every { repository.getTasksForDateRange(any(), any()) } returns flowOf(tasks)

        val alarms = bootRescheduler.rescheduleAll()

        alarms.forEach { alarm ->
            assert(alarm.startTriggerTimeMillis > 0)
            assert(alarm.endTriggerTimeMillis > alarm.startTriggerTimeMillis)
        }
    }

    @Test
    fun `reschedule should include daily recurring tasks`() = runTest {
        val today = LocalDate.now()
        val tasks = listOf(
            createTask("1", "Daily Task", TaskTime.interval(8, 0, 8, 30))
                .copy(recurrence = Recurrence.daily())
        )
        every { repository.getTasksForDateRange(any(), any()) } returns flowOf(tasks)

        val alarms = bootRescheduler.rescheduleAll()

        assert(alarms.size == 1)
        assert(alarms[0].title == "Daily Task")
    }

    @Test
    fun `reschedule should handle many tasks`() = runTest {
        val tasks = (1..50).map { i ->
            val startHour = i % 23
            val endHour = startHour + 1
            createTask("$i", "Task $i", TaskTime.interval(startHour, 0, endHour, 0))
        }
        every { repository.getTasksForDateRange(any(), any()) } returns flowOf(tasks)

        val alarms = bootRescheduler.rescheduleAll()

        assert(alarms.size == 50)
    }

    private fun createTask(id: String = "id", title: String, taskTime: TaskTime): Task {
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
