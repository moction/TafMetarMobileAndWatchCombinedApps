package com.example.tafmetar.mobile.favorites

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "favorites")

/**
 * Persiste la liste des stations favorites côté téléphone.
 * C'est la source de vérité : la montre ne fait que recevoir une copie via le Data Layer.
 */
class FavoritesStore(private val context: Context) {

    private val key = stringSetPreferencesKey("icao_codes")

    val favorites: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[key] ?: setOf("LFBO") // station par défaut au premier lancement
    }

    suspend fun add(icao: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[key] ?: setOf("LFBO")
            prefs[key] = current + icao.uppercase()
        }
    }

    suspend fun remove(icao: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[key] ?: emptySet()
            prefs[key] = current - icao.uppercase()
        }
    }
}
