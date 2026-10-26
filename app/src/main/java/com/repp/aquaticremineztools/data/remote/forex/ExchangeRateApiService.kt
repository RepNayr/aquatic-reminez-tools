package com.repp.aquaticremineztools.data.remote.forex

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface ExchangeRateApiService {
    @GET("v6/latest/USD")
    suspend fun getLatestRates(): Response<ExchangeRateResponse>
}

data class ExchangeRateResponse(
    val result: String?,
    val rates: Map<String, Double>?
)

object ExchangeRateApiClient {
    private const val BASE_URL = "https://open.er-api.com/"

    val service: ExchangeRateApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExchangeRateApiService::class.java)
    }
}
