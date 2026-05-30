package com.example.mwallet.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mwallet.domain.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

class TokenStorage(private val context: Context) {

    private val tokenKey = stringPreferencesKey("token")
    private val userIdKey = longPreferencesKey("user_id")
    private val usernameKey = stringPreferencesKey("username")

    suspend fun saveUser(user: User) {
        context.dataStore.edit { prefs ->
            prefs[tokenKey] = user.token
            prefs[userIdKey] = user.id
            prefs[usernameKey] = user.username
        }
    }

    suspend fun getUser(): User? {
        val prefs = context.dataStore.data.first()
        val token = prefs[tokenKey] ?: return null
        val userId = prefs[userIdKey] ?: return null
        val username = prefs[usernameKey] ?: return null
        return User(id = userId, username = username, token = token)
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun getToken(): String? = context.dataStore.data.map { it[tokenKey] }.first()
}
