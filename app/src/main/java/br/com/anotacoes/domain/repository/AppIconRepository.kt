package br.com.anotacoes.domain.repository

import br.com.anotacoes.domain.model.AppTheme

interface AppIconRepository {
    fun updateIconForTheme(theme: AppTheme)
}
