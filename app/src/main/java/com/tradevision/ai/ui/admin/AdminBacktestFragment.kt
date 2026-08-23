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

    private var selectedAsset = "EUR/USD"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_backtest, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAssetChips(view)
        view.findViewById<Button>(R.id.btnRunBacktest)?.setOnClickListener { runBacktest(view) }
    }

    private fun setupAssetChips(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.backtestAssetContainer) ?: return
        container.removeAllViews()

        Constants.SUPPORTED_ASSETS.forEach { asset ->
            val button = Button(requireContext()).apply {
                text = asset
                textSize = 12f
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
                    setupAssetChips(view)
                }
            }
            container.addView(button)
        }
    }

    private fun runBacktest(view: View) {
        val btn = view.findViewById<Button>(R.id.btnRunBacktest) ?: return
        val pb = view.findViewById<ProgressBar>(R.id.pbBacktest)
        val cardResults = view.findViewById<MaterialCardView>(R.id.cardResults)
        val tvWinRate = view.findViewById<TextView>(R.id.tvWinRate)
        val tvProfitFactor = view.findViewById<TextView>(R.id.tvProfitFactor)
        val tvDrawdown = view.findViewById<TextView>(R.id.tvDrawdown)
        val tvTradesStats = view.findViewById<TextView>(R.id.tvTradesStats)
        val tvTradesList = view.findViewById<TextView>(R.id.tvTradesList)

        btn.isEnabled = false
        btn.text = "⏳ CALCUL EN COURS..."
        pb?.visibility = View.VISIBLE
        cardResults?.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val response = api.runBacktest(symbol = selectedAsset)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val metrics = body["metrics"] as? Map<*, *>

                    if (metrics != null) {
                        tvWinRate?.text = "WIN RATE : ${metrics["win_rate_pct"]}%"
                        tvProfitFactor?.text = "Profit Factor : ${metrics["profit_factor"]} | Profit Net : +${metrics["net_profit_pct"]}%"
                        tvDrawdown?.text = "Max Drawdown : -${metrics["max_drawdown_pct"]}%"
                        tvTradesStats?.text = "Total Trades: ${metrics["total_trades"]} | Gagnants: ${metrics["winning_trades"]} | Perdants: ${metrics["losing_trades"]}"

                        val trades = body["trades"] as? List<*>
                        if (trades != null && trades.isNotEmpty()) {
                            tvTradesList?.text = trades.take(15).joinToString("\n") { trade ->
                                val t = trade as? Map<*, *>
                                val actionEmoji = if (t?.get("action") == "BUY") "🟢 BUY" else "🔴 SELL"
                                val resEmoji = if (t?.get("result") == "WIN") "✅ WIN (+${t["pips"]} pips)" else "❌ LOSS (${t?.get("pips")} pips)"
                                "$actionEmoji @ ${t?.get("entry_price")} -> $resEmoji"
                            }
                        } else {
                            tvTradesList?.text = "Aucun trade historique généré."
                        }

                        cardResults?.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), "✅ Backtest terminé avec succès !", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Erreur backtest (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur réseau : ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btn.isEnabled = true
                btn.text = "🚀 LANCER LE BACKTEST"
                pb?.visibility = View.GONE
            }
        }
    }
}
