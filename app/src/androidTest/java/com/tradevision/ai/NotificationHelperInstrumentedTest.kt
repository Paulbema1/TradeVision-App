package com.tradevision.ai

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tradevision.ai.utils.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test P0 : réception d'un payload de données -> notification correctement créée
 * (avec ses deux actions "Ouvrir le signal" / "Copier les niveaux").
 */
@RunWith(AndroidJUnit4::class)
class NotificationHelperInstrumentedTest {

    @Test
    fun showSignalNotification_posts_notification_with_actions() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationHelper.showSignalNotification(
            context = context,
            symbol = "EUR/USD",
            action = "BUY",
            confidence = 82,
            entry = 1.1050,
            sl = 1.1020,
            tp1 = 1.1090,
            signalId = "smoke-test-signal-id"
        )

        val active = notificationManager.activeNotifications
        assertTrue("Une notification aurait dû être postée", active.isNotEmpty())

        val posted = active.first { it.notification.actions?.isNotEmpty() == true }
        assertEquals(2, posted.notification.actions.size)
    }
}
