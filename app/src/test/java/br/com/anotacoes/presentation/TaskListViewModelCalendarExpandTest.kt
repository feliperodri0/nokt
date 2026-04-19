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

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelCalendarExpandTest {

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
        every { getTasksForDateUseCase(any()) } returns flowOf(emptyList())
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

    // --- Onboarding flag ---

    @Test
    fun `init with onboarding not seen should set showExpandOnboarding to true`() = runTest {
        every { getCalendarExpandOnboardingSeenUseCase() } returns flowOf(false)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.showExpandOnboarding).isTrue()
    }

    @Test
    fun `init with onboarding already seen should keep showExpandOnboarding false`() = runTest {
        every { getCalendarExpandOnboardingSeenUseCase() } returns flowOf(true)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.showExpandOnboarding).isFalse()
    }

    // --- ToggleCalendarExpanded intent ---

    @Test
    fun `ToggleCalendarExpanded(true) should set isCalendarExpanded to true`() = runTest {
        every { getCalendarExpandOnboardingSeenUseCase() } returns flowOf(true)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.ToggleCalendarExpanded(true))

        val state = viewModel.uiState.first()
        assertThat(state.isCalendarExpanded).isTrue()
    }

    @Test
    fun `ToggleCalendarExpanded(false) should set isCalendarExpanded to false`() = runTest {
        every { getCalendarExpandOnboardingSeenUseCase() } returns flowOf(true)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.ToggleCalendarExpanded(true))
        viewModel.onIntent(TaskListIntent.ToggleCalendarExpanded(false))

        val state = viewModel.uiState.first()
        assertThat(state.isCalendarExpanded).isFalse()
    }

    // --- DismissExpandOnboarding intent ---

    @Test
    fun `DismissExpandOnboarding should call markCalendarExpandOnboardingSeenUseCase`() = runTest {
        every { getCalendarExpandOnboardingSeenUseCase() } returns flowOf(false)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.DismissExpandOnboarding)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { markCalendarExpandOnboardingSeenUseCase() }
    }

    @Test
    fun `DismissExpandOnboarding should set showExpandOnboarding to false`() = runTest {
        every { getCalendarExpandOnboardingSeenUseCase() } returns flowOf(false)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.DismissExpandOnboarding)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.showExpandOnboarding).isFalse()
    }

    // --- tasksByDate after LoadMonthIndicators ---

    @Test
    fun `LoadMonthIndicators should populate tasksByDate with single task on correct date`() = runTest {
        val yearMonth = YearMonth.of(2026, 4)
        val taskDate = LocalDate.of(2026, 4, 15)
        val task = makeTaskOnDate("t1", "Meeting", taskDate, Recurrence.Single(taskDate))

        every { getCalendarExpandOnboardingSeenUseCase() } returns flowOf(true)
        every { getAllTasksUseCase() } returns flowOf(listOf(task))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.LoadMonthIndicators(yearMonth))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.tasksByDate[taskDate]).isNotNull()
        assertThat(state.tasksByDate[taskDate]).hasSize(1)
        assertThat(state.tasksByDate[taskDate]!!.first().id).isEqualTo("t1")
    }

    @Test
    fun `LoadMonthIndicators tasksByDate should NOT include advance reminder dates`() = runTest {
        val yearMonth = YearMonth.of(2026, 4)
        val taskDate = LocalDate.of(2026, 4, 15)
        // task with advance reminder 2 days before → April 13 should not appear in tasksByDate
        val task = Task(
            id = "t2",
            title = "Task with reminder",
            date = taskDate,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.Single(taskDate),
            advanceReminderDays = listOf(2)
        )

        every { getCalendarExpandOnboardingSeenUseCase() } returns flowOf(true)
        every { getAllTasksUseCase() } returns flowOf(listOf(task))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.LoadMonthIndicators(yearMonth))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        val reminderDate = LocalDate.of(2026, 4, 13)
        // datesWithTasks should include the reminder date (indicator only)
        assertThat(state.datesWithTasks).contains(reminderDate)
        // tasksByDate must NOT include the reminder date
        assertThat(state.tasksByDate[reminderDate]).isNull()
        // tasksByDate must include the actual task date
        assertThat(state.tasksByDate[taskDate]).hasSize(1)
    }

    @Test
    fun `LoadMonthIndicators tasksByDate should include all days for daily task`() = runTest {
        val yearMonth = YearMonth.of(2026, 4)
        val task = makeTaskOnDate("daily", "Daily Task", LocalDate.of(2026, 1, 1), Recurrence.daily())

        every { getCalendarExpandOnboardingSeenUseCase() } returns flowOf(true)
        every { getAllTasksUseCase() } returns flowOf(listOf(task))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskListIntent.LoadMonthIndicators(yearMonth))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        // April has 30 days; all should map the daily task
        assertThat(state.tasksByDate).hasSize(30)
        assertThat(state.tasksByDate[LocalDate.of(2026, 4, 1)]).hasSize(1)
        assertThat(state.tasksByDate[LocalDate.of(2026, 4, 30)]).hasSize(1)
    }

    // ---- helpers ----

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
