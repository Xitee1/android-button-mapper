package de.xitee.buttonmapper.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.xitee.buttonmapper.model.AppConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "config")

/** Stores the whole [AppConfig] as one JSON document. */
class ConfigRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val key = stringPreferencesKey("app_config")

    val config: Flow<AppConfig> = context.dataStore.data
        .map { prefs -> prefs[key]?.let(::decode) ?: AppConfig() }
        .distinctUntilChanged()

    suspend fun current(): AppConfig = config.first()

    suspend fun update(transform: (AppConfig) -> AppConfig) {
        context.dataStore.edit { prefs ->
            val old = prefs[key]?.let(::decode) ?: AppConfig()
            prefs[key] = json.encodeToString(AppConfig.serializer(), transform(old))
        }
    }

    private fun decode(text: String): AppConfig = try {
        json.decodeFromString(AppConfig.serializer(), text)
    } catch (e: Exception) {
        Log.e("ConfigRepository", "Invalid config, using defaults", e)
        AppConfig()
    }
}
