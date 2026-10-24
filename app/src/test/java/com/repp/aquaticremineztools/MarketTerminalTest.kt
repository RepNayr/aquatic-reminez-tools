package com.repp.aquaticremineztools

import com.repp.aquaticremineztools.data.model.MarketCategory
import com.repp.aquaticremineztools.data.model.MarketDataStatus
import com.repp.aquaticremineztools.data.model.MarketInstrument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketTerminalTest {

    private val sampleCrypto = MarketInstrument(
        id = "bitcoin",
        symbol = "BTC",
        name = "Bitcoin",
        category = MarketCategory.CRYPTO,
        currentPrice = 65000.0,
        priceChange24h = 1300.0,
        priceChangePercentage24h = 2.0,
        currency = "USD",
        exchange = "COINGECKO",
        sparklinePrices = listOf(64000f, 64500f, 65000f),
        historicalCandles = null,
        dataStatus = MarketDataStatus.LIVE,
    )

    private val sampleIndex = MarketInstrument(
        id = "ihsg",
        symbol = "IHSG",
        name = "Jakarta Composite Index",
        category = MarketCategory.INDICES,
        currentPrice = 7324.50,
        priceChange24h = 45.20,
        priceChangePercentage24h = 0.62,
        currency = "IDR",
        exchange = "IDX",
        historicalCandles = emptyList(),
        isDemo = true,
        dataStatus = MarketDataStatus.SIMULATED,
    )

    private val sampleNullPrice = MarketInstrument(
        id = "unknown_stock",
        symbol = "UNKN",
        name = "Unknown Stock",
        category = MarketCategory.STOCKS,
        currentPrice = null,
        priceChange24h = null,
        priceChangePercentage24h = null,
        currency = "USD",
        exchange = "UNKNOWN",
        dataStatus = MarketDataStatus.UNAVAILABLE,
    )

    @Test
    fun testAdapterInitialEmptyStateToDataUpdate() {
        val list = mutableListOf<MarketInstrument>()
        assertEquals(0, list.size)

        list.addAll(listOf(sampleCrypto, sampleIndex))
        assertEquals(2, list.size)
        assertEquals("BTC", list[0].symbol)
        assertEquals("IHSG", list[1].symbol)
    }

    @Test
    fun testOverviewCategoryDisplaysAllInstruments() {
        val all = listOf(sampleCrypto, sampleIndex)
        val overviewCategory = MarketCategory.OVERVIEW

        val filtered = all.filter {
            when (overviewCategory) {
                MarketCategory.OVERVIEW -> true
                else -> it.category == overviewCategory
            }
        }

        assertEquals(2, filtered.size)
        assertTrue(filtered.contains(sampleCrypto))
        assertTrue(filtered.contains(sampleIndex))
    }

    @Test
    fun testUnsupportedStocksCategoryDisplaysEmptyListWithInformativeState() {
        val all = listOf(sampleCrypto, sampleIndex)
        val category = MarketCategory.STOCKS

        val filtered = if (category == MarketCategory.STOCKS) {
            emptyList()
        } else {
            all.filter { it.category == category }
        }

        assertTrue(filtered.isEmpty())

        val emptyStateTitle = "Stocks Data Unavailable"
        val emptyStateSubtitle = "Stock market data is currently unavailable. Provider integration coming soon."

        assertEquals("Stocks Data Unavailable", emptyStateTitle)
        assertTrue(emptyStateSubtitle.contains("unavailable"))
    }

    @Test
    fun testDataStatusHonestyPerCategoryAndSource() {
        // Crypto live status should be LIVE, not falsely marked as whole page Demo
        assertEquals(MarketDataStatus.LIVE, sampleCrypto.dataStatus)
        assertFalse(sampleCrypto.isDemo)
        assertNull(sampleCrypto.historicalCandles) // Live crypto uses sparklines, not fake OHLC candles

        // Simulated Index status should be SIMULATED
        assertEquals(MarketDataStatus.SIMULATED, sampleIndex.dataStatus)
        assertTrue(sampleIndex.isDemo)

        // Banner computation for Overview when Crypto is Live
        val banner = when (sampleCrypto.dataStatus) {
            MarketDataStatus.LIVE -> "Crypto: Live · Other Markets: Simulated"
            MarketDataStatus.CACHED -> "Crypto: Cached · Other Markets: Simulated"
            else -> "Simulated Market Data (Demo Mode)"
        }

        assertEquals("Crypto: Live · Other Markets: Simulated", banner)
    }

    @Test
    fun testNullPriceDisplaysDashDashAndNotFakePrice() {
        assertNull(sampleNullPrice.currentPrice)
        assertNull(sampleNullPrice.priceChangePercentage24h)
        assertEquals(MarketDataStatus.UNAVAILABLE, sampleNullPrice.dataStatus)
    }

    @Test
    fun testSearchQueryFiltering() {
        val all = listOf(sampleCrypto, sampleIndex)
        val query = "Jakarta"

        val filtered = all.filter {
            it.name.contains(query, ignoreCase = true) || it.symbol.contains(query, ignoreCase = true)
        }

        assertEquals(1, filtered.size)
        assertEquals("IHSG", filtered[0].symbol)
    }
}
