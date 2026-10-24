package com.repp.aquaticremineztools.presentation.market

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repp.aquaticremineztools.data.model.MarketCategory
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
        val isCached: Boolean,
        val isDemo: Boolean,
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

        val filtered = allInstruments.filter { instrument ->
            val matchesCategory = when (currentCategory) {
                MarketCategory.OVERVIEW -> true
                else -> instrument.category == currentCategory
            }

            val matchesQuery = currentQuery.isEmpty() ||
                    instrument.name.contains(currentQuery, ignoreCase = true) ||
                    instrument.symbol.contains(currentQuery, ignoreCase = true)

            matchesCategory && matchesQuery
        }

        val isCached = allInstruments.any { it.isCached }
        val isDemo = allInstruments.any { it.isDemo }

        _uiState.value = MarketUiState.Success(
            instruments = allInstruments,
            filteredInstruments = filtered,
            selectedCategory = currentCategory,
            searchQuery = currentQuery,
            watchlistIds = watchlistIds,
            isCached = isCached,
            isDemo = isDemo,
        )
    }
}
