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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstancesState: Bundle?): View {
        _binding = FragmentAdminMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadMembers()
    }

    private fun loadMembers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvMembersContent.text = "Chargement..."
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val res = api.listUsers()
                if (res.isSuccessful && res.body() != null) {
                    val users = res.body()!!
                    if (users.isEmpty()) {
                        binding.tvMembersContent.text = "Aucun membre."
                    } else {
                        binding.tvMembersContent.text = "TOTAL: ${users.size}\n\n" + users.joinToString("\n\n----------------\n\n") { u ->
                            val notif = if (u.hasFcmToken) "Notif ON" else "Notif OFF"
                            "${u.username} [${u.role}]\n${u.createdAt}\n$notif"
                        }
                    }
                } else {
                    binding.tvMembersContent.text = "Erreur ${res.code()}"
                }
            } catch (e: Exception) {
                binding.tvMembersContent.text = "Erreur: ${e.message}"
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
