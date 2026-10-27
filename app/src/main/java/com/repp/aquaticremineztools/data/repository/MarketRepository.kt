package com.repp.aquaticremineztools.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.repp.aquaticremineztools.core.settings.MarketPreferences
import com.repp.aquaticremineztools.data.model.AlertType
import com.repp.aquaticremineztools.data.model.EconomicEvent
import com.repp.aquaticremineztools.data.model.ImpactLevel
import com.repp.aquaticremineztools.data.model.MarketAlert
import com.repp.aquaticremineztools.data.model.MarketCategory
import com.repp.aquaticremineztools.data.model.MarketDataStatus
import com.repp.aquaticremineztools.data.model.MarketInstrument
import com.repp.aquaticremineztools.data.model.PortfolioHolding
import com.repp.aquaticremineztools.data.model.WatchlistEntry
import com.repp.aquaticremineztools.data.remote.forex.ExchangeRateApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

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
    private val exchangeRateApi = ExchangeRateApiClient.service

    var isDemoModeEnabled: Boolean
        get() = marketPreferences.isDemoModeEnabled
        set(value) {
            marketPreferences.isDemoModeEnabled = value
        }

    suspend fun getAllMarketInstruments(forceRefresh: Boolean = false): Result<List<MarketInstrument>> = withContext(Dispatchers.IO) {
        try {
            val list = mutableListOf<MarketInstrument>()

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
                            historicalCandles = null,
                            isCached = marketResult.isCached,
                            isDemo = false,
                            dataStatus = cryptoStatus
                        )
                    )
                }
            }.onFailure { e ->
                Log.w("MarketRepository", "Crypto market fetch failed: ${e.message}")
            }

            // 2. Fetch live Forex rates from ExchangeRate-API (Free open API)
            var ratesMap: Map<String, Double>? = null
            var forexStatus = MarketDataStatus.UNAVAILABLE
            var forexTimestamp = System.currentTimeMillis()

            val cachedFxJson = prefs.getString("cached_forex_json", null)
            val cachedFxTime = prefs.getLong("cached_forex_timestamp", 0L)
            val currentTime = System.currentTimeMillis()
            val cacheMaxAgeMs = 15 * 60 * 1000L

            try {
                val fxResponse = exchangeRateApi.getLatestRates()
                if (fxResponse.isSuccessful && fxResponse.body()?.result.equals("success", ignoreCase = true)) {
                    ratesMap = fxResponse.body()?.rates
                    if (!ratesMap.isNullOrEmpty()) {
                        forexStatus = MarketDataStatus.LIVE
                        forexTimestamp = currentTime
                        prefs.edit()
                            .putString("cached_forex_json", gson.toJson(ratesMap))
                            .putLong("cached_forex_timestamp", currentTime)
                            .apply()
                    }
                }
            } catch (e: Exception) {
                Log.w("MarketRepository", "Forex fetch failed: ${e.message}")
            }

            if (ratesMap.isNullOrEmpty()) {
                if (!cachedFxJson.isNullOrEmpty() && (currentTime - cachedFxTime) < cacheMaxAgeMs) {
                    try {
                        val type = object : TypeToken<Map<String, Double>>() {}.type
                        ratesMap = gson.fromJson(cachedFxJson, type)
                        forexStatus = MarketDataStatus.CACHED
                        forexTimestamp = cachedFxTime
                    } catch (_: Exception) {}
                }
            }

            val idrRate = ratesMap?.get("IDR")
            val eurRate = ratesMap?.get("EUR")
            val eurUsdRate = if (eurRate != null && eurRate > 0.0) 1.0 / eurRate else null
            val gbpRate = ratesMap?.get("GBP")
            val gbpUsdRate = if (gbpRate != null && gbpRate > 0.0) 1.0 / gbpRate else null
            val jpyRate = ratesMap?.get("JPY")
            val audRate = ratesMap?.get("AUD")
            val audUsdRate = if (audRate != null && audRate > 0.0) 1.0 / audRate else null
            val chfRate = ratesMap?.get("CHF")

            // 3. Regional & Global Indices (Simulated reference data)
            list.addAll(getIndicesInstruments())

            // 4. Forex Instruments (Live/Cached from ExchangeRate-API)
            list.addAll(getForexInstruments(idrRate, eurUsdRate, gbpUsdRate, jpyRate, audUsdRate, chfRate, forexStatus, forexTimestamp))

            // 5. Commodities (Simulated reference data)
            list.addAll(getCommoditiesInstruments())

            // 6. Economic Indicators (Simulated reference data)
            list.addAll(getEconomicsInstruments())

            // 7. Stocks & ETFs (Simulated reference data)
            list.addAll(getStocksAndEtfsInstruments())

            return@withContext Result.success(list)
        } catch (e: Exception) {
            Log.e("MarketRepository", "Error assembling market instruments: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    private fun getIndicesInstruments(): List<MarketInstrument> {
        val now = System.currentTimeMillis()
        return listOf(
            MarketInstrument(
                id = "ihsg",
                symbol = "IHSG",
                name = "Jakarta Composite Index",
                category = MarketCategory.INDICES,
                currentPrice = 7324.50,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "IDR",
                exchange = "IDX",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                countryCode = "ID"
            ),
            MarketInstrument(
                id = "sp500",
                symbol = "SPX",
                name = "S&P 500",
                category = MarketCategory.INDICES,
                currentPrice = 5815.20,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "USD",
                exchange = "NYSE",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                countryCode = "US"
            ),
            MarketInstrument(
                id = "nasdaq",
                symbol = "IXIC",
                name = "Nasdaq Composite",
                category = MarketCategory.INDICES,
                currentPrice = 18518.60,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "USD",
                exchange = "NASDAQ",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                countryCode = "US"
            ),
            MarketInstrument(
                id = "nikkei",
                symbol = "NI225",
                name = "Nikkei 225",
                category = MarketCategory.INDICES,
                currentPrice = 38981.75,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "JPY",
                exchange = "TSE",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                countryCode = "JP"
            )
        )
    }

    private fun getForexInstruments(
        idr: Double?,
        eurUsd: Double?,
        gbpUsd: Double?,
        jpy: Double?,
        audUsd: Double?,
        chf: Double?,
        status: MarketDataStatus,
        timestamp: Long
    ): List<MarketInstrument> {
        return listOf(
            MarketInstrument(
                id = "usd_idr",
                symbol = "USD/IDR",
                name = "US Dollar / Indonesian Rupiah",
                category = MarketCategory.FOREX,
                currentPrice = idr,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "IDR",
                exchange = "FX",
                timestampEpoch = timestamp,
                dataStatus = if (idr != null) status else MarketDataStatus.UNAVAILABLE
            ),
            MarketInstrument(
                id = "eur_usd",
                symbol = "EUR/USD",
                name = "Euro / US Dollar",
                category = MarketCategory.FOREX,
                currentPrice = eurUsd,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "USD",
                exchange = "FX",
                timestampEpoch = timestamp,
                dataStatus = if (eurUsd != null) status else MarketDataStatus.UNAVAILABLE
            ),
            MarketInstrument(
                id = "gbp_usd",
                symbol = "GBP/USD",
                name = "British Pound / US Dollar",
                category = MarketCategory.FOREX,
                currentPrice = gbpUsd,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "USD",
                exchange = "FX",
                timestampEpoch = timestamp,
                dataStatus = if (gbpUsd != null) status else MarketDataStatus.UNAVAILABLE
            ),
            MarketInstrument(
                id = "usd_jpy",
                symbol = "USD/JPY",
                name = "US Dollar / Japanese Yen",
                category = MarketCategory.FOREX,
                currentPrice = jpy,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "JPY",
                exchange = "FX",
                timestampEpoch = timestamp,
                dataStatus = if (jpy != null) status else MarketDataStatus.UNAVAILABLE
            ),
            MarketInstrument(
                id = "aud_usd",
                symbol = "AUD/USD",
                name = "Australian Dollar / US Dollar",
                category = MarketCategory.FOREX,
                currentPrice = audUsd,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "USD",
                exchange = "FX",
                timestampEpoch = timestamp,
                dataStatus = if (audUsd != null) status else MarketDataStatus.UNAVAILABLE
            ),
            MarketInstrument(
                id = "usd_chf",
                symbol = "USD/CHF",
                name = "US Dollar / Swiss Franc",
                category = MarketCategory.FOREX,
                currentPrice = chf,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "CHF",
                exchange = "FX",
                timestampEpoch = timestamp,
                dataStatus = if (chf != null) status else MarketDataStatus.UNAVAILABLE
            )
        )
    }

    private fun getCommoditiesInstruments(): List<MarketInstrument> {
        val now = System.currentTimeMillis()
        return listOf(
            MarketInstrument(
                id = "gold",
                symbol = "XAU/USD",
                name = "Gold Spot",
                category = MarketCategory.COMMODITIES,
                currentPrice = 2735.80,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "USD",
                exchange = "COMEX",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                measurementUnit = "USD/oz"
            ),
            MarketInstrument(
                id = "silver",
                symbol = "XAG/USD",
                name = "Silver Spot",
                category = MarketCategory.COMMODITIES,
                currentPrice = 32.45,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "USD",
                exchange = "COMEX",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                measurementUnit = "USD/oz"
            ),
            MarketInstrument(
                id = "oil_wti",
                symbol = "WTI",
                name = "Crude Oil WTI",
                category = MarketCategory.COMMODITIES,
                currentPrice = 71.42,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "USD",
                exchange = "NYMEX",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                measurementUnit = "USD/bbl"
            ),
            MarketInstrument(
                id = "nat_gas",
                symbol = "NG",
                name = "Natural Gas",
                category = MarketCategory.COMMODITIES,
                currentPrice = 2.78,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "USD",
                exchange = "NYMEX",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                measurementUnit = "USD/MMBtu"
            )
        )
    }

    private fun getEconomicsInstruments(): List<MarketInstrument> {
        val now = System.currentTimeMillis()
        return listOf(
            MarketInstrument(
                id = "bi_rate",
                symbol = "BI7DRR",
                name = "Bank Indonesia Policy Rate",
                category = MarketCategory.ECONOMICS,
                currentPrice = 6.00,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "%",
                exchange = "BI",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                measurementUnit = "%",
                macroPeriod = "Target Rate",
                countryCode = "ID"
            ),
            MarketInstrument(
                id = "fed_rate",
                symbol = "FEDFUNDS",
                name = "US Federal Reserve Target Rate",
                category = MarketCategory.ECONOMICS,
                currentPrice = 4.75,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "%",
                exchange = "FED",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                measurementUnit = "%",
                macroPeriod = "Target Rate",
                countryCode = "US"
            ),
            MarketInstrument(
                id = "id_inflation",
                symbol = "ID-CPI",
                name = "Indonesia Inflation Rate (YoY)",
                category = MarketCategory.ECONOMICS,
                currentPrice = 2.57,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "%",
                exchange = "BPS",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                measurementUnit = "%",
                macroPeriod = "YoY",
                countryCode = "ID"
            ),
            MarketInstrument(
                id = "us_10y",
                symbol = "US10Y",
                name = "US 10-Year Treasury Yield",
                category = MarketCategory.ECONOMICS,
                currentPrice = 4.24,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "%",
                exchange = "BOND",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                measurementUnit = "%",
                macroPeriod = "Yield",
                countryCode = "US"
            ),
            MarketInstrument(
                id = "id_gdp",
                symbol = "ID-GDP",
                name = "Indonesia GDP Growth (YoY)",
                category = MarketCategory.ECONOMICS,
                currentPrice = 5.05,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "%",
                exchange = "BPS",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                measurementUnit = "%",
                macroPeriod = "YoY",
                countryCode = "ID"
            )
        )
    }

    private fun getStocksAndEtfsInstruments(): List<MarketInstrument> {
        val now = System.currentTimeMillis()
        return listOf(
            // Indonesian Stocks
            MarketInstrument(
                id = "bbca",
                symbol = "BBCA",
                name = "Bank Central Asia Tbk",
                category = MarketCategory.STOCKS,
                currentPrice = 10250.0,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "IDR",
                exchange = "IDX",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                countryCode = "ID"
            ),
            MarketInstrument(
                id = "bbri",
                symbol = "BBRI",
                name = "Bank Rakyat Indonesia Tbk",
                category = MarketCategory.STOCKS,
                currentPrice = 4980.0,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "IDR",
                exchange = "IDX",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                countryCode = "ID"
            ),
            MarketInstrument(
                id = "bmri",
                symbol = "BMRI",
                name = "Bank Mandiri Tbk",
                category = MarketCategory.STOCKS,
                currentPrice = 6725.0,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "IDR",
                exchange = "IDX",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                countryCode = "ID"
            ),
            // Global Stocks
            MarketInstrument(
                id = "aapl",
                symbol = "AAPL",
                name = "Apple Inc.",
                category = MarketCategory.STOCKS,
                currentPrice = 235.40,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "USD",
                exchange = "NASDAQ",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                countryCode = "US"
            ),
            MarketInstrument(
                id = "nvda",
                symbol = "NVDA",
                name = "NVIDIA Corporation",
                category = MarketCategory.STOCKS,
                currentPrice = 138.20,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "USD",
                exchange = "NASDAQ",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                countryCode = "US"
            ),
            MarketInstrument(
                id = "tsla",
                symbol = "TSLA",
                name = "Tesla, Inc.",
                category = MarketCategory.STOCKS,
                currentPrice = 218.50,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "USD",
                exchange = "NASDAQ",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                countryCode = "US"
            ),
            // ETFs
            MarketInstrument(
                id = "spy",
                symbol = "SPY",
                name = "SPDR S&P 500 ETF Trust",
                category = MarketCategory.ETFS,
                currentPrice = 580.40,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "USD",
                exchange = "NYSE",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                countryCode = "US"
            ),
            MarketInstrument(
                id = "qqq",
                symbol = "QQQ",
                name = "Invesco QQQ Trust",
                category = MarketCategory.ETFS,
                currentPrice = 498.20,
                priceChange24h = null,
                priceChangePercentage24h = null,
                currency = "USD",
                exchange = "NASDAQ",
                timestampEpoch = now,
                isDemo = true,
                dataStatus = MarketDataStatus.SIMULATED,
                countryCode = "US"
            )
        )
    }

    fun getEconomicEvents(): List<EconomicEvent> {
        return listOf(
            EconomicEvent(
                id = "event_bi_rdg",
                title = "Bank Indonesia Board of Governors Rate Decision",
                country = "Indonesia",
                countryFlag = "🇮🇩",
                eventDate = "Monthly Release",
                impact = ImpactLevel.HIGH,
                actualValue = "6.00%",
                forecastValue = "6.00%",
                previousValue = "6.00%"
            ),
            EconomicEvent(
                id = "event_us_fomc",
                title = "US Federal Reserve FOMC Rate Decision",
                country = "United States",
                countryFlag = "🇺🇸",
                eventDate = "FOMC Calendar",
                impact = ImpactLevel.HIGH,
                actualValue = "4.75%",
                forecastValue = "4.75%",
                previousValue = "5.00%"
            ),
            EconomicEvent(
                id = "event_id_cpi",
                title = "Indonesia Consumer Price Index (CPI Inflation)",
                country = "Indonesia",
                countryFlag = "🇮🇩",
                eventDate = "Monthly Release",
                impact = ImpactLevel.MEDIUM,
                actualValue = "2.57%",
                forecastValue = "2.60%",
                previousValue = "2.62%"
            ),
            EconomicEvent(
                id = "event_us_cpi",
                title = "US CPI Inflation Rate (YoY)",
                country = "United States",
                countryFlag = "🇺🇸",
                eventDate = "Monthly Release",
                impact = ImpactLevel.HIGH,
                actualValue = "2.40%",
                forecastValue = "2.30%",
                previousValue = "2.50%"
            )
        )
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
            val price = inst.currentPrice
            val pctChange = inst.priceChangePercentage24h

            if (price == null || inst.dataStatus == MarketDataStatus.UNAVAILABLE) {
                continue
            }

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
