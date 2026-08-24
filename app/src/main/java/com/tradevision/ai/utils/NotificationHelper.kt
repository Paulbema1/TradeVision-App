package com.tradevision.ai.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tradevision.ai.R

object NotificationHelper {

    private const val CHANNEL_ID = "tradevision_signals_channel"
    private const val CHANNEL_NAME = "Alertes Signaux TradeVision AI"

    fun showSignalNotification(
        context: Context,
        symbol: String,
        action: String,
        confidence: Int,
        entry: Double?,
        sl: Double?,
        tp1: Double?
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Création du canal de notification pour Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications prioritaires pour les signaux de trading BUY/SELL"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val actionEmoji = if (action == "BUY") "🟢 BUY" else "🔴 SELL"
        val title = "$actionEmoji — $symbol ($confidence%)"
        val body = "Entrée: ${entry ?: 0.0} | SL: ${sl ?: 0.0} | TP1: ${tp1 ?: 0.0}"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
