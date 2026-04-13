package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.model.TaskStatus
import br.com.anotacoes.domain.repository.TaskRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ReactivateTaskUseCaseTest {

    private lateinit var repository: TaskRepository
    private lateinit var scheduleTaskAlarmUseCase: ScheduleTaskAlarmUseCase
    private lateinit var useCase: ReactivateTaskUseCase

    @Before
    fun setup() {
        repository = mockk()
        scheduleTaskAlarmUseCase = mockk(relaxed = true)
        useCase = ReactivateTaskUseCase(repository, scheduleTaskAlarmUseCase)
    }

    @Test
    fun `invoke should set task status to ACTIVE`() = runTest {
        coEvery { repository.setTaskStatus(any(), any()) } returns Unit

        useCase("task-1")

        coVerify { repository.setTaskStatus("task-1", TaskStatus.ACTIVE) }
    }

    @Test
    fun `invoke should reschedule alarm after reactivating`() = runTest {
        coEvery { repository.setTaskStatus(any(), any()) } returns Unit

        useCase("task-1")

        coVerify { scheduleTaskAlarmUseCase("task-1") }
    }

    @Test
    fun `invoke should set ACTIVE before scheduling alarm`() = runTest {
        val order = mutableListOf<String>()
        coEvery { repository.setTaskStatus(any(), any()) } answers { order.add("setStatus") }
        coEvery { scheduleTaskAlarmUseCase(any()) } answers { order.add("scheduleAlarm") }

        useCase("task-1")

        assertThat(order).isEqualTo(listOf("setStatus", "scheduleAlarm"))
    }
}
