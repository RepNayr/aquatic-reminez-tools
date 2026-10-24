package com.repp.aquaticremineztools.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.repp.aquaticremineztools.core.settings.MarketPreferences
import com.repp.aquaticremineztools.data.model.AlertType
import com.repp.aquaticremineztools.data.model.CandleData
import com.repp.aquaticremineztools.data.model.MarketAlert
import com.repp.aquaticremineztools.data.model.MarketCategory
import com.repp.aquaticremineztools.data.model.MarketDataStatus
import com.repp.aquaticremineztools.data.model.MarketInstrument
import com.repp.aquaticremineztools.data.model.PortfolioHolding
import com.repp.aquaticremineztools.data.model.WatchlistEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sin

data class PortfolioValuationResult(
    val totalValuationUsd: Double,
    val totalCostUsd: Double,
    val unrealizedPlUsd: Double,
    val unrealizedPlPctUsd: Double,
    val totalValuationIdr: Double,
    val totalCostIdr: Double,
    val unrealizedPlIdr: Double,
    val unrealizedPlPctIdr: Double,
    val hasUnpricedHoldings: Boolean
)

class MarketRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("market_terminal_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val cryptoRepository = CryptoRepository.getInstance(appContext)
    private val marketPreferences = MarketPreferences.getInstance(appContext)

    var isDemoModeEnabled: Boolean
        get() = marketPreferences.isDemoModeEnabled
        set(value) {
            marketPreferences.isDemoModeEnabled = value
        }

    suspend fun getAllMarketInstruments(forceRefresh: Boolean = false): Result<List<MarketInstrument>> = withContext(Dispatchers.IO) {
        try {
            val list = mutableListOf<MarketInstrument>()
            val demoMode = isDemoModeEnabled

            // 1. Fetch live Crypto from CoinGecko via CryptoRepository
            val cryptoResult = cryptoRepository.getMarketData(forceRefresh)
            cryptoResult.onSuccess { marketResult ->
                marketResult.assets.forEach { asset ->
                    val priceChangePct = asset.priceChange24h
                    val currentPrice = asset.currentPriceUsd
                    val absChange = if (currentPrice != null && priceChangePct != null) {
                        currentPrice * (priceChangePct / 100.0)
                    } else null

                    val cryptoStatus = when {
                        marketResult.isDemo -> MarketDataStatus.SIMULATED
                        marketResult.isCached -> MarketDataStatus.CACHED
                        currentPrice != null -> MarketDataStatus.LIVE
                        else -> MarketDataStatus.UNAVAILABLE
                    }
                    val isDemoCrypto = marketResult.isDemo
                    list.add(
                        MarketInstrument(
                            id = asset.id,
                            symbol = asset.symbol,
                            name = asset.name,
                            category = MarketCategory.CRYPTO,
                            currentPrice = asset.currentPriceUsd,
                            priceChange24h = absChange,
                            priceChangePercentage24h = priceChangePct,
                            currency = "USD",
                            exchange = "COINGECKO",
                            imageUrl = asset.imageUrl,
                            sparklinePrices = asset.sparklinePrices,
                            historicalCandles = if (isDemoCrypto && currentPrice != null) {
                                generateMockCandles(currentPrice, priceChangePct ?: 0.0)
                            } else null,
                            isCached = marketResult.isCached,
                            isDemo = isDemoCrypto,
                            dataStatus = cryptoStatus
                        )
                    )
                }
            }.onFailure { e ->
                Log.w("MarketRepository", "Crypto market fetch failed: ${e.message}")
            }

            // 2. Regional & Global Indices
            list.addAll(getIndicesInstruments(demoMode))

            // 3. Forex Instruments
            list.addAll(getForexInstruments(demoMode))

            // 4. Commodities
            list.addAll(getCommoditiesInstruments(demoMode))

            // 5. Economic Indicators
            list.addAll(getEconomicsInstruments(demoMode))

            // 6. ETFs & Sectors
            list.addAll(getEtfsInstruments(demoMode))

            return@withContext Result.success(list)
        } catch (e: Exception) {
            Log.e("MarketRepository", "Error assembling market instruments: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    private fun getIndicesInstruments(isDemo: Boolean): List<MarketInstrument> {
        val now = System.currentTimeMillis()
        return listOf(
            MarketInstrument(
                id = "ihsg",
                symbol = "IHSG",
                name = "Jakarta Composite Index",
                category = MarketCategory.INDICES,
                currentPrice = if (isDemo) 7324.50 else null,
                priceChange24h = if (isDemo) 45.20 else null,
                priceChangePercentage24h = if (isDemo) 0.62 else null,
                currency = "IDR",
                exchange = "IDX",
                historicalCandles = if (isDemo) generateMockCandles(7324.50, 0.62) else null,
                timestampEpoch = now,
                isDemo = isDemo,
                dataStatus = if (isDemo) MarketDataStatus.SIMULATED else MarketDataStatus.UNAVAILABLE
            ),
            MarketInstrument(
                id = "sp500",
                symbol = "SPX",
                name = "S&P 500",
                category = MarketCategory.INDICES,
                currentPrice = if (isDemo) 5815.20 else null,
                priceChange24h = if (isDemo) 32.40 else null,
                priceChangePercentage24h = if (isDemo) 0.56 else null,
                currency = "USD",
                exchange = "NYSE",
                historicalCandles = if (isDemo) generateMockCandles(5815.20, 0.56) else null,
                timestampEpoch = now,
                isDemo = isDemo,
                dataStatus = if (isDemo) MarketDataStatus.SIMULATED else MarketDataStatus.UNAVAILABLE
            ),
            MarketInstrument(
                id = "nasdaq",
                symbol = "IXIC",
                name = "Nasdaq Composite",
                category = MarketCategory.INDICES,
                currentPrice = if (isDemo) 18518.60 else null,
                priceChange24h = if (isDemo) 142.10 else null,
                priceChangePercentage24h = if (isDemo) 0.77 else null,
                currency = "USD",
                exchange = "NASDAQ",
                historicalCandles = if (isDemo) generateMockCandles(18518.60, 0.77) else null,
                timestampEpoch = now,
                isDemo = isDemo,
                dataStatus = if (isDemo) MarketDataStatus.SIMULATED else MarketDataStatus.UNAVAILABLE
            ),
            MarketInstrument(
                id = "nikkei",
                symbol = "NI225",
                name = "Nikkei 225",
                category = MarketCategory.INDICES,
                currentPrice = if (isDemo) 38981.75 else null,
                priceChange24h = if (isDemo) -125.50 else null,
                priceChangePercentage24h = if (isDemo) -0.32 else null,
                currency = "JPY",
                exchange = "TSE",
                historicalCandles = if (isDemo) generateMockCandles(38981.75, -0.32) else null,
                timestampEpoch = now,
                isDemo = isDemo,
                dataStatus = if (isDemo) MarketDataStatus.SIMULATED else MarketDataStatus.UNAVAILABLE
            )
        )
    }

    private fun getForexInstruments(isDemo: Boolean): List<MarketInstrument> {
        val now = System.currentTimeMillis()
        return listOf(
            MarketInstrument(
                id = "usd_idr",
                symbol = "USD/IDR",
                name = "US Dollar / Indonesian Rupiah",
                category = MarketCategory.FOREX,
                currentPrice = if (isDemo) 15680.0 else null,
                priceChange24h = if (isDemo) 35.0 else null,
                priceChangePercentage24h = if (isDemo) 0.22 else null,
                currency = "IDR",
                exchange = "FX",
                historicalCandles = if (isDemo) generateMockCandles(15680.0, 0.22) else null,
                timestampEpoch = now,
                isDemo = isDemo,
                dataStatus = if (isDemo) MarketDataStatus.SIMULATED else MarketDataStatus.UNAVAILABLE
            ),
            MarketInstrument(
                id = "eur_usd",
                symbol = "EUR/USD",
                name = "Euro / US Dollar",
                category = MarketCategory.FOREX,
                currentPrice = if (isDemo) 1.0852 else null,
                priceChange24h = if (isDemo) -0.0015 else null,
                priceChangePercentage24h = if (isDemo) -0.14 else null,
                currency = "USD",
                exchange = "FX",
                historicalCandles = if (isDemo) generateMockCandles(1.0852, -0.14) else null,
                timestampEpoch = now,
                isDemo = isDemo,
                dataStatus = if (isDemo) MarketDataStatus.SIMULATED else MarketDataStatus.UNAVAILABLE
            ),
            MarketInstrument(
                id = "gbp_usd",
                symbol = "GBP/USD",
                name = "British Pound / US Dollar",
                category = MarketCategory.FOREX,
                currentPrice = if (isDemo) 1.2985 else null,
                priceChange24h = if (isDemo) 0.0024 else null,
                priceChangePercentage24h = if (isDemo) 0.19 else null,
                currency = "USD",
                exchange = "FX",
                historicalCandles = if (isDemo) generateMockCandles(1.2985, 0.19) else null,
                timestampEpoch = now,
                isDemo = isDemo,
                dataStatus = if (isDemo) MarketDataStatus.SIMULATED else MarketDataStatus.UNAVAILABLE
            )
        )
    }

    private fun getCommoditiesInstruments(isDemo: Boolean): List<MarketInstrument> {
        val now = System.currentTimeMillis()
        return listOf(
            MarketInstrument(
                id = "gold",
                symbol = "XAU/USD",
                name = "Gold Spot",
                category = MarketCategory.COMMODITIES,
                currentPrice = if (isDemo) 2735.80 else null,
                priceChange24h = if (isDemo) 18.40 else null,
                priceChangePercentage24h = if (isDemo) 0.68 else null,
                currency = "USD",
                exchange = "COMEX",
                historicalCandles = if (isDemo) generateMockCandles(2735.80, 0.68) else null,
                timestampEpoch = now,
                isDemo = isDemo,
                dataStatus = if (isDemo) MarketDataStatus.SIMULATED else MarketDataStatus.UNAVAILABLE
            ),
            MarketInstrument(
                id = "oil_wti",
                symbol = "WTI",
                name = "Crude Oil WTI",
                category = MarketCategory.COMMODITIES,
                currentPrice = if (isDemo) 71.42 else null,
                priceChange24h = if (isDemo) -1.12 else null,
                priceChangePercentage24h = if (isDemo) -1.54 else null,
                currency = "USD",
                exchange = "NYMEX",
                historicalCandles = if (isDemo) generateMockCandles(71.42, -1.54) else null,
                timestampEpoch = now,
                isDemo = isDemo,
                dataStatus = if (isDemo) MarketDataStatus.SIMULATED else MarketDataStatus.UNAVAILABLE
            )
        )
    }

    private fun getEconomicsInstruments(isDemo: Boolean): List<MarketInstrument> {
        val now = System.currentTimeMillis()
        return listOf(
            MarketInstrument(
                id = "us_10y",
                symbol = "US10Y",
                name = "US 10-Year Treasury Yield",
                category = MarketCategory.ECONOMICS,
                currentPrice = if (isDemo) 4.24 else null,
                priceChange24h = if (isDemo) 0.03 else null,
                priceChangePercentage24h = if (isDemo) 0.71 else null,
                currency = "%",
                exchange = "BOND",
                historicalCandles = if (isDemo) generateMockCandles(4.24, 0.71) else null,
                timestampEpoch = now,
                isDemo = isDemo,
                dataStatus = if (isDemo) MarketDataStatus.SIMULATED else MarketDataStatus.UNAVAILABLE
            ),
            MarketInstrument(
                id = "id_inflation",
                symbol = "ID-CPI",
                name = "Indonesia Inflation Rate (YoY)",
                category = MarketCategory.ECONOMICS,
                currentPrice = if (isDemo) 2.57 else null,
                priceChange24h = if (isDemo) -0.05 else null,
                priceChangePercentage24h = if (isDemo) -1.91 else null,
                currency = "%",
                exchange = "BPS",
                historicalCandles = if (isDemo) generateMockCandles(2.57, -1.91) else null,
                timestampEpoch = now,
                isDemo = isDemo,
                dataStatus = if (isDemo) MarketDataStatus.SIMULATED else MarketDataStatus.UNAVAILABLE
            )
        )
    }

    private fun getEtfsInstruments(isDemo: Boolean): List<MarketInstrument> {
        val now = System.currentTimeMillis()
        return listOf(
            MarketInstrument(
                id = "spy",
                symbol = "SPY",
                name = "SPDR S&P 500 ETF Trust",
                category = MarketCategory.ETFS,
                currentPrice = if (isDemo) 580.40 else null,
                priceChange24h = if (isDemo) 3.10 else null,
                priceChangePercentage24h = if (isDemo) 0.54 else null,
                currency = "USD",
                exchange = "NYSE",
                historicalCandles = if (isDemo) generateMockCandles(580.40, 0.54) else null,
                timestampEpoch = now,
                isDemo = isDemo,
                dataStatus = if (isDemo) MarketDataStatus.SIMULATED else MarketDataStatus.UNAVAILABLE
            ),
            MarketInstrument(
                id = "qqq",
                symbol = "QQQ",
                name = "Invesco QQQ Trust",
                category = MarketCategory.ETFS,
                currentPrice = if (isDemo) 498.20 else null,
                priceChange24h = if (isDemo) 4.50 else null,
                priceChangePercentage24h = if (isDemo) 0.91 else null,
                currency = "USD",
                exchange = "NASDAQ",
                historicalCandles = if (isDemo) generateMockCandles(498.20, 0.91) else null,
                timestampEpoch = now,
                isDemo = isDemo,
                dataStatus = if (isDemo) MarketDataStatus.SIMULATED else MarketDataStatus.UNAVAILABLE
            )
        )
    }

    private fun generateMockCandles(basePrice: Double, change24h: Double): List<CandleData> {
        val candles = mutableListOf<CandleData>()
        val count = 30
        val startTime = System.currentTimeMillis() - (count * 24 * 60 * 60 * 1000L)
        var price = basePrice * (1.0 - (change24h / 100.0))

        for (i in 0 until count) {
            val time = startTime + (i * 24 * 60 * 60 * 1000L)
            val variation = price * 0.01 * sin(i.toDouble())
            val open = price
            val close = price + variation
            val high = maxOf(open, close) + (abs(variation) * 0.5)
            val low = minOf(open, close) - (abs(variation) * 0.5)
            val volume = 1000000f + (abs(variation) * 50000f).toFloat()

            candles.add(
                CandleData(
                    timestamp = time,
                    open = open.toFloat(),
                    high = high.toFloat(),
                    low = low.toFloat(),
                    close = close.toFloat(),
                    volume = volume
                )
            )
            price = close
        }
        return candles
    }

    // Watchlist Management
    fun getWatchlist(): List<WatchlistEntry> {
        val json = prefs.getString("watchlist_json", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WatchlistEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun toggleWatchlist(instrument: MarketInstrument): Boolean {
        val current = getWatchlist().toMutableList()
        val existing = current.find { it.instrumentId == instrument.id }
        val isAdded = if (existing != null) {
            current.remove(existing)
            false
        } else {
            current.add(
                WatchlistEntry(
                    instrumentId = instrument.id,
                    symbol = instrument.symbol,
                    name = instrument.name,
                    category = instrument.category
                )
            )
            true
        }
        prefs.edit().putString("watchlist_json", gson.toJson(current)).apply()
        return isAdded
    }

    fun isInWatchlist(instrumentId: String): Boolean {
        return getWatchlist().any { it.instrumentId == instrumentId }
    }

    // Portfolio Management
    fun getPortfolioHoldings(): List<PortfolioHolding> {
        val json = prefs.getString("portfolio_json", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PortfolioHolding>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addPortfolioHolding(holding: PortfolioHolding) {
        val current = getPortfolioHoldings().toMutableList()
        current.add(holding)
        prefs.edit().putString("portfolio_json", gson.toJson(current)).apply()
    }

    fun removePortfolioHolding(holdingId: String) {
        val current = getPortfolioHoldings().toMutableList()
        current.removeAll { it.id == holdingId }
        prefs.edit().putString("portfolio_json", gson.toJson(current)).apply()
    }

    fun calculatePortfolioValuation(
        holdings: List<PortfolioHolding>,
        instrumentsMap: Map<String, MarketInstrument>
    ): PortfolioValuationResult {
        var totalValuationUsd = 0.0
        var totalCostUsd = 0.0
        var totalValuationIdr = 0.0
        var totalCostIdr = 0.0
        var hasUnpriced = false

        holdings.forEach { holding ->
            val inst = instrumentsMap[holding.instrumentId]
            val currentUnitPrice = inst?.currentPrice
            val isUnpriced = (currentUnitPrice == null)
            if (isUnpriced) {
                hasUnpriced = true
            }

            val priceToUse = currentUnitPrice ?: holding.averageCost
            val valuation = holding.quantity * priceToUse
            val cost = holding.quantity * holding.averageCost

            if (holding.currency.equals("IDR", ignoreCase = true) || inst?.currency?.equals("IDR", ignoreCase = true) == true) {
                totalValuationIdr += valuation
                totalCostIdr += cost
            } else {
                totalValuationUsd += valuation
                totalCostUsd += cost
            }
        }

        val plUsd = totalValuationUsd - totalCostUsd
        val plPctUsd = if (totalCostUsd > 0.0) (plUsd / totalCostUsd) * 100.0 else 0.0

        val plIdr = totalValuationIdr - totalCostIdr
        val plPctIdr = if (totalCostIdr > 0.0) (plIdr / totalCostIdr) * 100.0 else 0.0

        return PortfolioValuationResult(
            totalValuationUsd = totalValuationUsd,
            totalCostUsd = totalCostUsd,
            unrealizedPlUsd = plUsd,
            unrealizedPlPctUsd = plPctUsd,
            totalValuationIdr = totalValuationIdr,
            totalCostIdr = totalCostIdr,
            unrealizedPlIdr = plIdr,
            unrealizedPlPctIdr = plPctIdr,
            hasUnpricedHoldings = hasUnpriced
        )
    }

    // Alerts Management
    fun getAlerts(): List<MarketAlert> {
        val json = prefs.getString("alerts_json", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<MarketAlert>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addAlert(alert: MarketAlert) {
        val current = getAlerts().toMutableList()
        current.add(alert)
        prefs.edit().putString("alerts_json", gson.toJson(current)).apply()
    }

    fun updateAlert(updatedAlert: MarketAlert) {
        val current = getAlerts().toMutableList()
        val index = current.indexOfFirst { it.id == updatedAlert.id }
        if (index != -1) {
            current[index] = updatedAlert
            prefs.edit().putString("alerts_json", gson.toJson(current)).apply()
        }
    }

    fun deleteAlert(alertId: String) {
        val current = getAlerts().toMutableList()
        current.removeAll { it.id == alertId }
        prefs.edit().putString("alerts_json", gson.toJson(current)).apply()
    }

    fun evaluateAlerts(instruments: List<MarketInstrument>): List<MarketAlert> {
        val alerts = getAlerts()
        if (alerts.isEmpty() || instruments.isEmpty()) return emptyList()

        val instrumentsMap = instruments.associateBy { it.id }
        val triggeredList = mutableListOf<MarketAlert>()
        val updatedAlerts = alerts.toMutableList()

        for (i in updatedAlerts.indices) {
            val alert = updatedAlerts[i]
            if (!alert.isActive || alert.isTriggered) continue

            val inst = instrumentsMap[alert.instrumentId] ?: continue
            val price = inst.currentPrice ?: continue
            val pctChange = inst.priceChangePercentage24h

            var isTriggered = false
            when (alert.alertType) {
                AlertType.PRICE_ABOVE -> if (price >= alert.targetValue) isTriggered = true
                AlertType.PRICE_BELOW -> if (price <= alert.targetValue) isTriggered = true
                AlertType.PCT_CHANGE_24H -> if (pctChange != null && abs(pctChange) >= alert.targetValue) isTriggered = true
            }

            if (isTriggered) {
                val newAlert = alert.copy(
                    isTriggered = true,
                    lastTriggeredEpoch = System.currentTimeMillis()
                )
                updatedAlerts[i] = newAlert
                triggeredList.add(newAlert)
            }
        }

        if (triggeredList.isNotEmpty()) {
            prefs.edit().putString("alerts_json", gson.toJson(updatedAlerts)).apply()
        }

        return triggeredList
    }

    companion object {
        @Volatile
        private var INSTANCE: MarketRepository? = null

        fun getInstance(context: Context): MarketRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = MarketRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
