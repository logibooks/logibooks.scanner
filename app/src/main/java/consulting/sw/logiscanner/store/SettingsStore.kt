// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.settingsDataStore)

    companion object {
        private val KEY_EXTERNAL_SCANNER_ENABLED = booleanPreferencesKey("external_scanner_enabled")
    }

    fun externalScannerEnabled(): Flow<Boolean> = dataStore.data.map {
        it[KEY_EXTERNAL_SCANNER_ENABLED] ?: false
    }

    suspend fun setExternalScannerEnabled(enabled: Boolean) {
        dataStore.edit {
            it[KEY_EXTERNAL_SCANNER_ENABLED] = enabled
        }
    }
}
