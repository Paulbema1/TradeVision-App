package com.tradevision.ai.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.messaging.FirebaseMessaging
import com.tradevision.ai.R
import com.tradevision.ai.data.model.FcmTokenRequest
import com.tradevision.ai.data.network.ApiClient
import com.tradevision.ai.data.network.SessionManager
import com.tradevision.ai.ui.admin.AdminBacktestFragment
import com.tradevision.ai.ui.admin.AdminControlFragment
import com.tradevision.ai.ui.admin.AdminMembersFragment
import com.tradevision.ai.ui.auth.LoginActivity
import com.tradevision.ai.ui.user.HistoryFragment
import com.tradevision.ai.ui.user.ProfileFragment
import com.tradevision.ai.ui.user.SignalFragment
import com.tradevision.ai.utils.CrashLogger
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * NOTE (corrigé v9.1.0) :
 * Cette Activity ne fait plus de polling en arrière-plan (l'ancien
 * startBackgroundMonitor() interrogeait /signals/analyze toutes les 15s pour
 * chaque actif, ce qui dupliquait l'Auto-Scan serveur et violait l'exigence
 * "pas de polling 15s"). Les notifications proviennent désormais uniquement
 * de FCM (voir FCMService.kt) ; le rafraîchissement d'écran (30s max) reste
 * géré localement par SignalFragment.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var userRole = "USER"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) registerRealFcmToken()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Capteur de crash minimal (diagnostic sans logcat/adb) — voir CrashLogger.kt.
        CrashLogger.install(this)
        CrashLogger.showLastCrashIfAny(this)

        sessionManager = SessionManager(this)
        userRole = sessionManager.getRole()

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        checkNotificationPermission()
        loadFragment(SignalFragment())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        if (userRole != "ADMIN") {
            menu?.findItem(R.id.action_cockpit)?.isVisible = false
            menu?.findItem(R.id.action_backtest)?.isVisible = false
            menu?.findItem(R.id.action_members)?.isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_signal -> loadFragment(SignalFragment())
            R.id.action_history -> loadFragment(HistoryFragment())
            R.id.action_cockpit -> loadFragment(AdminControlFragment())
            R.id.action_backtest -> loadFragment(AdminBacktestFragment())
            R.id.action_members -> loadFragment(AdminMembersFragment())
            R.id.action_profile -> loadFragment(ProfileFragment())
            R.id.action_logout -> {
                sessionManager.clear()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                registerRealFcmToken()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            registerRealFcmToken()
        }
    }

    /**
     * Récupère le VRAI token FCM auprès de Firebase et l'enregistre côté backend.
     * Remplace l'ancien registerDummyFcmToken() qui envoyait un identifiant factice
     * ("android_device_" + username) et cassait silencieusement les notifications push.
     */
    private fun registerRealFcmToken() {
        lifecycleScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                val api = ApiClient.getApiService(this@MainActivity)
                api.updateFcmToken(FcmTokenRequest(token))
            } catch (e: Exception) {
                Log.w("MainActivity", "Échec de l'enregistrement du token FCM : ${e.message}")
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}

