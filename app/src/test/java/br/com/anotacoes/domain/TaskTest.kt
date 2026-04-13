package br.com.anotacoes.domain

import br.com.anotacoes.domain.model.ChecklistItem
import br.com.anotacoes.domain.model.Recurrence
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskTime
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for Task model (Requirement 1, 3).
 */
class TaskTest {

    @Test
    fun `should create minimal task with title and date`() {
        val today = LocalDate.now()
        val task = Task(
            title = "Meeting",
            date = today,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.single(today)
        )

        assertThat(task.title).isEqualTo("Meeting")
        assertThat(task.date).isEqualTo(today)
        assertThat(task.taskTime.isAllDay).isTrue()
        assertThat(task.description).isNull()
        assertThat(task.attachments).isEmpty()
        assertThat(task.checklistItems).isEmpty()
    }

    @Test
    fun `should create task with time interval`() {
        val today = LocalDate.now()
        val task = Task(
            title = "Lunch",
            date = today,
            taskTime = TaskTime.interval(12, 0, 13, 30),
            recurrence = Recurrence.single(today)
        )

        assertThat(task.taskTime.isAllDay).isFalse()
        assertThat(task.taskTime.startTime?.hour).isEqualTo(12)
        assertThat(task.taskTime.endTime?.hour).isEqualTo(13)
    }

    @Test
    fun `should create task with optional description`() {
        val task = Task(
            title = "Task",
            date = LocalDate.now(),
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.single(LocalDate.now()),
            description = "Some description"
        )

        assertThat(task.description).isEqualTo("Some description")
    }

    @Test
    fun `should create task with checklist items`() {
        val checklist = listOf(
            ChecklistItem(title = "Step 1"),
            ChecklistItem(title = "Step 2")
        )

        val task = Task(
            title = "Project",
            date = LocalDate.now(),
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.single(LocalDate.now()),
            checklistItems = checklist
        )

        assertThat(task.checklistItems).hasSize(2)
        assertThat(task.checklistItems.map { it.title })
            .containsExactly("Step 1", "Step 2")
    }

    @Test
    fun `should create task with attachments`() {
        val attachments = listOf("attachment_001.jpg", "doc_002.pdf")

        val task = Task(
            title = "Project",
            date = LocalDate.now(),
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.single(LocalDate.now()),
            attachments = attachments
        )

        assertThat(task.attachments).isEqualTo(attachments)
    }

    @Test
    fun `should assign id automatically`() {
        val task1 = createDefaultTask("Task 1")
        val task2 = createDefaultTask("Task 2")

        assertThat(task1.id).isNotEmpty()
        assertThat(task2.id).isNotEmpty()
        assertThat(task1.id).isNotEqualTo(task2.id)
    }

    @Test
    fun `should create task with recurrence daily`() {
        val task = Task(
            title = "Daily standup",
            date = LocalDate.now(),
            taskTime = TaskTime.interval(9, 0, 9, 15),
            recurrence = Recurrence.daily()
        )

        assertThat(task.recurrence.type).isEqualTo(br.com.anotacoes.domain.model.RecurrenceType.Daily)
    }

    @Test
    fun `should create task with custom weekly recurrence`() {
        val task = Task(
            title = "Gym",
            date = LocalDate.now(),
            taskTime = TaskTime.interval(18, 0, 19, 30),
            recurrence = Recurrence.customWeekly(
                listOf(
                    br.com.anotacoes.domain.model.DayOfWeek.MONDAY,
                    br.com.anotacoes.domain.model.DayOfWeek.WEDNESDAY
                )
            )
        )

        assertThat(task.recurrence.type).isEqualTo(br.com.anotacoes.domain.model.RecurrenceType.CustomWeekly)
        assertThat(task.recurrence.daysOfWeek)
            .hasSize(2)
    }

    @Test
    fun `should reject task with empty title`() {
        assertThrows(IllegalArgumentException::class.java) {
            Task(
                title = "",
                date = LocalDate.now(),
                taskTime = TaskTime.allDay(),
                recurrence = Recurrence.single(LocalDate.now())
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            Task(
                title = "   ",
                date = LocalDate.now(),
                taskTime = TaskTime.allDay(),
                recurrence = Recurrence.single(LocalDate.now())
            )
        }
    }

    @Test
    fun `should update task title`() {
        val task = createDefaultTask("Old")
        val updated = task.copy(title = "New")

        assertThat(updated.title).isEqualTo("New")
        assertThat(updated.id).isEqualTo(task.id)
    }

    @Test
    fun `should update task date`() {
        val task = createDefaultTask("Task")
        val tomorrow = LocalDate.now().plusDays(1)
        val updated = task.copy(date = tomorrow)

        assertThat(updated.date).isEqualTo(tomorrow)
    }

    @Test
    fun `should update task time`() {
        val task = createDefaultTask("Task")
        val newTime = TaskTime.interval(14, 0, 15, 0)
        val updated = task.copy(taskTime = newTime)

        assertThat(updated.taskTime).isEqualTo(newTime)
    }

    // Helper to reduce boilerplate
    private fun createDefaultTask(title: String): Task {
        val today = LocalDate.now()
        return Task(
            title = title,
            date = today,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.single(today)
        )
    }
}
