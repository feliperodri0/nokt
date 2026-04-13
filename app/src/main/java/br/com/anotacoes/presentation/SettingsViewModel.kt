package br.com.anotacoes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.anotacoes.domain.model.AppTheme
import br.com.anotacoes.domain.repository.SettingsRepository
import br.com.anotacoes.domain.usecase.UpdateAppIconUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val updateAppIconUseCase: UpdateAppIconUseCase
) : ViewModel() {

    private val _closeRequired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val closeRequired: SharedFlow<Unit> = _closeRequired.asSharedFlow()

    val appTheme: StateFlow<AppTheme> = settingsRepository.getAppTheme()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.getAppThemeSync())

    val showAllInNotifications: StateFlow<Boolean> = settingsRepository.getShowAllInNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun applyThemeAndClose(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.setAppTheme(theme)  // suspend — aguarda DataStore commitar
            updateAppIconUseCase(theme)
            kotlinx.coroutines.delay(500)// síncrono — atualiza alias do launcher
            _closeRequired.emit(Unit)              // fecha o app APÓS tudo salvo
        }
    }

    fun setShowAllInNotifications(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowAllInNotifications(enabled) }
    }
}
