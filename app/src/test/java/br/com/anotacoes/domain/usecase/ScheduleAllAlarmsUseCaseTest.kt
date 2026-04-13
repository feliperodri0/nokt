package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.model.Recurrence
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskTime
import br.com.anotacoes.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for ScheduleAllAlarmsUseCase (Requirement 6, 10, 12).
 * Schedules alarms for all tasks on a given date range.
 */
class ScheduleAllAlarmsUseCaseTest {

    private lateinit var repository: TaskRepository
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var useCase: ScheduleAllAlarmsUseCase

    @Before
    fun setup() {
        repository = mockk()
        alarmScheduler = AlarmScheduler()
        useCase = ScheduleAllAlarmsUseCase(repository, alarmScheduler)
    }

    @Test
    fun `should return alarms for tasks in date range`() = runTest {
        val futureDate = LocalDate.now().plusDays(1)
        val tasks = listOf(
            createTask("1", "Meeting", TaskTime.interval(9, 0, 10, 0)),
            createTask("2", "Lunch", TaskTime.interval(12, 0, 13, 0)),
            createTask("3", "Birthday", TaskTime.allDay())
        )

        every { repository.getTasksForDateRange(any(), any()) } returns flowOf(tasks)

        val alarms = useCase(LocalDate.now(), futureDate.plusDays(7))

        assert(alarms.size == 3)
    }

    @Test
    fun `should return empty when no tasks in range`() = runTest {
        every { repository.getTasksForDateRange(any(), any()) } returns flowOf(emptyList())

        val alarms = useCase(LocalDate.now(), LocalDate.now().plusDays(7))

        assert(alarms.isEmpty())
    }

    @Test
    fun `should compute correct trigger times`() = runTest {
        val today = LocalDate.now()
        val task = createTask("1", "Gym", TaskTime.interval(7, 0, 8, 0))
        every { repository.getTasksForDateRange(any(), any()) } returns flowOf(listOf(task))

        val alarms = useCase(today, today.plusDays(1))

        val alarm = alarms[0]
        assert(alarm.endTriggerTimeMillis > alarm.startTriggerTimeMillis)
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
