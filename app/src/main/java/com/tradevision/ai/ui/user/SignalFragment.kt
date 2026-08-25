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
import kotlinx.coroutines.launch
import java.util.Locale

class SignalFragment : Fragment() {

    private var _binding: FragmentSignalBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private var selectedAsset = "EUR/USD"
    private var lastNotifiedSignal = ""

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
        checkTestStatus()
        loadSignal(selectedAsset)

        binding.btnCopyEntry.setOnClickListener { copySingleValue("Entry", binding.tvEntry.text.toString()) }
        binding.btnCopySL.setOnClickListener { copySingleValue("Stop Loss", binding.tvSL.text.toString()) }
        binding.btnCopyTP1.setOnClickListener { copySingleValue("TP1", binding.tvTP1.text.toString()) }
        binding.btnCopyTP2.setOnClickListener { copySingleValue("TP2", binding.tvTP2.text.toString()) }
        binding.btnCopyTP3.setOnClickListener { copySingleValue("TP3", binding.tvTP3.text.toString()) }

        binding.btnCopy.setOnClickListener { copyAllLevels() }
    }

    private fun checkTestStatus() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val resp = api.getTestStatus()
                if (resp.isSuccessful && resp.body() != null) {
                    val isSimulation = resp.body()!!["simulation_mode"] as? Boolean ?: false
                    binding.cardTestBanner.visibility = if (isSimulation) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                binding.cardTestBanner.visibility = View.GONE
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

        lifecycleScope.launch {
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

                        val signalKey = "${sig.symbol}_${sig.action}_${sig.entryPrice}"
                        if (signalKey != lastNotifiedSignal && sig.confidence >= 70) {
                            lastNotifiedSignal = signalKey
                            NotificationHelper.showSignalNotification(
                                context = requireContext(),
                                symbol = sig.symbol,
                                action = sig.action,
                                confidence = sig.confidence,
                                entry = sig.entryPrice,
                                sl = sig.stopLoss,
                                tp1 = sig.takeProfit1
                            )
                        }
                    } else {
                        binding.levelsContainer.visibility = View.GONE
                    }

                    binding.tvReasons.text = sig.reasons ?: "Analyse terminée avec score déterministe."

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
        _binding = null
    }
}
