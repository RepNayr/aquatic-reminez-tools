package com.repp.aquaticremineztools.presentation.market

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repp.aquaticremineztools.data.model.MarketCategory
import com.repp.aquaticremineztools.data.model.MarketDataStatus
import com.repp.aquaticremineztools.data.model.MarketInstrument
import com.repp.aquaticremineztools.data.repository.MarketRepository
import kotlinx.coroutines.launch

sealed class MarketUiState {
    object Loading : MarketUiState()
    data class Success(
        val instruments: List<MarketInstrument>,
        val filteredInstruments: List<MarketInstrument>,
        val selectedCategory: MarketCategory,
        val searchQuery: String,
        val isDemoMode: Boolean,
        val watchlistIds: Set<String>,
        val activeAlertsCount: Int,
        val bannerMessage: String?,
        val emptyStateTitle: String,
        val emptyStateSubtitle: String,
    ) : MarketUiState()
    data class Error(val message: String) : MarketUiState()
}

class MarketViewModel : ViewModel() {

    private val _uiState = MutableLiveData<MarketUiState>(MarketUiState.Loading)
    val uiState: LiveData<MarketUiState> = _uiState

    private var allInstruments = listOf<MarketInstrument>()
    private var currentCategory = MarketCategory.OVERVIEW
    private var currentQuery = ""

    fun loadMarket(context: Context, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh || _uiState.value !is MarketUiState.Success) {
                _uiState.value = MarketUiState.Loading
            }

            val repository = MarketRepository.getInstance(context)
            val result = repository.getAllMarketInstruments(forceRefresh)

            result.onSuccess { instruments ->
                allInstruments = instruments
                repository.evaluateAlerts(instruments)
                updateFilteredState(context)
            }.onFailure { e ->
                _uiState.value = MarketUiState.Error(e.localizedMessage ?: "Failed to load market data")
            }
        }
    }

    fun setCategory(context: Context, category: MarketCategory) {
        currentCategory = category
        updateFilteredState(context)
    }

    fun setSearchQuery(context: Context, query: String) {
        currentQuery = query.trim()
        updateFilteredState(context)
    }

    fun toggleDemoMode(context: Context) {
        val repository = MarketRepository.getInstance(context)
        repository.isDemoModeEnabled = !repository.isDemoModeEnabled
        loadMarket(context, forceRefresh = true)
    }

    fun toggleWatchlist(context: Context, instrument: MarketInstrument) {
        val repository = MarketRepository.getInstance(context)
        repository.toggleWatchlist(instrument)
        updateFilteredState(context)
    }

    private fun updateFilteredState(context: Context) {
        val repository = MarketRepository.getInstance(context)
        val watchlistEntries = repository.getWatchlist()
        val watchlistIds = watchlistEntries.map { it.instrumentId }.toSet()
        val isDemo = repository.isDemoModeEnabled
        val activeAlertsCount = repository.getAlerts().count { it.isActive }

        val filtered = when (currentCategory) {
            MarketCategory.WATCHLIST -> {
                allInstruments.filter { watchlistIds.contains(it.id) && matchesQuery(it, currentQuery) }
            }
            else -> {
                allInstruments.filter { instrument ->
                    val matchesCategory = when (currentCategory) {
                        MarketCategory.OVERVIEW -> true
                        else -> instrument.category == currentCategory
                    }
                    matchesCategory && matchesQuery(instrument, currentQuery)
                }
            }
        }

        val cryptoInstruments = allInstruments.filter { it.category == MarketCategory.CRYPTO }
        val cryptoStatus = cryptoInstruments.firstOrNull()?.dataStatus ?: MarketDataStatus.SIMULATED

        val bannerMessage = when {
            currentCategory == MarketCategory.WATCHLIST -> {
                if (watchlistIds.isEmpty()) "Your Watchlist is Empty" else "Watchlist (${filtered.size} items)"
            }
            !isDemo -> when (cryptoStatus) {
                MarketDataStatus.LIVE -> "Live Crypto Data (CoinGecko) · Other Markets Unavailable (Demo Mode Off)"
                MarketDataStatus.CACHED -> "Cached Crypto Data · Other Markets Unavailable (Demo Mode Off)"
                else -> "Crypto Data Unavailable · Demo Mode Off"
            }
            else -> when (cryptoStatus) {
                MarketDataStatus.LIVE -> "Live Crypto Data (CoinGecko) · Other Markets Simulated (Demo Mode)"
                MarketDataStatus.CACHED -> "Cached Crypto Data · Other Markets Simulated (Demo Mode)"
                else -> "Simulated Market Terminal Data (Demo Mode)"
            }
        }

        val (emptyTitle, emptySubtitle) = when {
            currentCategory == MarketCategory.WATCHLIST && watchlistIds.isEmpty() ->
                Pair("Watchlist is Empty", "Tap the star icon on any instrument detail page to add it to your watchlist.")
            currentCategory == MarketCategory.STOCKS && !isDemo ->
                Pair("Stocks Data Unavailable", "Live stock feed is currently unavailable. Enable Demo Mode in the top header to view simulated stock data.")
            currentQuery.isNotEmpty() ->
                Pair("No Instruments Found", "No results matching \"$currentQuery\". Try searching another symbol or name.")
            else ->
                Pair("No Instruments Available", "No market instruments available in this category.")
        }

        _uiState.value = MarketUiState.Success(
            instruments = allInstruments,
            filteredInstruments = filtered,
            selectedCategory = currentCategory,
            searchQuery = currentQuery,
            isDemoMode = isDemo,
            watchlistIds = watchlistIds,
            activeAlertsCount = activeAlertsCount,
            bannerMessage = bannerMessage,
            emptyStateTitle = emptyTitle,
            emptyStateSubtitle = emptySubtitle,
        )
    }

    private fun matchesQuery(instrument: MarketInstrument, query: String): Boolean {
        if (query.isEmpty()) return true
        return instrument.name.contains(query, ignoreCase = true) ||
                instrument.symbol.contains(query, ignoreCase = true) ||
                instrument.exchange.contains(query, ignoreCase = true)
    }
}
