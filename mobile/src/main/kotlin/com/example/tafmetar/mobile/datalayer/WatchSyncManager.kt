package com.example.tafmetar.mobile.datalayer

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.example.tafmetar.shared.datalayer.DataLayerKeys
import com.example.tafmetar.shared.datalayer.DataLayerPaths
import com.example.tafmetar.shared.model.MetarReport
import com.example.tafmetar.shared.model.TafReport
import kotlinx.coroutines.tasks.await

/**
 * Pousse les données météo récupérées depuis internet vers la montre via le DataClient.
 * Chaque station a son propre DataItem ("/metar/LFBO", "/taf/LFBO", ...) afin que la montre
 * ne reçoive que ce qui a changé, sans retransmettre tout l'historique à chaque update.
 *
 * Rappel : la livraison ne fonctionne que parce que les modules :mobile et :wear partagent le
 * même applicationId et la même clé de signature (le Data Layer route par ce couple).
 */
class WatchSyncManager(context: Context) {

    private val dataClient = Wearable.getDataClient(context)

    suspend fun pushMetar(report: MetarReport) {
        val request = PutDataMapRequest.create(DataLayerPaths.metarPath(report.icao)).apply {
            dataMap.putString(DataLayerKeys.ICAO, report.icao)
            dataMap.putString(DataLayerKeys.RAW, report.raw)
            dataMap.putLong(DataLayerKeys.TIMESTAMP, report.observationEpochMillis)
        }.setUrgent() // priorité de livraison élevée : on veut que ça arrive vite sur la montre
            .asPutDataRequest()

        dataClient.putDataItem(request).await()
    }

    suspend fun pushTaf(report: TafReport) {
        val request = PutDataMapRequest.create(DataLayerPaths.tafPath(report.icao)).apply {
            dataMap.putString(DataLayerKeys.ICAO, report.icao)
            dataMap.putString(DataLayerKeys.RAW, report.raw)
            dataMap.putLong(DataLayerKeys.TIMESTAMP, report.issueEpochMillis)
        }.setUrgent()
            .asPutDataRequest()

        dataClient.putDataItem(request).await()
    }

    suspend fun pushFavorites(icaoCodes: Set<String>) {
        val request = PutDataMapRequest.create(DataLayerPaths.FAVORITES_PATH).apply {
            dataMap.putString(DataLayerKeys.ICAO_LIST, icaoCodes.joinToString(","))
        }.setUrgent()
            .asPutDataRequest()

        dataClient.putDataItem(request).await()
        deleteStationsNotIn(icaoCodes)
    }

    /**
     * Supprime les DataItem des stations qui ne sont plus suivies.
     *
     * Un DataItem persiste dans le Data Layer jusqu'à suppression explicite : sans ce nettoyage,
     * une station retirée des favoris y resterait indéfiniment et la montre la réimporterait
     * dans son cache à chaque syncAll.
     *
     * On réconcilie sur l'ensemble des favoris plutôt que de supprimer au coup par coup lors du
     * retrait : cela rattrape aussi les stations retirées pendant que l'app ne tournait pas, ainsi
     * que les résidus d'anciennes versions.
     */
    private suspend fun deleteStationsNotIn(icaoCodes: Set<String>) {
        val keep = icaoCodes.map { it.uppercase() }.toSet()

        val buffer = dataClient.dataItems.await()
        // Les DataItem deviennent invalides après release() : on copie les URI en String d'abord.
        val obsolete = try {
            buffer.mapNotNull { item ->
                val path = item.uri.path ?: return@mapNotNull null
                val icao = when {
                    path.startsWith("/metar/") -> path.removePrefix("/metar/")
                    path.startsWith("/taf/") -> path.removePrefix("/taf/")
                    else -> return@mapNotNull null
                }
                item.uri.toString().takeIf { icao.uppercase() !in keep }
            }
        } finally {
            buffer.release()
        }

        // Un échec de suppression ne doit pas faire échouer la mise à jour des favoris.
        obsolete.forEach { uri ->
            runCatching { dataClient.deleteDataItems(Uri.parse(uri)).await() }
        }
    }
}
