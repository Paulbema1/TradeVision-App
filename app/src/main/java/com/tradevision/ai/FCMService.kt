package com.tradevision.ai

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tradevision.ai.data.network.ApiClient
import com.tradevision.ai.data.model.FcmTokenRequest
import com.tradevision.ai.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to backend
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = ApiClient.getApiService(applicationContext)
                api.updateFcmToken(FcmTokenRequest(token))
            } catch (e: Exception) {
                Log.w("FCMService", "Failed to send token to backend: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Prefer data payload
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val signalId = data["signal_id"]
            val symbol = data["symbol"] ?: ""
            val action = data["action"] ?: "WAIT"
            val confidence = data["confidence"]?.toIntOrNull() ?: 0
            val entry = data["entry_price"]?.toDoubleOrNull()
            val sl = data["stop_loss"]?.toDoubleOrNull()
            val tp1 = data["take_profit_1"]?.toDoubleOrNull()

            try {
                val session = com.tradevision.ai.data.network.SessionManager(applicationContext)
                if (!signalId.isNullOrEmpty()) {
                    if (session.hasSeenSignal(signalId)) {
                        // already processed
                        return
                    }
                    session.markSignalSeen(signalId)
                }

                // Show notification
                NotificationHelper.showSignalNotification(
                    context = applicationContext,
                    symbol = symbol,
                    action = action,
                    confidence = confidence,
                    entry = entry,
                    sl = sl,
                    tp1 = tp1,
                    signalId = signalId
                )
            } catch (e: Exception) {
                Log.w("FCMService", "Error handling message: ${e.message}")
            }
        }
    }
}
