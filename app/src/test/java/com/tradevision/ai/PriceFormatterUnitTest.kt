package com.tradevision.ai

import com.tradevision.ai.utils.PriceFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests unitaires purs JVM (pas besoin d'un device/émulateur).
 * Remplace le test générique par défaut d'Android Studio.
 */
class PriceFormatterUnitTest {

    @Test
    fun `null value returns dash`() {
        assertEquals("-", PriceFormatter.format(null, "EUR/USD"))
    }

    @Test
    fun `EUR USD formatted with 5 decimals`() {
        assertEquals("1.10500", PriceFormatter.format(1.105, "EUR/USD"))
    }

    @Test
    fun `USD JPY formatted with 3 decimals`() {
        assertEquals("148.500", PriceFormatter.format(148.5, "USD/JPY"))
    }

    @Test
    fun `XAU USD formatted with 2 decimals`() {
        assertEquals("1950.50", PriceFormatter.format(1950.5, "XAU/USD"))
    }

    @Test
    fun `unknown symbol falls back to 5 decimals`() {
        assertEquals("1.23000", PriceFormatter.format(1.23, "UNKNOWN/PAIR"))
    }
}
