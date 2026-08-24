package com.tradevision.ai.data.model

import com.google.gson.annotations.SerializedName

data class AuthRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("role") val role: String,
    @SerializedName("username") val username: String
)

data class FcmTokenRequest(
    @SerializedName("fcm_token") val fcmToken: String
)

data class UserResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("role") val role: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("notifications_enabled") val notificationsEnabled: Boolean,
    @SerializedName("preferred_assets") val preferredAssets: String
)

data class SignalResponse(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("action") val action: String,
    @SerializedName("confidence") val confidence: Int,
    @SerializedName("score") val score: Int,
    @SerializedName("entry_price") val entryPrice: Double?,
    @SerializedName("stop_loss") val stopLoss: Double?,
    @SerializedName("take_profit_1") val takeProfit1: Double?,
    @SerializedName("take_profit_2") val takeProfit2: Double?,
    @SerializedName("take_profit_3") val takeProfit3: Double?,
    @SerializedName("risk_reward") val riskReward: Double?,
    @SerializedName("main_timeframe") val mainTimeframe: String,
    @SerializedName("confirmation_timeframe") val confirmationTimeframe: String?,
    @SerializedName("news_used") val newsUsed: Boolean,
    @SerializedName("news_status") val newsStatus: String?,
    @SerializedName("news_summary") val newsSummary: String?,
    @SerializedName("data_quality") val dataQuality: String,
    @SerializedName("ai_confirmed") val aiConfirmed: Boolean?,
    @SerializedName("reasons") val reasons: String?
)

data class SignalHistoryItem(
    @SerializedName("id") val id: Int,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("action") val action: String,
    @SerializedName("score") val score: Int,
    @SerializedName("confidence") val confidence: Int,
    @SerializedName("main_timeframe") val mainTimeframe: String,
    @SerializedName("confirmation_timeframe") val confirmationTimeframe: String?,
    @SerializedName("news_used") val newsUsed: Boolean,
    @SerializedName("news_status") val newsStatus: String?,
    @SerializedName("data_quality") val dataQuality: String,
    @SerializedName("created_at") val createdAt: String
)

data class AdminUserListItem(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("role") val role: String,
    @SerializedName("has_fcm_token") val hasFcmToken: Boolean,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("notifications_enabled") val notificationsEnabled: Boolean,
    @SerializedName("created_at") val createdAt: String
)

data class KeyMetricItem(
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: String,
    @SerializedName("is_ready") val isReady: Boolean,
    @SerializedName("cooldown_remaining_sec") val cooldownRemainingSec: Int,
    @SerializedName("total_requests") val totalRequests: Int,
    @SerializedName("total_success") val totalSuccess: Int,
    @SerializedName("total_429") val total429: Int
)

data class MessageResponse(
    @SerializedName("message") val message: String
)

// ── BACKTEST MODELS ───────────────────────────────────────

data class BacktestMetrics(
    @SerializedName("initial_balance") val initialBalance: Double,
    @SerializedName("final_balance") val finalBalance: Double,
    @SerializedName("net_profit_pct") val netProfitPct: Double,
    @SerializedName("total_trades") val totalTrades: Int,
    @SerializedName("closed_trades") val closedTrades: Int,
    @SerializedName("open_trades") val openTrades: Int = 0,
    @SerializedName("winning_trades") val winningTrades: Int,
    @SerializedName("losing_trades") val losingTrades: Int,
    @SerializedName("win_rate_pct") val winRatePct: Double,
    @SerializedName("profit_factor") val profitFactor: Double,
    @SerializedName("expectancy_r") val expectancyR: Double,
    @SerializedName("max_drawdown_pct") val maxDrawdownPct: Double
)

data class BacktestTrade(
    @SerializedName("entry_time") val entryTime: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("action") val action: String,
    @SerializedName("score") val score: Int,
    @SerializedName("entry_price") val entryPrice: Double,
    @SerializedName("exit_price") val exitPrice: Double,
    @SerializedName("result") val result: String,
    @SerializedName("pips") val pips: Double,
    @SerializedName("hit_tp") val hitTp: Int,
    @SerializedName("r_multiple") val rMultiple: Double
)

data class BacktestResponse(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("main_tf") val mainTf: String,
    @SerializedName("period") val period: String,
    @SerializedName("metrics") val metrics: BacktestMetrics?,
    @SerializedName("trades") val trades: List<BacktestTrade>?
)
