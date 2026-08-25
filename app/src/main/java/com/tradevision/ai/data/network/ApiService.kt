package com.tradevision.ai.data.network

import com.tradevision.ai.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/register")
    suspend fun register(@Body request: AuthRequest): Response<TokenResponse>

    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): Response<TokenResponse>

    @POST("auth/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest): Response<MessageResponse>

    @GET("auth/me")
    suspend fun getProfile(): Response<UserResponse>

    @GET("signals/analyze/{symbol}")
    suspend fun analyzeAsset(
        @Path(value = "symbol", encoded = true) symbol: String,
        @Query("main_tf") mainTf: String = "1h",
        @Query("confirm_tf") confirmTf: String = "4h"
    ): Response<SignalResponse>

    @GET("signals/history")
    suspend fun getHistory(
        @Query("symbol") symbol: String? = null,
        @Query("limit") limit: Int = 30
    ): Response<List<SignalHistoryItem>>

    @GET("admin/users")
    suspend fun listUsers(): Response<List<AdminUserListItem>>

    @GET("admin/keys-metrics")
    suspend fun getKeysMetrics(): Response<List<KeyMetricItem>>

    @POST("admin/cache/clear")
    suspend fun clearCache(): Response<MessageResponse>

    @POST("admin/scan-all")
    suspend fun scanAll(): Response<Map<String, Any>>

    @POST("admin/backtest")
    suspend fun runBacktest(
        @Query("symbol") symbol: String,
        @Query("main_tf") mainTf: String = "1h",
        @Query("confirm_tf") confirmTf: String = "4h"
    ): Response<BacktestResponse>

    // 🧪 ENDPOINTS TEST LAB
    @POST("test/mode")
    suspend fun setTestMode(@Body request: Map<String, Boolean>): Response<Map<String, Any>>

    @GET("test/status")
    suspend fun getTestStatus(): Response<Map<String, Any>>
}
