package com.tradevision.ai.utils

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Capteur de crash minimal, sans dépendance externe ni besoin de logcat/adb.
 *
 * Installe un gestionnaire global qui intercepte tout crash non rattrapé,
 * sauvegarde la trace complète en local (SharedPreferences), puis relance
 * proprement l'app. Au prochain lancement, MainActivity affiche cette trace
 * dans une boîte de dialogue copiable — permet de diagnostiquer un crash
 * sans connexion USB/PC, juste en rouvrant l'app après le plantage.
 */
object CrashLogger {

    private const val PREFS = "crash_logger_prefs"
    private const val KEY_LAST_CRASH = "last_crash_trace"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LAST_CRASH, sw.toString())
                    .apply()
            } catch (_: Exception) {
                // Ne jamais planter dans le crash handler lui-meme.
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * À appeler au démarrage de l'app (ex: MainActivity.onCreate). Si un crash
     * a été capturé depuis le dernier lancement, l'affiche dans une popup
     * copiable puis l'efface (ne s'affiche qu'une seule fois).
     */
    fun showLastCrashIfAny(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val trace = prefs.getString(KEY_LAST_CRASH, null) ?: return

        AlertDialog.Builder(context)
            .setTitle("Dernier crash détecté")
            .setMessage(trace)
            .setPositiveButton("Copier") { _, _ ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Crash TradeVision AI", trace))
                Toast.makeText(context, "Trace copiée dans le presse-papiers", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Fermer", null)
            .setCancelable(true)
            .show()

        prefs.edit().remove(KEY_LAST_CRASH).apply()
    }
}

