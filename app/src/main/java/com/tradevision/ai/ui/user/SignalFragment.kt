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
import com.tradevision.ai.databinding.FragmentSignalBinding
import com.tradevision.ai.utils.Constants
import kotlinx.coroutines.launch

class SignalFragment : Fragment() {

    private var _binding: FragmentSignalBinding? = null
    private val binding get() = _binding!!
    private var selectedAsset = "EUR/USD"

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

        setupAssetChips()
        loadSignal(selectedAsset)

        binding.btnCopy.setOnClickListener { copyLevels() }
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

        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val response = api.analyzeAsset(symbol)

                if (response.isSuccessful && response.body() != null) {
                    val sig = response.body()!!

                    binding.tvAsset.text = sig.symbol
                    binding.tvAction.text = sig.action
                    binding.tvConfidence.text = "Confiance : ${sig.confidence}%"
                    binding.tvScore.text = "Score : ${sig.score} / 100"

                    val mainTf = sig.mainTimeframe.uppercase()
                    val confirmTf = sig.confirmationTimeframe?.uppercase() ?: "4H"
                    binding.tvTimeframes.text = "Main : $mainTf   •   Confirmation : $confirmTf"

                    val actionColor = when (sig.action) {
                        "BUY" -> ContextCompat.getColor(requireContext(), R.color.signal_buy)
                        "SELL" -> ContextCompat.getColor(requireContext(), R.color.signal_sell)
                        else -> ContextCompat.getColor(requireContext(), R.color.signal_wait)
                    }
                    binding.tvAction.setTextColor(actionColor)

                    if (sig.action != "WAIT") {
                        binding.levelsContainer.visibility = View.VISIBLE
                        binding.tvEntry.text = String.format("%.5f", sig.entryPrice ?: 0.0)
                        binding.tvSL.text = String.format("%.5f", sig.stopLoss ?: 0.0)
                        binding.tvTP1.text = String.format("%.5f", sig.takeProfit1 ?: 0.0)
                        binding.tvTP2.text = String.format("%.5f", sig.takeProfit2 ?: 0.0)
                        binding.tvTP3.text = String.format("%.5f", sig.takeProfit3 ?: 0.0)
                        binding.tvRR.text = "Risk / Reward : 1 : ${sig.riskReward ?: 2.5}"
                    } else {
                        binding.levelsContainer.visibility = View.GONE
                    }

                    binding.tvReasons.text = sig.reasons ?: "Analysis completed with deterministic score."

                } else {
                    binding.tvAction.text = "ERROR"
                    binding.tvReasons.text = "Impossible de récupérer les données du marché."
                }
            } catch (e: Exception) {
                binding.tvAction.text = "OFFLINE"
                binding.tvReasons.text = "Erreur réseau : ${e.message}"
            }
        }
    }

    private fun copyLevels() {
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

        Toast.makeText(requireContext(), "📋 COPY LEVELS : Niveaux copiés !", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
