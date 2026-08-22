package com.tradevision.ai.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tradevision.ai.data.network.ApiClient
import com.tradevision.ai.databinding.FragmentHistoryBinding
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadHistory()
    }

    private fun loadHistory() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvHistoryContent.text = "Chargement..."

        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val res = api.getHistory(limit = 30)
                if (res.isSuccessful && res.body() != null) {
                    val items = res.body()!!
                    if (items.isEmpty()) {
                        binding.tvHistoryContent.text = "Aucun signal dans l'historique."
                    } else {
                        binding.tvHistoryContent.text = items.joinToString("\n\n----------------\n\n") { i ->
                            val tag = when (i.action) {
                                "BUY" -> "BUY"
                                "SELL" -> "SELL"
                                else -> "WAIT"
                            }
                            "$tag ${i.symbol}\nScore ${i.score}/100 | Conf ${i.confidence}%\nTF ${i.mainTimeframe} | ${i.createdAt}"
                        }
                    }
                } else {
                    binding.tvHistoryContent.text = "Erreur ${res.code()}"
                }
            } catch (e: Exception) {
                binding.tvHistoryContent.text = "Erreur: ${e.message}"
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
