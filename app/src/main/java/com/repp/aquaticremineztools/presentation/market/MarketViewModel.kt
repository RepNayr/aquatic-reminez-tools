package com.repp.aquaticremineztools.presentation.market

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repp.aquaticremineztools.data.model.EconomicEvent
import com.repp.aquaticremineztools.data.model.MarketCategory
import com.repp.aquaticremineztools.data.model.MarketDataStatus
import com.repp.aquaticremineztools.data.model.MarketInstrument
import com.repp.aquaticremineztools.data.repository.MarketRepository
import kotlinx.coroutines.launch

enum class MarketNavDestination {
    OVERVIEW,
    WATCHLIST,
    ALERTS,
    PORTFOLIO
}

enum class MarketScope {
    OVERVIEW,
    INDONESIA,
    GLOBAL
}

sealed class MarketUiState {
    object Loading : MarketUiState()
    data class Success(
        val allInstruments: List<MarketInstrument>,
        val marketPulse: List<MarketInstrument>,
        val keyMarkets: List<MarketInstrument>,
        val topGainers: List<MarketInstrument>,
        val topLosers: List<MarketInstrument>,
        val cryptoInstruments: List<MarketInstrument>,
        val forexInstruments: List<MarketInstrument>,
        val stocksInstruments: List<MarketInstrument>,
        val commoditiesInstruments: List<MarketInstrument>,
        val economicIndicators: List<MarketInstrument>,
        val economicEvents: List<EconomicEvent>,
        val activeAlertsCount: Int,
        val bannerMessage: String?,
        val activeScope: MarketScope = MarketScope.OVERVIEW,
        val selectedHeroBenchmarkId: String = "sp500",
        val selectedHeroTimeRange: String = "1D"
    ) : MarketUiState()
    data class Error(val message: String) : MarketUiState()
}

class MarketViewModel : ViewModel() {

    private val _uiState = MutableLiveData<MarketUiState>(MarketUiState.Loading)
    val uiState: LiveData<MarketUiState> = _uiState

    private val _currentDestination = MutableLiveData(MarketNavDestination.OVERVIEW)
    val currentDestination: LiveData<MarketNavDestination> = _currentDestination

    private var currentScope = MarketScope.OVERVIEW
    private var heroBenchmarkId = "sp500"
    private var heroTimeRange = "1D"

    fun setDestination(destination: MarketNavDestination) {
        _currentDestination.value = destination
    }

    fun setScope(context: Context, scope: MarketScope) {
        currentScope = scope
        val currentState = _uiState.value
        if (currentState is MarketUiState.Success) {
            val instruments = currentState.allInstruments
            val byCategory = instruments.groupBy { it.category }

            val cryptoList = byCategory[MarketCategory.CRYPTO] ?: emptyList()
            val forexList = byCategory[MarketCategory.FOREX] ?: emptyList()
            val commoditiesList = byCategory[MarketCategory.COMMODITIES] ?: emptyList()
            val economicsList = byCategory[MarketCategory.ECONOMICS] ?: emptyList()
            val stocksList = (byCategory[MarketCategory.STOCKS] ?: emptyList()) + (byCategory[MarketCategory.ETFS] ?: emptyList())

            val pulseTargetIds = listOf("ihsg", "sp500", "nasdaq", "nikkei", "usd_idr")
            val marketPulse = pulseTargetIds.mapNotNull { id -> instruments.find { it.id == id } }

            val keyTargetIds = listOf("ihsg", "sp500", "nasdaq", "usd_idr", "bitcoin")
            val keyMarkets = keyTargetIds.mapNotNull { id -> instruments.find { it.id == id } }

            _uiState.value = currentState.copy(
                marketPulse = filterInstrumentsByScope(marketPulse, scope),
                keyMarkets = filterInstrumentsByScope(keyMarkets, scope),
                cryptoInstruments = filterInstrumentsByScope(cryptoList, scope),
                forexInstruments = filterInstrumentsByScope(forexList, scope),
                stocksInstruments = filterInstrumentsByScope(stocksList, scope),
                commoditiesInstruments = filterInstrumentsByScope(commoditiesList, scope),
                economicIndicators = filterInstrumentsByScope(economicsList, scope),
                activeScope = scope
            )
        } else {
            loadMarket(context, forceRefresh = false)
        }
    }

    fun setHeroBenchmark(benchmarkId: String) {
        heroBenchmarkId = benchmarkId
        val currentState = _uiState.value
        if (currentState is MarketUiState.Success) {
            _uiState.value = currentState.copy(selectedHeroBenchmarkId = benchmarkId)
        }
    }

