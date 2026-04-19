package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.repository.SettingsRepository
import javax.inject.Inject

class MarkCalendarExpandOnboardingSeenUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke() = repository.setCalendarExpandOnboardingSeen(true)
}
