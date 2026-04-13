package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.model.AppTheme
import br.com.anotacoes.domain.repository.AppIconRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class UpdateAppIconUseCaseTest {

    private lateinit var repository: AppIconRepository
    private lateinit var useCase: UpdateAppIconUseCase

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        useCase = UpdateAppIconUseCase(repository)
    }

    @Test
    fun `invoke delegates to repository for each theme`() {
        AppTheme.entries.forEach { theme ->
            useCase(theme)
            verify { repository.updateIconForTheme(theme) }
        }
    }

    @Test
    fun `SYSTEM theme delegates correctly`() {
        useCase(AppTheme.SYSTEM)
        verify { repository.updateIconForTheme(AppTheme.SYSTEM) }
    }

    @Test
    fun `PINK_ROSE theme delegates correctly`() {
        useCase(AppTheme.ROSA_CLARO)
        verify { repository.updateIconForTheme(AppTheme.ROSA_CLARO) }
    }

    @Test
    fun `OCEAN_BLUE theme delegates correctly`() {
        useCase(AppTheme.OCEANO_CLARO)
        verify { repository.updateIconForTheme(AppTheme.OCEANO_CLARO) }
    }

    @Test
    fun `DEEP_PURPLE theme delegates correctly`() {
        useCase(AppTheme.ROXO_CLARO)
        verify { repository.updateIconForTheme(AppTheme.ROXO_CLARO) }
    }
}
