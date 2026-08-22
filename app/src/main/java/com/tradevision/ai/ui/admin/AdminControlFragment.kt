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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnScanAll.setOnClickListener { scanAll() }
        binding.btnClearCache.setOnClickListener { clearCache() }
        binding.btnRefreshMetrics.setOnClickListener { loadKeys() }
        loadKeys()
    }

    private fun scanAll() {
        binding.btnScanAll.isEnabled = false
        binding.btnScanAll.text = "SCANNING..."
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val res = api.scanAll()
                if (res.isSuccessful) {
                    Toast.makeText(requireContext(), "Scan global OK", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Erreur ${res.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnScanAll.isEnabled = true
                binding.btnScanAll.text = "LAUNCH GLOBAL SCAN"
                loadKeys()
            }
        }
    }

    private fun loadKeys() {
        binding.tvKeysStatus.text = "Chargement..."
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val res = api.getKeysMetrics()
                if (res.isSuccessful && res.body() != null) {
                    binding.tvKeysStatus.text = res.body()!!.joinToString("\n\n") { k ->
                        val st = if (k.isReady) "AVAILABLE" else "COOLDOWN ${k.cooldownRemainingSec}s"
                        "${k.name}: $st\nReq=${k.totalRequests} Success=${k.totalSuccess} 429=${k.total429}"
                    }
                } else {
                    binding.tvKeysStatus.text = "Erreur ${res.code()}"
                }
            } catch (e: Exception) {
                binding.tvKeysStatus.text = "Erreur: ${e.message}"
            }
        }
    }

    private fun clearCache() {
        binding.btnClearCache.isEnabled = false
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val res = api.clearCache()
                Toast.makeText(
                    requireContext(),
                    if (res.isSuccessful) "Cache vidé" else "Erreur cache",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
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
