package com.example.tafmetar.mobile.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Client Retrofit vers l'API publique aviationweather.gov (NOAA).
 * Gratuite, sans clé d'API. Reprise ici depuis l'ancienne implémentation
 * qui vivait auparavant côté montre.
 */
interface AviationWeatherApi {

    @GET("api/data/metar")
    suspend fun getMetar(
        @Query("ids") icaoCodes: String, // ex: "LFBO,LFPG"
        @Query("format") format: String = "json"
    ): List<MetarDto>

    @GET("api/data/taf")
    suspend fun getTaf(
        @Query("ids") icaoCodes: String,
        @Query("format") format: String = "json"
    ): List<TafDto>
}

// DTOs reflétant (de façon simplifiée) le JSON retourné par l'API AWC.
// Gson ignore les champs absents du DTO : la réponse contient bien plus (vent, visibilité,
// température, QNH, nuages…), on ne déclare que ce qui est réellement affiché.
data class MetarDto(
    val icaoId: String?,
    val rawOb: String?,
    val obsTime: Long?        // epoch seconds
)

data class TafDto(
    val icaoId: String?,
    val rawTAF: String?,
    // ATTENTION : contrairement à `obsTime` du METAR qui est un nombre (epoch seconds), l'API
    // renvoie ici une chaîne ISO-8601 ("2026-07-27T11:00:00.000Z"). Déclarer ce champ en Long
    // fait échouer tout le parsing Gson de la réponse, donc plus aucun TAF n'est remonté.
    val issueTime: String?
)
