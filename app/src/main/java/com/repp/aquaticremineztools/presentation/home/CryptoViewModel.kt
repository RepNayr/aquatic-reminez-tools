package com.repp.aquaticremineztools.presentation.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repp.aquaticremineztools.data.model.CryptoAsset
import com.repp.aquaticremineztools.data.repository.CryptoRepository
import kotlinx.coroutines.launch

sealed class CryptoUiState {
    object Loading : CryptoUiState()
    data class Success(
        val assets: List<CryptoAsset>,
        val isCached: Boolean,
        val isDemo: Boolean
    ) : CryptoUiState()
    data class Error(
        val message: String
    ) : CryptoUiState()
}

class CryptoViewModel : ViewModel() {

    private val _uiState = MutableLiveData<CryptoUiState>(CryptoUiState.Loading)
    val uiState: LiveData<CryptoUiState> = _uiState

    fun loadMarketData(context: Context, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh || _uiState.value !is CryptoUiState.Success) {
                _uiState.value = CryptoUiState.Loading
            }
            val repository = CryptoRepository.getInstance(context)
            val result = repository.getMarketData(forceRefresh)

            result.onSuccess { marketResult ->
                _uiState.value = CryptoUiState.Success(
                    assets = marketResult.assets,
                    isCached = marketResult.isCached,
                    isDemo = marketResult.isDemo
                )
            }.onFailure { exception ->
                _uiState.value = CryptoUiState.Error(
                    message = exception.localizedMessage ?: "Failed to load market data"
                )
            }
        }
    }
}
