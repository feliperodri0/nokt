package br.com.anotacoes.data.repository

import br.com.anotacoes.data.db.TaskDao
import br.com.anotacoes.data.db.TaskEntity
import br.com.anotacoes.data.db.converter.TypeConverters
import br.com.anotacoes.data.db.toEntity
import br.com.anotacoes.domain.model.Recurrence
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskTime
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Tests for TaskRepositoryImpl (Requirement 1-5).
 */
class TaskRepositoryTest {

    private lateinit var dao: TaskDao
    private lateinit var repository: TaskRepositoryImpl
    private lateinit var converters: TypeConverters

    @Before
    fun setup() {
        converters = TypeConverters()
        dao = mockk()
        repository = TaskRepositoryImpl(dao, converters)
    }

    @Test
    fun `insertTask should map domain to entity and call DAO`() = runBlocking {
        val task = createTestTask("uuid-1", "Meeting")

        coEvery { dao.insertTask(any<TaskEntity>()) } returns Unit

        repository.insertTask(task)

        coVerify { dao.insertTask(match { it.id == "uuid-1" && it.title == "Meeting" }) }
    }

    @Test
    fun `updateTask should map domain to entity and call DAO`() = runBlocking {
        val task = createTestTask("uuid-1", "Updated")

        coEvery { dao.updateTask(any<TaskEntity>()) } returns Unit

        repository.updateTask(task)

        coVerify { dao.updateTask(match { it.id == "uuid-1" }) }
    }

    @Test
    fun `deleteTask should call DAO with ID`() = runBlocking {
        coEvery { dao.deleteTask(any()) } returns Unit

        repository.deleteTask("uuid-1")

        coVerify { dao.deleteTask("uuid-1") }
    }

    @Test
    fun `getTasksForDate should return mapped domain tasks`() = runBlocking {
        val today = LocalDate.now()
        val todayMillis = today.toEpochDay() * 86400000
        val entity = createTestTask("uuid-1", "Lunch")
            .toEntity(converters)

        every { dao.getTasksForDateWithRecurrence(todayMillis) } returns flowOf(listOf(entity))

        val result = repository.getTasksForDate(today).first()

        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("Lunch")
        assertThat(result[0].id).isEqualTo("uuid-1")
    }

    @Test
    fun `getTasksForDateRange should return mapped domain tasks`() = runBlocking {
        val start = LocalDate.now()
        val end = LocalDate.now().plusDays(7)
        val tasks = listOf(
            createTestTask("1", "Task A").toEntity(converters),
            createTestTask("2", "Task B").toEntity(converters)
        )

        every { dao.getTasksForDateRange(any(), any()) } returns flowOf(tasks)

        val result = repository.getTasksForDateRange(start, end).first()

        assertThat(result).hasSize(2)
    }

    @Test
    fun `getTaskById should return domain task or null`() = runBlocking {
        val entity = createTestTask("uuid-1", "Single").toEntity(converters)

        coEvery { dao.getTaskById("uuid-1") } returns entity

        val result = repository.getTaskById("uuid-1")

        assertThat(result?.title).isEqualTo("Single")
    }

    @Test
    fun `getTaskById should return null for unknown ID`() = runBlocking {
        coEvery { dao.getTaskById("unknown") } returns null

        val result = repository.getTaskById("unknown")

        assertThat(result).isNull()
    }

    @Test
    fun `getActiveTasksForDate should return active tasks at given time`() = runBlocking {
        val entity = createTestTask("uuid-1", "Lunch")
            .toEntity(converters)

        every { dao.getActiveTasksForDate(any(), 12, 0) } returns flowOf(listOf(entity))

        val result = repository.getActiveTasksForDate(LocalDate.now(), 12, 0).first()

        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("Lunch")
    }

    @Test
    fun `dismissTask should update status to DISMISSED`() = runBlocking {
        coEvery { dao.updateStatus(any(), any()) } returns Unit

        repository.dismissTask("uuid-1")

        coVerify { dao.updateStatus("uuid-1", "DISMISSED") }
    }

    @Test
    fun `getAllTasks should return all mapped domain tasks`() = runBlocking {
        val tasks = listOf(
            createTestTask("1", "A").toEntity(converters),
            createTestTask("2", "B").toEntity(converters)
        )

        every { dao.getAllTasks() } returns flowOf(tasks)

        val result = repository.getAllTasks().first()

        assertThat(result).hasSize(2)
    }

    // --- Yearly recurrence filter ---

    @Test
    fun `getTasksForDate should include yearly task on matching month and day`() = runBlocking {
        val april24 = LocalDate.of(2026, 4, 24)
        val millis = april24.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val yearlyEntity = TaskEntity(
            id = "yearly-1",
            title = "Anniversary",
            dateMillis = millis,
            isAllDay = true,
            startHour = null,
            startMinute = null,
            endHour = null,
            endMinute = null,
            recurrenceType = "YEARLY",
            recurrenceDateMillis = null,
            recurrenceDaysJson = "4-24",
            description = null,
            attachmentsJson = "[]",
            checklistJson = "[]"
        )

        every { dao.getTasksForDateWithRecurrence(millis) } returns flowOf(listOf(yearlyEntity))

        val result = repository.getTasksForDate(april24).first()

        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("Anniversary")
    }

    @Test
    fun `getTasksForDate should exclude yearly task on non-matching day`() = runBlocking {
        val april25 = LocalDate.of(2026, 4, 25)
        val millis = april25.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        // yearly task is for April 24, but we query April 25
        val yearlyEntity = TaskEntity(
            id = "yearly-2",
            title = "Anniversary",
            dateMillis = millis,
            isAllDay = true,
            startHour = null,
            startMinute = null,
            endHour = null,
            endMinute = null,
            recurrenceType = "YEARLY",
            recurrenceDateMillis = null,
            recurrenceDaysJson = "4-24",
            description = null,
            attachmentsJson = "[]",
            checklistJson = "[]"
        )

        every { dao.getTasksForDateWithRecurrence(millis) } returns flowOf(listOf(yearlyEntity))

        val result = repository.getTasksForDate(april25).first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `getTasksForDate should exclude yearly task on matching day but wrong month`() = runBlocking {
        val may24 = LocalDate.of(2026, 5, 24)
        val millis = may24.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val yearlyEntity = TaskEntity(
            id = "yearly-3",
            title = "Anniversary",
            dateMillis = millis,
            isAllDay = true,
            startHour = null,
            startMinute = null,
            endHour = null,
            endMinute = null,
            recurrenceType = "YEARLY",
            recurrenceDateMillis = null,
            recurrenceDaysJson = "4-24", // April 24, but querying May 24
            description = null,
            attachmentsJson = "[]",
            checklistJson = "[]"
        )

        every { dao.getTasksForDateWithRecurrence(millis) } returns flowOf(listOf(yearlyEntity))

        val result = repository.getTasksForDate(may24).first()

        assertThat(result).isEmpty()
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
