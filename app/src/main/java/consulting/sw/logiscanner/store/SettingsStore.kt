// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.settingsDataStore)

    companion object {
        private val KEY_EXTERNAL_SCANNER_ENABLED = booleanPreferencesKey("external_scanner_enabled")
        private val KEY_PRINTER_AUTO_PRINT_ENABLED = booleanPreferencesKey("printer_auto_print_enabled")
        private val KEY_PRINTER_BLUETOOTH_ADDRESS = stringPreferencesKey("printer_bluetooth_address")
    }

    fun externalScannerEnabled(): Flow<Boolean> = preferences()
        .map {
            it[KEY_EXTERNAL_SCANNER_ENABLED] ?: false
        }

    fun printerAutoPrintEnabled(): Flow<Boolean> = preferences()
        .map {
            it[KEY_PRINTER_AUTO_PRINT_ENABLED] ?: false
        }

    fun printerBluetoothAddress(): Flow<String?> = preferences()
        .map {
            it[KEY_PRINTER_BLUETOOTH_ADDRESS]
        }

    private fun preferences(): Flow<Preferences> = dataStore.data
        .catch { ex ->
            if (ex is IOException) {
                emit(emptyPreferences())
            } else {
                throw ex
            }
        }

    suspend fun setExternalScannerEnabled(enabled: Boolean) {
        dataStore.edit {
            it[KEY_EXTERNAL_SCANNER_ENABLED] = enabled
        }
    }

    suspend fun setPrinterAutoPrintEnabled(enabled: Boolean) {
        dataStore.edit {
            it[KEY_PRINTER_AUTO_PRINT_ENABLED] = enabled
        }
    }

    suspend fun setPrinterBluetoothAddress(address: String?) {
        dataStore.edit {
            if (address.isNullOrBlank()) {
                it.remove(KEY_PRINTER_BLUETOOTH_ADDRESS)
            } else {
                it[KEY_PRINTER_BLUETOOTH_ADDRESS] = address
            }
        }
    }
}
