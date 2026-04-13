package br.com.anotacoes.receiver

import br.com.anotacoes.domain.usecase.DismissTaskUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * TDD tests for TaskDismissHandler (Requirement 9 - Dismiss from Lock Screen).
 * Validates that dismiss action extracts task ID and calls DismissTaskUseCase.
 */
class TaskDismissHandlerTest {

    private lateinit var dismissTaskUseCase: DismissTaskUseCase
    private lateinit var handler: TaskDismissHandler

    @Before
    fun setup() {
        dismissTaskUseCase = mockk()
        handler = TaskDismissHandler(dismissTaskUseCase)
    }

    // --- Action validation ---

    @Test
    fun `should recognize valid dismiss action`() {
        val isValid = handler.isValidAction(TaskDismissHandler.ACTION_DISMISS_TASK)

        assertThat(isValid).isTrue()
    }

    @Test
    fun `should reject unknown action`() {
        val isValid = handler.isValidAction("com.unknown.ACTION")

        assertThat(isValid).isFalse()
    }

    @Test
    fun `should reject null action`() {
        val isValid = handler.isValidAction(null)

        assertThat(isValid).isFalse()
    }

    // --- Task ID extraction ---

    @Test
    fun `should extract task id from extras`() {
        val extras = mapOf(TaskDismissHandler.EXTRA_TASK_ID to "task-123")

        val taskId = handler.extractTaskId(extras)

        assertThat(taskId).isEqualTo("task-123")
    }

    @Test
    fun `should return null when task id is missing`() {
        val taskId = handler.extractTaskId(emptyMap())

        assertThat(taskId).isNull()
    }

    @Test
    fun `should return null when task id is blank`() {
        val extras = mapOf(TaskDismissHandler.EXTRA_TASK_ID to "  ")

        val taskId = handler.extractTaskId(extras)

        assertThat(taskId).isNull()
    }

    @Test
    fun `should return null when task id is null`() {
        val extras = mapOf(TaskDismissHandler.EXTRA_TASK_ID to null)

        val taskId = handler.extractTaskId(extras)

        assertThat(taskId).isNull()
    }

    // --- Dismiss execution ---

    @Test
    fun `should call DismissTaskUseCase with correct task id`() = runTest {
        coEvery { dismissTaskUseCase("task-456") } returns Unit

        handler.dismiss("task-456")

        coVerify(exactly = 1) { dismissTaskUseCase("task-456") }
    }

    @Test
    fun `should call DismissTaskUseCase only once per invocation`() = runTest {
        coEvery { dismissTaskUseCase(any()) } returns Unit

        handler.dismiss("task-1")

        coVerify(exactly = 1) { dismissTaskUseCase(any()) }
    }
}
