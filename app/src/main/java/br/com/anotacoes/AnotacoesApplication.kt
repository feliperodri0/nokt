package br.com.anotacoes

import android.app.Application
import br.com.anotacoes.data.settings.SettingsRepositoryImpl
import br.com.anotacoes.domain.model.AppTheme
import br.com.anotacoes.domain.repository.AppIconRepository
import br.com.anotacoes.widget.TaskWidgetWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AnotacoesApplication : Application() {

    @Inject lateinit var appIconRepository: AppIconRepository

    override fun onCreate() {
        super.onCreate()  // Hilt injeta dependências aqui
        restoreIconAlias()
        TaskWidgetWorker.schedule(this)
    }

    /**
     * Garante que o alias correto esteja habilitado no PackageManager.
     * O estado de setComponentEnabledSetting persiste entre reinstalações do APK,
     * então precisamos restaurá-lo a cada inicialização.
     */
    private fun restoreIconAlias() {
        val themeName = getSharedPreferences(SettingsRepositoryImpl.SPLASH_PREFS, MODE_PRIVATE)
            .getString(SettingsRepositoryImpl.KEY_SPLASH_THEME, AppTheme.SYSTEM.name)
        val theme = AppTheme.entries.firstOrNull { it.name == themeName } ?: AppTheme.SYSTEM
        appIconRepository.updateIconForTheme(theme)
    }
}
