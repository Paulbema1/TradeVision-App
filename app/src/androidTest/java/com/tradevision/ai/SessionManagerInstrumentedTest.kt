package com.tradevision.ai

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tradevision.ai.data.network.SessionManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests P0 obligatoires (cahier des charges v9.1.0) :
 * - Le JWT est bien chiffré (pas de SharedPreferences en clair)
 * - Le logout efface bien le token
 * - La déduplication des signal_id fonctionne
 *
 * Remplace le test générique par défaut d'Android Studio (ExampleInstrumentedTest).
 */
@RunWith(AndroidJUnit4::class)
class SessionManagerInstrumentedTest {

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        sessionManager = SessionManager(context)
        sessionManager.clear()
    }

    @After
    fun tearDown() {
        sessionManager.clear()
    }

    @Test
    fun useAppContext() {
        assertEquals("com.tradevision.ai", context.packageName)
    }

    @Test
    fun jwt_token_is_not_stored_in_plaintext_shared_preferences() {
        val token = "test.jwt.token.value"
        sessionManager.saveToken(token)

        // Le token doit être lisible via SessionManager (EncryptedSharedPreferences)
        assertEquals(token, sessionManager.getToken())

        // Mais il ne doit JAMAIS apparaître en clair dans un fichier SharedPreferences
        // non chiffré (ancien comportement risqué que l'audit a signalé comme critique).
        val plainPrefs = context.getSharedPreferences("tradevision_prefs", Context.MODE_PRIVATE)
        assertFalse(
            "Le JWT ne doit pas être présent dans les préférences non chiffrées",
            plainPrefs.all.values.any { it == token }
        )
    }

    @Test
    fun logout_clears_jwt_token() {
        sessionManager.saveToken("some.jwt.token")
        assertTrue(sessionManager.isLoggedIn())

        sessionManager.clear()

        assertFalse(sessionManager.isLoggedIn())
        assertNull(sessionManager.getToken())
    }

    @Test
    fun deduplication_prevents_seeing_same_signal_twice() {
        val signalId = "11111111-1111-1111-1111-111111111111"

        assertFalse(sessionManager.hasSeenSignal(signalId))

        sessionManager.markSignalSeen(signalId)

        assertTrue(sessionManager.hasSeenSignal(signalId))
    }

    @Test
    fun deduplication_distinguishes_different_signal_ids() {
        val signalA = "aaaaaaaa-0000-0000-0000-000000000000"
        val signalB = "bbbbbbbb-0000-0000-0000-000000000000"

        sessionManager.markSignalSeen(signalA)

        assertTrue(sessionManager.hasSeenSignal(signalA))
        assertFalse(sessionManager.hasSeenSignal(signalB))
    }

    @Test
    fun deduplication_handles_null_or_empty_signal_id_gracefully() {
        assertFalse(sessionManager.hasSeenSignal(null))
        assertFalse(sessionManager.hasSeenSignal(""))
        // Ne doit pas lever d'exception
        sessionManager.markSignalSeen(null)
        sessionManager.markSignalSeen("")
    }
}
