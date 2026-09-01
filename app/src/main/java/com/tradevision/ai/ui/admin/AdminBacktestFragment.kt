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
        insertDownloadHistoricalDataButton()
    }

    /**
     * Ajoute le bouton "Télécharger l'historique" AU-DESSUS du bouton
     * "Lancer le backtest" existant, sans dépendre du layout XML (inséré
     * directement dans le parent de btnRunBacktest, quel qu'il soit).
     * Prérequis : le backtest ne peut fonctionner que si des données
     * historiques Parquet locales existent déjà (voir historical_data.py).
     */
    private fun insertDownloadHistoricalDataButton() {
        val parent = binding.btnRunBacktest.parent as? ViewGroup ?: return
        val index = parent.indexOfChild(binding.btnRunBacktest)

        val downloadButton = Button(requireContext()).apply {
            text = "📥 TÉLÉCHARGER L'HISTORIQUE (une fois)"
            textSize = 12f
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg_card))
            setTextColor(ContextCompat.getColor(requireContext(), R.color.admin_accent))
            val params = when (val lp = binding.btnRunBacktest.layoutParams) {
                is LinearLayout.LayoutParams -> LinearLayout.LayoutParams(lp).apply {
                    topMargin = 12
                    bottomMargin = 12
                }
                else -> ViewGroup.MarginLayoutParams(lp)
            }
            layoutParams = params
            setOnClickListener { downloadHistoricalData(this) }
        }

        parent.addView(downloadButton, index)
    }

    private fun downloadHistoricalData(button: Button) {
        button.isEnabled = false
        button.text = "⏳ Démarrage du téléchargement..."

        binding.cardLogs.visibility = View.VISIBLE
        logBuilder.clear()
        appendLog("📥 Demande de téléchargement des données historiques (4 actifs x 4 timeframes)...")
        appendLog("⏱️ Cette opération tourne en arrière-plan côté serveur (~2-3 min). Consultez les logs Render pour le détail.")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val response = api.downloadHistoricalData()

                if (response.isSuccessful && response.body() != null) {
                    appendLog("✅ ${response.body()!!.message}")
                    Toast.makeText(requireContext(), "Téléchargement démarré, voir les logs.", Toast.LENGTH_LONG).show()
                } else {
                    appendLog("❌ Erreur serveur : ${response.code()}")
                    Toast.makeText(requireContext(), "Erreur serveur : ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                appendLog("❌ Erreur : ${e.message}")
                Toast.makeText(requireContext(), "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                if (_binding != null) {
                    button.isEnabled = true
                    button.text = "📥 TÉLÉCHARGER L'HISTORIQUE (une fois)"
                }
            }
        }
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
        binding.btnRunBacktest.text = "⏳ SIMULATION $selectedAsset..."
        binding.pbBacktest.visibility = View.VISIBLE
        binding.cardResults.visibility = View.GONE
        binding.cardLogs.visibility = View.VISIBLE

        logBuilder.clear()
        appendLog("🚀 Démarrage de l'Audit Backtest pour $selectedAsset...")
        appendLog("⏱️ Timeframes : Main ${mainTf.uppercase()} | Confirmation ${confirmTf.uppercase()}")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                appendLog("📡 Chargement du dataset local (Parquet) sur Render...")
                delay(200)

                val api = ApiClient.getApiService(requireContext())
                val response = api.runBacktest(symbol = selectedAsset, mainTf = mainTf, confirmTf = confirmTf)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    // Le backend peut renvoyer un HTTP 200 avec un champ "error"
                    // (ex: données historiques Parquet manquantes) plutôt qu'une
                    // exception HTTP. Il faut l'afficher clairement, sinon l'app
                    // semble "ne rien faire" silencieusement.
                    if (!body.error.isNullOrBlank()) {
                        appendLog("⚠️ ${body.error}")
                        appendLog("💡 Astuce : utilisez d'abord le bouton \"Télécharger l'historique\" ci-dessus.")
                        Toast.makeText(requireContext(), "Backtest impossible : données manquantes.", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val m = body.metrics

                    if (m != null) {
                        appendLog("📊 Analyse des résultats audités...")

                        val displayMainTf = (body.mainTf ?: mainTf).uppercase()
                        val displayPeriod = body.period ?: "N/A"
                        binding.tvBacktestTfInfo.text = "TF Principal : $displayMainTf   •   Confirmation : ${confirmTf.uppercase()}"
                        binding.tvBacktestPeriodInfo.text = "Période simulée : $displayPeriod"

                        binding.tvWinRate.text = "WIN RATE : ${m.winRatePct}%"
                        binding.tvProfitFactor.text = "Profit Factor : ${m.profitFactor}   |   Net : +${m.netProfitPct}%"
                        binding.tvDrawdown.text = "Max Drawdown : -${m.maxDrawdownPct}%"
                        binding.tvTradesStats.text = "Clôturés: ${m.closedTrades} (Win: ${m.winningTrades}, Loss: ${m.losingTrades}) | En Cours: ${m.openTrades}"

                        val trades = body.trades
                        if (!trades.isNullOrEmpty()) {
                            appendLog("🎯 Total Trades : ${trades.size} (Détails ci-dessous)")
                            binding.tvTradesList.text = trades.take(25).joinToString("\n") { t ->
                                val actionEmoji = if (t.action == "BUY") "🟢 BUY" else "🔴 SELL"
                                val resEmoji = when (t.result) {
                                    "WIN" -> "✅ WIN (+${t.pips} pips)"
                                    "LOSS" -> "❌ LOSS (${t.pips} pips)"
                                    else -> "⏳ OPEN (Trade en cours)"
                                }
                                "$actionEmoji @ ${t.entryPrice} ➔ $resEmoji [${t.entryTime}]"
                            }
                        } else {
                            binding.tvTradesList.text = "Aucun trade exécuté sur cette période."
                        }

                        appendLog("✅ Audit terminé avec succès !")
                        binding.cardResults.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), "✅ Audit Backtest $selectedAsset terminé !", Toast.LENGTH_SHORT).show()
                    } else {
                        appendLog("⚠️ Réponse du serveur sans résultats exploitables.")
                        Toast.makeText(requireContext(), "Aucun résultat exploitable.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    appendLog("❌ Erreur serveur : ${response.code()}")
                    Toast.makeText(requireContext(), "Erreur serveur : ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                appendLog("❌ Erreur : ${e.message}")
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

