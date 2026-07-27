package com.example.tafmetar.shared.datalayer

/**
 * Contrat partagé entre l'app mobile et l'app Wear OS pour le Wearable Data Layer.
 * Les DEUX apps doivent utiliser exactement les mêmes chemins/clés, sinon la synchro échoue
 * silencieusement (aucune erreur, juste rien ne se passe).
 */
object DataLayerPaths {
    /** DataItem : "/metar/{icao}" -> dernier METAR connu pour une station. */
    fun metarPath(icao: String) = "/metar/${icao.uppercase()}"

    /** DataItem : "/taf/{icao}" -> dernier TAF connu pour une station. */
    fun tafPath(icao: String) = "/taf/${icao.uppercase()}"

    /** DataItem : "/favorites" -> liste des codes OACI suivis, partagée entre les deux apps. */
    const val FAVORITES_PATH = "/favorites"

    /** Message : la montre demande au téléphone de rafraîchir une station précise. */
    const val MESSAGE_REQUEST_REFRESH = "/request-refresh"

    /** Message : la montre demande un refresh de TOUTES les stations favorites. */
    const val MESSAGE_REQUEST_REFRESH_ALL = "/request-refresh-all"
}

object DataLayerKeys {
    // Clés utilisées dans les DataMap des DataItems metar/taf.
    // Seul le texte brut est transporté : voir le commentaire de MetarReport.
    const val ICAO = "icao"
    const val RAW = "raw"
    const val TIMESTAMP = "timestamp"

    // Clé utilisée dans la DataMap "/favorites"
    const val ICAO_LIST = "icao_list" // stocké en String, séparateur virgule
}
