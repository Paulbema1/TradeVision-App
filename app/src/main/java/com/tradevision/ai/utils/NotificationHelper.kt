package com.tradevision.ai.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tradevision.ai.R
import java.util.Locale

object PriceFormatter {
    // Precision map per instrument
    private val precisionMap = mapOf(
        "EUR/USD" to 5,
        "GBP/USD" to 5,
        "USD/JPY" to 3,
        "XAU/USD" to 2
    )

    fun format(value: Double?, symbol: String?): String {
        if (value == null) return "-"
        val precision = precisionMap[symbol] ?: 5
        val pattern = "%.${precision}f"
        return String.format(Locale.US, pattern, value)
    }
}

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
        tp1: Double?,
        signalId: String? = null
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

        val entryStr = PriceFormatter.format(entry, symbol)
        val slStr = PriceFormatter.format(sl, symbol)
        val tp1Str = PriceFormatter.format(tp1, symbol)

        val body = "Entrée: $entryStr | SL: $slStr | TP1: $tp1Str"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.tradevision_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        // Use signalId-based notification id if present to avoid duplicates; fall back to timestamp
        val notifId = signalId?.hashCode() ?: ((System.currentTimeMillis() / 1000L).toInt())
        notificationManager.notify(notifId, builder.build())
    }
}
