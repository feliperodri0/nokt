package br.com.anotacoes.presentation

import android.content.Context
import br.com.anotacoes.domain.model.ChecklistItem
import br.com.anotacoes.domain.model.DayOfWeek
import br.com.anotacoes.domain.model.Recurrence
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
import io.mockk.slot
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

/**
 * TDD tests for TaskFormViewModel with MVI intents (Requirement 4 - Task creation/editing).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskFormViewModelTest {

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
        createTaskUseCase = mockk()
        updateTaskUseCase = mockk()
        scheduleTaskAlarmUseCase = mockk()
        getTaskByIdUseCase = mockk()
        completeTaskUseCase = mockk(relaxed = true)
        dismissTaskUseCase = mockk(relaxed = true)
        deleteTaskUseCase = mockk(relaxed = true)
        toggleTaskNotificationUseCase = mockk(relaxed = true)
        reactivateTaskUseCase = mockk(relaxed = true)
        context = mockk(relaxed = true)
        viewModel = TaskFormViewModel(
            createTaskUseCase, updateTaskUseCase, scheduleTaskAlarmUseCase, getTaskByIdUseCase,
            context, completeTaskUseCase, dismissTaskUseCase, deleteTaskUseCase,
            toggleTaskNotificationUseCase, reactivateTaskUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Initial state ---

    @Test
    fun `initial state should have empty title and today as date`() = runTest {
        val state = viewModel.uiState.first()

        assertThat(state.title).isEmpty()
        assertThat(state.date).isEqualTo(LocalDate.now())
        assertThat(state.isAllDay).isTrue()
        assertThat(state.recurrenceType).isEqualTo(RecurrenceTypeOption.SINGLE)
        assertThat(state.description).isEmpty()
        assertThat(state.checklistItems).isEmpty()
        assertThat(state.isSaved).isFalse()
        assertThat(state.validationError).isNull()
    }

    // --- UpdateTitle intent ---

    @Test
    fun `UpdateTitle should update title`() = runTest {
        viewModel.onIntent(TaskFormIntent.UpdateTitle("Meeting"))

        val state = viewModel.uiState.first()
        assertThat(state.title).isEqualTo("Meeting")
    }

    @Test
    fun `UpdateTitle should clear validation error`() = runTest {
        // Trigger validation error first
        viewModel.onIntent(TaskFormIntent.Save)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.UpdateTitle("Valid"))

        val state = viewModel.uiState.first()
        assertThat(state.validationError).isNull()
    }

    // --- UpdateDate intent ---

    @Test
    fun `UpdateDate should update date`() = runTest {
        val newDate = LocalDate.now().plusDays(3)
        viewModel.onIntent(TaskFormIntent.UpdateDate(newDate))

        val state = viewModel.uiState.first()
        assertThat(state.date).isEqualTo(newDate)
    }

    // --- ToggleAllDay intent ---

    @Test
    fun `ToggleAllDay should switch from all-day to interval`() = runTest {
        viewModel.onIntent(TaskFormIntent.ToggleAllDay(false))

        val state = viewModel.uiState.first()
        assertThat(state.isAllDay).isFalse()
    }

    @Test
    fun `ToggleAllDay should switch from interval to all-day`() = runTest {
        viewModel.onIntent(TaskFormIntent.ToggleAllDay(false))
        viewModel.onIntent(TaskFormIntent.ToggleAllDay(true))

        val state = viewModel.uiState.first()
        assertThat(state.isAllDay).isTrue()
    }

    // --- UpdateStartTime / UpdateEndTime ---

    @Test
    fun `UpdateStartTime should update start hour and minute`() = runTest {
        viewModel.onIntent(TaskFormIntent.ToggleAllDay(false))
        viewModel.onIntent(TaskFormIntent.UpdateStartTime(9, 30))

        val state = viewModel.uiState.first()
        assertThat(state.startHour).isEqualTo(9)
        assertThat(state.startMinute).isEqualTo(30)
    }

    @Test
    fun `UpdateEndTime should update end hour and minute`() = runTest {
        viewModel.onIntent(TaskFormIntent.ToggleAllDay(false))
        viewModel.onIntent(TaskFormIntent.UpdateEndTime(17, 0))

        val state = viewModel.uiState.first()
        assertThat(state.endHour).isEqualTo(17)
        assertThat(state.endMinute).isEqualTo(0)
    }

    // --- UpdateRecurrenceType ---

    @Test
    fun `UpdateRecurrenceType to DAILY should update state`() = runTest {
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.DAILY))

        val state = viewModel.uiState.first()
        assertThat(state.recurrenceType).isEqualTo(RecurrenceTypeOption.DAILY)
    }

    @Test
    fun `UpdateRecurrenceType to CUSTOM_WEEKLY should update state`() = runTest {
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.CUSTOM_WEEKLY))

        val state = viewModel.uiState.first()
        assertThat(state.recurrenceType).isEqualTo(RecurrenceTypeOption.CUSTOM_WEEKLY)
    }

    // --- ToggleDayOfWeek ---

    @Test
    fun `ToggleDayOfWeek should add day when not selected`() = runTest {
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.CUSTOM_WEEKLY))
        viewModel.onIntent(TaskFormIntent.ToggleDayOfWeek(DayOfWeek.MONDAY))

        val state = viewModel.uiState.first()
        assertThat(state.selectedDays).contains(DayOfWeek.MONDAY)
    }

    @Test
    fun `ToggleDayOfWeek should remove day when already selected`() = runTest {
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.CUSTOM_WEEKLY))
        viewModel.onIntent(TaskFormIntent.ToggleDayOfWeek(DayOfWeek.MONDAY))
        viewModel.onIntent(TaskFormIntent.ToggleDayOfWeek(DayOfWeek.MONDAY))

        val state = viewModel.uiState.first()
        assertThat(state.selectedDays).doesNotContain(DayOfWeek.MONDAY)
    }

    @Test
    fun `ToggleDayOfWeek should support multiple days`() = runTest {
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.CUSTOM_WEEKLY))
        viewModel.onIntent(TaskFormIntent.ToggleDayOfWeek(DayOfWeek.MONDAY))
        viewModel.onIntent(TaskFormIntent.ToggleDayOfWeek(DayOfWeek.WEDNESDAY))
        viewModel.onIntent(TaskFormIntent.ToggleDayOfWeek(DayOfWeek.FRIDAY))

        val state = viewModel.uiState.first()
        assertThat(state.selectedDays).containsExactly(
            DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY
        )
    }

    // --- UpdateDescription ---

    @Test
    fun `UpdateDescription should update description`() = runTest {
        viewModel.onIntent(TaskFormIntent.UpdateDescription("Sprint review details"))

        val state = viewModel.uiState.first()
        assertThat(state.description).isEqualTo("Sprint review details")
    }

    // --- Checklist ---

    @Test
    fun `AddChecklistItem should add item to list`() = runTest {
        viewModel.onIntent(TaskFormIntent.AddChecklistItem("Buy milk"))

        val state = viewModel.uiState.first()
        assertThat(state.checklistItems).hasSize(1)
        assertThat(state.checklistItems[0].title).isEqualTo("Buy milk")
    }

    @Test
    fun `AddChecklistItem should not add blank item`() = runTest {
        viewModel.onIntent(TaskFormIntent.AddChecklistItem("  "))

        val state = viewModel.uiState.first()
        assertThat(state.checklistItems).isEmpty()
    }

    @Test
    fun `RemoveChecklistItem should remove item by id`() = runTest {
        viewModel.onIntent(TaskFormIntent.AddChecklistItem("Item 1"))
        val itemId = viewModel.uiState.first().checklistItems[0].id

        viewModel.onIntent(TaskFormIntent.RemoveChecklistItem(itemId))

        val state = viewModel.uiState.first()
        assertThat(state.checklistItems).isEmpty()
    }

    @Test
    fun `ToggleChecklistItem should toggle checked state`() = runTest {
        viewModel.onIntent(TaskFormIntent.AddChecklistItem("Item 1"))
        val itemId = viewModel.uiState.first().checklistItems[0].id

        viewModel.onIntent(TaskFormIntent.ToggleChecklistItem(itemId))

        val state = viewModel.uiState.first()
        assertThat(state.checklistItems[0].isChecked).isTrue()
    }

    // --- Save intent ---

    @Test
    fun `Save should fail with validation error when title is blank`() = runTest {
        viewModel.onIntent(TaskFormIntent.Save)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.validationError).isNotNull()
        assertThat(state.isSaved).isFalse()
    }

    @Test
    fun `Save should fail when interval end time is before start time`() = runTest {
        viewModel.onIntent(TaskFormIntent.UpdateTitle("Meeting"))
        viewModel.onIntent(TaskFormIntent.ToggleAllDay(false))
        viewModel.onIntent(TaskFormIntent.UpdateStartTime(14, 0))
        viewModel.onIntent(TaskFormIntent.UpdateEndTime(10, 0))

        viewModel.onIntent(TaskFormIntent.Save)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.validationError).isNotNull()
        assertThat(state.isSaved).isFalse()
    }

    @Test
    fun `Save should fail when custom weekly has no days selected`() = runTest {
        viewModel.onIntent(TaskFormIntent.UpdateTitle("Weekly Task"))
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.CUSTOM_WEEKLY))

        viewModel.onIntent(TaskFormIntent.Save)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.validationError).isNotNull()
        assertThat(state.isSaved).isFalse()
    }

    @Test
    fun `Save should create all-day task with single recurrence`() = runTest {
        coEvery { createTaskUseCase(any()) } returns Unit
        coEvery { scheduleTaskAlarmUseCase(any()) } returns Unit

        viewModel.onIntent(TaskFormIntent.UpdateTitle("Birthday"))
        viewModel.onIntent(TaskFormIntent.Save)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.isSaved).isTrue()
        assertThat(state.validationError).isNull()
        coVerify { createTaskUseCase(any()) }
    }

    @Test
    fun `Save should create interval task correctly`() = runTest {
        coEvery { createTaskUseCase(any()) } returns Unit
        coEvery { scheduleTaskAlarmUseCase(any()) } returns Unit

        viewModel.onIntent(TaskFormIntent.UpdateTitle("Meeting"))
        viewModel.onIntent(TaskFormIntent.ToggleAllDay(false))
        viewModel.onIntent(TaskFormIntent.UpdateStartTime(9, 0))
        viewModel.onIntent(TaskFormIntent.UpdateEndTime(10, 0))

        viewModel.onIntent(TaskFormIntent.Save)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.isSaved).isTrue()
        coVerify { createTaskUseCase(any()) }
    }

    @Test
    fun `Save should create daily recurrence task`() = runTest {
        coEvery { createTaskUseCase(any()) } returns Unit
        coEvery { scheduleTaskAlarmUseCase(any()) } returns Unit

        viewModel.onIntent(TaskFormIntent.UpdateTitle("Standup"))
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.DAILY))

        viewModel.onIntent(TaskFormIntent.Save)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.isSaved).isTrue()
    }

    @Test
    fun `Save should create custom weekly task with selected days`() = runTest {
        coEvery { createTaskUseCase(any()) } returns Unit
        coEvery { scheduleTaskAlarmUseCase(any()) } returns Unit

        viewModel.onIntent(TaskFormIntent.UpdateTitle("Gym"))
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.CUSTOM_WEEKLY))
        viewModel.onIntent(TaskFormIntent.ToggleDayOfWeek(DayOfWeek.MONDAY))
        viewModel.onIntent(TaskFormIntent.ToggleDayOfWeek(DayOfWeek.WEDNESDAY))

        viewModel.onIntent(TaskFormIntent.Save)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.isSaved).isTrue()
    }

    @Test
    fun `Save should include description and checklist`() = runTest {
        coEvery { createTaskUseCase(any()) } returns Unit
        coEvery { scheduleTaskAlarmUseCase(any()) } returns Unit

        viewModel.onIntent(TaskFormIntent.UpdateTitle("Shopping"))
        viewModel.onIntent(TaskFormIntent.UpdateDescription("Grocery list"))
        viewModel.onIntent(TaskFormIntent.AddChecklistItem("Milk"))
        viewModel.onIntent(TaskFormIntent.AddChecklistItem("Bread"))

        viewModel.onIntent(TaskFormIntent.Save)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.isSaved).isTrue()
        coVerify { createTaskUseCase(match { task ->
            task.description == "Grocery list" && task.checklistItems.size == 2
        }) }
    }

    @Test
    fun `Save should schedule alarm after creating task`() = runTest {
        coEvery { createTaskUseCase(any()) } returns Unit
        coEvery { scheduleTaskAlarmUseCase(any()) } returns Unit

        viewModel.onIntent(TaskFormIntent.UpdateTitle("Alarm Test"))
        viewModel.onIntent(TaskFormIntent.Save)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { scheduleTaskAlarmUseCase(any()) }
    }

    // --- UpdateYearlyMonth / UpdateYearlyDay ---

    @Test
    fun `UpdateYearlyMonth should update selectedYearlyMonth`() = runTest {
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.YEARLY))
        viewModel.onIntent(TaskFormIntent.UpdateYearlyMonth(7))

        val state = viewModel.uiState.first()
        assertThat(state.selectedYearlyMonth).isEqualTo(7)
    }

    @Test
    fun `UpdateYearlyDay should update selectedYearlyDay`() = runTest {
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.YEARLY))
        viewModel.onIntent(TaskFormIntent.UpdateYearlyDay(15))

        val state = viewModel.uiState.first()
        assertThat(state.selectedYearlyDay).isEqualTo(15)
    }

    @Test
    fun `Save should fail when yearly month is out of range`() = runTest {
        viewModel.onIntent(TaskFormIntent.UpdateTitle("Yearly Task"))
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.YEARLY))
        viewModel.onIntent(TaskFormIntent.UpdateYearlyMonth(0))

        viewModel.onIntent(TaskFormIntent.Save)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.validationError).isNotNull()
        assertThat(state.isSaved).isFalse()
    }

    @Test
    fun `Save should fail when yearly day is out of range`() = runTest {
        viewModel.onIntent(TaskFormIntent.UpdateTitle("Yearly Task"))
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.YEARLY))
        viewModel.onIntent(TaskFormIntent.UpdateYearlyMonth(6))
        viewModel.onIntent(TaskFormIntent.UpdateYearlyDay(32))

        viewModel.onIntent(TaskFormIntent.Save)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.validationError).isNotNull()
        assertThat(state.isSaved).isFalse()
    }

    @Test
    fun `Save should create yearly recurrence task`() = runTest {
        coEvery { createTaskUseCase(any()) } returns Unit
        coEvery { scheduleTaskAlarmUseCase(any()) } returns Unit

        viewModel.onIntent(TaskFormIntent.UpdateTitle("Anniversary"))
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.YEARLY))
        viewModel.onIntent(TaskFormIntent.UpdateYearlyMonth(4))
        viewModel.onIntent(TaskFormIntent.UpdateYearlyDay(24))

        viewModel.onIntent(TaskFormIntent.Save)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.isSaved).isTrue()
        coVerify { createTaskUseCase(match { task ->
            task.recurrence.month == 4 && task.recurrence.dayOfMonth == 24
        }) }
    }

    // --- ToggleAdvanceReminderDay / AddCustomAdvanceReminderDay ---

    @Test
    fun `ToggleAdvanceReminderDay should add day when not present`() = runTest {
        viewModel.onIntent(TaskFormIntent.ToggleAdvanceReminderDay(7))

        val state = viewModel.uiState.first()
        assertThat(state.advanceReminderDays).contains(7)
    }

    @Test
    fun `ToggleAdvanceReminderDay should remove day when already present`() = runTest {
        viewModel.onIntent(TaskFormIntent.ToggleAdvanceReminderDay(7))
        viewModel.onIntent(TaskFormIntent.ToggleAdvanceReminderDay(7))

        val state = viewModel.uiState.first()
        assertThat(state.advanceReminderDays).doesNotContain(7)
    }

    @Test
    fun `ToggleAdvanceReminderDay should replace previous selection with new value`() = runTest {
        // Selecting N days = range [1..N]; only one boundary value is kept at a time
        viewModel.onIntent(TaskFormIntent.ToggleAdvanceReminderDay(7))
        viewModel.onIntent(TaskFormIntent.ToggleAdvanceReminderDay(2))
        viewModel.onIntent(TaskFormIntent.ToggleAdvanceReminderDay(5))

        val state = viewModel.uiState.first()
        assertThat(state.advanceReminderDays).isEqualTo(listOf(5))
    }

    @Test
    fun `AddCustomAdvanceReminderDay should add unique positive day`() = runTest {
        viewModel.onIntent(TaskFormIntent.AddCustomAdvanceReminderDay(10))

        val state = viewModel.uiState.first()
        assertThat(state.advanceReminderDays).contains(10)
    }

    @Test
    fun `AddCustomAdvanceReminderDay should not add zero or negative`() = runTest {
        viewModel.onIntent(TaskFormIntent.AddCustomAdvanceReminderDay(0))
        viewModel.onIntent(TaskFormIntent.AddCustomAdvanceReminderDay(-1))

        val state = viewModel.uiState.first()
        assertThat(state.advanceReminderDays).isEmpty()
    }

    @Test
    fun `AddCustomAdvanceReminderDay should not duplicate existing day`() = runTest {
        viewModel.onIntent(TaskFormIntent.AddCustomAdvanceReminderDay(3))
        viewModel.onIntent(TaskFormIntent.AddCustomAdvanceReminderDay(3))

        val state = viewModel.uiState.first()
        assertThat(state.advanceReminderDays).hasSize(1)
    }

    @Test
    fun `Save should include the last selected advance reminder day in task`() = runTest {
        coEvery { createTaskUseCase(any()) } returns Unit
        coEvery { scheduleTaskAlarmUseCase(any()) } returns Unit

        viewModel.onIntent(TaskFormIntent.UpdateTitle("Conference"))
        // Each toggle replaces the previous; last selection wins
        viewModel.onIntent(TaskFormIntent.ToggleAdvanceReminderDay(7))
        viewModel.onIntent(TaskFormIntent.ToggleAdvanceReminderDay(3))

        viewModel.onIntent(TaskFormIntent.Save)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { createTaskUseCase(match { task ->
            task.advanceReminderDays == listOf(3)
        }) }
    }

    // --- RemoveAttachment ---

    // --- setInitialDate ---

    @Test
    fun `setInitialDate should override default date`() = runTest {
        val futureDate = LocalDate.now().plusDays(5)
        viewModel.setInitialDate(futureDate)

        val state = viewModel.uiState.first()
        assertThat(state.date).isEqualTo(futureDate)
    }

    // --- isDateFieldEnabled (campo data esmaecido por periodicidade) ---

    @Test
    fun `date field should be enabled by default with SINGLE recurrence`() = runTest {
        val state = viewModel.uiState.first()
        assertThat(state.isDateFieldEnabled).isTrue()
    }

    @Test
    fun `date field should be disabled when recurrence is DAILY`() = runTest {
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.DAILY))
        val state = viewModel.uiState.first()
        assertThat(state.isDateFieldEnabled).isFalse()
    }

    @Test
    fun `date field should be disabled when recurrence is CUSTOM_WEEKLY`() = runTest {
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.CUSTOM_WEEKLY))
        val state = viewModel.uiState.first()
        assertThat(state.isDateFieldEnabled).isFalse()
    }

    @Test
    fun `date field should be disabled when recurrence is MONTHLY`() = runTest {
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.MONTHLY))
        val state = viewModel.uiState.first()
        assertThat(state.isDateFieldEnabled).isFalse()
    }

    @Test
    fun `date field should be disabled when recurrence is YEARLY`() = runTest {
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.YEARLY))
        val state = viewModel.uiState.first()
        assertThat(state.isDateFieldEnabled).isFalse()
    }

    @Test
    fun `date field should re-enable when switching back to SINGLE`() = runTest {
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.DAILY))
        viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(RecurrenceTypeOption.SINGLE))
        val state = viewModel.uiState.first()
        assertThat(state.isDateFieldEnabled).isTrue()
    }

    // --- RemoveAttachment ---

    @Test
    fun `RemoveAttachment should remove path from attachments list`() = runTest {
        // Pre-load a task with attachments to avoid needing file I/O
        coEvery { getTaskByIdUseCase("task-1") } returns br.com.anotacoes.domain.model.Task(
            id = "task-1",
            title = "Test",
            date = LocalDate.now(),
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.Single(LocalDate.now()),
            attachments = listOf("/data/attachments/images/photo.jpg", "/data/attachments/docs/doc.pdf")
        )
        viewModel.loadTask("task-1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(TaskFormIntent.RemoveAttachment("/data/attachments/images/photo.jpg"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertThat(state.attachments).doesNotContain("/data/attachments/images/photo.jpg")
        assertThat(state.attachments).contains("/data/attachments/docs/doc.pdf")
    }
}
