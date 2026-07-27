package com.example.tafmetar.wear.datalayer

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import androidx.wear.tiles.TileService
import com.example.tafmetar.shared.model.MetarReport
import com.example.tafmetar.shared.model.TafReport
import com.example.tafmetar.wear.cache.WatchCacheStore
import com.example.tafmetar.wear.tile.MetarTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Reçoit les DataItem poussés par l'app mobile (nouveau METAR/TAF, mise à jour des favoris)
 * et les écrit simplement dans le cache local. C'est la SEULE classe côté montre qui touche
 * au Data Layer en réception — l'UI, elle, ne fait qu'observer le cache (Flow).
 *
 * Aucun appel réseau ici : c'est exactement le but recherché, alléger la montre.
 */
class WatchDataListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val cache = WatchCacheStore(applicationContext)

        // Le buffer n'est valide que pendant ce callback : on extrait tout avant de lancer
        // la moindre coroutine.
        val metars = mutableListOf<MetarReport>()
        val tafs = mutableListOf<TafReport>()
        var favorites: Set<String>? = null

        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val map = DataMapItem.fromDataItem(event.dataItem).dataMap
            val path = event.dataItem.uri.path ?: continue

            when {
                path.startsWith("/metar/") -> DataItemSync.parseMetar(map)?.let { metars += it }
                path.startsWith("/taf/") -> DataItemSync.parseTaf(map)?.let { tafs += it }
                path == "/favorites" -> favorites = DataItemSync.parseFavorites(map)
            }
        }

        if (metars.isEmpty() && tafs.isEmpty() && favorites == null) return

        // Une seule coroutine, donc un ordre garanti : lancer une coroutine par événement
        // laissait la purge de saveFavorites s'exécuter avant les saveMetar/saveTaf du même
        // lot, ce qui pouvait effacer une donnée qui venait d'arriver.
        scope.launch {
            metars.forEach { cache.saveMetar(it) }
            tafs.forEach { cache.saveTaf(it) }
            favorites?.let { cache.saveFavorites(it) }

            // La tuile ne relit pas le cache d'elle-même : sans cette demande, elle afficherait
            // la donnée précédente jusqu'au prochain rafraîchissement décidé par le système.
            runCatching {
                TileService.getUpdater(applicationContext)
                    .requestUpdate(MetarTileService::class.java)
            }
        }
    }
}
