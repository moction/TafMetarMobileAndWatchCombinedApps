package com.example.tafmetar.mobile.repository

import com.example.tafmetar.mobile.network.MetarDto
import com.example.tafmetar.mobile.network.RetrofitClient
import com.example.tafmetar.mobile.network.TafDto
import com.example.tafmetar.shared.model.MetarReport
import com.example.tafmetar.shared.model.TafReport
import java.time.Instant

/**
 * Point d'entrée unique pour récupérer les données météo depuis internet.
 * C'est la SEULE classe de toute l'application (mobile + wear) qui fait un appel réseau.
 *
 * Le décodage des bulletins est fait par l'API elle-même ; ici on se limite au pont entre la
 * forme réseau (DTO, champs nullables) et la forme partagée avec la montre (modèles non-nuls).
 */
class WeatherRepository {

    suspend fun fetchMetars(icaoCodes: List<String>): List<MetarReport> {
        if (icaoCodes.isEmpty()) return emptyList()
        return RetrofitClient.api.getMetar(icaoCodes.joinToString(",")).mapNotNull { it.toReportOrNull() }
    }

    suspend fun fetchTafs(icaoCodes: List<String>): List<TafReport> {
        if (icaoCodes.isEmpty()) return emptyList()
        return RetrofitClient.api.getTaf(icaoCodes.joinToString(",")).mapNotNull { it.toReportOrNull() }
    }

    private fun MetarDto.toReportOrNull(): MetarReport? {
        return MetarReport(
            icao = icaoId ?: return null,
            raw = rawOb ?: return null,
            observationEpochMillis = (obsTime ?: 0L) * 1000 // l'API donne des secondes
        )
    }

    private fun TafDto.toReportOrNull(): TafReport? {
        return TafReport(
            icao = icaoId ?: return null,
            raw = rawTAF ?: return null,
            // Contrairement au METAR, l'API renvoie ici une date ISO-8601 et non un epoch.
            // Une date illisible ne doit pas faire perdre le TAF : on retombe sur 0L.
            issueEpochMillis = issueTime
                ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
                ?: 0L
        )
    }
}
