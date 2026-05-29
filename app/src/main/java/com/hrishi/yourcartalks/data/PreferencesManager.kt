package com.hrishi.yourcartalks.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.store: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class TtsMethod {
    SYSTEM, SHERPA_MALE, SHERPA_FEMALE, KOKORO
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class PreferencesManager(private val context: Context) {

    private object Keys {
        val CAR_NAME = stringPreferencesKey("car_name")
        val DRIVER_NAME = stringPreferencesKey("driver_name")
        val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
        val TTS_METHOD = stringPreferencesKey("tts_method")
        val SHERPA_MODEL_PATH = stringPreferencesKey("sherpa_model_path")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SELECTED_GREETING = stringPreferencesKey("selected_greeting")
    }

    val carName: Flow<String> = context.store.data.map { prefs ->
        prefs[Keys.CAR_NAME] ?: ""
    }

    val isSetupComplete: Flow<Boolean> = context.store.data.map { prefs ->
        prefs[Keys.IS_SETUP_COMPLETE] ?: false
    }

    val ttsMethod: Flow<TtsMethod> = context.store.data.map { prefs ->
        parseTtsMethod(prefs[Keys.TTS_METHOD])
    }

    val sherpaModelPath: Flow<String> = context.store.data.map { prefs ->
        prefs[Keys.SHERPA_MODEL_PATH] ?: ""
    }

    val themeMode: Flow<ThemeMode> = context.store.data.map { prefs ->
        parseThemeMode(prefs[Keys.THEME_MODE])
    }

    val driverName: Flow<String> = context.store.data.map { prefs ->
        prefs[Keys.DRIVER_NAME] ?: ""
    }

    val selectedGreeting: Flow<String> = context.store.data.map { prefs ->
        prefs[Keys.SELECTED_GREETING] ?: ""
    }

    private fun parseTtsMethod(value: String?): TtsMethod {
        if (value == null) return TtsMethod.SYSTEM
        return when (value) {
            "SHERPA" -> TtsMethod.SHERPA_MALE
            else -> try {
                TtsMethod.valueOf(value)
            } catch (_: Exception) {
                TtsMethod.SYSTEM
            }
        }
    }

    private fun parseThemeMode(value: String?): ThemeMode {
        if (value == null) return ThemeMode.SYSTEM
        return try {
            ThemeMode.valueOf(value)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    suspend fun saveCarName(name: String) {
        context.store.edit { prefs ->
            prefs[Keys.CAR_NAME] = name
        }
    }

    suspend fun setSetupComplete() {
        context.store.edit { prefs ->
            prefs[Keys.IS_SETUP_COMPLETE] = true
        }
    }

    suspend fun getCarName(): String {
        return context.store.data.first()[Keys.CAR_NAME] ?: ""
    }

    suspend fun setTtsMethod(method: TtsMethod) {
        context.store.edit { prefs ->
            prefs[Keys.TTS_METHOD] = method.name
        }
    }

    suspend fun getTtsMethod(): TtsMethod {
        return parseTtsMethod(context.store.data.first()[Keys.TTS_METHOD])
    }

    suspend fun setSherpaModelPath(path: String) {
        context.store.edit { prefs ->
            prefs[Keys.SHERPA_MODEL_PATH] = path
        }
    }

    suspend fun getSherpaModelPath(): String {
        return context.store.data.first()[Keys.SHERPA_MODEL_PATH] ?: ""
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.store.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun getThemeMode(): ThemeMode {
        return parseThemeMode(context.store.data.first()[Keys.THEME_MODE])
    }

    suspend fun saveDriverName(name: String) {
        context.store.edit { prefs ->
            prefs[Keys.DRIVER_NAME] = name
        }
    }

    suspend fun getDriverName(): String {
        return context.store.data.first()[Keys.DRIVER_NAME] ?: ""
    }

    suspend fun saveSelectedGreeting(message: String) {
        context.store.edit { prefs ->
            prefs[Keys.SELECTED_GREETING] = message
        }
    }

    suspend fun getSelectedGreeting(): String {
        return context.store.data.first()[Keys.SELECTED_GREETING] ?: ""
    }
}
