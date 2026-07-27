package com.example.tafmetar.shared.model

/**
 * METAR d'une station, tel que poussé par l'app mobile vers la montre via le Data Layer.
 *
 * On ne transporte que le texte brut : la montre affiche le bulletin tel quel, et les champs
 * décodés (vent, visibilité, température, QNH, catégorie de vol) n'étaient lus par personne.
 * `observationEpochMillis` est conservé : c'est le seul moyen programmatique de détecter une
 * donnée périmée, sans avoir à ré-analyser le groupe date/heure du texte brut.
 */
data class MetarReport(
    val icao: String,
    val raw: String,
    val observationEpochMillis: Long
)
