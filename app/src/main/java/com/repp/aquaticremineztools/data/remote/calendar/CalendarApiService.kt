package com.repp.aquaticremineztools.data.remote.calendar

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CalendarApiService {

    @GET("v1/gToHCalendar/{month}/{year}")
    suspend fun getGregorianToHijriCalendar(
        @Path("month") month: Int,
        @Path("year") year: Int,
        @Query("method") method: Int = 20,
        @Query("adjustment") adjustment: Int = 0
    ): Response<AladhanCalendarResponse>
}

object CalendarApiClient {
    private const val BASE_URL = "https://api.aladhan.com/"

    val service: CalendarApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CalendarApiService::class.java)
    }
}
