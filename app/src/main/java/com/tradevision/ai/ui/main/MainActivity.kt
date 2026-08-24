package com.tradevision.ai.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
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
import com.tradevision.ai.utils.Constants
import com.tradevision.ai.utils.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var userRole = "USER"
    private var lastNotifiedKeys = mutableSetOf<String>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) registerDummyFcmToken()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)
        userRole = sessionManager.getRole()

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        checkNotificationPermission()
        setupNavigation(userRole)
        startBackgroundMonitor()
    }

    // SURVEILLANCE AUTOMATIQUE DES 4 ACTIFS EN ARRIÈRE-PLAN
    private fun startBackgroundMonitor() {
        lifecycleScope.launch {
            while (true) {
                try {
                    val api = ApiClient.getApiService(this@MainActivity)
                    val mainTf = sessionManager.getMainTf()
                    val confirmTf = sessionManager.getConfirmTf()

                    Constants.SUPPORTED_ASSETS.forEach { symbol ->
                        val res = api.analyzeAsset(symbol, mainTf = mainTf, confirmTf = confirmTf)
                        if (res.isSuccessful && res.body() != null) {
                            val sig = res.body()!!
                            if (sig.action != "WAIT" && sig.confidence >= 70) {
                                val key = "${sig.symbol}_${sig.action}_${sig.entryPrice}"
                                if (!lastNotifiedKeys.contains(key)) {
                                    lastNotifiedKeys.add(key)
                                    NotificationHelper.showSignalNotification(
                                        context = this@MainActivity,
                                        symbol = sig.symbol,
                                        action = sig.action,
                                        confidence = sig.confidence,
                                        entry = sig.entryPrice,
                                        sl = sig.stopLoss,
                                        tp1 = sig.takeProfit1
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
                delay(120000) // Vérification automatique toutes les 2 minutes !
            }
        }
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
                registerDummyFcmToken()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            registerDummyFcmToken()
        }
    }

    private fun registerDummyFcmToken() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(this@MainActivity)
                val deviceToken = "android_device_" + sessionManager.getUsername()
                api.updateFcmToken(FcmTokenRequest(deviceToken))
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
