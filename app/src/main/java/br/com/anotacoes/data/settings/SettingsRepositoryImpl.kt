package br.com.anotacoes.data.settings

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.anotacoes.domain.model.AppTheme
import br.com.anotacoes.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val dataStore get() = context.dataStore

    override fun getAppTheme(): Flow<AppTheme> {
        return dataStore.data.map { prefs ->
            val name = prefs[KEY_APP_THEME] ?: AppTheme.SYSTEM.name
            AppTheme.entries.firstOrNull { it.name == name } ?: AppTheme.SYSTEM
        }
    }

    override fun getAppThemeSync(): AppTheme {
        val name = context.getSharedPreferences(SPLASH_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SPLASH_THEME, AppTheme.SYSTEM.name)
        return AppTheme.entries.firstOrNull { it.name == name } ?: AppTheme.SYSTEM
    }

    override suspend fun setAppTheme(theme: AppTheme) {
        dataStore.edit { prefs ->
            prefs[KEY_APP_THEME] = theme.name
        }
        // Also persist synchronously so MainActivity can read it before the splash screen
        context.getSharedPreferences(SPLASH_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SPLASH_THEME, theme.name)
            .commit()
    }

    override fun getShowAllInNotifications(): Flow<Boolean> {
        return dataStore.data.map { prefs ->
            prefs[KEY_SHOW_ALL_NOTIFICATIONS] ?: false
        }
    }

    override suspend fun setShowAllInNotifications(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SHOW_ALL_NOTIFICATIONS] = enabled
        }
    }

    override fun getCalendarExpandOnboardingSeen(): Flow<Boolean> {
        return dataStore.data.map { prefs ->
            prefs[KEY_CALENDAR_EXPAND_ONBOARDING_SEEN] ?: false
        }
    }

    override suspend fun setCalendarExpandOnboardingSeen(seen: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_CALENDAR_EXPAND_ONBOARDING_SEEN] = seen
        }
    }

    companion object {
        private val KEY_APP_THEME = stringPreferencesKey("app_theme")
        private val KEY_SHOW_ALL_NOTIFICATIONS = booleanPreferencesKey("show_all_notifications")
        private val KEY_CALENDAR_EXPAND_ONBOARDING_SEEN =
            booleanPreferencesKey("calendar_expand_onboarding_seen")
        const val SPLASH_PREFS = "anotacoes_splash_prefs"
        const val KEY_SPLASH_THEME = "splash_theme"
    }
}
