package io.yarburart.reiproxy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class ProjectSettings(
    val host: String = "127.0.0.1",
    val port: Int = 8080,
    val interceptEnabled: Boolean = false,
    val handleSsl: Boolean = true,
    val themeMode: String = "system",
)

class SettingsManager(private val context: Context) {

    private fun projectKey(projectId: Long, suffix: String) = "project_${projectId}_$suffix"

    fun getSettingsFlow(projectId: Long): Flow<ProjectSettings> {
        return context.dataStore.data.map { prefs ->
            ProjectSettings(
                host = prefs[stringPreferencesKey(projectKey(projectId, "host"))] ?: "127.0.0.1",
                port = prefs[intPreferencesKey(projectKey(projectId, "port"))] ?: 8080,
                interceptEnabled = prefs[booleanPreferencesKey(projectKey(projectId, "intercept"))] ?: false,
                handleSsl = prefs[booleanPreferencesKey(projectKey(projectId, "ssl"))] ?: true,
                themeMode = prefs[stringPreferencesKey(projectKey(projectId, "theme"))] ?: "system",
            )
        }
    }

    suspend fun updateHost(projectId: Long, host: String) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey(projectKey(projectId, "host"))] = host
        }
    }

    suspend fun updatePort(projectId: Long, port: Int) {
        context.dataStore.edit { prefs ->
            prefs[intPreferencesKey(projectKey(projectId, "port"))] = port
        }
    }

    suspend fun updateInterceptEnabled(projectId: Long, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(projectKey(projectId, "intercept"))] = enabled
        }
    }

    suspend fun updateHandleSsl(projectId: Long, handle: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[booleanPreferencesKey(projectKey(projectId, "ssl"))] = handle
        }
    }

    suspend fun updateThemeMode(projectId: Long, theme: String) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey(projectKey(projectId, "theme"))] = theme
        }
    }
}
