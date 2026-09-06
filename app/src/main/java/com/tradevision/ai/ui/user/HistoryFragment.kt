package com.tradevision.ai.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tradevision.ai.R
import com.tradevision.ai.data.network.ApiClient
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: com.tradevision.ai.databinding.FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = com.tradevision.ai.databinding.FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadHistory()
    }

    private fun loadHistory() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvHistoryContent.text = "Chargement de l'historique..."

        // viewLifecycleOwner.lifecycleScope (et non lifecycleScope) : annule le
        // coroutine dès que la VUE est détruite (changement d'écran), pas seulement
        // quand le Fragment lui-même l'est (bien plus tard). Même bug corrigé dans
        // SignalFragment et AdminControlFragment (crash confirmé en prod).
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val response = api.getHistory(limit = 30)

                if (response.isSuccessful && response.body() != null) {
                    val items = response.body()!!
                    val actionItems = items.filter { it.action != "WAIT" }

                    if (actionItems.isEmpty()) {
                        if (_binding != null) {
                            binding.tvHistoryContent.text = "Aucun signal d'action (BUY/SELL) dans l'historique."
                        }
                    } else {
                        val formattedText = actionItems.joinToString("\n\n─────────────────────────────\n\n") { item ->
                            val actionTag = if (item.action == "BUY") "🟢 BUY" else "🔴 SELL"
                            """
                            $actionTag — ${item.symbol}
                            Confiance : ${item.confidence}%   •   Score : ${item.score} / 100
                            Timeframe : ${item.mainTimeframe.uppercase()} (${item.confirmationTimeframe?.uppercase() ?: "4H"})
                            Date : ${item.createdAt}
                            """.trimIndent()
                        }
                        if (_binding != null) {
                            binding.tvHistoryContent.text = formattedText
                        }
                    }
                } else {
                    if (_binding != null) {
                        binding.tvHistoryContent.text = "Erreur de chargement (${response.code()})"
                    }
                }
            } catch (e: Exception) {
                if (_binding != null) {
                    binding.tvHistoryContent.text = "Erreur réseau : ${e.message}"
                }
            } finally {
                if (_binding != null) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

