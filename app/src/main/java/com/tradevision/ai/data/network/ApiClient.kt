package com.tradevision.ai.data.network

import android.content.Context
import com.tradevision.ai.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    @Volatile
    private var retrofit: Retrofit? = null

    fun getApiService(context: Context): ApiService {
        val appContext = context.applicationContext

        if (retrofit == null) {
            synchronized(this) {
                if (retrofit == null) {
                    val loggingInterceptor = HttpLoggingInterceptor().apply {
                        level = if (com.tradevision.ai.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                    }

                    try {
                        // redact Authorization header from logs when supported
                        loggingInterceptor.redactHeader("Authorization")
                    } catch (_: Throwable) {
                        // ignore if method not available on older versions
                    }

                    val okHttpClient = OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .addInterceptor { chain ->
                            // Retrieve token dynamically from secure SessionManager
                            val sessionManager = SessionManager(appContext)
                            val requestBuilder = chain.request().newBuilder()
                            val token = sessionManager.getToken()
                            if (!token.isNullOrEmpty()) {
                                requestBuilder.addHeader("Authorization", "Bearer $token")
                            }
                            chain.proceed(requestBuilder.build())
                        }
                        .addInterceptor(loggingInterceptor)
                        .build()

                    retrofit = Retrofit.Builder()
                        .baseUrl(Constants.BASE_URL)
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                }
            }
        }

        return retrofit!!.create(ApiService::class.java)
    }
}
