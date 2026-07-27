package com.example.tafmetar.wear.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.HierarchicalFocusCoordinator
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * Écran principal : un pager horizontal, une page par station favorite.
 * Toutes les données viennent du cache local (donc affichage instantané, sans spinner réseau).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalWearFoundationApi::class)
@Composable
fun StationPagerScreen(viewModel: StationViewModel = viewModel()) {
    val favorites by viewModel.favorites.collectAsState()

    if (favorites.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Aucune station favorite.\nAjoutez-en une depuis l'app sur votre téléphone.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body2
            )
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { favorites.size })
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        // Le pager compose aussi les pages voisines. Sans ce coordinateur, chacune réclamerait
        // le focus et la bague ferait défiler une page hors écran : on désigne explicitement la
        // page courante comme seule destinataire des événements rotatifs.
        HierarchicalFocusCoordinator(requiresFocus = { pagerState.currentPage == page }) {
            StationDetailScreen(icao = favorites[page], viewModel = viewModel)
        }
    }
}
