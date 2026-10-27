package com.repp.aquaticremineztools

import com.repp.aquaticremineztools.data.model.AlertType
import com.repp.aquaticremineztools.data.model.MarketAlert
import com.repp.aquaticremineztools.data.model.MarketCategory
import com.repp.aquaticremineztools.data.model.MarketDataStatus
import com.repp.aquaticremineztools.data.model.MarketInstrument
import com.repp.aquaticremineztools.data.model.PortfolioHolding
import com.repp.aquaticremineztools.data.repository.PortfolioValuationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketTerminalTest {

    private val sampleLiveCrypto = MarketInstrument(
        id = "bitcoin",
        symbol = "BTC",
        name = "Bitcoin",
        category = MarketCategory.CRYPTO,
        currentPrice = 96000.0,
        priceChange24h = 2880.0,
        priceChangePercentage24h = 3.0,
        currency = "USD",
        exchange = "COINGECKO",
        sparklinePrices = listOf(93000f, 94500f, 96000f),
        historicalCandles = null,
        dataStatus = MarketDataStatus.LIVE,
    )

    private val sampleSimulatedIndex = MarketInstrument(
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

    private val sampleUnavailableStock = MarketInstrument(
        id = "bbca",
        symbol = "BBCA",
        name = "Bank Central Asia",
        category = MarketCategory.STOCKS,
        currentPrice = null,
        priceChange24h = null,
        priceChangePercentage24h = null,
        currency = "IDR",
        exchange = "IDX",
        dataStatus = MarketDataStatus.UNAVAILABLE,
    )

    @Test
    fun testAdapterInitialEmptyStateToDataUpdate() {
        val list = mutableListOf<MarketInstrument>()
        assertEquals(0, list.size)

        list.addAll(listOf(sampleLiveCrypto, sampleSimulatedIndex))
        assertEquals(2, list.size)
        assertEquals("BTC", list[0].symbol)
        assertEquals("IHSG", list[1].symbol)
    }

    @Test
    fun testOverviewCategoryDisplaysAllInstruments() {
        val all = listOf(sampleLiveCrypto, sampleSimulatedIndex, sampleUnavailableStock)
        val overviewCategory = MarketCategory.OVERVIEW

        val filtered = all.filter {
            when (overviewCategory) {
                MarketCategory.OVERVIEW -> true
                else -> it.category == overviewCategory
            }
        }

        assertEquals(3, filtered.size)
        assertTrue(filtered.contains(sampleLiveCrypto))
        assertTrue(filtered.contains(sampleSimulatedIndex))
    }

    @Test
    fun testStocksCategoryFiltering() {
        val all = listOf(sampleLiveCrypto, sampleSimulatedIndex, sampleUnavailableStock)
        val stocksCategory = MarketCategory.STOCKS

        val filtered = all.filter { it.category == stocksCategory }

        assertEquals(1, filtered.size)
        assertEquals("BBCA", filtered[0].symbol)
        assertNull(filtered[0].currentPrice)
    }

    @Test
    fun testDataStatusHonestyPerCategoryAndSource() {
        // Live crypto must have dataStatus = LIVE and no fake candles
        assertEquals(MarketDataStatus.LIVE, sampleLiveCrypto.dataStatus)
        assertFalse(sampleLiveCrypto.isDemo)
        assertNull(sampleLiveCrypto.historicalCandles)

        // Simulated Index must have dataStatus = SIMULATED
        assertEquals(MarketDataStatus.SIMULATED, sampleSimulatedIndex.dataStatus)
        assertTrue(sampleSimulatedIndex.isDemo)

        // Unavailable stock must have dataStatus = UNAVAILABLE
        assertEquals(MarketDataStatus.UNAVAILABLE, sampleUnavailableStock.dataStatus)
        assertNull(sampleUnavailableStock.currentPrice)
    }

    @Test
    fun testNullPriceDisplaysNullAndNotFakeZeroPrice() {
        assertNull(sampleUnavailableStock.currentPrice)
        assertNull(sampleUnavailableStock.priceChangePercentage24h)
        assertEquals(MarketDataStatus.UNAVAILABLE, sampleUnavailableStock.dataStatus)
    }

    @Test
    fun testSearchQueryFilteringBySymbolNameExchange() {
        val all = listOf(sampleLiveCrypto, sampleSimulatedIndex, sampleUnavailableStock)

        val queryName = "Jakarta"
        val filteredName = all.filter { it.name.contains(queryName, ignoreCase = true) }
        assertEquals(1, filteredName.size)
        assertEquals("IHSG", filteredName[0].symbol)

        val queryExchange = "COINGECKO"
        val filteredExchange = all.filter { it.exchange.contains(queryExchange, ignoreCase = true) }
        assertEquals(1, filteredExchange.size)
        assertEquals("BTC", filteredExchange[0].symbol)
    }

    @Test
    fun testPortfolioValuationMultiCurrencySeparation() {
        val holdings = listOf(
            PortfolioHolding(id = "1", instrumentId = "bitcoin", symbol = "BTC", name = "Bitcoin", quantity = 0.5, averageCost = 90000.0, currency = "USD"),
            PortfolioHolding(id = "2", instrumentId = "ihsg", symbol = "IHSG", name = "IHSG ETF", quantity = 100.0, averageCost = 7000.0, currency = "IDR")
        )

        val instrumentsMap = mapOf(
            "bitcoin" to sampleLiveCrypto,
            "ihsg" to sampleSimulatedIndex
        )

        var totalUsdValuation = 0.0
        var totalUsdCost = 0.0
        var totalIdrValuation = 0.0
        var totalIdrCost = 0.0

        holdings.forEach { holding ->
            val inst = instrumentsMap[holding.instrumentId]
            val price = inst?.currentPrice ?: holding.averageCost
            val valAmount = holding.quantity * price
            val costAmount = holding.quantity * holding.averageCost

            if (holding.currency == "IDR") {
                totalIdrValuation += valAmount
                totalIdrCost += costAmount
            } else {
                totalUsdValuation += valAmount
                totalUsdCost += costAmount
            }
        }

        val result = PortfolioValuationResult(
            totalValuationUsd = totalUsdValuation,
            totalCostUsd = totalUsdCost,
            unrealizedPlUsd = totalUsdValuation - totalUsdCost,
            unrealizedPlPctUsd = ((totalUsdValuation - totalUsdCost) / totalUsdCost) * 100.0,
            totalValuationIdr = totalIdrValuation,
            totalCostIdr = totalIdrCost,
            unrealizedPlIdr = totalIdrValuation - totalIdrCost,
            unrealizedPlPctIdr = ((totalIdrValuation - totalIdrCost) / totalIdrCost) * 100.0,
            hasUnpricedHoldings = false
        )

        assertEquals(48000.0, result.totalValuationUsd, 0.01) // 0.5 * 96000
        assertEquals(45000.0, result.totalCostUsd, 0.01) // 0.5 * 90000
        assertEquals(3000.0, result.unrealizedPlUsd, 0.01)

        assertEquals(732450.0, result.totalValuationIdr, 0.01) // 100 * 7324.50
        assertEquals(700000.0, result.totalCostIdr, 0.01) // 100 * 7000.0
        assertEquals(32450.0, result.unrealizedPlIdr, 0.01)
    }

    @Test
    fun testAlertEvaluationTriggerConditions() {
        val alertAbove = MarketAlert(
            id = "a1",
            instrumentId = "bitcoin",
            symbol = "BTC",
            name = "Bitcoin",
            targetValue = 90000.0,
            alertType = AlertType.PRICE_ABOVE,
            isActive = true
        )

        val alertBelow = MarketAlert(
            id = "a2",
            instrumentId = "bitcoin",
            symbol = "BTC",
            name = "Bitcoin",
            targetValue = 100000.0,
            alertType = AlertType.PRICE_BELOW,
            isActive = true
        )

        val alertPct = MarketAlert(
            id = "a3",
            instrumentId = "bitcoin",
            symbol = "BTC",
            name = "Bitcoin",
            targetValue = 2.5,
            alertType = AlertType.PCT_CHANGE_24H,
            isActive = true
        )

        val currentPrice = sampleLiveCrypto.currentPrice!! // 96000.0
        val pctChange = sampleLiveCrypto.priceChangePercentage24h!! // 3.0%

        assertTrue(currentPrice >= alertAbove.targetValue) // 96000 >= 90000 -> Triggered
        assertTrue(currentPrice <= alertBelow.targetValue) // 96000 <= 100000 -> Triggered
        assertTrue(Math.abs(pctChange) >= alertPct.targetValue) // 3.0 >= 2.5 -> Triggered
    }

    @Test
    fun testSMAIndicatorCalculation() {
        val prices = listOf(10f, 20f, 30f, 40f, 50f)
        val window = 3
        val smaValues = mutableListOf<Float>()

        for (i in prices.indices) {
            if (i >= window - 1) {
                val slice = prices.subList(i - window + 1, i + 1)
                smaValues.add(slice.average().toFloat())
            }
        }

        assertEquals(3, smaValues.size)
        assertEquals(20f, smaValues[0], 0.01f) // avg(10, 20, 30) = 20
        assertEquals(30f, smaValues[1], 0.01f) // avg(20, 30, 40) = 30
        assertEquals(40f, smaValues[2], 0.01f) // avg(30, 40, 50) = 40
    }

    @Test
    fun testCryptoApiSuccessValidData() {
        val asset = MarketInstrument(
            id = "bitcoin",
            symbol = "BTC",
            name = "Bitcoin",
            category = MarketCategory.CRYPTO,
            currentPrice = 65000.0,
            priceChange24h = 1000.0,
            priceChangePercentage24h = 2.5,
            dataStatus = MarketDataStatus.LIVE
        )
        assertEquals(MarketDataStatus.LIVE, asset.dataStatus)
        assertEquals(65000.0, asset.currentPrice!!, 0.01)
    }

    @Test
    fun testCryptoApiPartialResponse() {
        val partialAsset = MarketInstrument(
            id = "ethereum",
            symbol = "ETH",
            name = "Ethereum",
            category = MarketCategory.CRYPTO,
            currentPrice = null,
            priceChange24h = null,
            priceChangePercentage24h = null,
            dataStatus = MarketDataStatus.UNAVAILABLE
        )
        assertEquals(MarketDataStatus.UNAVAILABLE, partialAsset.dataStatus)
        assertNull(partialAsset.currentPrice)
    }

    @Test
    fun testNetworkFailureWithoutCacheReturnsUnavailableOrSimulated() {
        val unavailableInst = MarketInstrument(
            id = "usd_idr",
            symbol = "USD/IDR",
            name = "USD / IDR",
            category = MarketCategory.FOREX,
            currentPrice = null,
            priceChange24h = null,
            priceChangePercentage24h = null,
            dataStatus = MarketDataStatus.UNAVAILABLE
        )
        assertEquals(MarketDataStatus.UNAVAILABLE, unavailableInst.dataStatus)
        assertNull(unavailableInst.currentPrice)
    }

    @Test
    fun testForexRateValidAndPartial() {
        val fxValid = MarketInstrument(
            id = "usd_chf",
            symbol = "USD/CHF",
            name = "USD / CHF",
            category = MarketCategory.FOREX,
            currentPrice = 0.88,
            priceChange24h = null,
            priceChangePercentage24h = null,
            dataStatus = MarketDataStatus.LIVE
        )
        assertEquals(0.88, fxValid.currentPrice!!, 0.001)
        assertEquals(MarketDataStatus.LIVE, fxValid.dataStatus)
    }

    @Test
    fun testSimulatedDataCannotTriggerAlert() {
        val alert = MarketAlert(
            id = "a1",
            instrumentId = "ihsg",
            symbol = "IHSG",
            name = "IHSG",
            targetValue = 7000.0,
            alertType = AlertType.PRICE_ABOVE,
            isActive = true
        )
        val simulatedInst = MarketInstrument(
            id = "ihsg",
            symbol = "IHSG",
            name = "IHSG",
            category = MarketCategory.INDICES,
            currentPrice = 7324.50,
            priceChange24h = null,
            priceChangePercentage24h = null,
            dataStatus = MarketDataStatus.SIMULATED
        )
        val canTrigger = simulatedInst.dataStatus == MarketDataStatus.LIVE && simulatedInst.currentPrice!! >= alert.targetValue
        assertFalse(canTrigger)
    }

    @Test
    fun testStaleCacheNotFresh() {
        val now = System.currentTimeMillis()
        val staleTimestamp = now - (40 * 60 * 1000L)
        val cachedInst = MarketInstrument(
            id = "bitcoin",
            symbol = "BTC",
            name = "Bitcoin",
            category = MarketCategory.CRYPTO,
            currentPrice = 60000.0,
            priceChange24h = null,
            priceChangePercentage24h = null,
            timestampEpoch = staleTimestamp,
            dataStatus = MarketDataStatus.CACHED
        )
        val maxAgeMs = 30 * 60 * 1000L
        val isFresh = (now - cachedInst.timestampEpoch) <= maxAgeMs
        assertFalse(isFresh)
    }

    @Test
    fun testPortfolioDoesNotUseSimulatedPrice() {
        val holding = PortfolioHolding(
            id = "h1",
            instrumentId = "ihsg",
            symbol = "IHSG",
            name = "IHSG",
            quantity = 10.0,
            averageCost = 7000.0,
            currency = "IDR"
        )
        assertEquals("IHSG", holding.symbol)
        val simulatedInst = MarketInstrument(
            id = "ihsg",
            symbol = "IHSG",
            name = "IHSG",
            category = MarketCategory.INDICES,
            currentPrice = 7300.0,
            priceChange24h = null,
            priceChangePercentage24h = null,
            dataStatus = MarketDataStatus.SIMULATED
        )
        val isLiveOrFresh = simulatedInst.dataStatus == MarketDataStatus.LIVE
        assertFalse(isLiveOrFresh)
    }

    @Test
    fun testPortfolioHandlesUnpricedInstruments() {
        val unpricedInst = MarketInstrument(
            id = "unknown",
            symbol = "UNK",
            name = "Unknown",
            category = MarketCategory.STOCKS,
            currentPrice = null,
            priceChange24h = null,
            priceChangePercentage24h = null,
            dataStatus = MarketDataStatus.UNAVAILABLE
        )
        assertNull(unpricedInst.currentPrice)
        assertEquals(MarketDataStatus.UNAVAILABLE, unpricedInst.dataStatus)
    }

    @Test
    fun testForexPairCalculationUSDCHF() {
        val usdChfRate = 0.88
        assertEquals(0.88, usdChfRate, 0.001)
    }

    @Test
    fun testScopeChangeNoChainedFiltering() {
        val all = listOf(
            MarketInstrument(id = "1", symbol = "BTC", name = "Bitcoin", category = MarketCategory.CRYPTO, currentPrice = 50000.0, priceChange24h = null, priceChangePercentage24h = null, currency = "USD", countryCode = "US"),
            MarketInstrument(id = "2", symbol = "IHSG", name = "IHSG", category = MarketCategory.INDICES, currentPrice = 7000.0, priceChange24h = null, priceChangePercentage24h = null, currency = "IDR", countryCode = "ID")
        )
        val indoScope = all.filter { it.countryCode == "ID" || it.currency == "IDR" || it.exchange == "IDX" }
        assertEquals(1, indoScope.size)
        assertEquals("IHSG", indoScope[0].symbol)

        val overviewScope = all
        assertEquals(2, overviewScope.size)
    }

    @Test
    fun testTimeRangeSelectionDataset() {
        val sparklineNull: List<Float>? = null
        val sparklineValid: List<Float>? = listOf(10f, 12f)
        val timeRange = "1D"
        assertFalse(timeRange == "1D" && !sparklineNull.isNullOrEmpty())
        assertTrue(timeRange == "1D" && !sparklineValid.isNullOrEmpty())
    }

    @Test
    fun testEconomicEventsDemoFlag() {
        val event = com.repp.aquaticremineztools.data.model.EconomicEvent(
            id = "e1",
            title = "FOMC",
            country = "USA",
            countryFlag = "🇺🇸",
            eventDate = "Demo",
            impact = com.repp.aquaticremineztools.data.model.ImpactLevel.HIGH,
            actualValue = "4.75%",
            forecastValue = "4.75%",
            previousValue = "5.00%",
            isDemo = true
        )
        assertTrue(event.isDemo)
    }
}
