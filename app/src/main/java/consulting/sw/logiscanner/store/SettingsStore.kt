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

enum class RelabelingSubmode(val storageValue: String) {
    KGT("kgt"),
    FULL("full");

    companion object {
        fun fromStorage(value: String?): RelabelingSubmode {
            return values().firstOrNull { it.storageValue == value } ?: KGT
        }
    }
}

class SettingsStore(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.settingsDataStore)

    companion object {
        private val KEY_EXTERNAL_SCANNER_ENABLED = booleanPreferencesKey("external_scanner_enabled")
        private val KEY_PRINTER_AUTO_PRINT_ENABLED = booleanPreferencesKey("printer_auto_print_enabled")
        private val KEY_PRINTER_BLUETOOTH_ADDRESS = stringPreferencesKey("printer_bluetooth_address")
        private val KEY_KGT_VOICE_ENABLED = booleanPreferencesKey("kgt_voice_enabled")
        private val KEY_RELABELING_SUBMODE = stringPreferencesKey("relabeling_submode")
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

    fun kgtVoiceEnabled(): Flow<Boolean> = preferences()
        .map {
            it[KEY_KGT_VOICE_ENABLED] ?: false
        }

    fun relabelingSubmode(): Flow<RelabelingSubmode> = preferences()
        .map {
            RelabelingSubmode.fromStorage(it[KEY_RELABELING_SUBMODE])
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

    suspend fun setKgtVoiceEnabled(enabled: Boolean) {
        dataStore.edit {
            it[KEY_KGT_VOICE_ENABLED] = enabled
        }
    }

    suspend fun setRelabelingSubmode(submode: RelabelingSubmode) {
        dataStore.edit {
            it[KEY_RELABELING_SUBMODE] = submode.storageValue
        }
    }
}
