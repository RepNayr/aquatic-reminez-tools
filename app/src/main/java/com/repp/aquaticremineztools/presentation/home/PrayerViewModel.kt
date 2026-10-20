package com.repp.aquaticremineztools.presentation.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repp.aquaticremineztools.core.settings.PrayerPreferences
import com.repp.aquaticremineztools.core.util.NextPrayerInfo
import com.repp.aquaticremineztools.core.util.PrayerCalculator
import com.repp.aquaticremineztools.data.model.PrayerSchedule
import com.repp.aquaticremineztools.data.repository.PrayerRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class PrayerUiState {
    object Loading : PrayerUiState()
    data class Success(
        val schedule: PrayerSchedule,
        val nextPrayerInfo: NextPrayerInfo
    ) : PrayerUiState()
    data class Error(
        val message: String,
        val cachedSchedule: PrayerSchedule?,
        val nextPrayerInfo: NextPrayerInfo?
    ) : PrayerUiState()
}

class PrayerViewModel : ViewModel() {

    private val _uiState = MutableLiveData<PrayerUiState>(PrayerUiState.Loading)
    val uiState: LiveData<PrayerUiState> = _uiState

    private var currentSchedule: PrayerSchedule? = null

    fun loadSchedule(
        context: Context,
        latitude: Double? = null,
        longitude: Double? = null,
        cityName: String? = null
    ) {
        val lat = latitude ?: PrayerPreferences.getLastLatitude(context)
        val lon = longitude ?: PrayerPreferences.getLastLongitude(context)
        val city = cityName ?: PrayerPreferences.getLastCity(context)
        val method = PrayerPreferences.getCalculationMethod(context)

        val dateKey = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())

        viewModelScope.launch {
            _uiState.value = PrayerUiState.Loading
            val repository = PrayerRepository.getInstance(context)

            val result = repository.getPrayerSchedule(
                context = context,
                dateKey = dateKey,
                latitude = lat,
                longitude = lon,
                cityName = city,
                method = method
            )

            result.onSuccess { schedule ->
                currentSchedule = schedule
                val nextInfo = PrayerCalculator.calculateNextPrayer(schedule)
                _uiState.value = PrayerUiState.Success(
                    schedule = schedule,
                    nextPrayerInfo = nextInfo
                )
            }.onFailure { exception ->
                val nextInfo = currentSchedule?.let { PrayerCalculator.calculateNextPrayer(it) }
                _uiState.value = PrayerUiState.Error(
                    message = exception.localizedMessage ?: "Failed to load prayer schedule",
                    cachedSchedule = currentSchedule,
                    nextPrayerInfo = nextInfo
                )
            }
        }
    }

    fun tickCountdown() {
        val schedule = currentSchedule ?: return
        val nextInfo = PrayerCalculator.calculateNextPrayer(schedule)
        val currentState = _uiState.value

        if (currentState is PrayerUiState.Success) {
            _uiState.value = currentState.copy(nextPrayerInfo = nextInfo)
        } else if (currentState is PrayerUiState.Error && currentState.cachedSchedule != null) {
            _uiState.value = currentState.copy(nextPrayerInfo = nextInfo)
        }
    }
}
