package com.example.pruebasnfc

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

object SessionManager {
    private val Context.dataStore by preferencesDataStore("user_prefs")
    private val KEY_REMEMBER_SESSION = booleanPreferencesKey("remember_session")

    suspend fun saveSession(context: Context, remember: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REMEMBER_SESSION] = remember
        }
    }

    suspend fun getSession(context: Context): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_REMEMBER_SESSION] ?: false
    }

    suspend fun clearSession(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_REMEMBER_SESSION)
        }
    }
}

