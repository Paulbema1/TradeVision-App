package com.tradevision.ai.ui.user

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        binding.tvUsername.setText("Username : $username")
        binding.tvRole.setText("Rôle : $role")
        binding.tvStatus.setText("Statut du compte : ACTIVE")

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
