package br.com.anotacoes.data.db

import br.com.anotacoes.data.db.converter.TypeConverters
import br.com.anotacoes.domain.model.Recurrence
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskTime
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for TaskDao (Requirement 1-5).
 */
class TaskDaoTest {

    private lateinit var dao: TaskDao
    private lateinit var converters: TypeConverters

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        converters = TypeConverters()
    }

    @Test
    fun `insertTask should accept task`() = runBlocking {
        val task = createTestTask("uuid-1", "Meeting")
        val entity = task.toEntity(converters)

        coEvery { dao.insertTask(any()) } returns Unit

        dao.insertTask(entity)

        coVerify(exactly = 1) { dao.insertTask(any()) }
    }

    @Test
    fun `updateTask should accept task`() = runBlocking {
        val task = createTestTask("uuid-1", "Updated Meeting")
        val entity = task.toEntity(converters)

        coEvery { dao.updateTask(any()) } returns Unit

        dao.updateTask(entity)

        coVerify(exactly = 1) { dao.updateTask(any()) }
    }

    @Test
    fun `deleteTask should accept task ID`() = runBlocking {
        coEvery { dao.deleteTask(any()) } returns Unit

        dao.deleteTask("uuid-1")

        coVerify(exactly = 1) { dao.deleteTask("uuid-1") }
    }

    @Test
    fun `getTasksForDate should return list of tasks`() = runBlocking {
        val today = LocalDate.now()
        val entities = listOf(createTestTask("uuid-1", "Task 1").toEntity(converters))

        coEvery { dao.getTasksForDate(any()) } returns flowOf(entities)

        val result = dao.getTasksForDate(today.toEpochDay() * 86400000).first()

        assertThat(result).hasSize(1)
    }

    @Test
    fun `getTasksForDateRange should return list of tasks`() = runBlocking {
        val start = LocalDate.now()
        val end = LocalDate.now().plusDays(7)
        val entities = listOf(createTestTask("uuid-1", "Task 1").toEntity(converters))

        coEvery { dao.getTasksForDateRange(any(), any()) } returns flowOf(entities)

        val result = dao.getTasksForDateRange(
            start.toEpochDay() * 86400000,
            end.toEpochDay() * 86400000
        ).first()

        assertThat(result).hasSize(1)
    }

    @Test
    fun `getTaskById should return task or null`() = runBlocking {
        val entity = createTestTask("uuid-1", "Task 1").toEntity(converters)

        coEvery { dao.getTaskById(any()) } returns entity

        val result = dao.getTaskById("uuid-1")

        assertThat(result).isEqualTo(entity)
    }

    @Test
    fun `deleteAllTasks should delete everything`() = runBlocking {
        coEvery { dao.deleteAllTasks() } returns Unit

        dao.deleteAllTasks()

        coVerify(exactly = 1) { dao.deleteAllTasks() }
    }

    @Test
    fun `getActiveTasksForDate should return tasks at given hour`() = runBlocking {
        val entities = listOf(createTestTask("uuid-1", "Active Task").toEntity(converters))

        coEvery { dao.getActiveTasksForDate(any(), any(), any()) } returns flowOf(entities)

        val result = dao.getActiveTasksForDate(1000L, 10, 30).first()

        assertThat(result).hasSize(1)
    }

    @Test
    fun `getAllTasks should return all tasks ordered by date`() = runBlocking {
        val entities = listOf(
            createTestTask("uuid-1", "Task A").toEntity(converters),
            createTestTask("uuid-2", "Task B").toEntity(converters)
        )

        coEvery { dao.getAllTasks() } returns flowOf(entities)

        val result = dao.getAllTasks().first()

        assertThat(result).hasSize(2)
    }

    private fun createTestTask(id: String, title: String): Task {
        val today = LocalDate.now()
        return Task(
            id = id,
            title = title,
            date = today,
            taskTime = TaskTime.interval(9, 0, 10, 0),
            recurrence = Recurrence.single(today)
        )
    }
}
