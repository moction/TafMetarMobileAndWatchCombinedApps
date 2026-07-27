package com.example.tafmetar.wear.presentation

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.tafmetar.wear.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun StationDetailScreen(icao: String, viewModel: StationViewModel) {
    val state by viewModel.stationState(icao).collectAsState()
    val refreshState by viewModel.refreshState.collectAsState()

    val listState = rememberScalingLazyListState()
    // La bague rotative (et la couronne) émettent des événements "rotary" qui ne sont délivrés
    // qu'au composant ayant le focus — d'où le focusRequester + focusable ci-dessous.
    // rememberActiveFocusRequester coopère avec le HierarchicalFocusCoordinator du pager pour
    // que seule la page affichée capte la rotation.
    val focusRequester = rememberActiveFocusRequester()
    val scope = rememberCoroutineScope()

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .onRotaryScrollEvent { event ->
                // scrollBy et non animateScrollBy : la bague est crantée, on veut un suivi
                // immédiat du doigt plutôt qu'une animation qui traîne derrière.
                scope.launch { listState.scrollBy(event.verticalScrollPixels) }
                true // événement consommé
            }
            .focusRequester(focusRequester)
            .focusable(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 24.dp)
    ) {
        item {
            Text(icao, style = MaterialTheme.typography.title2)
        }

        item { Spacer(Modifier.height(8.dp)) }

        // Pas de titre "METAR"/"TAF" : les bulletins bruts commencent déjà par ce mot.
        item {
            if (state.metar != null) {
                Text(state.metar!!.raw, style = MaterialTheme.typography.body2)
            } else {
                Text("Aucune donnée METAR en cache.", style = MaterialTheme.typography.body2)
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        item {
            if (state.taf != null) {
                Text(state.taf!!.raw, style = MaterialTheme.typography.body2)
            } else {
                Text("Aucune donnée TAF en cache.", style = MaterialTheme.typography.body2)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        item {
            val waiting = refreshState is RefreshUiState.WaitingForPhone

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Le Button de Wear Compose est circulaire par défaut : il porte donc une icône
                // sans mise en forme supplémentaire.
                Button(
                    onClick = { viewModel.refresh(icao) },
                    enabled = !waiting
                ) {
                    if (waiting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ButtonDefaults.DefaultIconSize),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            // Le libellé disparaît visuellement mais reste indispensable pour
                            // l'accessibilité (TalkBack).
                            contentDescription = "Rafraîchir",
                            modifier = Modifier.size(ButtonDefaults.DefaultIconSize)
                        )
                    }
                }

                // L'icône seule ne peut pas exprimer cet échec : on garde le message, sinon
                // l'utilisateur n'a plus aucun retour quand le téléphone ne répond pas.
                if (refreshState is RefreshUiState.PhoneUnreachable) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Téléphone injoignable",
                        style = MaterialTheme.typography.caption2,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

