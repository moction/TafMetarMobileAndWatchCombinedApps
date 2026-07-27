package com.example.tafmetar.shared.model

/**
 * TAF d'une station. On ne pousse ici que le texte brut formaté : le décodage complet
 * d'un TAF (groupes FM/BECMG/TEMPO) est plus complexe et n'apporte pas grand-chose
 * de plus qu'un affichage bien formaté sur un petit écran de montre.
 */
data class TafReport(
    val icao: String,
    val raw: String,
    val issueEpochMillis: Long
)
