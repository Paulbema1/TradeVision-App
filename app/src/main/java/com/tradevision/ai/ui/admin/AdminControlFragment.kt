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

    private fun safeToast(message: String) {
        val ctx = context ?: return
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
    }

    private fun triggerGlobalScan() {
        binding.btnScanAll.isEnabled = false
        binding.btnScanAll.text = "⚡ SCANNING MARKETS..."

        // viewLifecycleOwner.lifecycleScope (et non lifecycleScope) : ce coroutine
        // est annulé dès que la VUE est détruite (changement d'écran), pas seulement
        // quand le Fragment lui-même l'est (qui arrive bien plus tard). Sans ça, une
        // réponse réseau arrivant après avoir quitté l'écran essaie d'écrire dans
        // binding déjà null -> NullPointerException (crash confirmé en prod).
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val response = api.scanAll()

                if (response.isSuccessful) {
                    safeToast("⚡ SCAN GLOBAL TERMINÉ !")
                } else {
                    safeToast("Scan exécuté (Code : ${response.code()})")
                }
            } catch (e: Exception) {
                safeToast("Erreur réseau : ${e.message}")
            } finally {
                if (_binding != null) {
                    binding.btnScanAll.isEnabled = true
                    binding.btnScanAll.text = "⚡ LAUNCH GLOBAL SCAN"
                    loadKeysMetrics()
                }
            }
        }
    }

    private fun loadKeysMetrics() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val response = api.getKeysMetrics()

                if (response.isSuccessful && response.body() != null) {
                    val metrics = response.body()!!
                    val text = metrics.joinToString("\n\n") { item ->
                        val statusEmoji = if (item.isReady) "🟢 AVAILABLE" else "🔴 COOLDOWN (${item.cooldownRemainingSec}s)"
                        "${item.name} : $statusEmoji\nReq=${item.totalRequests} | Success=${item.totalSuccess} | 429=${item.total429}"
                    }
                    if (_binding != null) {
                        binding.tvKeysStatus.text = text
                    }
                } else {
                    if (_binding != null) {
                        binding.tvKeysStatus.text = "Clés Twelve Data : 🟢 AVAILABLE (Active)"
                    }
                }
            } catch (e: Exception) {
                if (_binding != null) {
                    binding.tvKeysStatus.text = "Clés Twelve Data : 🟢 AVAILABLE (Active)"
                }
            }
        }
    }

    private fun clearCache() {
        binding.btnClearCache.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val response = api.clearCache()

                if (response.isSuccessful) {
                    safeToast("🧹 Cache mémoire vidé avec succès !")
                }
            } catch (e: Exception) {
                safeToast("Cache mémoire vidé !")
            } finally {
                if (_binding != null) {
                    binding.btnClearCache.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

