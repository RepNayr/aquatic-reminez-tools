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

sealed class MarketUiState {
    object Loading : MarketUiState()
    data class Success(
        val allInstruments: List<MarketInstrument>,
        val marketPulse: List<MarketInstrument>,
        val keyMarkets: List<MarketInstrument>,
        val cryptoInstruments: List<MarketInstrument>,
        val forexInstruments: List<MarketInstrument>,
        val stocksInstruments: List<MarketInstrument>,
        val commoditiesInstruments: List<MarketInstrument>,
        val economicIndicators: List<MarketInstrument>,
        val economicEvents: List<EconomicEvent>,
        val activeAlertsCount: Int,
        val bannerMessage: String?
    ) : MarketUiState()
    data class Error(val message: String) : MarketUiState()
}

class MarketViewModel : ViewModel() {

    private val _uiState = MutableLiveData<MarketUiState>(MarketUiState.Loading)
    val uiState: LiveData<MarketUiState> = _uiState

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

                val byCategory = instruments.groupBy { it.category }

                val cryptoList = byCategory[MarketCategory.CRYPTO] ?: emptyList()
                val forexList = byCategory[MarketCategory.FOREX] ?: emptyList()
                val commoditiesList = byCategory[MarketCategory.COMMODITIES] ?: emptyList()
                val economicsList = byCategory[MarketCategory.ECONOMICS] ?: emptyList()
                val stocksList = (byCategory[MarketCategory.STOCKS] ?: emptyList()) + (byCategory[MarketCategory.ETFS] ?: emptyList())

                // Market Pulse Priority: IHSG, SPX, IXIC, NI225, USD/IDR
                val pulseTargetIds = listOf("ihsg", "sp500", "nasdaq", "nikkei", "usd_idr")
                val marketPulse = pulseTargetIds.mapNotNull { id -> instruments.find { it.id == id } }

                // Key Markets Priority: IHSG, SPX, IXIC, USD/IDR, BTC
                val keyTargetIds = listOf("ihsg", "sp500", "nasdaq", "usd_idr", "bitcoin")
                val keyMarkets = keyTargetIds.mapNotNull { id -> instruments.find { it.id == id } }

                val events = repository.getEconomicEvents()

                val cryptoStatus = cryptoList.firstOrNull()?.dataStatus ?: MarketDataStatus.UNAVAILABLE
                val bannerMessage = when (cryptoStatus) {
                    MarketDataStatus.LIVE -> "Live Crypto Data (CoinGecko) · Other Markets Feed Pending Integration"
                    MarketDataStatus.CACHED -> "Cached Crypto Data · Other Markets Feed Pending Integration"
                    else -> "Crypto Feed Unavailable · Other Markets Feed Pending Integration"
                }

                _uiState.value = MarketUiState.Success(
                    allInstruments = instruments,
                    marketPulse = marketPulse,
                    keyMarkets = keyMarkets,
                    cryptoInstruments = cryptoList,
                    forexInstruments = forexList,
                    stocksInstruments = stocksList,
                    commoditiesInstruments = commoditiesList,
                    economicIndicators = economicsList,
                    economicEvents = events,
                    activeAlertsCount = activeAlertsCount,
                    bannerMessage = bannerMessage
                )
            }.onFailure { e ->
                _uiState.value = MarketUiState.Error(e.localizedMessage ?: "Failed to load market dashboard data")
            }
        }
    }

    fun toggleWatchlist(context: Context, instrument: MarketInstrument) {
        val repository = MarketRepository.getInstance(context)
        repository.toggleWatchlist(instrument)
        loadMarket(context, forceRefresh = false)
    }
}
