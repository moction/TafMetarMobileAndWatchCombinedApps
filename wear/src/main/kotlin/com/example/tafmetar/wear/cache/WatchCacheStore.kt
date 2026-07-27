package com.example.tafmetar.wear.cache

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.tafmetar.shared.model.MetarReport
import com.example.tafmetar.shared.model.TafReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "watch_cache")

/**
 * Cache local très simple côté montre : permet d'afficher instantanément la dernière donnée
 * connue au démarrage de l'app, avant même qu'une synchro Data Layer n'ait eu lieu (par exemple
 * juste après un redémarrage de la montre). Aucun appel réseau n'est fait ici.
 */
class WatchCacheStore(private val context: Context) {

    private fun metarKey(icao: String) = stringPreferencesKey("metar_$icao")
    private fun tafKey(icao: String) = stringPreferencesKey("taf_$icao")
    private val favoritesKey = stringSetPreferencesKey("favorites")

    val favorites: Flow<Set<String>> = context.dataStore.data.map { it[favoritesKey] ?: emptySet() }

    fun metarFlow(icao: String): Flow<MetarReport?> =
        context.dataStore.data.map { prefs -> prefs[metarKey(icao)]?.let { deserializeMetar(it) } }

    fun tafFlow(icao: String): Flow<TafReport?> =
        context.dataStore.data.map { prefs -> prefs[tafKey(icao)]?.let { deserializeTaf(it) } }

    suspend fun saveMetar(report: MetarReport) {
        context.dataStore.edit { prefs -> prefs[metarKey(report.icao)] = serialize(report) }
    }

    suspend fun saveTaf(report: TafReport) {
        context.dataStore.edit { prefs -> prefs[tafKey(report.icao)] = serialize(report) }
    }

    /**
     * Enregistre la liste des favoris ET purge les METAR/TAF des stations qui n'en font plus
     * partie : la liste des favoris est la source de vérité de ce qui doit rester en cache.
     * Les deux opérations sont dans la même transaction `edit`, donc jamais d'état incohérent.
     */
    suspend fun saveFavorites(icaoCodes: Set<String>) {
        val keep = icaoCodes.map { it.uppercase() }.toSet()
        context.dataStore.edit { prefs ->
            prefs[favoritesKey] = icaoCodes

            // `map` produit une nouvelle liste : l'itération est terminée avant les remove,
            // donc pas de ConcurrentModificationException.
            val obsolete = prefs.asMap().keys
                .map { it.name }
                .filter { name ->
                    (name.startsWith("metar_") || name.startsWith("taf_")) &&
                        name.substringAfter('_').uppercase() !in keep
                }
            obsolete.forEach { prefs.remove(stringPreferencesKey(it)) }
        }
    }

    private fun serialize(r: MetarReport): String = JSONObject().apply {
        put("icao", r.icao); put("raw", r.raw); put("ts", r.observationEpochMillis)
    }.toString()

    private fun serialize(r: TafReport): String = JSONObject().apply {
        put("icao", r.icao); put("raw", r.raw); put("ts", r.issueEpochMillis)
    }.toString()

    private fun deserializeMetar(json: String): MetarReport {
        val o = JSONObject(json)
        return MetarReport(
            icao = o.getString("icao"),
            raw = o.getString("raw"),
            observationEpochMillis = o.getLong("ts")
        )
    }

    private fun deserializeTaf(json: String): TafReport {
        val o = JSONObject(json)
        return TafReport(icao = o.getString("icao"), raw = o.getString("raw"), issueEpochMillis = o.getLong("ts"))
    }
}
