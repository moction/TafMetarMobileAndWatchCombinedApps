package com.example.tafmetar.mobile.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tafmetar.mobile.cache.ReportStore
import com.example.tafmetar.mobile.datalayer.WatchSyncManager
import com.example.tafmetar.mobile.favorites.FavoritesStore
import com.example.tafmetar.mobile.repository.WeatherRepository
import com.example.tafmetar.mobile.widget.MetarWidgetProvider
import kotlinx.coroutines.launch

private const val TAG = "FavoritesViewModel"

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val favoritesStore = FavoritesStore(application)
    private val repository = WeatherRepository()
    private val syncManager = WatchSyncManager(application)
    private val reportStore = ReportStore(application)

    val favorites = favoritesStore.favorites

    fun addStation(icao: String) {
        viewModelScope.launch {
            favoritesStore.add(icao)
            pushFavoritesAndRefresh()
        }
    }

    fun removeStation(icao: String) {
        viewModelScope.launch {
            favoritesStore.remove(icao)
            pushFavoritesAndRefresh()
        }
    }

    private suspend fun pushFavoritesAndRefresh() {
        val current = favoritesStore.favorites
        current.collect { icaoSet ->
            syncManager.pushFavorites(icaoSet)
            // On ne propage pas l'échec (la montre garde son dernier cache, préférable à un écran
            // vide), mais on le journalise : un runCatching totalement muet a déjà masqué une
            // erreur de parsing qui supprimait silencieusement tous les TAF.
            runCatching { repository.fetchMetars(icaoSet.toList()) }
                .onSuccess { reports ->
                    reports.forEach {
                        syncManager.pushMetar(it)
                        reportStore.saveMetar(it) // alimente le widget
                    }
                }
                .onFailure { Log.w(TAG, "Récupération des METAR échouée", it) }
            runCatching { repository.fetchTafs(icaoSet.toList()) }
                .onSuccess { reports -> reports.forEach { syncManager.pushTaf(it) } }
                .onFailure { Log.w(TAG, "Récupération des TAF échouée", it) }

            reportStore.keepOnly(icaoSet)
            MetarWidgetProvider.requestUpdate(getApplication())
            return@collect
        }
    }
}
