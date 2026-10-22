package com.repp.aquaticremineztools.presentation.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repp.aquaticremineztools.data.model.CalendarDay
import com.repp.aquaticremineztools.data.model.CalendarMonthData
import com.repp.aquaticremineztools.data.repository.CalendarRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class CalendarViewModel : ViewModel() {

    private val _monthData = MutableLiveData<CalendarMonthData>()
    val monthData: LiveData<CalendarMonthData> = _monthData

    private val activeCalendar: Calendar = Calendar.getInstance()
    private var selectedDayNum: Int? = activeCalendar.get(Calendar.DAY_OF_MONTH)

    private var isInitialized = false

    fun initCalendar(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val today = Calendar.getInstance()
        activeCalendar.timeInMillis = today.timeInMillis
        selectedDayNum = today.get(Calendar.DAY_OF_MONTH)

        loadCurrentMonth(context)
    }

    fun loadCurrentMonth(context: Context) {
        val year = activeCalendar.get(Calendar.YEAR)
        val month1Based = activeCalendar.get(Calendar.MONTH) + 1
        val selDay = selectedDayNum

        viewModelScope.launch {
            val repository = CalendarRepository.getInstance(context)
            val data = repository.getMonthCalendarData(year, month1Based, selDay)
            _monthData.value = data
        }
    }

    fun onSelectDay(context: Context, day: CalendarDay) {
        if (!day.isInCurrentMonth) return
        selectedDayNum = day.dayOfMonth
        loadCurrentMonth(context)
    }

    fun onNextMonth(context: Context) {
        activeCalendar.add(Calendar.MONTH, 1)
        selectedDayNum = null
        loadCurrentMonth(context)
    }

    fun onPrevMonth(context: Context) {
        activeCalendar.add(Calendar.MONTH, -1)
        selectedDayNum = null
        loadCurrentMonth(context)
    }

    fun onGoToToday(context: Context) {
        val today = Calendar.getInstance()
        activeCalendar.timeInMillis = today.timeInMillis
        selectedDayNum = today.get(Calendar.DAY_OF_MONTH)
        loadCurrentMonth(context)
    }
}
