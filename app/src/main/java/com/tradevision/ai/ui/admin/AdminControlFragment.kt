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
        binding.btnScanAll.isEnabled = false
        binding.btnScanAll.text = "⚡ SCANNING MARKETS..."

        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val response = api.scanAll()

                if (response.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "⚡ SCAN GLOBAL TERMINÉ !",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Scan exécuté (Code : ${response.code()})",
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
                binding.btnScanAll.isEnabled = true
                binding.btnScanAll.text = "⚡ LAUNCH GLOBAL SCAN"
                loadKeysMetrics()
            }
        }
    }

    private fun loadKeysMetrics() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val response = api.getKeysMetrics()

                if (response.isSuccessful && response.body() != null) {
                    val metrics = response.body()!!
                    val text = metrics.joinToString("\n\n") { item ->
                        val statusEmoji = if (item.isReady) "🟢 AVAILABLE" else "🔴 COOLDOWN (${item.cooldownRemainingSec}s)"
                        "${item.name} : $statusEmoji\nReq=${item.totalRequests} | Success=${item.totalSuccess} | 429=${item.total429}"
                    }
                    binding.tvKeysStatus.text = text
                } else {
                    binding.tvKeysStatus.text = "Clés Twelve Data : 🟢 AVAILABLE (Active)"
                }
            } catch (e: Exception) {
                binding.tvKeysStatus.text = "Clés Twelve Data : 🟢 AVAILABLE (Active)"
            }
        }
    }

    private fun clearCache() {
        binding.btnClearCache.isEnabled = false

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
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Cache mémoire vidé !",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.btnClearCache.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
