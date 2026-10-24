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
        val watchlistIds: Set<String>,
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

    @Suppress("unused")
    fun toggleWatchlist(context: Context, instrument: MarketInstrument) {
        val repository = MarketRepository.getInstance(context)
        repository.toggleWatchlist(instrument)
        updateFilteredState(context)
    }

    private fun updateFilteredState(context: Context) {
        val repository = MarketRepository.getInstance(context)
        val watchlistIds = repository.getWatchlist().map { it.instrumentId }.toSet()

        val filtered = if (currentCategory == MarketCategory.STOCKS) {
            emptyList()
        } else {
            allInstruments.filter { instrument ->
                val matchesCategory = when (currentCategory) {
                    MarketCategory.OVERVIEW -> true
                    else -> instrument.category == currentCategory
                }

                val matchesQuery = currentQuery.isEmpty() ||
                        instrument.name.contains(currentQuery, ignoreCase = true) ||
                        instrument.symbol.contains(currentQuery, ignoreCase = true)

                matchesCategory && matchesQuery
            }
        }

        val cryptoInstruments = allInstruments.filter { it.category == MarketCategory.CRYPTO }
        val cryptoStatus = cryptoInstruments.firstOrNull()?.dataStatus ?: MarketDataStatus.SIMULATED

        val bannerMessage = when (currentCategory) {
            MarketCategory.STOCKS -> "Stocks Data Unavailable"
            MarketCategory.CRYPTO -> when (cryptoStatus) {
                MarketDataStatus.LIVE -> "Live Market Data · Powered by CoinGecko"
                MarketDataStatus.CACHED -> "Offline Mode · Showing Cached CoinGecko Data"
                else -> "Simulated Crypto Data (Demo)"
            }
            MarketCategory.OVERVIEW -> when (cryptoStatus) {
                MarketDataStatus.LIVE -> "Crypto: Live · Other Markets: Simulated"
                MarketDataStatus.CACHED -> "Crypto: Cached · Other Markets: Simulated"
                else -> "Simulated Market Data (Demo Mode)"
            }
            else -> "Simulated Market Data (Demo Mode)"
        }

        val (emptyTitle, emptySubtitle) = when {
            currentCategory == MarketCategory.STOCKS ->
                Pair("Stocks Data Unavailable", "Stock market data is currently unavailable. Provider integration coming soon.")
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
            watchlistIds = watchlistIds,
            bannerMessage = bannerMessage,
            emptyStateTitle = emptyTitle,
            emptyStateSubtitle = emptySubtitle,
        )
    }
}
