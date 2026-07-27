package com.example.tafmetar.wear.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tafmetar.shared.model.MetarReport
import com.example.tafmetar.shared.model.TafReport
import com.example.tafmetar.wear.cache.WatchCacheStore
import com.example.tafmetar.wear.datalayer.DataItemSync
import com.example.tafmetar.wear.datalayer.PhoneRequestSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StationUiState(
    val icao: String,
    val metar: MetarReport?,
    val taf: TafReport?
)

sealed class RefreshUiState {
    object Idle : RefreshUiState()
    object WaitingForPhone : RefreshUiState()
    object PhoneUnreachable : RefreshUiState()
}

/**
 * ViewModel côté montre : il ne fait QUE lire le cache local (alimenté par le Data Layer)
 * et déclencher des demandes de refresh via message. Zéro accès réseau direct.
 */
class StationViewModel(application: Application) : AndroidViewModel(application) {

    private val cache = WatchCacheStore(application)
    private val requestSender = PhoneRequestSender(application)

    private val _refreshState = MutableStateFlow<RefreshUiState>(RefreshUiState.Idle)
    val refreshState: StateFlow<RefreshUiState> = _refreshState

    init {
        // Complète l'écoute passive (WatchDataListenerService) par un pull actif au lancement :
        // si le système a empêché la livraison en arrière-plan pendant que l'app était fermée,
        // ce qui est déjà présent dans le Data Layer local est quand même récupéré ici.
        viewModelScope.launch { DataItemSync.syncAll(application) }
    }

    val favorites: StateFlow<List<String>> = cache.favorites
        .map { it.sorted() }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())

    fun stationState(icao: String): StateFlow<StationUiState> =
        combine(cache.metarFlow(icao), cache.tafFlow(icao)) { metar, taf ->
            StationUiState(icao, metar, taf)
        }.stateIn(
            viewModelScope,
            kotlinx.coroutines.flow.SharingStarted.Eagerly,
            StationUiState(icao, null, null)
        )

    fun refresh(icao: String) {
        viewModelScope.launch {
            _refreshState.value = RefreshUiState.WaitingForPhone
            val reachable = requestSender.isPhoneAppReachable()
            if (!reachable) {
                _refreshState.value = RefreshUiState.PhoneUnreachable
                return@launch
            }
            requestSender.requestRefresh(icao)
            // La mise à jour de l'UI se fera automatiquement via le Flow du cache
            // dès que le téléphone aura poussé la nouvelle donnée (pas d'attente bloquante ici).
            _refreshState.value = RefreshUiState.Idle
        }
    }
}
