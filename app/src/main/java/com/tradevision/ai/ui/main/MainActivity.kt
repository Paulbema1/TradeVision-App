package com.tradevision.ai.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var userRole = "USER"

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
        loadFragment(SignalFragment())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        
        // Cacher les options Admin si l'utilisateur est un simple USER
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
