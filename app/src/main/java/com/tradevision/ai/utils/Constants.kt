package com.tradevision.ai.utils

import com.tradevision.ai.BuildConfig

object Constants {
    // URL backend désormais configurable par variante de build (voir app/build.gradle.kts,
    // buildConfigField BASE_URL) plutôt que codée en dur — audit F-02.
    val BASE_URL: String = BuildConfig.BASE_URL

    val SUPPORTED_ASSETS = listOf(
        "EUR/USD",
        "GBP/USD",
        "USD/JPY",
        "XAU/USD"
    )
}