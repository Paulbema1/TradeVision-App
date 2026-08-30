package com.tradevision.ai.receivers

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Copie l'ensemble des niveaux (Entry/SL/TP1) d'un signal dans le presse-papiers
 * depuis l'action "Copier les niveaux" de la notification, sans ouvrir l'app.
 *
 * Répond à l'exigence mobile du cahier des charges v9.1.0 :
 * "Copy ALL LEVELS button in UI" / notification actions (Open signal, Copy levels).
 */
class CopyLevelsReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_SYMBOL = "extra_symbol"
        const val EXTRA_ACTION = "extra_action"
        const val EXTRA_ENTRY = "extra_entry"
        const val EXTRA_SL = "extra_sl"
        const val EXTRA_TP1 = "extra_tp1"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val symbol = intent.getStringExtra(EXTRA_SYMBOL) ?: ""
        val action = intent.getStringExtra(EXTRA_ACTION) ?: ""
        val entry = intent.getStringExtra(EXTRA_ENTRY) ?: "-"
        val sl = intent.getStringExtra(EXTRA_SL) ?: "-"
        val tp1 = intent.getStringExtra(EXTRA_TP1) ?: "-"

        val text = "$symbol $action\nEntrée: $entry\nSL: $sl\nTP1: $tp1"

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("TradeVision signal levels", text))

        Toast.makeText(context, "Niveaux copiés", Toast.LENGTH_SHORT).show()
    }
}
