package com.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.EarthquakeDatabase
import com.example.data.db.EarthquakeEntity
import com.example.data.model.Gempa
import com.example.data.model.EarthquakeListResponse
import com.example.data.repository.EarthquakeRepository
import com.example.service.GempaService
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "MainViewModel"
    private val repository: EarthquakeRepository
    private val prefs = application.getSharedPreferences("gempa_prefs", Context.MODE_PRIVATE)

    // Expose database history
    val earthquakeHistory: StateFlow<List<EarthquakeEntity>>
    val filteredEarthquakes: StateFlow<List<EarthquakeEntity>>

    // Service Status
    val isServiceRunning: StateFlow<Boolean> = GempaService.serviceRunning
    val latestPolledGempa: StateFlow<Gempa?> = GempaService.latestFetchedGempa

    // User settings flows
    private val _magnitudeThreshold = MutableStateFlow(3.0)
    val magnitudeThreshold = _magnitudeThreshold.asStateFlow()

    // Sort and filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortBy = MutableStateFlow(SortOption.DATE_DESC)
    val sortBy = _sortBy.asStateFlow()

    private val _minMagnitudeFilter = MutableStateFlow(0.0)
    val minMagnitudeFilter = _minMagnitudeFilter.asStateFlow()

    private val _testCountdown = MutableStateFlow(-1)
    val testCountdown = _testCountdown.asStateFlow()

    enum class SortOption(val displayName: String) {
        DATE_DESC("Terbaru"),
        DATE_ASC("Terlama"),
        MAG_DESC("Magnitudo Terbesar"),
        DISTANCE_ASC("Jarak Terdekat")
    }

    init {
        val database = EarthquakeDatabase.getDatabase(application)
        repository = EarthquakeRepository(database.earthquakeDao())

        // Load baseline historical logs
        earthquakeHistory = repository.allEarthquakes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Combine filter and sort flows reactively
        filteredEarthquakes = combine(
            repository.allEarthquakes,
            _searchQuery,
            _sortBy,
            _minMagnitudeFilter
        ) { list, query, sort, minMag ->
            var result = list

            // 1. Filter by search query (wilayah / location)
            if (query.isNotEmpty()) {
                result = result.filter { it.wilayah.contains(query, ignoreCase = true) }
            }

            // 2. Filter by minimum magnitude
            if (minMag > 0.0) {
                result = result.filter { (it.magnitude.toDoubleOrNull() ?: 0.0) >= minMag }
            }

            // 3. Sort accordingly
            result = when (sort) {
                SortOption.DATE_DESC -> result.sortedByDescending { it.timestamp }
                SortOption.DATE_ASC -> result.sortedBy { it.timestamp }
                SortOption.MAG_DESC -> result.sortedByDescending { it.magnitude.toDoubleOrNull() ?: 0.0 }
                SortOption.DISTANCE_ASC -> result.sortedWith(compareBy { it.userDistance ?: Double.MAX_VALUE })
            }

            result
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Load setting threshold
        val savedThreshold = prefs.getFloat("magnitude_threshold", 3.0f)
        _magnitudeThreshold.value = savedThreshold.toDouble()

        // Fetch recent major BMKG earthquakes to populate history
        refreshRecentEarthquakes()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortBy(option: SortOption) {
        _sortBy.value = option
    }

    fun setMinMagnitudeFilter(minMag: Double) {
        _minMagnitudeFilter.value = minMag
    }

    fun startService() {
        Log.d(TAG, "Starting GempaService...")
        val intent = Intent(getApplication(), GempaService::class.java).apply {
            action = GempaService.ACTION_START
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(intent)
            } else {
                getApplication<Application>().startService(intent)
            }
            prefs.edit().putBoolean("service_enabled", true).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start GempaService: ${e.message}", e)
        }
    }

    fun stopService() {
        Log.d(TAG, "Stopping GempaService...")
        val intent = Intent(getApplication(), GempaService::class.java).apply {
            action = GempaService.ACTION_STOP
        }
        try {
            getApplication<Application>().stopService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop GempaService: ${e.message}", e)
        }
        prefs.edit().putBoolean("service_enabled", false).apply()
    }

    fun setMagnitudeThreshold(threshold: Double) {
        _magnitudeThreshold.value = threshold
        prefs.edit().putFloat("magnitude_threshold", threshold.toFloat()).apply()
    }

    fun startTestSimulation() {
        viewModelScope.launch {
            Log.d(TAG, "Starting background test simulation with auto-minimize...")
            // 1. Minimize the app immediately
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                getApplication<Application>().startActivity(homeIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to minimize app: ${e.message}")
            }

            // 2. Countdown 10 seconds in background
            for (i in 10 downTo 1) {
                _testCountdown.value = i
                delay(1000)
            }
            _testCountdown.value = 0

            // 3. Trigger GempaService test alarm
            triggerTestAlarm()

            delay(1500)
            _testCountdown.value = -1
        }
    }

    private fun triggerTestAlarm() {
        Log.d(TAG, "Triggering Test Alarm via Service action")
        val intent = Intent(getApplication(), GempaService::class.java).apply {
            action = GempaService.ACTION_TRIGGER_TEST
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(intent)
            } else {
                getApplication<Application>().startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger test alarm service: ${e.message}", e)
        }
    }

    fun refreshRecentEarthquakes() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Fetching recent earthquakes from BMKG API to populate history...")
                val location = getLastKnownLocation()
                val response = repository.fetchRecentEarthquakes()
                val recentGempas = response.infoGempa.gempa
                for (gempa in recentGempas) {
                    val distance = calculateDistance(location, gempa.coordinates)
                    val entity = EarthquakeEntity(
                        dateTime = gempa.dateTime,
                        tanggal = gempa.tanggal,
                        jam = gempa.jam,
                        coordinates = gempa.coordinates,
                        lintang = gempa.lintang,
                        bujur = gempa.bujur,
                        magnitude = gempa.magnitude,
                        kedalaman = gempa.kedalaman,
                        wilayah = gempa.wilayah,
                        potensi = gempa.potensi,
                        dirasakan = gempa.dirasakan,
                        shakemap = gempa.shakemap,
                        userDistance = distance,
                        timestamp = parseIso8601ToMillis(gempa.dateTime)
                    )
                    repository.insertEarthquake(entity)
                }
                Log.d(TAG, "Successfully populated historical database with ${recentGempas.size} recent events.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch recent earthquakes: ${e.message}", e)
            }
        }
    }

    private fun parseIso8601ToMillis(isoString: String): Long {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                java.time.Instant.parse(isoString).toEpochMilli()
            } else {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                sdf.parse(isoString.substring(0, 19))?.time ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplication<Application>())
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    continuation.resume(location)
                }.addOnFailureListener {
                    continuation.resume(null)
                }
            } catch (e: Exception) {
                continuation.resume(null)
            }
        }
    }

    private fun calculateDistance(userLoc: Location?, eqCoords: String): Double? {
        if (userLoc == null) return null
        return try {
            val parts = eqCoords.split(",")
            if (parts.size == 2) {
                val eqLat = parts[0].trim().toDouble()
                val eqLng = parts[1].trim().toDouble()

                val results = FloatArray(1)
                Location.distanceBetween(userLoc.latitude, userLoc.longitude, eqLat, eqLng, results)
                (results[0] / 1000.0) // convert to km
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
