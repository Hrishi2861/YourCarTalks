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

class PreferencesManager(private val context: Context) {

    private object Keys {
        val CAR_NAME = stringPreferencesKey("car_name")
        val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
    }

    val carName: Flow<String> = context.store.data.map { prefs ->
        prefs[Keys.CAR_NAME] ?: ""
    }

    val isSetupComplete: Flow<Boolean> = context.store.data.map { prefs ->
        prefs[Keys.IS_SETUP_COMPLETE] ?: false
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
}
