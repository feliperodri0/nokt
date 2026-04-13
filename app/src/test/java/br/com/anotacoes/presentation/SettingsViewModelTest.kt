package br.com.anotacoes.presentation

import br.com.anotacoes.domain.model.AppTheme
import br.com.anotacoes.domain.repository.SettingsRepository
import br.com.anotacoes.domain.usecase.UpdateAppIconUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var updateAppIconUseCase: UpdateAppIconUseCase
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk(relaxed = true)
        updateAppIconUseCase = mockk(relaxed = true)

        every { settingsRepository.getAppTheme() } returns flowOf(AppTheme.SYSTEM)
        every { settingsRepository.getShowAllInNotifications() } returns flowOf(false)

        viewModel = SettingsViewModel(settingsRepository, updateAppIconUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setAppTheme persists theme to repository`() = runTest {
        coEvery { settingsRepository.setAppTheme(any()) } returns Unit

        viewModel.applyThemeAndClose(AppTheme.ROSA_CLARO)
        advanceUntilIdle()

        coVerify { settingsRepository.setAppTheme(AppTheme.ROSA_CLARO) }
    }

    @Test
    fun `setAppTheme also updates app icon`() = runTest {
        coEvery { settingsRepository.setAppTheme(any()) } returns Unit

        viewModel.applyThemeAndClose(AppTheme.ROSA_CLARO)
        advanceUntilIdle()

        verify { updateAppIconUseCase(AppTheme.ROSA_CLARO) }
    }

    @Test
    fun `setAppTheme updates icon for OCEAN_BLUE`() = runTest {
        coEvery { settingsRepository.setAppTheme(any()) } returns Unit

        viewModel.applyThemeAndClose(AppTheme.OCEANO_CLARO)
        advanceUntilIdle()

        verify { updateAppIconUseCase(AppTheme.OCEANO_CLARO) }
    }

    @Test
    fun `setAppTheme updates icon for DEEP_PURPLE`() = runTest {
        coEvery { settingsRepository.setAppTheme(any()) } returns Unit

        viewModel.applyThemeAndClose(AppTheme.ROXO_CLARO)
        advanceUntilIdle()

        verify { updateAppIconUseCase(AppTheme.ROXO_CLARO) }
    }

    @Test
    fun `setAppTheme updates icon for SYSTEM`() = runTest {
        coEvery { settingsRepository.setAppTheme(any()) } returns Unit

        viewModel.applyThemeAndClose(AppTheme.SYSTEM)
        advanceUntilIdle()

        verify { updateAppIconUseCase(AppTheme.SYSTEM) }
    }

    @Test
    fun `icon and theme update happen for all theme values`() = runTest {
        coEvery { settingsRepository.setAppTheme(any()) } returns Unit

        AppTheme.entries.forEach { theme ->
            viewModel.applyThemeAndClose(theme)
            advanceUntilIdle()

            coVerify { settingsRepository.setAppTheme(theme) }
            verify { updateAppIconUseCase(theme) }
        }
    }

    @Test
    fun `initial appTheme state is SYSTEM`() = runTest {
        assertThat(viewModel.appTheme.value).isEqualTo(AppTheme.SYSTEM)
    }
}
