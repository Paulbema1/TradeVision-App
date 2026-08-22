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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChips()
        loadSignal(selectedAsset)
        binding.btnCopy.setOnClickListener { copyLevels() }
    }

    private fun setupChips() {
        binding.assetContainer.removeAllViews()
        Constants.SUPPORTED_ASSETS.forEach { asset ->
            val btn = Button(requireContext())
            btn.text = asset
            btn.textSize = 12f
            btn.isAllCaps = true
            if (asset == selectedAsset) {
                btn.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.admin_accent))
                btn.setTextColor(Color.WHITE)
            } else {
                btn.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg_card))
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, 16, 0)
            btn.layoutParams = lp
            btn.setOnClickListener {
                selectedAsset = asset
                setupChips()
                loadSignal(asset)
            }
            binding.assetContainer.addView(btn)
        }
    }

    private fun loadSignal(symbol: String) {
        binding.tvAsset.text = symbol
        binding.tvAction.text = "LOADING..."
        binding.tvAction.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))

        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService(requireContext())
                val res = api.analyzeAsset(symbol)
                if (res.isSuccessful && res.body() != null) {
                    val s = res.body()!!
                    binding.tvAsset.text = s.symbol
                    binding.tvAction.text = s.action
                    binding.tvConfidence.text = "Confiance : ${s.confidence}%"
                    binding.tvScore.text = "Score : ${s.score} / 100"
                    binding.tvTimeframes.text =
                        "Main : ${s.mainTimeframe.uppercase()}   •   Confirmation : ${(s.confirmationTimeframe ?: "4h").uppercase()}"

                    val color = when (s.action) {
                        "BUY" -> ContextCompat.getColor(requireContext(), R.color.signal_buy)
                        "SELL" -> ContextCompat.getColor(requireContext(), R.color.signal_sell)
                        else -> ContextCompat.getColor(requireContext(), R.color.signal_wait)
                    }
                    binding.tvAction.setTextColor(color)

                    if (s.action != "WAIT") {
                        binding.levelsContainer.visibility = View.VISIBLE
                        binding.tvEntry.text = String.format("%.5f", s.entryPrice ?: 0.0)
                        binding.tvSL.text = String.format("%.5f", s.stopLoss ?: 0.0)
                        binding.tvTP1.text = String.format("%.5f", s.takeProfit1 ?: 0.0)
                        binding.tvTP2.text = String.format("%.5f", s.takeProfit2 ?: 0.0)
                        binding.tvTP3.text = String.format("%.5f", s.takeProfit3 ?: 0.0)
                        binding.tvRR.text = "Risk / Reward : 1 : ${s.riskReward ?: 2.5}"
                    } else {
                        binding.levelsContainer.visibility = View.GONE
                    }
                    binding.tvReasons.text = s.reasons ?: "Analyse terminée."
                } else {
                    binding.tvAction.text = "ERROR"
                    binding.tvReasons.text = "Impossible de récupérer le signal."
                }
            } catch (e: Exception) {
                binding.tvAction.text = "OFFLINE"
                binding.tvReasons.text = "Erreur réseau : ${e.message}"
            }
        }
    }

    private fun copyLevels() {
        val text = """
            TradeVision AI Signal
            Asset  : ${binding.tvAsset.text}
            Action : ${binding.tvAction.text}
            Entry  : ${binding.tvEntry.text}
            SL     : ${binding.tvSL.text}
            TP1    : ${binding.tvTP1.text}
            TP2    : ${binding.tvTP2.text}
            TP3    : ${binding.tvTP3.text}
            ${binding.tvRR.text}
        """.trimIndent()

        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("TradeVision Signal", text))
        Toast.makeText(requireContext(), "Niveaux copiés", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
