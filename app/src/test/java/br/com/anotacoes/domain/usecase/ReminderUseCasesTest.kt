package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.model.Recurrence
import br.com.anotacoes.domain.model.Reminder
import br.com.anotacoes.domain.model.ReminderStatus
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskStatus
import br.com.anotacoes.domain.model.TaskTime
import br.com.anotacoes.domain.port.TaskNotificationPort
import br.com.anotacoes.domain.repository.ReminderRepository
import br.com.anotacoes.domain.repository.TaskRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ReminderUseCasesTest {

    private lateinit var taskRepository: TaskRepository
    private lateinit var reminderRepository: ReminderRepository
    private lateinit var notificationPort: TaskNotificationPort

    private lateinit var getRemindersForDate: GetRemindersForDateUseCase
    private lateinit var dismissReminder: DismissReminderUseCase
    private lateinit var completeReminder: CompleteReminderUseCase
    private lateinit var deleteReminder: DeleteReminderUseCase

    private val today = LocalDate.of(2026, 4, 11)
    private val futureDate = today.plusDays(3)

    @Before
    fun setup() {
        taskRepository = mockk()
        reminderRepository = mockk()
        notificationPort = mockk(relaxed = true)

        getRemindersForDate = GetRemindersForDateUseCase(taskRepository, reminderRepository)
        dismissReminder = DismissReminderUseCase(reminderRepository)
        completeReminder = CompleteReminderUseCase(reminderRepository)
        deleteReminder = DeleteReminderUseCase(reminderRepository)
    }

    // ─── GetRemindersForDateUseCase ───────────────────────────────────────────

    @Test
    fun `should return reminder when task has matching advanceReminderDays`() = runTest {
        val task = makeTaskWithReminder(id = "t1", title = "Dentista", date = futureDate, reminderDays = listOf(3))
        every { taskRepository.getAllTasks() } returns flowOf(listOf(task))
        every { reminderRepository.getAllStatuses() } returns flowOf(emptyMap())

        val result = getRemindersForDate(today).first()

        assertThat(result).hasSize(1)
        assertThat(result[0].taskId).isEqualTo("t1")
        assertThat(result[0].taskTitle).isEqualTo("Dentista")
        assertThat(result[0].daysBeforeTask).isEqualTo(3)
        assertThat(result[0].status).isEqualTo(ReminderStatus.ACTIVE)
    }

    @Test
    fun `should return empty when task has no advanceReminderDays`() = runTest {
        val task = makeTaskWithReminder(id = "t1", title = "Task", date = futureDate, reminderDays = emptyList())
        every { taskRepository.getAllTasks() } returns flowOf(listOf(task))
        every { reminderRepository.getAllStatuses() } returns flowOf(emptyMap())

        val result = getRemindersForDate(today).first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `should show reminder when days until task is within selected range`() = runTest {
        // task is 3 days away; "7 days before" means remind for ALL days 1..7 before → day 3 is in range
        val task = makeTaskWithReminder(id = "t1", title = "Task", date = futureDate, reminderDays = listOf(7))
        every { taskRepository.getAllTasks() } returns flowOf(listOf(task))
        every { reminderRepository.getAllStatuses() } returns flowOf(emptyMap())

        val result = getRemindersForDate(today).first()

        assertThat(result).hasSize(1)
        assertThat(result[0].daysBeforeTask).isEqualTo(3)
    }

    @Test
    fun `should return empty when days until task exceeds selected reminder range`() = runTest {
        // task is 3 days away, but reminder is set for only 1 day before
        val task = makeTaskWithReminder(id = "t1", title = "Task", date = futureDate, reminderDays = listOf(1))
        every { taskRepository.getAllTasks() } returns flowOf(listOf(task))
        every { reminderRepository.getAllStatuses() } returns flowOf(emptyMap())

        val result = getRemindersForDate(today).first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `should return empty when task is in the past`() = runTest {
        val pastDate = today.minusDays(3)
        val task = makeTaskWithReminder(id = "t1", title = "Task", date = pastDate, reminderDays = listOf(3))
        every { taskRepository.getAllTasks() } returns flowOf(listOf(task))
        every { reminderRepository.getAllStatuses() } returns flowOf(emptyMap())

        val result = getRemindersForDate(today).first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `should not return DELETED reminder`() = runTest {
        val task = makeTaskWithReminder(id = "t1", title = "Dentista", date = futureDate, reminderDays = listOf(3))
        val reminderId = "t1_adv_3"
        every { taskRepository.getAllTasks() } returns flowOf(listOf(task))
        every { reminderRepository.getAllStatuses() } returns flowOf(mapOf(reminderId to ReminderStatus.DELETED))

        val result = getRemindersForDate(today).first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `should return DISMISSED reminder with correct status`() = runTest {
        val task = makeTaskWithReminder(id = "t1", title = "Dentista", date = futureDate, reminderDays = listOf(3))
        val reminderId = "t1_adv_3"
        every { taskRepository.getAllTasks() } returns flowOf(listOf(task))
        every { reminderRepository.getAllStatuses() } returns flowOf(mapOf(reminderId to ReminderStatus.DISMISSED))

        val result = getRemindersForDate(today).first()

        assertThat(result).hasSize(1)
        assertThat(result[0].status).isEqualTo(ReminderStatus.DISMISSED)
    }

    @Test
    fun `should not return reminder for DISMISSED task`() = runTest {
        val task = makeTaskWithReminder(
            id = "t1", title = "Task", date = futureDate,
            reminderDays = listOf(3), taskStatus = TaskStatus.DISMISSED
        )
        every { taskRepository.getAllTasks() } returns flowOf(listOf(task))
        every { reminderRepository.getAllStatuses() } returns flowOf(emptyMap())

        val result = getRemindersForDate(today).first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `reminder id should be taskId_adv_daysBeforeTask`() = runTest {
        val task = makeTaskWithReminder(id = "abc123", title = "T", date = futureDate, reminderDays = listOf(3))
        every { taskRepository.getAllTasks() } returns flowOf(listOf(task))
        every { reminderRepository.getAllStatuses() } returns flowOf(emptyMap())

        val result = getRemindersForDate(today).first()

        assertThat(result[0].id).isEqualTo("abc123_adv_3")
    }

    @Test
    fun `should return multiple reminders when multiple tasks qualify`() = runTest {
        val task1 = makeTaskWithReminder(id = "t1", title = "A", date = futureDate, reminderDays = listOf(3))
        val task2 = makeTaskWithReminder(id = "t2", title = "B", date = today.plusDays(7), reminderDays = listOf(7))
        every { taskRepository.getAllTasks() } returns flowOf(listOf(task1, task2))
        every { reminderRepository.getAllStatuses() } returns flowOf(emptyMap())

        val result = getRemindersForDate(today).first()

        assertThat(result).hasSize(2)
    }

    // ─── DismissReminderUseCase ───────────────────────────────────────────────

    @Test
    fun `dismiss reminder should call setStatus DISMISSED`() = runTest {
        coEvery { reminderRepository.setStatus(any(), any(), any()) } returns Unit

        dismissReminder(reminderId = "t1_adv_3", taskId = "t1")

        coVerify { reminderRepository.setStatus("t1_adv_3", "t1", ReminderStatus.DISMISSED) }
    }

    // ─── CompleteReminderUseCase ──────────────────────────────────────────────

    @Test
    fun `complete reminder should call setStatus COMPLETED`() = runTest {
        coEvery { reminderRepository.setStatus(any(), any(), any()) } returns Unit

        completeReminder(reminderId = "t1_adv_3", taskId = "t1")

        coVerify { reminderRepository.setStatus("t1_adv_3", "t1", ReminderStatus.COMPLETED) }
    }

    // ─── DeleteReminderUseCase ────────────────────────────────────────────────

    @Test
    fun `delete reminder should call setStatus DELETED`() = runTest {
        coEvery { reminderRepository.setStatus(any(), any(), any()) } returns Unit

        deleteReminder(reminderId = "t1_adv_3", taskId = "t1")

        coVerify { reminderRepository.setStatus("t1_adv_3", "t1", ReminderStatus.DELETED) }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun makeTaskWithReminder(
        id: String,
        title: String,
        date: LocalDate,
        reminderDays: List<Int>,
        taskStatus: TaskStatus = TaskStatus.ACTIVE
    ): Task {
        return Task(
            id = id,
            title = title,
            date = date,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.Single(date),
            advanceReminderDays = reminderDays,
            status = taskStatus
        )
    }
}
