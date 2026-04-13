package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.model.Recurrence
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskStatus
import br.com.anotacoes.domain.model.TaskTime
import br.com.anotacoes.domain.port.TaskAlarmPort
import br.com.anotacoes.domain.port.TaskNotificationPort
import br.com.anotacoes.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for all Task use cases (Requirement 1-5).
 */
class TaskUseCasesTest {

    private lateinit var repository: TaskRepository
    private lateinit var notificationPort: TaskNotificationPort
    private lateinit var alarmPort: TaskAlarmPort
    private lateinit var createTask: CreateTaskUseCase
    private lateinit var updateTask: UpdateTaskUseCase
    private lateinit var deleteTask: DeleteTaskUseCase
    private lateinit var getTasksForDate: GetTasksForDateUseCase
    private lateinit var dismissTask: DismissTaskUseCase
    private lateinit var completeTask: CompleteTaskUseCase
    private lateinit var getAllTasks: GetAllTasksUseCase

    @Before
    fun setup() {
        repository = mockk()
        notificationPort = mockk()
        alarmPort = mockk()
        createTask = CreateTaskUseCase(repository)
        updateTask = UpdateTaskUseCase(repository)
        deleteTask = DeleteTaskUseCase(repository, notificationPort, alarmPort)
        getTasksForDate = GetTasksForDateUseCase(repository)
        dismissTask = DismissTaskUseCase(repository, notificationPort, alarmPort)
        completeTask = CompleteTaskUseCase(repository, notificationPort, alarmPort)
        getAllTasks = GetAllTasksUseCase(repository)

        justRun { notificationPort.cancelNotification(any()) }
        justRun { alarmPort.cancelAlarm(any()) }
    }

    // --- Create ---

    @Test
    fun `create task should delegate to repository`() = runTest {
        val task = makeTask("1", "Meeting")
        coEvery { repository.insertTask(any()) } returns Unit

        createTask(task)

        coVerify { repository.insertTask(task) }
    }

    // --- Update ---

    @Test
    fun `update task should delegate to repository`() = runTest {
        val task = makeTask("1", "Updated")
        coEvery { repository.updateTask(any()) } returns Unit

        updateTask(task)

        coVerify { repository.updateTask(task) }
    }

    // --- Delete ---

    @Test
    fun `delete task should delegate to repository`() = runTest {
        coEvery { repository.deleteTask(any()) } returns Unit

        deleteTask("1")

        coVerify { repository.deleteTask("1") }
    }

    @Test
    fun `delete task should cancel notification and alarm`() = runTest {
        coEvery { repository.deleteTask(any()) } returns Unit

        deleteTask("task-abc")

        coVerify { notificationPort.cancelNotification("task-abc") }
        coVerify { alarmPort.cancelAlarm(any()) }
    }

    // --- Dismiss ---

    @Test
    fun `dismiss task should set status to DISMISSED`() = runTest {
        coEvery { repository.setTaskStatus(any(), any()) } returns Unit

        dismissTask("task-1")

        coVerify { repository.setTaskStatus("task-1", TaskStatus.DISMISSED) }
    }

    @Test
    fun `dismiss task should cancel notification and alarm`() = runTest {
        coEvery { repository.setTaskStatus(any(), any()) } returns Unit

        dismissTask("task-1")

        coVerify { notificationPort.cancelNotification("task-1") }
        coVerify { alarmPort.cancelAlarm(any()) }
    }

    // --- Complete ---

    @Test
    fun `complete task should set status to COMPLETED`() = runTest {
        coEvery { repository.setTaskStatus(any(), any()) } returns Unit

        completeTask("task-2")

        coVerify { repository.setTaskStatus("task-2", TaskStatus.COMPLETED) }
    }

    @Test
    fun `complete task should cancel notification and alarm`() = runTest {
        coEvery { repository.setTaskStatus(any(), any()) } returns Unit

        completeTask("task-2")

        coVerify { notificationPort.cancelNotification("task-2") }
        coVerify { alarmPort.cancelAlarm(any()) }
    }

    // --- Get Tasks For Date ---

    @Test
    fun `get tasks for date should return repository tasks`() = runTest {
        val today = LocalDate.now()
        val tasks = listOf(makeTask("1", "Task"))
        every { repository.getTasksForDate(today) } returns flowOf(tasks)

        val result = getTasksForDate(today).first()

        assert(result.size == 1)
    }

    @Test
    fun `get tasks for date should return empty when none`() = runTest {
        every { repository.getTasksForDate(any()) } returns flowOf(emptyList())

        val result = getTasksForDate(LocalDate.now()).first()

        assert(result.isEmpty())
    }

    // --- GetAll ---

    @Test
    fun `get all tasks should return repository tasks`() = runTest {
        val tasks = listOf(makeTask("1", "A"), makeTask("2", "B"))
        every { repository.getAllTasks() } returns flowOf(tasks)

        val result = getAllTasks().first()

        assert(result.size == 2)
    }

    private fun makeTask(id: String, title: String): Task {
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
