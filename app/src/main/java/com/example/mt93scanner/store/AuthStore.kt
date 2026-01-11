package com.example.mt93scanner.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth")

data class AuthSnapshot(
    val token: String?,
    val displayName: String?,
    val userId: Int?
)

class AuthStore(private val context: Context) {

    private val KEY_TOKEN = stringPreferencesKey("bearer_token")
    private val KEY_DISPLAY = stringPreferencesKey("display_name")
    private val KEY_USER_ID = intPreferencesKey("user_id")

    val authFlow: Flow<AuthSnapshot> = context.dataStore.data.map { prefs ->
        AuthSnapshot(
            token = prefs[KEY_TOKEN],
            displayName = prefs[KEY_DISPLAY],
            userId = prefs[KEY_USER_ID]
        )
    }

    suspend fun setAuth(token: String, displayName: String, userId: Int) {
        context.dataStore.edit {
            it[KEY_TOKEN] = token
            it[KEY_DISPLAY] = displayName
            it[KEY_USER_ID] = userId
        }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(KEY_TOKEN)
            it.remove(KEY_DISPLAY)
            it.remove(KEY_USER_ID)
        }
    }
}
