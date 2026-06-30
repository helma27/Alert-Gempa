package com.example.data.repository

import com.example.data.api.BmkgApi
import com.example.data.db.EarthquakeDao
import com.example.data.db.EarthquakeEntity
import com.example.data.model.EarthquakeResponse
import com.example.data.model.EarthquakeListResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class EarthquakeRepository(private val earthquakeDao: EarthquakeDao) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val bmkgApi: BmkgApi = Retrofit.Builder()
        .baseUrl("https://data.bmkg.go.id/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(BmkgApi::class.java)

    // DB Operations
    val allEarthquakes: Flow<List<EarthquakeEntity>> = earthquakeDao.getAllEarthquakes()

    suspend fun insertEarthquake(entity: EarthquakeEntity) {
        earthquakeDao.insertEarthquake(entity)
    }

    suspend fun clearHistory() {
        earthquakeDao.clearAll()
    }

    // Network Operations
    suspend fun fetchLatestEarthquake(): EarthquakeResponse {
        return bmkgApi.getLatestEarthquake()
    }

    suspend fun fetchRecentEarthquakes(): EarthquakeListResponse {
        return bmkgApi.getRecentEarthquakes()
    }
}
