package com.repp.aquaticremineztools.data.remote.crypto

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface CoinGeckoApiService {

    @GET("api/v3/coins/markets")
    suspend fun getMarkets(
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("ids") ids: String = "bitcoin,ethereum,solana,binancecoin",
        @Query("sparkline") sparkline: Boolean = true,
        @Query("price_change_percentage") priceChangePercentage: String = "24h"
    ): Response<List<CoinMarketDto>>
}

object CoinGeckoApiClient {
    private const val BASE_URL = "https://api.coingecko.com/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "AquaticReminezTools/1.0 (Android)")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    val service: CoinGeckoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CoinGeckoApiService::class.java)
    }
}
