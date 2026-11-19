package com.hereapp.messenger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hereapp.messenger.core.i18n.LanguageCatalog
import com.hereapp.messenger.core.i18n.SupportedLanguage
import com.hereapp.messenger.core.preferences.AppPreferencesRepository
import com.hereapp.messenger.ui.theme.HereAppAccent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUiState(
    val selectedLanguage: SupportedLanguage = LanguageCatalog.findByCode("en"),
    val selectedAccent: HereAppAccent = HereAppAccent.CHAMPAGNE_GOLD,
    val secondaryAccent: HereAppAccent = HereAppAccent.WARM_SAND,
    val isDarkMode: Boolean = true
)

class AppViewModel(
    private val preferencesRepository: AppPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<AppUiState> = preferencesRepository.preferencesFlow
        .map { prefs ->
            AppUiState(
                selectedLanguage = LanguageCatalog.findByCode(prefs.languageCode),
                selectedAccent = prefs.accent,
                secondaryAccent = prefs.secondaryAccent,
                isDarkMode = prefs.isDarkMode
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppUiState()
        )

    fun selectLanguage(code: String) {
        viewModelScope.launch {
            preferencesRepository.setLanguageCode(code)
        }
    }

    fun selectAccent(accent: HereAppAccent) {
        viewModelScope.launch {
            preferencesRepository.setAccent(accent)
        }
    }

    fun applyTheme(isDarkMode: Boolean, primary: HereAppAccent, secondary: HereAppAccent) {
        viewModelScope.launch {
            preferencesRepository.setThemePreferences(isDarkMode, primary, secondary)
        }
    }

    class Factory(
        private val preferencesRepository: AppPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(preferencesRepository) as T
        }
    }
}
