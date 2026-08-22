package com.tradevision.ai.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tradevision.ai.data.network.ApiClient
import com.tradevision.ai.databinding.FragmentHistoryBinding
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadHistory()
    }

    private fun loadHistory() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvHistoryContent.text = "Chargement de l'historique..."

        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val response = api.getHistory(limit = 30)

                if (response.isSuccessful && response.body() != null) {
                    val items = response.body()!!
                    if (items.isEmpty()) {
                        binding.tvHistoryContent.text = "Aucun signal enregistré dans l'historique pour le moment."
                    } else {
                        val formattedText = items.joinToString("\n\n─────────────────────────────\n\n") { item ->
                            val actionTag = when (item.action) {
                                "BUY" -> "🟢 BUY"
                                "SELL" -> "🔴 SELL"
                                else -> "🟡 WAIT"
                            }
                            """
                            $actionTag — ${item.symbol}
                            Score : ${item.score} / 100   •   Confiance : ${item.confidence}%
                            Timeframe : ${item.mainTimeframe.uppercase()} (${item.confirmationTimeframe?.uppercase() ?: "4H"})
                            News Utilisée : ${if (item.newsUsed) "OUI" else "NON"}
                            Date : ${item.createdAt}
                            """.trimIndent()
                        }
                        binding.tvHistoryContent.text = formattedText
                    }
                } else {
                    binding.tvHistoryContent.text = "Erreur de chargement (Code: ${response.code()})"
                }
            } catch (e: Exception) {
                binding.tvHistoryContent.text = "Erreur réseau : ${e.message}"
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