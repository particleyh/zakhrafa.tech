package com.zakhrafa.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

object FavoritesManager {
    private val FAVORITES_KEY = stringSetPreferencesKey("fav_list")

    fun getFavorites(context: Context): Flow<Set<String>> {
        return context.dataStore.data.map { preferences ->
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
}
