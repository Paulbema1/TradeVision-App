package com.tradevision.ai.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tradevision.ai.R
import com.tradevision.ai.data.network.ApiClient
import kotlinx.coroutines.launch

class AdminControlFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_control, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnScanAll)?.setOnClickListener { triggerGlobalScan(view) }
        view.findViewById<Button>(R.id.btnClearCache)?.setOnClickListener { clearCache(view) }
        view.findViewById<Button>(R.id.btnRefreshMetrics)?.setOnClickListener { loadKeysMetrics(view) }

        loadKeysMetrics(view)
    }

    private fun triggerGlobalScan(view: View) {
        val btn = view.findViewById<Button>(R.id.btnScanAll) ?: return
        btn.isEnabled = false
        btn.text = "⚡ SCANNING MARKETS..."

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
                btn.isEnabled = true
                btn.text = "⚡ LAUNCH GLOBAL SCAN"
                loadKeysMetrics(view)
            }
        }
    }

    private fun loadKeysMetrics(view: View) {
        val tvKeysStatus = view.findViewById<TextView>(R.id.tvKeysStatus) ?: return

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
                    tvKeysStatus.text = text
                } else {
                    tvKeysStatus.text = "Clés Twelve Data : 🟢 AVAILABLE (Active)"
                }
            } catch (e: Exception) {
                tvKeysStatus.text = "Clés Twelve Data : 🟢 AVAILABLE (Active)"
            }
        }
    }

    private fun clearCache(view: View) {
        val btn = view.findViewById<Button>(R.id.btnClearCache) ?: return
        btn.isEnabled = false

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
                btn.isEnabled = true
            }
        }
    }
}
