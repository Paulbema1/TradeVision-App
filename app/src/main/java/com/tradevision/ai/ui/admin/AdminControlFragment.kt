package com.tradevision.ai.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tradevision.ai.data.network.ApiClient
import com.tradevision.ai.databinding.FragmentAdminControlBinding
import kotlinx.coroutines.launch

class AdminControlFragment : Fragment() {

    private var _binding: FragmentAdminControlBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnScanAll.setOnClickListener { triggerGlobalScan() }
        binding.btnClearCache.setOnClickListener { clearCache() }
        binding.btnRefreshMetrics.setOnClickListener { loadKeysMetrics() }

        loadKeysMetrics()
    }

    private fun triggerGlobalScan() {
        binding.btnScanAll.setEnabled(false)
        binding.btnScanAll.setText("⚡ SCANNING MARKETS...")

        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val response = api.scanAll()

                if (response.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "⚡ SCAN GLOBAL TERMINÉ & NOTIFS DIFFUSÉES !",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Erreur scan : ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Erreur réseau : ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.btnScanAll.setEnabled(true)
                binding.btnScanAll.setText("⚡ LAUNCH GLOBAL SCAN")
                loadKeysMetrics()
            }
        }
    }

    private fun loadKeysMetrics() {
        binding.tvKeysStatus.setText("Chargement des métriques des clés...")

        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val response = api.getKeysMetrics()

                if (response.isSuccessful && response.body() != null) {
                    val metrics = response.body()!!
                    val text = metrics.joinToString("\n\n") { item ->
                        val statusEmoji = if (item.isReady) "🟢 AVAILABLE" else "🔴 COOLDOWN (${item.cooldownRemainingSec}s)"
                        "${item.name} : $statusEmoji\nTotal Requests: ${item.totalRequests} | Success: ${item.totalSuccess} | 429 Errors: ${item.total429}"
                    }
                    binding.tvKeysStatus.setText(text)
                } else {
                    binding.tvKeysStatus.setText("Impossible de récupérer les métriques (Code: ${response.code()})")
                }
            } catch (e: Exception) {
                binding.tvKeysStatus.setText("Erreur de connexion : ${e.message}")
            }
        }
    }

    private fun clearCache() {
        binding.btnClearCache.setEnabled(false)

        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val response = api.clearCache()

                if (response.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "🧹 Cache mémoire vidé avec succès !",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Erreur vidage cache",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Erreur : ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.btnClearCache.setEnabled(true)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
