package com.tradevision.ai.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tradevision.ai.data.network.ApiClient
import com.tradevision.ai.databinding.FragmentAdminMembersBinding
import kotlinx.coroutines.launch

class AdminMembersFragment : Fragment() {

    private var _binding: FragmentAdminMembersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadMembers()
    }

    private fun loadMembers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvMembersContent.text = "Chargement de la liste des membres..."

        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val response = api.listUsers()

                if (response.isSuccessful && response.body() != null) {
                    val users = response.body()!!
                    if (users.isEmpty()) {
                        binding.tvMembersContent.text = "Aucun membre inscrit pour le moment."
                    } else {
                        val formattedText = users.joinToString("\n\n─────────────────────────────\n\n") { user ->
                            val fcmStatus = if (user.hasFcmToken) "🟢 Notifications active" else "🔴 Notifications disabled"
                            val roleBadge = if (user.role == "ADMIN") "👑 ADMIN" else "👤 USER"
                            
                            """
                            ${user.username.uppercase()} ($roleBadge)
                            Inscrit le : ${user.createdAt}
                            Statut Push : $fcmStatus
                            Compte Actif : ${if (user.isActive) "OUI" else "NON"}
                            """.trimIndent()
                        }
                        binding.tvMembersContent.text = "TOTAL MEMBRES : ${users.size}\n\n$formattedText"
                    }
                } else {
                    binding.tvMembersContent.text = "Erreur de chargement (Code: ${response.code()})"
                }
            } catch (e: Exception) {
                binding.tvMembersContent.text = "Erreur réseau : ${e.message}"
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}