package com.repp.aquaticremineztools.data.remote.prayer

import com.google.gson.annotations.SerializedName

data class AladhanResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: AladhanData?
)

data class AladhanData(
    @SerializedName("timings") val timings: AladhanTimings?,
    @SerializedName("date") val date: AladhanDate?,
    @SerializedName("meta") val meta: AladhanMeta?
)

data class AladhanTimings(
    @SerializedName("Fajr") val fajr: String?,
    @SerializedName("Sunrise") val sunrise: String?,
    @SerializedName("Dhuhr") val dhuhr: String?,
    @SerializedName("Asr") val asr: String?,
    @SerializedName("Sunset") val sunset: String?,
    @SerializedName("Maghrib") val maghrib: String?,
    @SerializedName("Isha") val isha: String?,
    @SerializedName("Imsak") val imsak: String?
)

data class AladhanDate(
    @SerializedName("readable") val readable: String?,
    @SerializedName("timestamp") val timestamp: String?
)

data class AladhanMeta(
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("timezone") val timezone: String?
)
