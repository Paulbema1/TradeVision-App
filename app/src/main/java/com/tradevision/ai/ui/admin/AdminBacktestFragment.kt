package com.tradevision.ai.ui.admin

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.tradevision.ai.R
import com.tradevision.ai.data.network.ApiClient
import com.tradevision.ai.utils.Constants
import kotlinx.coroutines.launch

class AdminBacktestFragment : Fragment() {

    private var _binding: com.tradevision.ai.databinding.FragmentAdminBacktestBinding? = null
    private val binding get() = _binding!!
    private var selectedAsset = "EUR/USD"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = com.tradevision.ai.databinding.FragmentAdminBacktestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAssetChips()
        binding.btnRunBacktest.setOnClickListener { runBacktest() }
    }

    private fun setupAssetChips() {
        binding.backtestAssetContainer.removeAllViews()

        Constants.SUPPORTED_ASSETS.forEach { asset ->
            val button = Button(requireContext()).apply {
                text = asset
                textSize = 13f
                isAllCaps = true

                val isSelected = (asset == selectedAsset)
                if (isSelected) {
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.admin_accent))
                    setTextColor(Color.WHITE)
                } else {
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg_card))
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                }

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 16, 0)
                }
                layoutParams = params

                setOnClickListener {
                    selectedAsset = asset
                    setupAssetChips()
                    Toast.makeText(requireContext(), "Actif sélectionné : $asset", Toast.LENGTH_SHORT).show()
                }
            }
            binding.backtestAssetContainer.addView(button)
        }
    }

    private fun runBacktest() {
        binding.btnRunBacktest.isEnabled = false
        binding.btnRunBacktest.text = "⏳ SIMULATION $selectedAsset..."
        binding.pbBacktest.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val response = api.runBacktest(symbol = selectedAsset)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val m = body.metrics

                    if (m != null) {
                        binding.tvWinRate.text = "WIN RATE : ${m.winRatePct}%"
                        binding.tvProfitFactor.text = "Profit Factor : ${m.profitFactor}   |   Net : +${m.netProfitPct}%"
                        binding.tvDrawdown.text = "Max Drawdown : -${m.maxDrawdownPct}%"
                        binding.tvTradesStats.text = "Total Trades : ${m.totalTrades}   |   Gagnants : ${m.winningTrades}   |   Perdants : ${m.losingTrades}"

                        val trades = body.trades
                        if (!trades.isNullOrEmpty()) {
                            binding.tvTradesList.text = trades.take(20).joinToString("\n") { t ->
                                val actionEmoji = if (t.action == "BUY") "🟢 BUY" else "🔴 SELL"
                                val resEmoji = if (t.result == "WIN") "✅ WIN (+${t.pips} pips)" else "❌ LOSS (${t.pips} pips)"
                                "$actionEmoji @ ${t.entryPrice}  ➔  $resEmoji (${t.entryTime})"
                            }
                        } else {
                            binding.tvTradesList.text = "Aucun trade exécuté sur cette période."
                        }

                        binding.cardResults.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), "✅ Backtest $selectedAsset terminé !", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Erreur serveur : ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                if (_binding != null) {
                    binding.btnRunBacktest.isEnabled = true
                    binding.btnRunBacktest.text = "🚀 LANCER LE BACKTEST"
                    binding.pbBacktest.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
