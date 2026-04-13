package br.com.anotacoes.presentation

import br.com.anotacoes.domain.model.Recurrence
import br.com.anotacoes.domain.model.Reminder
import br.com.anotacoes.domain.model.ReminderStatus
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskTime
import br.com.anotacoes.domain.usecase.CompleteReminderUseCase
import br.com.anotacoes.domain.usecase.CompleteTaskUseCase
import br.com.anotacoes.domain.usecase.DeleteReminderUseCase
import br.com.anotacoes.domain.usecase.DeleteTaskUseCase
import br.com.anotacoes.domain.usecase.DismissReminderUseCase
import br.com.anotacoes.domain.usecase.DismissTaskUseCase
import br.com.anotacoes.domain.usecase.GetAllTasksUseCase
import br.com.anotacoes.domain.usecase.GetRemindersForDateUseCase
import br.com.anotacoes.domain.usecase.GetTasksForDateUseCase
import br.com.anotacoes.domain.usecase.ReactivateReminderUseCase
import br.com.anotacoes.domain.usecase.ReactivateTaskUseCase
import br.com.anotacoes.domain.usecase.ToggleTaskNotificationUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelReminderTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getTasksForDateUseCase: GetTasksForDateUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase
    private lateinit var dismissTaskUseCase: DismissTaskUseCase
    private lateinit var completeTaskUseCase: CompleteTaskUseCase
    private lateinit var getAllTasksUseCase: GetAllTasksUseCase
    private lateinit var toggleTaskNotificationUseCase: ToggleTaskNotificationUseCase
    private lateinit var getRemindersForDateUseCase: GetRemindersForDateUseCase
    private lateinit var dismissReminderUseCase: DismissReminderUseCase
    private lateinit var completeReminderUseCase: CompleteReminderUseCase
    private lateinit var deleteReminderUseCase: DeleteReminderUseCase
    private lateinit var reactivateReminderUseCase: ReactivateReminderUseCase
    private lateinit var reactivateTaskUseCase: ReactivateTaskUseCase

    private val today: LocalDate = LocalDate.now()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getTasksForDateUseCase = mockk()
        deleteTaskUseCase = mockk()
        dismissTaskUseCase = mockk()
        completeTaskUseCase = mockk()
        getAllTasksUseCase = mockk()
        toggleTaskNotificationUseCase = mockk(relaxed = true)
        getRemindersForDateUseCase = mockk()
        dismissReminderUseCase = mockk(relaxed = true)
        completeReminderUseCase = mockk(relaxed = true)
        deleteReminderUseCase = mockk(relaxed = true)
        reactivateReminderUseCase = mockk(relaxed = true)
        reactivateTaskUseCase = mockk(relaxed = true)

        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())
        every { getAllTasksUseCase() } returns flowOf(emptyList())
        every { getRemindersForDateUseCase(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TaskListViewModel {
        return TaskListViewModel(
            getTasksForDateUseCase,
            deleteTaskUseCase,
            dismissTaskUseCase,
            completeTaskUseCase,
            getAllTasksUseCase,
            toggleTaskNotificationUseCase,
            getRemindersForDateUseCase,
            dismissReminderUseCase,
            completeReminderUseCase,
            deleteReminderUseCase,
            reactivateReminderUseCase,
            reactivateTaskUseCase
        )
    }

    @Test
    fun `init should load reminders for today`() = runTest {
        val reminder = makeReminder("r1", "Consulta")
        every { getRemindersForDateUseCase(any()) } returns flowOf(listOf(reminder))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.reminders).hasSize(1)
        assertThat(state.reminders[0].taskId).isEqualTo("r1")
        assertThat(state.reminders[0].id).isEqualTo("r1_adv_3")
    }

    @Test
    fun `init should have empty reminders when none exist`() = runTest {
        every { getRemindersForDateUseCase(any()) } returns flowOf(emptyList())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.reminders).isEmpty()
    }

    @Test
    fun `SelectDate should also reload reminders for new date`() = runTest {
        val tomorrow = today.plusDays(1)
        val reminder = makeReminder("r2", "Viagem")
        every { getRemindersForDateUseCase(today) } returns flowOf(emptyList())
        every { getRemindersForDateUseCase(tomorrow) } returns flowOf(listOf(reminder))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.SelectDate(tomorrow))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.reminders).hasSize(1)
    }

    @Test
    fun `DismissReminder intent should call dismiss use case`() = runTest {
        every { getRemindersForDateUseCase(any()) } returns flowOf(emptyList())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.DismissReminder(reminderId = "r1_adv_3", taskId = "r1"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { dismissReminderUseCase("r1_adv_3", "r1") }
    }

    @Test
    fun `CompleteReminder intent should call complete use case`() = runTest {
        every { getRemindersForDateUseCase(any()) } returns flowOf(emptyList())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.CompleteReminder(reminderId = "r1_adv_3", taskId = "r1"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { completeReminderUseCase("r1_adv_3", "r1") }
    }

    @Test
    fun `DeleteReminder intent should call delete use case`() = runTest {
        every { getRemindersForDateUseCase(any()) } returns flowOf(emptyList())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.DeleteReminder(reminderId = "r1_adv_3", taskId = "r1"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { deleteReminderUseCase("r1_adv_3", "r1") }
    }

    @Test
    fun `SelectReminderDetail intent should set selectedReminder in state`() = runTest {
        val reminder = makeReminder("r1", "Consulta")
        every { getRemindersForDateUseCase(any()) } returns flowOf(listOf(reminder))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.SelectReminderDetail(reminder))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.selectedReminder).isEqualTo(reminder)
    }

    @Test
    fun `DismissReminderDetail intent should clear selectedReminder`() = runTest {
        val reminder = makeReminder("r1", "Consulta")
        every { getRemindersForDateUseCase(any()) } returns flowOf(listOf(reminder))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.SelectReminderDetail(reminder))
        viewModel.onIntent(TaskListIntent.DismissReminderDetail)

        val state = viewModel.uiState.first()
        assertThat(state.selectedReminder).isNull()
    }

    @Test
    fun `ReactivateReminder intent should call reactivate use case`() = runTest {
        every { getRemindersForDateUseCase(any()) } returns flowOf(emptyList())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.ReactivateReminder(reminderId = "r1_adv_3", taskId = "r1"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { reactivateReminderUseCase("r1_adv_3", "r1") }
    }

    @Test
    fun `RequestDeleteReminder intent should set reminderPendingDelete in state`() = runTest {
        val reminder = makeReminder("r1", "Consulta")
        every { getRemindersForDateUseCase(any()) } returns flowOf(listOf(reminder))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.RequestDeleteReminder(reminder))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.reminderPendingDelete).isEqualTo(reminder)
    }

    @Test
    fun `CancelDeleteReminder intent should clear reminderPendingDelete`() = runTest {
        val reminder = makeReminder("r1", "Consulta")
        every { getRemindersForDateUseCase(any()) } returns flowOf(listOf(reminder))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.RequestDeleteReminder(reminder))
        viewModel.onIntent(TaskListIntent.CancelDeleteReminder)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.reminderPendingDelete).isNull()
    }

    @Test
    fun `ConfirmDeleteReminder intent should call delete use case and clear pending`() = runTest {
        val reminder = makeReminder("r1", "Consulta")
        every { getRemindersForDateUseCase(any()) } returns flowOf(listOf(reminder))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.RequestDeleteReminder(reminder))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onIntent(TaskListIntent.ConfirmDeleteReminder)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { deleteReminderUseCase("r1_adv_3", "r1") }
        val state = viewModel.uiState.first()
        assertThat(state.reminderPendingDelete).isNull()
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun makeReminder(id: String, title: String): Reminder {
        val now = LocalDate.now()
        val task = Task(
            id = id,
            title = title,
            date = now.plusDays(3),
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.Single(now.plusDays(3)),
            advanceReminderDays = listOf(3)
        )
        return Reminder(
            id = "${id}_adv_3",
            taskId = id,
            taskTitle = title,
            task = task,
            daysBeforeTask = 3,
            reminderDate = now,
            status = ReminderStatus.ACTIVE
        )
    }
}
