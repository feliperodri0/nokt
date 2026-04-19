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
import br.com.anotacoes.domain.usecase.GetCalendarExpandOnboardingSeenUseCase
import br.com.anotacoes.domain.usecase.GetRemindersForDateUseCase
import br.com.anotacoes.domain.usecase.GetTasksForDateUseCase
import br.com.anotacoes.domain.usecase.MarkCalendarExpandOnboardingSeenUseCase
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
    private lateinit var getCalendarExpandOnboardingSeenUseCase: GetCalendarExpandOnboardingSeenUseCase
    private lateinit var markCalendarExpandOnboardingSeenUseCase: MarkCalendarExpandOnboardingSeenUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getTasksForDateUseCase = mockk()
        deleteTaskUseCase = mockk()
        dismissTaskUseCase = mockk()
        completeTaskUseCase = mockk()
        getAllTasksUseCase = mockk()
        every { getAllTasksUseCase() } returns flowOf(emptyList())
        toggleTaskNotificationUseCase = mockk(relaxed = true)
        getRemindersForDateUseCase = mockk()
        every { getRemindersForDateUseCase(any()) } returns flowOf(emptyList())
        dismissReminderUseCase = mockk(relaxed = true)
        completeReminderUseCase = mockk(relaxed = true)
        deleteReminderUseCase = mockk(relaxed = true)
        reactivateReminderUseCase = mockk(relaxed = true)
        reactivateTaskUseCase = mockk(relaxed = true)
        getCalendarExpandOnboardingSeenUseCase = mockk()
        every { getCalendarExpandOnboardingSeenUseCase() } returns flowOf(true)
        markCalendarExpandOnboardingSeenUseCase = mockk(relaxed = true)
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
            reactivateTaskUseCase,
            getCalendarExpandOnboardingSeenUseCase,
            markCalendarExpandOnboardingSeenUseCase
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
        // datesWithTasks covers prev+current+next months; April 15 must be present
        assertThat(state.datesWithTasks).contains(taskDate)
    }

    @Test
    fun `LoadMonthIndicators should NOT include SINGLE task two months away`() = runTest {
        val yearMonth = YearMonth.of(2026, 4)
        // June is two months ahead — outside the 3-month window (March, April, May)
        val taskDate = LocalDate.of(2026, 6, 10)
        val task = makeTaskOnDate("t2", "Task", taskDate, Recurrence.Single(taskDate))

        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())
        every { getAllTasksUseCase() } returns flowOf(listOf(task))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.LoadMonthIndicators(yearMonth))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.datesWithTasks).doesNotContain(taskDate)
    }

    @Test
    fun `LoadMonthIndicators should expand DAILY task to prev+current+next months`() = runTest {
        val yearMonth = YearMonth.of(2026, 4)
        val task = makeTaskOnDate("t3", "Daily", LocalDate.of(2026, 1, 1), Recurrence.daily())

        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())
        every { getAllTasksUseCase() } returns flowOf(listOf(task))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.LoadMonthIndicators(yearMonth))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        // March=31, April=30, May=31 → 92 days total
        assertThat(state.datesWithTasks).hasSize(92)
        assertThat(state.datesWithTasks).contains(LocalDate.of(2026, 4, 1))
        assertThat(state.datesWithTasks).contains(LocalDate.of(2026, 4, 30))
    }

    @Test
    fun `LoadMonthIndicators should expand CUSTOM_WEEKLY task to matching weekdays across 3 months`() = runTest {
        val yearMonth = YearMonth.of(2026, 4)
        // Mondays in March 2026: 2, 9, 16, 23, 30
        // Mondays in April 2026: 6, 13, 20, 27
        // Mondays in May 2026: 4, 11, 18, 25
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
        // April Mondays must be present
        assertThat(state.datesWithTasks).containsAtLeast(
            LocalDate.of(2026, 4, 6),
            LocalDate.of(2026, 4, 13),
            LocalDate.of(2026, 4, 20),
            LocalDate.of(2026, 4, 27)
        )
        // March and May Mondays are also present (3-month window)
        assertThat(state.datesWithTasks).contains(LocalDate.of(2026, 3, 2))
        assertThat(state.datesWithTasks).contains(LocalDate.of(2026, 5, 4))
    }

    @Test
    fun `LoadMonthIndicators should cancel previous month job when called again`() = runTest {
        // Two months apart so the 3-month windows do NOT overlap
        val farPastMonth = YearMonth.of(2020, 1)
        val farFutureMonth = YearMonth.of(2030, 6)

        val pastDate = farPastMonth.atDay(15)
        val futureDate = farFutureMonth.atDay(15)

        val pastTask = makeTaskOnDate("t5", "Past", pastDate, Recurrence.Single(pastDate))
        val futureTask = makeTaskOnDate("t6", "Future", futureDate, Recurrence.Single(futureDate))

        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())
        every { getAllTasksUseCase() } returns flowOf(listOf(pastTask, futureTask))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.LoadMonthIndicators(farPastMonth))
        viewModel.onIntent(TaskListIntent.LoadMonthIndicators(farFutureMonth))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        // Last call was farFutureMonth; its 3-month window should include futureDate
        assertThat(state.datesWithTasks).contains(futureDate)
        // pastDate is far outside the future window → should not be present
        assertThat(state.datesWithTasks).doesNotContain(pastDate)
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
