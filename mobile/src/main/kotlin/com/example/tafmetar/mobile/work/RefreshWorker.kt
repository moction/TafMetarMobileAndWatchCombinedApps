package com.example.tafmetar.mobile.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tafmetar.mobile.cache.ReportStore
import com.example.tafmetar.mobile.datalayer.WatchSyncManager
import com.example.tafmetar.mobile.widget.MetarWidgetProvider
import com.example.tafmetar.mobile.favorites.FavoritesStore
import com.example.tafmetar.mobile.repository.WeatherRepository
import kotlinx.coroutines.flow.first

/**
 * Rafraîchit périodiquement les stations favorites depuis internet et pousse le résultat
 * vers la montre. C'est ce worker qui remplace l'ancien polling fait directement par la montre.
 */
class RefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val icaoCodes = FavoritesStore(applicationContext).favorites.first().toList()
        if (icaoCodes.isEmpty()) return Result.success()

        val repository = WeatherRepository()
        val syncManager = WatchSyncManager(applicationContext)

        val reportStore = ReportStore(applicationContext)

        return try {
            repository.fetchMetars(icaoCodes).forEach {
                syncManager.pushMetar(it)
                reportStore.saveMetar(it) // alimente le widget
            }
            repository.fetchTafs(icaoCodes).forEach { syncManager.pushTaf(it) }
            reportStore.keepOnly(icaoCodes.toSet())
            MetarWidgetProvider.requestUpdate(applicationContext)
            Result.success()
        } catch (e: Exception) {
            // Réseau indisponible ou API en erreur : on retente au prochain cycle WorkManager,
            // pas besoin de retry agressif ici, ça consommerait de la batterie pour peu de gain.
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "refresh_weather_periodic"
    }
}
