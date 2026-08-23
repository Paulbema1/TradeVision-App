package com.tradevision.ai.ui.user

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.tradevision.ai.data.network.SessionManager
import com.tradevision.ai.databinding.FragmentProfileBinding
import com.tradevision.ai.ui.auth.LoginActivity

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        val username = sessionManager.getUsername()
        val role = sessionManager.getRole()

        binding.tvUsername.text = "Username : $username"
        binding.tvRole.text = "Rôle : $role"
        binding.tvStatus.text = "Statut du compte : ACTIVE"

        updateTimeframeUI()

        // Clics pour modifier les Timeframes
        binding.tvMainTf.setOnClickListener { showMainTfDialog() }
        binding.tvConfirmTf.setOnClickListener { showConfirmTfDialog() }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            val statusText = if (isChecked) "Notifications activées" else "Notifications désactivées"
            Toast.makeText(requireContext(), statusText, Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.clear()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }
    }

    private fun showMainTfDialog() {
        val options = arrayOf("15m (Scalping)", "30m (Intraday)", "1h (Standard)", "4h (Swing)")
        val values = arrayOf("15m", "30m", "1h", "4h")

        AlertDialog.Builder(requireContext())
            .setTitle("Sélecteur : Timeframe Principal")
            .setItems(options) { _, which ->
                val selected = values[which]
                sessionManager.saveMainTf(selected)
                updateTimeframeUI()
                Toast.makeText(requireContext(), "Main TF réglé sur : ${selected.uppercase()}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showConfirmTfDialog() {
        val options = arrayOf("30m", "1h", "4h (Standard)")
        val values = arrayOf("30m", "1h", "4h")

        AlertDialog.Builder(requireContext())
            .setTitle("Sélecteur : Timeframe Confirmation")
            .setItems(options) { _, which ->
                val selected = values[which]
                sessionManager.saveConfirmTf(selected)
                updateTimeframeUI()
                Toast.makeText(requireContext(), "Confirmation TF réglé sur : ${selected.uppercase()}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun updateTimeframeUI() {
        val mainTf = sessionManager.getMainTf().uppercase()
        val confirmTf = sessionManager.getConfirmTf().uppercase()

        binding.tvMainTf.text = "Main Timeframe : $mainTf  ✏️ (Toucher pour modifier)"
        binding.tvConfirmTf.text = "Confirmation Timeframe : $confirmTf  ✏️ (Toucher pour modifier)"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
