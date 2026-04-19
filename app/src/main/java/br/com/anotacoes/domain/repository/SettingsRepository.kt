package br.com.anotacoes.domain.repository

import br.com.anotacoes.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getAppTheme(): Flow<AppTheme>
    fun getAppThemeSync(): AppTheme
    suspend fun setAppTheme(theme: AppTheme)
    fun getShowAllInNotifications(): Flow<Boolean>
    suspend fun setShowAllInNotifications(enabled: Boolean)
    fun getCalendarExpandOnboardingSeen(): Flow<Boolean>
    suspend fun setCalendarExpandOnboardingSeen(seen: Boolean)
}
