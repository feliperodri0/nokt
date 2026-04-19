package br.com.anotacoes.domain.usecase

import br.com.anotacoes.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCalendarExpandOnboardingSeenUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.getCalendarExpandOnboardingSeen()
}
