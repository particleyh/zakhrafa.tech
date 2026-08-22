package com.zakhrafa.app

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

private val Context.safePreferences: Flow<Preferences>
    get() = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

object FavoritesManager {
    private val FAVORITES_KEY = stringSetPreferencesKey("fav_list")

    fun getFavorites(context: Context): Flow<Set<String>> {
        return context.safePreferences.map { preferences ->
            preferences[FAVORITES_KEY] ?: emptySet()
        }
    }

    suspend fun toggleFavorite(context: Context, text: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY] ?: emptySet()
            if (current.contains(text)) {
                preferences[FAVORITES_KEY] = current - text
            } else {
                preferences[FAVORITES_KEY] = current + text
            }
        }
    }

    suspend fun clearFavorites(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.remove(FAVORITES_KEY)
        }
    }
}

object AppearanceManager {
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

    fun getDarkMode(context: Context): Flow<Boolean?> {
        return context.safePreferences.map { preferences ->
            preferences[DARK_MODE_KEY]
        }
    }

    suspend fun setDarkMode(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }
}

data class CopyHistoryItem(val id: String, val text: String)

object CopyHistoryManager {
    private const val MAX_HISTORY = 24
    private val HISTORY_KEY = stringSetPreferencesKey("copy_history")

    fun getHistory(context: Context): Flow<List<CopyHistoryItem>> {
        return context.safePreferences.map { preferences ->
            preferences[HISTORY_KEY]
                .orEmpty()
                .mapNotNull(::decodeEntry)
                .sortedByDescending { it.id.toLongOrNull() ?: 0L }
        }
    }

    suspend fun recordCopy(context: Context, text: String) {
        if (text.isBlank()) return
        context.dataStore.edit { preferences ->
            val existing = preferences[HISTORY_KEY]
                .orEmpty()
                .mapNotNull(::decodeEntry)
                .filterNot { it.text == text }
                .sortedByDescending { it.id.toLongOrNull() ?: 0L }

            val encoded = Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val newest = "${System.currentTimeMillis()}:$encoded"
            preferences[HISTORY_KEY] = (listOf(newest) + existing.map(::encodeEntry))
                .take(MAX_HISTORY)
                .toSet()
        }
    }

    suspend fun clearHistory(context: Context) {
        context.dataStore.edit { preferences -> preferences.remove(HISTORY_KEY) }
    }

    private fun encodeEntry(item: CopyHistoryItem): String {
        val encoded = Base64.encodeToString(item.text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "${item.id}:$encoded"
    }

    private fun decodeEntry(value: String): CopyHistoryItem? {
        val separator = value.indexOf(':')
        if (separator <= 0 || separator == value.lastIndex) return null
        return runCatching {
            val id = value.substring(0, separator)
            val text = String(
                Base64.decode(value.substring(separator + 1), Base64.NO_WRAP),
                Charsets.UTF_8
            )
            CopyHistoryItem(id = id, text = text)
        }.getOrNull()
    }
}

object LocalDataManager {
    suspend fun clearAll(context: Context) {
        context.dataStore.edit { preferences -> preferences.clear() }
    }
}
