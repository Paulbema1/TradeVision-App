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
import com.tradevision.ai.data.network.SessionManager
import com.tradevision.ai.utils.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminBacktestFragment : Fragment() {

    private var _binding: com.tradevision.ai.databinding.FragmentAdminBacktestBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private var selectedAsset = "EUR/USD"
    private val logBuilder = StringBuilder()

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

        sessionManager = SessionManager(requireContext())

        setupAssetChips()
        binding.btnRunBacktest.setOnClickListener { runBacktest() }
    }

    private fun appendLog(text: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logBuilder.append("[$time] $text\n")
        binding.tvConsoleLogs.text = logBuilder.toString()
    }

    private fun setupAssetChips() {
        binding.backtestAssetContainer.removeAllViews()

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
                    setupAssetChips()
                    Toast.makeText(requireContext(), "Actif : $asset", Toast.LENGTH_SHORT).show()
                }
            }
            binding.backtestAssetContainer.addView(button)
        }
    }

    private fun runBacktest() {
        val mainTf = sessionManager.getMainTf()
        val confirmTf = sessionManager.getConfirmTf()

        binding.btnRunBacktest.isEnabled = false
        binding.btnRunBacktest.text = "⏳ SIMULATION $selectedAsset (${mainTf.uppercase()})..."
        binding.pbBacktest.visibility = View.VISIBLE
        binding.cardResults.visibility = View.GONE
        binding.cardLogs.visibility = View.VISIBLE

        logBuilder.clear()
        appendLog("🚀 Démarrage du Backtest pour $selectedAsset...")
        appendLog("⏱️ Timeframe Principal : ${mainTf.uppercase()} | Confirmation : ${confirmTf.uppercase()}")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                appendLog("📡 Connexion au serveur Render (Timeout 60s)...")
                delay(300)
                appendLog("📦 Chargement des bougies historiques (${mainTf.uppercase()} / ${confirmTf.uppercase()})...")

                val api = ApiClient.getApiService(requireContext())
                val response = api.runBacktest(symbol = selectedAsset, mainTf = mainTf, confirmTf = confirmTf)

                if (response.isSuccessful && response.body() != null) {
                    appendLog("🧠 Exécution des moteurs TA + SMC + MTF + News...")
                    delay(300)

                    val body = response.body()!!
                    val m = body.metrics

                    if (m != null) {
                        appendLog("📊 Calcul des pips, Win Rate et Drawdown...")

                        // Affichage explicite des Timeframes et de la Période
                        val tfUsedDisplay = "TF Principal : ${body.mainTf.uppercase()}   •   Confirmation : ${confirmTf.uppercase()}"
                        binding.tvBacktestTfInfo.text = tfUsedDisplay
                        binding.tvBacktestPeriodInfo.text = "Période simulée : ${body.period}"

                        binding.tvWinRate.text = "WIN RATE : ${m.winRatePct}%"
                        binding.tvProfitFactor.text = "Profit Factor : ${m.profitFactor}   |   Net : +${m.netProfitPct}%"
                        binding.tvDrawdown.text = "Max Drawdown : -${m.maxDrawdownPct}%"
                        binding.tvTradesStats.text = "Total Trades : ${m.totalTrades}   |   Gagnants : ${m.winningTrades}   |   Perdants : ${m.losingTrades}"

                        val trades = body.trades
                        if (!trades.isNullOrEmpty()) {
                            appendLog("🎯 ${trades.size} trades simulés dans l'historique.")
                            binding.tvTradesList.text = trades.take(20).joinToString("\n") { t ->
                                val actionEmoji = if (t.action == "BUY") "🟢 BUY" else "🔴 SELL"
                                val resEmoji = if (t.result == "WIN") "✅ WIN (+${t.pips} pips)" else "❌ LOSS (${t.pips} pips)"
                                "$actionEmoji @ ${t.entryPrice}  ➔  $resEmoji (${t.entryTime})"
                            }
                        } else {
                            binding.tvTradesList.text = "Aucun trade exécuté sur cette période."
                        }

                        appendLog("✅ Simulation terminée avec succès !")
                        binding.cardResults.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), "✅ Backtest $selectedAsset (${body.mainTf.uppercase()}) terminé !", Toast.LENGTH_SHORT).show()
                    } else {
                        appendLog("❌ Aucune métrique renvoyée par le serveur.")
                    }
                } else {
                    appendLog("❌ Erreur serveur HTTP : ${response.code()}")
                    Toast.makeText(requireContext(), "Erreur serveur : ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                appendLog("❌ Erreur Réseau : ${e.message}")
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
