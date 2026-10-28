package com.iilmchat.chessproblem.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iilmchat.chessproblem.core.model.AppLanguage
import com.iilmchat.chessproblem.core.model.BoardTheme
import com.iilmchat.chessproblem.core.model.ThemeMode
import com.iilmchat.chessproblem.core.model.UserSettings
import com.iilmchat.chessproblem.core.repository.HintRepository
import com.iilmchat.chessproblem.core.repository.ProgressRepository
import com.iilmchat.chessproblem.core.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val hintBalance: Int = 3,
    val resetDone: Boolean = false,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val progressRepository: ProgressRepository,
    private val hintRepository: HintRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            hintRepository.balance.collect { balance ->
                _state.update { it.copy(hintBalance = balance) }
            }
        }
        viewModelScope.launch {
            hintRepository.refillIfNeeded()
        }
    }

    fun setLanguage(language: AppLanguage) = update { it.copy(language = language) }

    fun setTheme(theme: ThemeMode) = update { it.copy(theme = theme) }

    fun setBoardTheme(boardTheme: BoardTheme) = update { it.copy(boardTheme = boardTheme) }

    fun setCoordinates(enabled: Boolean) = update { it.copy(coordinates = enabled) }

    fun setAutoFlip(enabled: Boolean) = update { it.copy(autoFlip = enabled) }

    fun setReduceMotion(enabled: Boolean) = update { it.copy(reduceMotion = enabled) }

    fun setSound(enabled: Boolean) = update { it.copy(sound = enabled) }

    fun setHaptics(enabled: Boolean) = update { it.copy(haptics = enabled) }

    fun resetProgress() {
        viewModelScope.launch {
            progressRepository.clear()
            _state.update { it.copy(resetDone = true) }
        }
    }

    fun clearResetDone() {
        _state.update { it.copy(resetDone = false) }
    }

    private fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            settingsRepository.update(transform)
        }
    }
}
