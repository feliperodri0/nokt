package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.model.AppTheme
import br.com.anotacoes.domain.repository.AppIconRepository
import javax.inject.Inject

class UpdateAppIconUseCase @Inject constructor(
    private val appIconRepository: AppIconRepository
) {
    operator fun invoke(theme: AppTheme) = appIconRepository.updateIconForTheme(theme)
}
