package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.model.Recurrence
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskTime
import br.com.anotacoes.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for GetTodaysActiveTasksUseCase (Requirement 6).
 * Returns tasks happening at the given time on the given date.
 */
class GetTodaysActiveTasksUseCaseTest {

    private lateinit var repository: TaskRepository
    private lateinit var useCase: GetTodaysActiveTasksUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetTodaysActiveTasksUseCase(repository)
    }

    @Test
    fun `should return tasks active at given time`() = runTest {
        val today = LocalDate.now()
        val expectedTasks = listOf(createTask("1", "Lunch", TaskTime.interval(12, 0, 13, 0)))

        every { repository.getActiveTasksForDate(today, 12, 30) } returns flowOf(expectedTasks)

        val result = useCase(today, 12, 30).first()

        assert(result.size == 1)
        assert(result[0].title == "Lunch")
    }

    @Test
    fun `should return empty list when no tasks active`() = runTest {
        every { repository.getActiveTasksForDate(any(), any(), any()) } returns flowOf(emptyList())

        val result = useCase(LocalDate.now(), 3, 0).first()

        assert(result.isEmpty())
    }

    @Test
    fun `should delegate to repository with correct params`() = runTest {
        every { repository.getActiveTasksForDate(any(), any(), any()) } returns flowOf(emptyList())

        useCase(LocalDate.of(2026, 5, 1), 14, 0).first()

        coVerify { repository.getActiveTasksForDate(LocalDate.of(2026, 5, 1), 14, 0) }
    }

    private fun createTask(id: String, title: String, taskTime: TaskTime): Task {
        return Task(
            id = id,
            title = title,
            date = LocalDate.now(),
            taskTime = taskTime,
            recurrence = Recurrence.single(LocalDate.now())
        )
    }
}
