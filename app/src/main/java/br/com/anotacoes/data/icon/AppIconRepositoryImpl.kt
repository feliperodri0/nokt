package br.com.anotacoes.data.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import br.com.anotacoes.domain.model.AppTheme
import br.com.anotacoes.domain.repository.AppIconRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppIconRepositoryImpl @Inject constructor(
    private val context: Context
) : AppIconRepository {

    override fun updateIconForTheme(theme: AppTheme) {
        val packageName = context.packageName
        val packageManager = context.packageManager

        val targetAlias = aliasForTheme(packageName, theme)

        ALL_ALIASES.forEach { alias ->
            val state = if (alias == targetAlias)
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED

            packageManager.setComponentEnabledSetting(
                ComponentName(packageName, alias),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    private fun aliasForTheme(packageName: String, theme: AppTheme): String = when (theme) {
        AppTheme.ROSA_CLARO, AppTheme.ROSA_ESCURO -> "$packageName.MainActivityRosa"
        AppTheme.VITORIA_CLARO, AppTheme.VITORIA_ESCURO -> "$packageName.MainActivityVitoria"
        AppTheme.OCEANO_CLARO, AppTheme.OCEANO_ESCURO -> "$packageName.MainActivityOceano"
        AppTheme.ROXO_CLARO, AppTheme.ROXO_ESCURO -> "$packageName.MainActivityRoxo"
        else -> "$packageName.MainActivity"
    }

    companion object {
        private val ALL_ALIASES = listOf(
            "MainActivity",
            "MainActivityRosa",
            "MainActivityVitoria",
            "MainActivityOceano",
            "MainActivityRoxo",
        ).map { "br.com.anotacoes.$it" }
    }
}