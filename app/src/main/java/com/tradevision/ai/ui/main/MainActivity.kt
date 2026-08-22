package com.tradevision.ai.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.tradevision.ai.R
import com.tradevision.ai.data.network.SessionManager
import com.tradevision.ai.databinding.ActivityMainBinding
import com.tradevision.ai.ui.admin.AdminControlFragment
import com.tradevision.ai.ui.admin.AdminMembersFragment
import com.tradevision.ai.ui.user.HistoryFragment
import com.tradevision.ai.ui.user.ProfileFragment
import com.tradevision.ai.ui.user.SignalFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        val role = sessionManager.getRole()

        setupNavigation(role)
    }

    private fun setupNavigation(role: String) {
        val menu = binding.bottomNavigation.menu
        menu.clear()

        if (role == "ADMIN") {
            menu.add(0, 1, 0, "🕹️ Cockpit").setIcon(android.R.drawable.ic_menu_manage)
            menu.add(0, 2, 1, "👥 Members").setIcon(android.R.drawable.ic_menu_myplaces)
            menu.add(0, 3, 2, "⚙️ Profil").setIcon(android.R.drawable.ic_menu_preferences)

            loadFragment(AdminControlFragment())

            binding.bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    1 -> loadFragment(AdminControlFragment())
                    2 -> loadFragment(AdminMembersFragment())
                    3 -> loadFragment(ProfileFragment())
                }
                true
            }
        } else {
            menu.add(0, 1, 0, "📊 Signal").setIcon(android.R.drawable.ic_menu_compass)
            menu.add(0, 2, 1, "📜 Historique").setIcon(android.R.drawable.ic_menu_recent_history)
            menu.add(0, 3, 2, "⚙️ Profil").setIcon(android.R.drawable.ic_menu_preferences)

            loadFragment(SignalFragment())

            binding.bottomNavigation.setOnItemSelectedListener { item ->
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