    fun setHeroTimeRange(timeRange: String) {
        heroTimeRange = timeRange
        val currentState = _uiState.value
        if (currentState is MarketUiState.Success) {
            _uiState.value = currentState.copy(selectedHeroTimeRange = timeRange)
        }
    }

    fun loadMarket(context: Context, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh || _uiState.value !is MarketUiState.Success) {
                _uiState.value = MarketUiState.Loading
            }

            val repository = MarketRepository.getInstance(context)
            val result = repository.getAllMarketInstruments(forceRefresh)

            result.onSuccess { instruments ->
                repository.evaluateAlerts(instruments)
                val activeAlertsCount = repository.getAlerts().count { it.isActive }

                val validChangeList = instruments.filter { it.priceChangePercentage24h != null }
                val gainers = validChangeList.sortedByDescending { it.priceChangePercentage24h }.take(4)
                val losers = validChangeList.sortedBy { it.priceChangePercentage24h }.take(4)

                val byCategory = instruments.groupBy { it.category }

                val cryptoList = byCategory[MarketCategory.CRYPTO] ?: emptyList()
                val forexList = byCategory[MarketCategory.FOREX] ?: emptyList()
                val commoditiesList = byCategory[MarketCategory.COMMODITIES] ?: emptyList()
                val economicsList = byCategory[MarketCategory.ECONOMICS] ?: emptyList()
                val stocksList = (byCategory[MarketCategory.STOCKS] ?: emptyList()) + (byCategory[MarketCategory.ETFS] ?: emptyList())

                val pulseTargetIds = listOf("ihsg", "sp500", "nasdaq", "nikkei", "usd_idr")
                val marketPulse = pulseTargetIds.mapNotNull { id -> instruments.find { it.id == id } }

                val keyTargetIds = listOf("ihsg", "sp500", "nasdaq", "usd_idr", "bitcoin")
                val keyMarkets = keyTargetIds.mapNotNull { id -> instruments.find { it.id == id } }

                val events = repository.getEconomicEvents()

                val cryptoStatus = cryptoList.firstOrNull()?.dataStatus ?: MarketDataStatus.UNAVAILABLE
                val forexStatus = forexList.firstOrNull()?.dataStatus ?: MarketDataStatus.UNAVAILABLE
                val bannerMessage = "Crypto: $cryptoStatus · Forex: $forexStatus · Reference: SIMULATED"

                val scopedPulse = filterInstrumentsByScope(marketPulse, currentScope)
                val scopedKey = filterInstrumentsByScope(keyMarkets, currentScope)
                val scopedCrypto = filterInstrumentsByScope(cryptoList, currentScope)
                val scopedForex = filterInstrumentsByScope(forexList, currentScope)
                val scopedStocks = filterInstrumentsByScope(stocksList, currentScope)
                val scopedCommodities = filterInstrumentsByScope(commoditiesList, currentScope)
                val scopedEconomics = filterInstrumentsByScope(economicsList, currentScope)

                _uiState.value = MarketUiState.Success(
                    allInstruments = instruments,
                    marketPulse = scopedPulse,
                    keyMarkets = scopedKey,
                    topGainers = gainers,
                    topLosers = losers,
                    cryptoInstruments = scopedCrypto,
                    forexInstruments = scopedForex,
                    stocksInstruments = scopedStocks,
                    commoditiesInstruments = scopedCommodities,
                    economicIndicators = scopedEconomics,
                    economicEvents = events,
                    activeAlertsCount = activeAlertsCount,
                    bannerMessage = bannerMessage,
                    activeScope = currentScope,
                    selectedHeroBenchmarkId = heroBenchmarkId,
                    selectedHeroTimeRange = heroTimeRange
                )
            }.onFailure { e ->
                _uiState.value = MarketUiState.Error(e.localizedMessage ?: "Failed to load market dashboard data")
            }
        }
    }

    private fun filterInstrumentsByScope(list: List<MarketInstrument>, scope: MarketScope): List<MarketInstrument> {
        return when (scope) {
            MarketScope.OVERVIEW -> list
            MarketScope.INDONESIA -> list.filter {
                it.countryCode == "ID" || it.currency == "IDR" || it.exchange == "IDX" || it.id == "usd_idr"
            }
            MarketScope.GLOBAL -> list.filter {
                it.countryCode != "ID" && it.currency != "IDR" && it.exchange != "IDX" && it.id != "usd_idr"
            }
        }
    }

    fun toggleWatchlist(context: Context, instrument: MarketInstrument) {
        val repository = MarketRepository.getInstance(context)
        repository.toggleWatchlist(instrument)
        loadMarket(context, forceRefresh = false)
    }
}
