package com.example.tafmetar.mobile.datalayer

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.example.tafmetar.mobile.favorites.FavoritesStore
import com.example.tafmetar.mobile.repository.WeatherRepository
import com.example.tafmetar.shared.datalayer.DataLayerPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Écoute les messages envoyés par la montre (ex. "rafraîchis la station LFPG maintenant").
 * Tourne indépendamment de l'UI : peut réagir même si MainActivity n'est pas au premier plan.
 */
private const val TAG = "PhoneMessageListener"

class PhoneMessageListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository = WeatherRepository()

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            DataLayerPaths.MESSAGE_REQUEST_REFRESH -> {
                val icao = String(event.data)
                scope.launch { refreshAndPush(listOf(icao)) }
            }
            DataLayerPaths.MESSAGE_REQUEST_REFRESH_ALL -> {
                scope.launch {
                    val favorites = FavoritesStore(applicationContext).favorites
                    // On prend le premier snapshot disponible du Flow de favoris
                    favorites.collect { icaoSet ->
                        refreshAndPush(icaoSet.toList())
                        return@collect
                    }
                }
            }
        }
    }

    private suspend fun refreshAndPush(icaoCodes: List<String>) {
        val syncManager = WatchSyncManager(applicationContext)
        runCatching { repository.fetchMetars(icaoCodes) }
            .onSuccess { reports -> reports.forEach { syncManager.pushMetar(it) } }
            .onFailure { Log.w(TAG, "Récupération des METAR échouée", it) }
        runCatching { repository.fetchTafs(icaoCodes) }
            .onSuccess { reports -> reports.forEach { syncManager.pushTaf(it) } }
            .onFailure { Log.w(TAG, "Récupération des TAF échouée", it) }
        // En cas d'échec réseau, on ne pousse rien : la montre garde alors son dernier cache connu,
        // ce qui est préférable à un écran vide.
    }
}
