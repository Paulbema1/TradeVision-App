package com.tradevision.ai.ui.user

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tradevision.ai.R
import com.tradevision.ai.data.network.ApiClient
import com.tradevision.ai.data.network.SessionManager
import com.tradevision.ai.databinding.FragmentSignalBinding
import com.tradevision.ai.utils.Constants
import com.tradevision.ai.utils.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class SignalFragment : Fragment() {

    private var _binding: FragmentSignalBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private var selectedAsset = "EUR/USD"
    private var autoRefreshJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        setupAssetChips()
        startAutoRefreshLoop()

        binding.btnCopyEntry.setOnClickListener { copySingleValue("Entry", binding.tvEntry.text.toString()) }
        binding.btnCopySL.setOnClickListener { copySingleValue("Stop Loss", binding.tvSL.text.toString()) }
        binding.btnCopyTP1.setOnClickListener { copySingleValue("TP1", binding.tvTP1.text.toString()) }
        binding.btnCopyTP2.setOnClickListener { copySingleValue("TP2", binding.tvTP2.text.toString()) }
        binding.btnCopyTP3.setOnClickListener { copySingleValue("TP3", binding.tvTP3.text.toString()) }

        binding.btnCopy.setOnClickListener { copyAllLevels() }
    }

    private fun startAutoRefreshLoop() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                loadSignal(selectedAsset)
                delay(30000)
            }
        }
    }

    private fun copySingleValue(label: String, value: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, value)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "🧾 $label ($value) copié !", Toast.LENGTH_SHORT).show()
    }

    private fun setupAssetChips() {
        binding.assetContainer.removeAllViews()

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
                    loadSignal(asset)
                }
            }
            binding.assetContainer.addView(button)
        }
    }

    private fun loadSignal(symbol: String) {
        binding.tvAsset.text = symbol
        binding.tvAction.text = "LOADING..."
        binding.tvAction.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))

        val mainTf = sessionManager.getMainTf()
        val confirmTf = sessionManager.getConfirmTf()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val response = api.analyzeAsset(symbol, mainTf = mainTf, confirmTf = confirmTf)

                if (response.isSuccessful && response.body() != null) {
                    val sig = response.body()!!

                    binding.tvAsset.text = sig.symbol
                    binding.tvAction.text = sig.action
                    binding.tvConfidence.text = "Confiance : ${sig.confidence}%"
                    binding.tvScore.text = "Score : ${sig.score} / 100"

                    val mainDisplay = sig.mainTimeframe.uppercase()
                    val confirmDisplay = sig.confirmationTimeframe?.uppercase() ?: "4H"
                    binding.tvTimeframes.text = "Main : $mainDisplay   •   Confirmation : $confirmDisplay"

                    val actionColor = when (sig.action) {
                        "BUY" -> ContextCompat.getColor(requireContext(), R.color.signal_buy)
                        "SELL" -> ContextCompat.getColor(requireContext(), R.color.signal_sell)
                        else -> ContextCompat.getColor(requireContext(), R.color.signal_wait)
                    }
                    binding.tvAction.setTextColor(actionColor)

                    if (sig.action != "WAIT") {
                        binding.levelsContainer.visibility = View.VISIBLE

                        val decCount = if (sig.symbol.contains("JPY")) 3 else (if (sig.symbol.contains("XAU")) 2 else 5)
                        val formatStr = "%.${decCount}f"

                        binding.tvEntry.text = String.format(Locale.US, formatStr, sig.entryPrice ?: 0.0)
                        binding.tvSL.text = String.format(Locale.US, formatStr, sig.stopLoss ?: 0.0)
                        binding.tvTP1.text = String.format(Locale.US, formatStr, sig.takeProfit1 ?: 0.0)
                        binding.tvTP2.text = String.format(Locale.US, formatStr, sig.takeProfit2 ?: 0.0)
                        binding.tvTP3.text = String.format(Locale.US, formatStr, sig.takeProfit3 ?: 0.0)
                        binding.tvRR.text = "Risk / Reward : 1 : ${sig.riskReward ?: 2.5}"

                        // Deduplication PERSISTANTE (survit a la destruction du fragment,
                        // contrairement a une simple variable en memoire). Utilise signal_id
                        // quand disponible (backend v9.1.0+), sinon repli sur une empreinte
                        // locale pour rester fonctionnel meme sans signal_id.
                        val dedupKey = sig.signalId ?: "${sig.symbol}_${sig.action}_${sig.entryPrice}"
                        if (!sessionManager.hasSeenSignal(dedupKey) && sig.confidence >= 70) {
                            sessionManager.markSignalSeen(dedupKey)
                            NotificationHelper.showSignalNotification(
                                context = requireContext(),
                                symbol = sig.symbol,
                                action = sig.action,
                                confidence = sig.confidence,
                                entry = sig.entryPrice,
                                sl = sig.stopLoss,
                                tp1 = sig.takeProfit1,
                                signalId = sig.signalId
                            )
                        }
                    } else {
                        binding.levelsContainer.visibility = View.GONE
                    }

                    binding.tvReasons.text = sig.reasons ?: "Analyse terminée avec score déterministe."
                    renderFactorBadges(sig)

                } else {
                    binding.tvAction.text = "ERROR"
                    binding.tvReasons.text = "Impossible de récupérer le signal (Code ${response.code()})"
                }
            } catch (e: Exception) {
                binding.tvAction.text = "OFFLINE"
                binding.tvReasons.text = "Vérifiez votre connexion Internet : ${e.message}"
            }
        }
    }

    /**
     * Génère dynamiquement les badges de statut de la section "WHY THIS SIGNAL?"
     * à partir de score_breakdown (déjà calculé par le moteur déterministe) et
     * de ai_confirmed. Barèmes réels par catégorie (cahier des charges v9.1.0) :
     * technical=25, smc=30, mtf=20 (tout ou rien), momentum=5, news=5, calendar=5.
     * N'invente aucune donnée : direction (BULLISH/BEARISH) déduite de sig.action
     * uniquement quand le score de la catégorie est fort.
     */
    private fun renderFactorBadges(sig: com.tradevision.ai.data.model.SignalResponse) {
        binding.factorsContainer.removeAllViews()
        val breakdown = sig.scoreBreakdown ?: emptyMap()
        val isBuy = sig.action == "BUY"

        fun addBadge(label: String, status: String, bgColor: Int, textColor: Int) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                val p = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 8) }
                layoutParams = p
            }
            val labelView = android.widget.TextView(requireContext()).apply {
                text = label
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val badgeView = android.widget.TextView(requireContext()).apply {
                text = status
                setTextColor(textColor)
                textSize = 11f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(20, 8, 20, 8)
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_status_badge)?.mutate()?.apply {
                    setTint(bgColor)
                }
            }
            row.addView(labelView)
            row.addView(badgeView)
            binding.factorsContainer.addView(row)
        }

        fun highLowBadge(score: Int, high: Int, mid: Int, highLabel: String, midLabel: String, lowLabel: String): Triple<String, Int, Int> {
            val green = ContextCompat.getColor(requireContext(), R.color.badge_green_bg)
            val greenTxt = ContextCompat.getColor(requireContext(), R.color.badge_green_text)
            val orange = ContextCompat.getColor(requireContext(), R.color.badge_orange_bg)
            val orangeTxt = ContextCompat.getColor(requireContext(), R.color.badge_orange_text)
            val red = ContextCompat.getColor(requireContext(), R.color.badge_red_bg)
            val redTxt = ContextCompat.getColor(requireContext(), R.color.badge_red_text)
            return when {
                score >= high -> Triple(highLabel, green, greenTxt)
                score >= mid -> Triple(midLabel, orange, orangeTxt)
                else -> Triple(lowLabel, red, redTxt)
            }
        }

        val direction = if (isBuy) "BULLISH" else "BEARISH"

        breakdown["technical"]?.let { score ->
            val (label, bg, txt) = highLowBadge(score, 18, 10, direction, "NEUTRAL", "CAUTION")
            addBadge("Technical Analysis", label, bg, txt)
        }
        breakdown["smc"]?.let { score ->
            val (label, bg, txt) = highLowBadge(score, 20, 8, direction, "NEUTRAL", "WEAK")
            addBadge("SMC", label, bg, txt)
        }
        breakdown["momentum"]?.let { score ->
            val (label, bg, txt) = highLowBadge(score, 5, 3, direction, "NEUTRAL", "WEAK")
            addBadge("Momentum", label, bg, txt)
        }
        breakdown["mtf"]?.let { score ->
            val (label, bg, txt) = highLowBadge(score, 20, 0, "CONFIRMED", "NEUTRAL", "NEUTRAL")
            addBadge("MTF", label, bg, txt)
        }
        breakdown["news"]?.let { score ->
            val (label, bg, txt) = highLowBadge(score, 5, 1, "GOOD", "CAUTION", "RISK")
            addBadge("News", label, bg, txt)
        }
        breakdown["calendar"]?.let { score ->
            val (label, bg, txt) = highLowBadge(score, 5, 2, "CLEAR", "CAUTION", "HIGH RISK")
            addBadge("Economic Calendar", label, bg, txt)
        }
        when (sig.aiConfirmed) {
            true -> addBadge(
                "AI Confirmation", "CONFIRMED",
                ContextCompat.getColor(requireContext(), R.color.badge_green_bg),
                ContextCompat.getColor(requireContext(), R.color.badge_green_text)
            )
            false -> addBadge(
                "AI Confirmation", "REJECTED",
                ContextCompat.getColor(requireContext(), R.color.badge_red_bg),
                ContextCompat.getColor(requireContext(), R.color.badge_red_text)
            )
            null -> { /* Pas d'évaluation IA (ex: WAIT) -> pas de badge affiché */ }
        }
    }

    private fun copyAllLevels() {
        val asset = binding.tvAsset.text.toString()
        val action = binding.tvAction.text.toString()
        val entry = binding.tvEntry.text.toString()
        val sl = binding.tvSL.text.toString()
        val tp1 = binding.tvTP1.text.toString()
        val tp2 = binding.tvTP2.text.toString()
        val tp3 = binding.tvTP3.text.toString()
        val rr = binding.tvRR.text.toString()

        val formattedText = """
            TradeVision AI Signal
            ────────────────────
            Asset  : $asset
            Action : $action
            Entry  : $entry
            SL     : $sl
            TP1    : $tp1
            TP2    : $tp2
            TP3    : $tp3
            $rr
            ────────────────────
        """.trimIndent()

        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("TradeVision Signal", formattedText)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(requireContext(), "📋 COPY ALL : Résumé copié !", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autoRefreshJob?.cancel()
        _binding = null
    }
}

