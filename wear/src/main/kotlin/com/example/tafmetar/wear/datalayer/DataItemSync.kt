package com.example.tafmetar.wear.datalayer

import android.content.Context
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.example.tafmetar.shared.datalayer.DataLayerKeys
import com.example.tafmetar.shared.model.MetarReport
import com.example.tafmetar.shared.model.TafReport
import com.example.tafmetar.wear.cache.WatchCacheStore
import kotlinx.coroutines.tasks.await

/**
 * Parsing des DataMap partagé entre la réception passive (WatchDataListenerService, via
 * BIND_LISTENER/DataItem ou Message) et la synchronisation active au démarrage de l'app
 * (syncAll, DataItem uniquement).
 *
 * syncAll() reste utile en complément du canal Message : sur les appareils où le DataClient
 * fonctionne normalement, il permet de retrouver au lancement ce qui a été poussé pendant que
 * l'app n'était pas en cours d'exécution.
 */
object DataItemSync {

    fun parseMetar(map: DataMap): MetarReport? {
        val icao = map.getString(DataLayerKeys.ICAO, "")
        if (icao.isBlank()) return null
        return MetarReport(
            icao = icao,
            raw = map.getString(DataLayerKeys.RAW, ""),
            observationEpochMillis = map.getLong(DataLayerKeys.TIMESTAMP, 0L)
        )
    }

    fun parseTaf(map: DataMap): TafReport? {
        val icao = map.getString(DataLayerKeys.ICAO, "")
        if (icao.isBlank()) return null
        return TafReport(
            icao = icao,
            raw = map.getString(DataLayerKeys.RAW, ""),
            issueEpochMillis = map.getLong(DataLayerKeys.TIMESTAMP, 0L)
        )
    }

    fun parseFavorites(map: DataMap): Set<String> =
        map.getString(DataLayerKeys.ICAO_LIST, "")
            .split(",")
            .filter { it.isNotBlank() }
            .toSet()

    /** Récupère tout ce que le Data Layer local connaît déjà et remplit le cache en conséquence. */
    suspend fun syncAll(context: Context) {
        val cache = WatchCacheStore(context)
        val buffer = Wearable.getDataClient(context).dataItems.await()

        val metars = mutableListOf<MetarReport>()
        val tafs = mutableListOf<TafReport>()
        var favorites: Set<String>? = null

        try {
            for (item in buffer) {
                val map = DataMapItem.fromDataItem(item).dataMap
                val path = item.uri.path ?: continue
                when {
                    path.startsWith("/metar/") -> parseMetar(map)?.let { metars += it }
                    path.startsWith("/taf/") -> parseTaf(map)?.let { tafs += it }
                    path == "/favorites" -> favorites = parseFavorites(map)
                }
            }
        } finally {
            buffer.release()
        }

        metars.forEach { cache.saveMetar(it) }
        tafs.forEach { cache.saveTaf(it) }
        // Les favoris sont appliqués en DERNIER : saveFavorites purge les stations non suivies,
        // et l'ordre de parcours des DataItem n'est pas garanti — appliqués en cours de boucle,
        // ils pourraient purger avant l'import et laisser des entrées obsolètes.
        favorites?.let { cache.saveFavorites(it) }
    }
}
