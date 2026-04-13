package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.model.ReminderStatus
import br.com.anotacoes.domain.repository.ReminderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ReactivateReminderUseCaseTest {

    private lateinit var reminderRepository: ReminderRepository
    private lateinit var useCase: ReactivateReminderUseCase

    @Before
    fun setup() {
        reminderRepository = mockk()
        useCase = ReactivateReminderUseCase(reminderRepository)
    }

    @Test
    fun `invoke should set reminder status to ACTIVE`() = runTest {
        coEvery { reminderRepository.setStatus(any(), any(), any()) } returns Unit

        useCase("r1_adv_3", "r1")

        coVerify { reminderRepository.setStatus("r1_adv_3", "r1", ReminderStatus.ACTIVE) }
    }

    @Test
    fun `invoke should work for any reminderId and taskId`() = runTest {
        coEvery { reminderRepository.setStatus(any(), any(), any()) } returns Unit

        useCase("task-abc_adv_7", "task-abc")

        coVerify { reminderRepository.setStatus("task-abc_adv_7", "task-abc", ReminderStatus.ACTIVE) }
    }

    @Test
    fun `invoke should call setStatus exactly once`() = runTest {
        coEvery { reminderRepository.setStatus(any(), any(), any()) } returns Unit

        useCase("r1_adv_3", "r1")

        coVerify(exactly = 1) { reminderRepository.setStatus(any(), any(), any()) }
    }
}
