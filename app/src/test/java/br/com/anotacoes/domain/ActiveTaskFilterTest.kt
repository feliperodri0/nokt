package br.com.anotacoes.domain

import br.com.anotacoes.domain.model.ChecklistItem
import br.com.anotacoes.domain.model.Recurrence
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskTime
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for active task business rules (Requirement 6, 7).
 * Verifies that only tasks matching the current time are considered "active"
 * and should appear on lock screen and widget.
 */
class ActiveTaskFilterTest {

    @Test
    fun `task within its time interval should be active`() {
        val task = createTask("Lunch", TaskTime.interval(10, 0, 12, 0))

        assertThat(isTaskActiveAt(task, 11, 30)).isTrue()
    }

    @Test
    fun `task exactly at start time should be active`() {
        val task = createTask("Meeting", TaskTime.interval(14, 0, 15, 0))

        assertThat(isTaskActiveAt(task, 14, 0)).isTrue()
    }

    @Test
    fun `task exactly at end time should NOT be active`() {
        val task = createTask("Gym", TaskTime.interval(18, 0, 19, 0))

        assertThat(isTaskActiveAt(task, 19, 0)).isFalse()
    }

    @Test
    fun `all-day task should always be active`() {
        val task = createTask("Birthday", TaskTime.allDay())

        assertThat(isTaskActiveAt(task, 3, 0)).isTrue()
        assertThat(isTaskActiveAt(task, 12, 0)).isTrue()
        assertThat(isTaskActiveAt(task, 23, 59)).isTrue()
    }

    @Test
    fun `task before start time should NOT be active`() {
        val task = createTask("Meeting", TaskTime.interval(10, 0, 11, 0))

        assertThat(isTaskActiveAt(task, 9, 0)).isFalse()
    }

    @Test
    fun `task after end time should NOT be active`() {
        val task = createTask("Dentist", TaskTime.interval(8, 0, 8, 30))

        assertThat(isTaskActiveAt(task, 9, 0)).isFalse()
    }

    @Test
    fun `task ending at current hour but before minutes should be active`() {
        val task = createTask("Call", TaskTime.interval(9, 0, 10, 30))

        assertThat(isTaskActiveAt(task, 10, 0)).isTrue()
    }

    @Test
    fun `task starting at current hour but after minutes should NOT be active`() {
        val task = createTask("Lunch", TaskTime.interval(12, 30, 13, 0))

        assertThat(isTaskActiveAt(task, 12, 0)).isFalse()
    }

    @Test
    fun `filter should exclude tasks outside active time`() {
        val activeTask = createTask("Lunch", TaskTime.interval(12, 0, 13, 0))
        val pastTask = createTask("Breakfast", TaskTime.interval(6, 0, 7, 0))
        val futureTask = createTask("Dinner", TaskTime.interval(20, 0, 21, 0))
        val allDayTask = createTask("Birthday", TaskTime.allDay())

        val allTasks = listOf(activeTask, pastTask, futureTask, allDayTask)
        val filtered = filterActiveTasks(allTasks, 12, 30)

        assertThat(filtered).hasSize(2)
        assertThat(filtered.map { it.title }).containsExactly("Lunch", "Birthday")
    }

    @Test
    fun `filter should return empty when no tasks active`() {
        val pastTask = createTask("Yoga", TaskTime.interval(6, 0, 7, 0))
        val futureTask = createTask("Dinner", TaskTime.interval(19, 0, 20, 0))

        val result = filterActiveTasks(listOf(pastTask, futureTask), 13, 0)

        assertThat(result).isEmpty()
    }

    @Test
    fun `task with description and attachments should be filterable`() {
        val task = createFullTask(
            "Project",
            TaskTime.interval(9, 0, 11, 0),
            description = "Complete project",
            checklistItems = listOf(ChecklistItem(title = "Step 1"))
        )

        assertThat(isTaskActiveAt(task, 10, 0)).isTrue()
    }

    private fun createTask(title: String, taskTime: TaskTime): Task {
        val today = LocalDate.now()
        return Task(
            title = title,
            date = today,
            taskTime = taskTime,
            recurrence = Recurrence.single(today)
        )
    }

    private fun createFullTask(
        title: String,
        taskTime: TaskTime,
        description: String?,
        checklistItems: List<ChecklistItem>
    ): Task {
        val today = LocalDate.now()
        return Task(
            title = title,
            date = today,
            taskTime = taskTime,
            recurrence = Recurrence.single(today),
            description = description,
            checklistItems = checklistItems
        )
    }

    private fun isTaskActiveAt(task: Task, hour: Int, minute: Int): Boolean {
        if (task.taskTime.isAllDay) return true
        val startTime = task.taskTime.startTime!!
        val endTime = task.taskTime.endTime!!

        val currentTotal = hour * 60 + minute
        val startTotal = startTime.hour * 60 + startTime.minute
        val endTotal = endTime.hour * 60 + endTime.minute

        return currentTotal in startTotal until endTotal
    }

    private fun filterActiveTasks(tasks: List<Task>, hour: Int, minute: Int): List<Task> {
        val currentTotal = hour * 60 + minute
        return tasks.filter { task ->
            if (task.taskTime.isAllDay) {
                true
            } else {
                val startTotal = task.taskTime.startTime!!.hour * 60 + task.taskTime.startTime!!.minute
                val endTotal = task.taskTime.endTime!!.hour * 60 + task.taskTime.endTime!!.minute
                currentTotal in startTotal until endTotal
            }
        }
    }
}
