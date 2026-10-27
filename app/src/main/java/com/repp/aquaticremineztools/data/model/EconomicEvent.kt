package com.repp.aquaticremineztools.data.model

data class EconomicEvent(
    val id: String,
    val title: String,
    val country: String,
    val countryFlag: String,
    val eventDate: String,
    val impact: ImpactLevel,
    val actualValue: String?,
    val forecastValue: String?,
    val previousValue: String?,
    val isDemo: Boolean = true
)

enum class ImpactLevel {
    HIGH,
    MEDIUM,
    LOW
}
