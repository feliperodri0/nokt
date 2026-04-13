package br.com.anotacoes.presentation

import android.content.Context
import br.com.anotacoes.domain.model.Recurrence
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskStatus
import br.com.anotacoes.domain.model.TaskTime
import br.com.anotacoes.domain.usecase.CompleteTaskUseCase
import br.com.anotacoes.domain.usecase.CreateTaskUseCase
import br.com.anotacoes.domain.usecase.DeleteTaskUseCase
import br.com.anotacoes.domain.usecase.DismissTaskUseCase
import br.com.anotacoes.domain.usecase.GetTaskByIdUseCase
import br.com.anotacoes.domain.usecase.ReactivateTaskUseCase
import br.com.anotacoes.domain.usecase.ScheduleTaskAlarmUseCase
import br.com.anotacoes.domain.usecase.ToggleTaskNotificationUseCase
import br.com.anotacoes.domain.usecase.UpdateTaskUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TaskFormViewModelStatusTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var createTaskUseCase: CreateTaskUseCase
    private lateinit var updateTaskUseCase: UpdateTaskUseCase
    private lateinit var scheduleTaskAlarmUseCase: ScheduleTaskAlarmUseCase
    private lateinit var getTaskByIdUseCase: GetTaskByIdUseCase
    private lateinit var completeTaskUseCase: CompleteTaskUseCase
    private lateinit var dismissTaskUseCase: DismissTaskUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase
    private lateinit var toggleTaskNotificationUseCase: ToggleTaskNotificationUseCase
    private lateinit var reactivateTaskUseCase: ReactivateTaskUseCase
    private lateinit var context: Context
    private lateinit var viewModel: TaskFormViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        createTaskUseCase = mockk(relaxed = true)
        updateTaskUseCase = mockk(relaxed = true)
        scheduleTaskAlarmUseCase = mockk(relaxed = true)
        getTaskByIdUseCase = mockk()
        completeTaskUseCase = mockk(relaxed = true)
        dismissTaskUseCase = mockk(relaxed = true)
        deleteTaskUseCase = mockk(relaxed = true)
        toggleTaskNotificationUseCase = mockk(relaxed = true)
        reactivateTaskUseCase = mockk(relaxed = true)
        context = mockk(relaxed = true)
        viewModel = TaskFormViewModel(
            createTaskUseCase, updateTaskUseCase, scheduleTaskAlarmUseCase,
            getTaskByIdUseCase, context, completeTaskUseCase, dismissTaskUseCase,
            deleteTaskUseCase, toggleTaskNotificationUseCase, reactivateTaskUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeTask(id: String, status: TaskStatus = TaskStatus.ACTIVE): Task =
        Task(
            id = id,
            title = "Test Task",
            date = LocalDate.now().plusDays(1),
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.Single(LocalDate.now().plusDays(1)),
            status = status
        )

    // ── CompleteTask ───────────────────────────────────────────────────────────

    @Test
    fun `CompleteTask should call completeTaskUseCase`() = runTest {
        coEvery { getTaskByIdUseCase("task-1") } returns makeTask("task-1")
        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.CompleteTask)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { completeTaskUseCase("task-1") }
    }

    @Test
    fun `CompleteTask should update currentTaskStatus to COMPLETED`() = runTest {
        coEvery { getTaskByIdUseCase("task-1") } returns makeTask("task-1")
        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.CompleteTask)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.first().currentTaskStatus).isEqualTo(TaskStatus.COMPLETED)
    }

    @Test
    fun `CompleteTask should set pendingTaskUndo`() = runTest {
        coEvery { getTaskByIdUseCase("task-1") } returns makeTask("task-1")
        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.CompleteTask)
        testDispatcher.scheduler.advanceUntilIdle()

        val undo = viewModel.uiState.first().pendingTaskUndo
        assertThat(undo).isNotNull()
        assertThat((undo as TaskFormPendingUndo.TaskStatusUndo).wasCompleted).isTrue()
    }

    @Test
    fun `CompleteTask should not proceed when no task is loaded`() = runTest {
        viewModel.onIntent(TaskFormIntent.CompleteTask)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { completeTaskUseCase(any()) }
    }

    // ── DismissTask ────────────────────────────────────────────────────────────

    @Test
    fun `DismissTask should call dismissTaskUseCase`() = runTest {
        coEvery { getTaskByIdUseCase("task-1") } returns makeTask("task-1")
        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.DismissTask)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { dismissTaskUseCase("task-1") }
    }

    @Test
    fun `DismissTask should update currentTaskStatus to DISMISSED`() = runTest {
        coEvery { getTaskByIdUseCase("task-1") } returns makeTask("task-1")
        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.DismissTask)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.first().currentTaskStatus).isEqualTo(TaskStatus.DISMISSED)
    }

    @Test
    fun `DismissTask should set pendingTaskUndo with wasCompleted false`() = runTest {
        coEvery { getTaskByIdUseCase("task-1") } returns makeTask("task-1")
        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.DismissTask)
        testDispatcher.scheduler.advanceUntilIdle()

        val undo = viewModel.uiState.first().pendingTaskUndo
        assertThat(undo).isNotNull()
        assertThat((undo as TaskFormPendingUndo.TaskStatusUndo).wasCompleted).isFalse()
    }

    @Test
    fun `DismissTask should not proceed when no task is loaded`() = runTest {
        viewModel.onIntent(TaskFormIntent.DismissTask)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { dismissTaskUseCase(any()) }
    }

    // ── UndoStatusChange ───────────────────────────────────────────────────────

    @Test
    fun `UndoStatusChange should call reactivateTaskUseCase after complete`() = runTest {
        coEvery { getTaskByIdUseCase("task-1") } returns makeTask("task-1")
        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.CompleteTask)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.UndoStatusChange)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { reactivateTaskUseCase("task-1") }
    }

    @Test
    fun `UndoStatusChange should restore currentTaskStatus to ACTIVE`() = runTest {
        coEvery { getTaskByIdUseCase("task-1") } returns makeTask("task-1")
        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.CompleteTask)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.UndoStatusChange)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.first().currentTaskStatus).isEqualTo(TaskStatus.ACTIVE)
    }

    @Test
    fun `UndoStatusChange should clear pendingTaskUndo`() = runTest {
        coEvery { getTaskByIdUseCase("task-1") } returns makeTask("task-1")
        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.CompleteTask)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.UndoStatusChange)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.first().pendingTaskUndo).isNull()
    }

    @Test
    fun `UndoStatusChange should do nothing when no pending undo`() = runTest {
        viewModel.onIntent(TaskFormIntent.UndoStatusChange)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { reactivateTaskUseCase(any()) }
    }

    // ── ClearUndo ─────────────────────────────────────────────────────────────

    @Test
    fun `ClearUndo should clear pendingTaskUndo without calling reactivate`() = runTest {
        coEvery { getTaskByIdUseCase("task-1") } returns makeTask("task-1")
        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.CompleteTask)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.ClearUndo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.first().pendingTaskUndo).isNull()
        coVerify(exactly = 0) { reactivateTaskUseCase(any()) }
        // Status remains COMPLETED
        assertThat(viewModel.uiState.first().currentTaskStatus).isEqualTo(TaskStatus.COMPLETED)
    }

    // ── RequestDeleteTask / ConfirmDeleteTask / CancelDeleteTask ───────────────

    @Test
    fun `RequestDeleteTask should set showDeleteTaskConfirmation to true`() = runTest {
        coEvery { getTaskByIdUseCase("task-1") } returns makeTask("task-1")
        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.RequestDeleteTask)

        assertThat(viewModel.uiState.first().showDeleteTaskConfirmation).isTrue()
    }

    @Test
    fun `CancelDeleteTask should set showDeleteTaskConfirmation to false`() = runTest {
        coEvery { getTaskByIdUseCase("task-1") } returns makeTask("task-1")
        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.RequestDeleteTask)
        viewModel.onIntent(TaskFormIntent.CancelDeleteTask)

        assertThat(viewModel.uiState.first().showDeleteTaskConfirmation).isFalse()
    }

    @Test
    fun `ConfirmDeleteTask should call deleteTaskUseCase`() = runTest {
        coEvery { getTaskByIdUseCase("task-1") } returns makeTask("task-1")
        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.RequestDeleteTask)
        viewModel.onIntent(TaskFormIntent.ConfirmDeleteTask)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { deleteTaskUseCase("task-1") }
    }

    @Test
    fun `ConfirmDeleteTask should set isTaskDeleted to true`() = runTest {
        coEvery { getTaskByIdUseCase("task-1") } returns makeTask("task-1")
        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.RequestDeleteTask)
        viewModel.onIntent(TaskFormIntent.ConfirmDeleteTask)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.first().isTaskDeleted).isTrue()
        assertThat(viewModel.uiState.first().showDeleteTaskConfirmation).isFalse()
    }

    @Test
    fun `ConfirmDeleteTask should do nothing when no task loaded`() = runTest {
        viewModel.onIntent(TaskFormIntent.ConfirmDeleteTask)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { deleteTaskUseCase(any()) }
    }

    // ── ToggleNotification ─────────────────────────────────────────────────────

    @Test
    fun `ToggleNotification should call toggleTaskNotificationUseCase`() = runTest {
        coEvery { getTaskByIdUseCase("task-1") } returns makeTask("task-1")
        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.ToggleNotification)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { toggleTaskNotificationUseCase("task-1") }
    }

    @Test
    fun `ToggleNotification should do nothing when no task loaded`() = runTest {
        viewModel.onIntent(TaskFormIntent.ToggleNotification)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { toggleTaskNotificationUseCase(any()) }
    }

    // ── loadTask should set currentTaskStatus from task ───────────────────────

    @Test
    fun `loadTask should set currentTaskStatus from loaded task`() = runTest {
        coEvery { getTaskByIdUseCase("task-1") } returns makeTask("task-1", TaskStatus.COMPLETED)

        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.first().currentTaskStatus).isEqualTo(TaskStatus.COMPLETED)
    }

    @Test
    fun `loadTask should set currentTaskStatus ACTIVE for active task`() = runTest {
        coEvery { getTaskByIdUseCase("task-1") } returns makeTask("task-1", TaskStatus.ACTIVE)

        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.first().currentTaskStatus).isEqualTo(TaskStatus.ACTIVE)
    }

    // ── Save should preserve task status ──────────────────────────────────────

    @Test
    fun `Save should preserve currentTaskStatus when updating task`() = runTest {
        coEvery { getTaskByIdUseCase("task-1") } returns makeTask("task-1", TaskStatus.COMPLETED)
        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.UpdateTitle("Updated Title"))
        viewModel.onIntent(TaskFormIntent.Save)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { updateTaskUseCase(match { it.status == TaskStatus.COMPLETED }) }
    }
}
