package br.com.anotacoes.presentation

import br.com.anotacoes.domain.model.DayOfWeek
import br.com.anotacoes.domain.model.Recurrence
import br.com.anotacoes.domain.model.RecurrenceType
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
import io.mockk.coEvery
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
import java.time.YearMonth

/**
 * TDD tests for TaskListViewModel with MVI intents.
 * ViewModel loads today's tasks automatically on init.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {

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
        every { getRemindersForDateUseCase(any()) } returns flowOf(emptyList())
        dismissReminderUseCase = mockk(relaxed = true)
        completeReminderUseCase = mockk(relaxed = true)
        deleteReminderUseCase = mockk(relaxed = true)
        reactivateReminderUseCase = mockk(relaxed = true)
        reactivateTaskUseCase = mockk(relaxed = true)
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

    // --- Init auto-loads today ---

    @Test
    fun `init should auto-load tasks for today`() = runTest {
        val today = LocalDate.now()
        val tasks = listOf(makeTask("1", "Meeting", TaskTime.interval(9, 0, 10, 0)))
        every { getTasksForDateUseCase(today) } returns flowOf(tasks)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.tasks).hasSize(1)
        assertThat(state.selectedDate).isEqualTo(today)
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `init should set empty list when no tasks today`() = runTest {
        every { getTasksForDateUseCase(LocalDate.now()) } returns flowOf(emptyList())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.tasks).isEmpty()
        assertThat(state.isLoading).isFalse()
    }

    // --- SelectDate intent ---

    @Test
    fun `SelectDate should update selectedDate and load tasks`() = runTest {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        every { getTasksForDateUseCase(today) } returns flowOf(emptyList())
        every { getTasksForDateUseCase(tomorrow) } returns flowOf(
            listOf(makeTask("2", "Gym", TaskTime.interval(18, 0, 19, 0)))
        )

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.SelectDate(tomorrow))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.selectedDate).isEqualTo(tomorrow)
        assertThat(state.tasks).hasSize(1)
    }

    @Test
    fun `SelectDate should cancel previous load when selecting new date`() = runTest {
        val today = LocalDate.now()
        val date1 = today.plusDays(1)
        val date2 = today.plusDays(2)
        every { getTasksForDateUseCase(today) } returns flowOf(emptyList())
        every { getTasksForDateUseCase(date1) } returns flowOf(emptyList())
        every { getTasksForDateUseCase(date2) } returns flowOf(
            listOf(makeTask("3", "Task", TaskTime.allDay()))
        )

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.SelectDate(date1))
        viewModel.onIntent(TaskListIntent.SelectDate(date2))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.selectedDate).isEqualTo(date2)
        assertThat(state.tasks).hasSize(1)
    }

    // --- DeleteTask intent ---

    @Test
    fun `DeleteTask should call delete use case`() = runTest {
        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())
        coEvery { deleteTaskUseCase(any()) } returns Unit

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.DeleteTask("task-1"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { deleteTaskUseCase("task-1") }
    }

    // --- DismissTask intent ---

    @Test
    fun `DismissTask should call dismiss use case`() = runTest {
        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())
        coEvery { dismissTaskUseCase(any()) } returns Unit

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.DismissTask("task-2"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { dismissTaskUseCase("task-2") }
    }

    // --- NavigateToCreateTask intent ---

    @Test
    fun `NavigateToCreateTask should set navigateToForm to true`() = runTest {
        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.NavigateToCreateTask)

        val state = viewModel.uiState.first()
        assertThat(state.navigateToForm).isTrue()
    }

    @Test
    fun `NavigationHandled should reset navigateToForm to false`() = runTest {
        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.NavigateToCreateTask)
        viewModel.onIntent(TaskListIntent.NavigationHandled)

        val state = viewModel.uiState.first()
        assertThat(state.navigateToForm).isFalse()
    }

    // --- Error handling ---

    @Test
    fun `should set error state when load fails`() = runTest {
        every { getTasksForDateUseCase(any()) } throws RuntimeException("DB error")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.error).isEqualTo("DB error")
        assertThat(state.isLoading).isFalse()
    }

    // --- ReloadTasks intent ---

    @Test
    fun `ReloadTasks should reload tasks for current selected date`() = runTest {
        val today = LocalDate.now()
        every { getTasksForDateUseCase(today) } returns flowOf(emptyList()) andThen flowOf(
            listOf(makeTask("new", "New Task", TaskTime.allDay()))
        )

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.ReloadTasks)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.tasks).hasSize(1)
    }

    // --- LoadMonthIndicators intent ---

    @Test
    fun `LoadMonthIndicators should include date of SINGLE task in the requested month`() = runTest {
        val yearMonth = YearMonth.of(2026, 4)
        val taskDate = LocalDate.of(2026, 4, 15)
        val task = makeTaskOnDate("t1", "Task", taskDate, Recurrence.Single(taskDate))

        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())
        every { getAllTasksUseCase() } returns flowOf(listOf(task))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.LoadMonthIndicators(yearMonth))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.datesWithTasks).contains(taskDate)
        assertThat(state.datesWithTasks).hasSize(1)
    }

    @Test
    fun `LoadMonthIndicators should NOT include SINGLE task from different month`() = runTest {
        val yearMonth = YearMonth.of(2026, 4)
        val taskDate = LocalDate.of(2026, 5, 10) // May, not April
        val task = makeTaskOnDate("t2", "Task", taskDate, Recurrence.Single(taskDate))

        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())
        every { getAllTasksUseCase() } returns flowOf(listOf(task))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.LoadMonthIndicators(yearMonth))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.datesWithTasks).isEmpty()
    }

    @Test
    fun `LoadMonthIndicators should expand DAILY task to all days in month`() = runTest {
        val yearMonth = YearMonth.of(2026, 4)
        val task = makeTaskOnDate("t3", "Daily", LocalDate.of(2026, 1, 1), Recurrence.daily())

        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())
        every { getAllTasksUseCase() } returns flowOf(listOf(task))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.LoadMonthIndicators(yearMonth))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.datesWithTasks).hasSize(30) // April has 30 days
        assertThat(state.datesWithTasks).contains(LocalDate.of(2026, 4, 1))
        assertThat(state.datesWithTasks).contains(LocalDate.of(2026, 4, 30))
    }

    @Test
    fun `LoadMonthIndicators should expand CUSTOM_WEEKLY task to matching weekdays`() = runTest {
        val yearMonth = YearMonth.of(2026, 4)
        // April 2026: Mondays are 6, 13, 20, 27
        val task = makeTaskOnDate(
            "t4", "Weekly Mon",
            LocalDate.of(2026, 4, 6),
            Recurrence.customWeekly(listOf(DayOfWeek.MONDAY))
        )

        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())
        every { getAllTasksUseCase() } returns flowOf(listOf(task))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.LoadMonthIndicators(yearMonth))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.datesWithTasks).containsExactly(
            LocalDate.of(2026, 4, 6),
            LocalDate.of(2026, 4, 13),
            LocalDate.of(2026, 4, 20),
            LocalDate.of(2026, 4, 27)
        )
    }

    @Test
    fun `LoadMonthIndicators should cancel previous month job when called again`() = runTest {
        // Use relative dates to avoid Recurrence.single() past-date validation
        val today = LocalDate.now()
        val thisMonth = YearMonth.now()
        val nextMonth = thisMonth.plusMonths(1)

        // A future date in this month and one in next month
        val thisMonthDate = if (today.dayOfMonth <= 25) today.plusDays(3) else thisMonth.atEndOfMonth()
        val nextMonthDate = nextMonth.atDay(5)

        val thisMonthTask = makeTaskOnDate("t5", "Current", thisMonthDate, Recurrence.Single(thisMonthDate))
        val nextMonthTask = makeTaskOnDate("t6", "Next", nextMonthDate, Recurrence.Single(nextMonthDate))

        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())
        every { getAllTasksUseCase() } returns flowOf(listOf(thisMonthTask, nextMonthTask))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.LoadMonthIndicators(thisMonth))
        viewModel.onIntent(TaskListIntent.LoadMonthIndicators(nextMonth))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        // Should only contain next month dates since that was the last request
        assertThat(state.datesWithTasks).contains(nextMonthDate)
        assertThat(state.datesWithTasks).doesNotContain(thisMonthDate)
    }

    // --- CompleteTask intent ---

    @Test
    fun `CompleteTask should call complete use case`() = runTest {
        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())
        coEvery { completeTaskUseCase(any()) } returns Unit

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.CompleteTask("task-complete"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { completeTaskUseCase("task-complete") }
    }

    // --- ReactivateTask intent ---

    @Test
    fun `ReactivateTask should call reactivate use case`() = runTest {
        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.ReactivateTask("task-reactivate"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { reactivateTaskUseCase("task-reactivate") }
    }

    // --- RequestDeleteTask / ConfirmDeleteTask / CancelDeleteTask ---

    @Test
    fun `RequestDeleteTask should set taskPendingDelete in state`() = runTest {
        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())
        val task = makeTask("task-del", "Delete me", TaskTime.allDay())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.RequestDeleteTask(task))

        val state = viewModel.uiState.first()
        assertThat(state.taskPendingDelete).isEqualTo(task)
    }

    @Test
    fun `CancelDeleteTask should clear taskPendingDelete`() = runTest {
        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())
        val task = makeTask("task-del", "Delete me", TaskTime.allDay())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.RequestDeleteTask(task))
        viewModel.onIntent(TaskListIntent.CancelDeleteTask)

        val state = viewModel.uiState.first()
        assertThat(state.taskPendingDelete).isNull()
    }

    @Test
    fun `ConfirmDeleteTask should call delete use case and clear taskPendingDelete`() = runTest {
        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())
        coEvery { deleteTaskUseCase(any()) } returns Unit
        val task = makeTask("task-del", "Delete me", TaskTime.allDay())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.RequestDeleteTask(task))
        viewModel.onIntent(TaskListIntent.ConfirmDeleteTask)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { deleteTaskUseCase("task-del") }
        val state = viewModel.uiState.first()
        assertThat(state.taskPendingDelete).isNull()
    }

    @Test
    fun `ConfirmDeleteTask when no pending task should not call delete use case`() = runTest {
        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.ConfirmDeleteTask)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { deleteTaskUseCase(any()) }
    }

    private fun makeTask(id: String, title: String, taskTime: TaskTime): Task {
        val today = LocalDate.now()
        return Task(
            id = id,
            title = title,
            date = today,
            taskTime = taskTime,
            recurrence = Recurrence.single(today)
        )
    }

    private fun makeTaskOnDate(id: String, title: String, date: LocalDate, recurrence: Recurrence): Task {
        return Task(
            id = id,
            title = title,
            date = date,
            taskTime = TaskTime.allDay(),
            recurrence = recurrence
        )
    }
}
