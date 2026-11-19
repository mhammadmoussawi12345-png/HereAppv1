package com.hereapp.messenger.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hereapp.messenger.ui.theme.HereAppAccent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.hereAppDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "hereapp_preferences"
)

data class AppPreferences(
    val languageCode: String = "en",
    val accent: HereAppAccent = HereAppAccent.CHAMPAGNE_GOLD,
    val secondaryAccent: HereAppAccent = HereAppAccent.WARM_SAND,
    val isDarkMode: Boolean = true
)

class AppPreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
        val SECONDARY_ACCENT = stringPreferencesKey("secondary_accent")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    }

    val preferencesFlow: Flow<AppPreferences> = context.hereAppDataStore.data.map { prefs ->
        AppPreferences(
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: "en",
            accent = HereAppAccent.fromId(prefs[Keys.ACCENT_THEME]),
            secondaryAccent = HereAppAccent.fromId(prefs[Keys.SECONDARY_ACCENT] ?: HereAppAccent.WARM_SAND.id),
            isDarkMode = prefs[Keys.IS_DARK_MODE] ?: true
        )
    }

    suspend fun setLanguageCode(code: String) {
        context.hereAppDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = code
        }
    }

    suspend fun setAccent(accent: HereAppAccent) {
        context.hereAppDataStore.edit { prefs ->
            prefs[Keys.ACCENT_THEME] = accent.id
        }
    }

    suspend fun setThemePreferences(
        isDarkMode: Boolean,
        primary: HereAppAccent,
        secondary: HereAppAccent
    ) {
        context.hereAppDataStore.edit { prefs ->
            prefs[Keys.IS_DARK_MODE] = isDarkMode
            prefs[Keys.ACCENT_THEME] = primary.id
            prefs[Keys.SECONDARY_ACCENT] = secondary.id
        }
    }
}
