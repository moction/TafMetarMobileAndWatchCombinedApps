package com.example.tafmetar.wear.tile

import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.example.tafmetar.wear.cache.WatchCacheStore
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val RESOURCES_VERSION = "1"

/** Le système re-sollicite la tuile à cet intervalle ; un METAR est réémis toutes les 30 min. */
private const val FRESHNESS_MILLIS = 15 * 60 * 1000L

/**
 * Tuile Wear OS affichant la première station favorite directement sur l'écran d'accueil,
 * sans ouvrir l'app. Lit uniquement le cache local (DataStore) : aucun appel réseau,
 * la tuile s'affiche donc instantanément même hors connectivité.
 */
class MetarTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onTileRequest(
        requestParams: androidx.wear.tiles.RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        // Les callbacks de TileService arrivent sur le thread principal : la lecture du DataStore
        // (I/O disque) est donc faite sur Dispatchers.IO et le résultat rendu via un Future,
        // plutôt qu'avec un runBlocking qui bloquerait l'UI.
        val future = SettableFuture.create<TileBuilders.Tile>()
        scope.launch {
            runCatching { buildTile() }
                .onSuccess { future.set(it) }
                .onFailure { future.setException(it) }
        }
        return future
    }

    override fun onTileResourcesRequest(
        requestParams: androidx.wear.tiles.RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(
            ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
        )

    private suspend fun buildTile(): TileBuilders.Tile {
        val cache = WatchCacheStore(applicationContext)
        // `sorted().first()` et non `first()` sur le Set : même station que la première page de
        // l'app, dont le pager trie aussi les favoris. Sinon l'ordre serait indéterminé.
        val icao = cache.favorites.first().sorted().firstOrNull()
        val metar = icao?.let { cache.metarFlow(it).first() }

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(FRESHNESS_MILLIS)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout(icao, metar?.raw)))
            .build()
    }

    private fun layout(icao: String?, raw: String?): LayoutElementBuilders.LayoutElement {
        val column = LayoutElementBuilders.Column.Builder()
            .setWidth(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setPadding(
                        ModifiersBuilders.Padding.Builder().setAll(dp(14f)).build()
                    )
                    .build()
            )

        if (icao == null) {
            return column.addContent(body("Aucune station favorite")).build()
        }

        return column
            .addContent(title(icao))
            .addContent(spacer(6f))
            .addContent(body(raw ?: "Pas de données en cache"))
            .build()
    }

    private fun title(text: String) = LayoutElementBuilders.Text.Builder()
        .setText(text)
        .setFontStyle(
            LayoutElementBuilders.FontStyle.Builder()
                .setSize(sp(22f))
                .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                .setColor(argb(0xFFFFFFFF.toInt()))
                .build()
        )
        .build()

    private fun body(text: String) = LayoutElementBuilders.Text.Builder()
        .setText(text)
        // Un METAR brut dépasse largement l'écran : on tronque proprement plutôt que de
        // laisser le rendu déborder.
        .setMaxLines(5)
        // Dépréciée, mais c'est la seule option d'ellipse en protolayout 1.0.0
        // (TEXT_OVERFLOW_ELLIPSIZE n'existe qu'à partir de 1.2). L'alternative non dépréciée,
        // TEXT_OVERFLOW_TRUNCATE, coupe sans "…".
        .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE_END)
        .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
        .setFontStyle(
            LayoutElementBuilders.FontStyle.Builder()
                .setSize(sp(13f))
                .setColor(argb(0xFFBBBBBB.toInt()))
                .build()
        )
        .build()

    private fun spacer(height: Float) = LayoutElementBuilders.Spacer.Builder()
        .setHeight(dp(height))
        .build()
}
