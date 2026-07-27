package com.example.tafmetar.mobile.cache

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.tafmetar.shared.model.MetarReport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.reportDataStore by preferencesDataStore(name = "reports")

/**
 * Dernier METAR connu par station, côté téléphone.
 *
 * L'app mobile se contentait jusqu'ici de récupérer les relevés puis de les pousser vers la
 * montre sans rien garder : le widget, qui ne peut pas déclencher d'appel réseau à chaque
 * redessin, n'aurait donc eu aucune source à lire. Ce cache comble ce manque.
 *
 * Seul le METAR est stocké : c'est ce qu'affiche le widget, un TAF étant trop long pour lui.
 */
class ReportStore(private val context: Context) {

    private fun metarKey(icao: String) = stringPreferencesKey("metar_${icao.uppercase()}")

    suspend fun saveMetar(report: MetarReport) {
        context.reportDataStore.edit { prefs -> prefs[metarKey(report.icao)] = report.raw }
    }

    suspend fun metarRaw(icao: String): String? =
        context.reportDataStore.data.map { it[metarKey(icao)] }.first()

    /** Purge les relevés des stations qui ne sont plus suivies. */
    suspend fun keepOnly(icaoCodes: Set<String>) {
        val keep = icaoCodes.map { it.uppercase() }.toSet()
        context.reportDataStore.edit { prefs ->
            val obsolete = prefs.asMap().keys
                .map { it.name }
                .filter { it.startsWith("metar_") && it.removePrefix("metar_") !in keep }
            obsolete.forEach { prefs.remove(stringPreferencesKey(it)) }
        }
    }
}
