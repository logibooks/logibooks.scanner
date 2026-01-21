// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

data class StoredAuth(
    val token: String?,
    val firstName: String?,
    val lastName: String?,
    val id: Int?
)

class AuthStore(private val context: Context) {

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_FIRST_NAME = stringPreferencesKey("first_name")
        private val KEY_LAST_NAME = stringPreferencesKey("last_name")
        private val KEY_ID = intPreferencesKey("id")
    }

    fun get(): Flow<StoredAuth> = context.dataStore.data.map {
        StoredAuth(
            token = it[KEY_TOKEN],
            firstName = it[KEY_FIRST_NAME],
            lastName = it[KEY_LAST_NAME],
            id = it[KEY_ID]
        )
    }

    suspend fun save(token: String, firstName: String, lastName: String, id: Int) {
        context.dataStore.edit {
            it[KEY_TOKEN] = token
            it[KEY_FIRST_NAME] = firstName
            it[KEY_LAST_NAME] = lastName
            it[KEY_ID] = id
        }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(KEY_TOKEN)
            it.remove(KEY_FIRST_NAME)
            it.remove(KEY_LAST_NAME)
            it.remove(KEY_ID)
        }
    }
}
