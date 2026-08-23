package com.tradevision.ai.ui.user

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.tradevision.ai.R
import com.tradevision.ai.data.network.SessionManager
import com.tradevision.ai.ui.auth.LoginActivity

class ProfileFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var selectedMainTf = "1H"
    private var selectedConfirmTf = "4H"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        val username = sessionManager.getUsername()
        val role = sessionManager.getRole()

        view.findViewById<TextView>(R.id.tvUsername)?.text = "Username : $username"
        view.findViewById<TextView>(R.id.tvRole)?.text = "Rôle : $role"
        view.findViewById<TextView>(R.id.tvStatus)?.text = "Statut du compte : ACTIVE"

        updateTimeframeText(view)

        // Clic pour changer le Timeframe Principal
        view.findViewById<View>(R.id.tvUsername)?.setOnClickListener {
            showTimeframePicker(view)
        }

        view.findViewById<SwitchCompat>(R.id.switchNotifications)?.setOnCheckedChangeListener { _, isChecked ->
            val statusText = if (isChecked) "Notifications PUSH activées" else "Notifications désactivées"
            Toast.makeText(requireContext(), statusText, Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnLogout)?.setOnClickListener {
            sessionManager.clear()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }
    }

    private fun showTimeframePicker(view: View) {
        val options = arrayOf("15m (Scalping)", "30m (Intraday)", "1H (Standard)", "4H (Swing)")
        AlertDialog.Builder(requireContext())
            .setTitle("Choisir le Timeframe Principal")
            .setItems(options) { _, which ->
                selectedMainTf = when (which) {
                    0 -> "15m"
                    1 -> "30m"
                    2 -> "1H"
                    else -> "4H"
                }
                updateTimeframeText(view)
                Toast.makeText(requireContext(), "Timeframe modifié : $selectedMainTf", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun updateTimeframeText(view: View) {
        val text = "Main Timeframe : $selectedMainTf (Toucher pour changer)\n" +
                "Confirmation Timeframe : $selectedConfirmTf\n" +
                "Auto Confirmation IA : ON\n" +
                "Minimum Confidence Required : 70%\n" +
                "Actifs Suivis : EUR/USD, GBP/USD, USD/JPY, XAU/USD"
    }
}
