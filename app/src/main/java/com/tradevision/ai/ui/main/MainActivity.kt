package com.tradevision.ai.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tradevision.ai.R
import com.tradevision.ai.data.model.FcmTokenRequest
import com.tradevision.ai.data.network.ApiClient
import com.tradevision.ai.data.network.SessionManager
import com.tradevision.ai.ui.admin.AdminBacktestFragment
import com.tradevision.ai.ui.admin.AdminControlFragment
import com.tradevision.ai.ui.admin.AdminMembersFragment
import com.tradevision.ai.ui.user.HistoryFragment
import com.tradevision.ai.ui.user.ProfileFragment
import com.tradevision.ai.ui.user.SignalFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            registerDummyFcmToken()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)
        val role = sessionManager.getRole()

        checkNotificationPermission()
        setupNavigation(role)
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

    private fun setupNavigation(role: String) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation) ?: return
        val menu = bottomNav.menu
        menu.clear()

        if (role == "ADMIN") {
            // L'ADMIN A ACCÈS À TOUT (5 ONGLETS)
            menu.add(0, 1, 0, "📊 Signal").setIcon(android.R.drawable.ic_menu_compass)
            menu.add(0, 2, 1, "📜 Hist.").setIcon(android.R.drawable.ic_menu_recent_history)
            menu.add(0, 3, 2, "🕹️ Cockpit").setIcon(android.R.drawable.ic_menu_manage)
            menu.add(0, 4, 3, "📈 Test").setIcon(android.R.drawable.ic_menu_sort_by_size)
            menu.add(0, 5, 4, "👥 Admin").setIcon(android.R.drawable.ic_menu_myplaces)

            loadFragment(SignalFragment())

            bottomNav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    1 -> loadFragment(SignalFragment())
                    2 -> loadFragment(HistoryFragment())
                    3 -> loadFragment(AdminControlFragment())
                    4 -> loadFragment(AdminBacktestFragment())
                    5 -> loadFragment(AdminMembersFragment())
                }
                true
            }
        } else {
            // UTILISATEUR PUBLIC (3 ONGLETS SIMPLES)
            menu.add(0, 1, 0, "📊 Signal").setIcon(android.R.drawable.ic_menu_compass)
            menu.add(0, 2, 1, "📜 Historique").setIcon(android.R.drawable.ic_menu_recent_history)
            menu.add(0, 3, 2, "⚙️ Profil").setIcon(android.R.drawable.ic_menu_preferences)

            loadFragment(SignalFragment())

            bottomNav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    1 -> loadFragment(SignalFragment())
                    2 -> loadFragment(HistoryFragment())
                    3 -> loadFragment(ProfileFragment())
                }
                true
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
