package com.example.tafmetar.mobile.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.tafmetar.mobile.MainActivity
import com.example.tafmetar.mobile.R
import com.example.tafmetar.mobile.cache.ReportStore
import com.example.tafmetar.mobile.favorites.FavoritesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Widget d'écran d'accueil affichant le dernier METAR de la première station suivie.
 *
 * Il ne fait aucun appel réseau : il lit le cache alimenté par RefreshWorker et par l'app.
 * Un widget peut être redessiné à tout moment par le lanceur, y compris hors connexion.
 */
class MetarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // onUpdate s'exécute sur le thread principal d'un BroadcastReceiver, dont le process
        // peut être tué dès le retour de la méthode : goAsync() maintient le process vivant
        // le temps de la lecture DataStore.
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val (icao, metar) = loadFirstStation(context)
                appWidgetIds.forEach { id ->
                    render(context, appWidgetManager, id, icao, metar)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun loadFirstStation(context: Context): Pair<String?, String?> {
        // `sorted().first()` : même station que la première ligne de l'app, dont la liste est
        // triée. Sur un Set brut, l'ordre serait indéterminé.
        val icao = FavoritesStore(context).favorites.first().sorted().firstOrNull()
            ?: return null to null
        return icao to ReportStore(context).metarRaw(icao)
    }

    private fun render(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        icao: String?,
        metarRaw: String?
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_metar).apply {
            setTextViewText(R.id.widget_icao, icao ?: context.getString(R.string.widget_no_station))
            setTextViewText(
                R.id.widget_metar,
                when {
                    icao == null -> ""
                    metarRaw == null -> context.getString(R.string.widget_no_data)
                    else -> metarRaw
                }
            )
            setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        }
        manager.updateAppWidget(widgetId, views)
    }

    private fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        /**
         * Redemande le dessin de toutes les instances du widget. À appeler après chaque
         * écriture du cache, sinon le widget garde la donnée précédente jusqu'au prochain
         * cycle updatePeriodMillis (30 min).
         */
        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, MetarWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return

            context.sendBroadcast(
                Intent(context, MetarWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
            )
        }
    }
}
