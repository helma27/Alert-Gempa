package com.example.data.api

import com.example.data.model.EarthquakeResponse
import com.example.data.model.EarthquakeListResponse
import retrofit2.http.GET

interface BmkgApi {
    @GET("DataMKG/TEWS/autogempa.json")
    suspend fun getLatestEarthquake(): EarthquakeResponse

    @GET("DataMKG/TEWS/gempaterkini.json")
    suspend fun getRecentEarthquakes(): EarthquakeListResponse
}
