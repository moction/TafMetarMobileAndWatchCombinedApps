package com.example.tafmetar.wear.datalayer

import android.content.Context
import com.google.android.gms.wearable.Wearable
import com.example.tafmetar.shared.datalayer.DataLayerPaths
import kotlinx.coroutines.tasks.await

/**
 * Envoie une demande de rafraîchissement au téléphone (bouton "refresh" côté montre).
 * La montre ne fait jamais l'appel réseau elle-même : elle délègue systématiquement au téléphone.
 */
class PhoneRequestSender(private val context: Context) {

    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    /** @return true si un téléphone joignable a été trouvé et le message envoyé. */
    suspend fun requestRefresh(icao: String): Boolean {
        val node = nodeClient.connectedNodes.await().firstOrNull() ?: return false
        messageClient.sendMessage(node.id, DataLayerPaths.MESSAGE_REQUEST_REFRESH, icao.toByteArray()).await()
        return true
    }

    suspend fun requestRefreshAll(): Boolean {
        val node = nodeClient.connectedNodes.await().firstOrNull() ?: return false
        messageClient.sendMessage(node.id, DataLayerPaths.MESSAGE_REQUEST_REFRESH_ALL, ByteArray(0)).await()
        return true
    }

    /** Utile pour afficher "Ouvrez l'app sur votre téléphone" si aucun nœud n'est joignable. */
    suspend fun isPhoneAppReachable(): Boolean =
        nodeClient.connectedNodes.await().isNotEmpty()
}
