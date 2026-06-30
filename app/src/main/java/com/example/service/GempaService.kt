package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.os.VibrationEffect
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.audio.EewChimePlayer
import com.example.data.db.EarthquakeDatabase
import com.example.data.db.EarthquakeEntity
import com.example.data.model.Gempa
import com.example.data.repository.EarthquakeRepository
import com.example.ui.AlertActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GempaService : Service() {

    private val TAG = "GempaService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var repository: EarthquakeRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var vibrator: Vibrator

    private var lastSeenDateTime: String? = null
    private var lastUserLocation: Location? = null

    companion object {
        const val CHANNEL_ID = "GempaAlertServiceChannel"
        const val ALERT_CHANNEL_ID = "GempaCriticalAlertChannel"
        const val NOTIFICATION_ID = 101
        const val ACTION_START = "ACTION_START_GEMPA_SERVICE"
        const val ACTION_STOP = "ACTION_STOP_GEMPA_SERVICE"
        const val ACTION_TRIGGER_TEST = "ACTION_TRIGGER_TEST_GEMPA"

        // Expose service status for the UI to bind to
        private val _serviceRunning = MutableStateFlow(false)
        val serviceRunning: StateFlow<Boolean> = _serviceRunning

        private val _latestFetchedGempa = MutableStateFlow<Gempa?>(null)
        val latestFetchedGempa: StateFlow<Gempa?> = _latestFetchedGempa
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "GempaService Created")
        _serviceRunning.value = true

        val db = EarthquakeDatabase.getDatabase(this)
        repository = EarthquakeRepository(db.earthquakeDao())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Load baseline last seen DateTime from SharedPreferences
        val prefs = getSharedPreferences("gempa_prefs", Context.MODE_PRIVATE)
        lastSeenDateTime = prefs.getString("last_seen_datetime", null)

        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand with Action: $action")

        when (action) {
            ACTION_START -> {
                startForegroundService()
                startPolling()
            }
            ACTION_STOP -> {
                stopSelf()
            }
            ACTION_TRIGGER_TEST -> {
                triggerTestAlert()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "GempaService Destroyed")
        _serviceRunning.value = false
        serviceScope.cancel()
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pemantau Gempa BMKG")
            .setContentText("Memantau gempa real-time setiap 2 detik...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Layanan Pemantauan Gempa",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel untuk memantau status aktivitas latar belakang"
            }

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Peringatan Gempa Kritis (NHK Chime)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Peringatan gempa kritis dengan suara chime keras dan getaran"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
            manager?.createNotificationChannel(alertChannel)
        }
    }

    private fun startPolling() {
        serviceScope.launch {
            while (true) {
                try {
                    // Update user's location coordinates before fetching
                    updateLocation()

                    val response = repository.fetchLatestEarthquake()
                    val gempa = response.infoGempa.gempa
                    _latestFetchedGempa.value = gempa

                    Log.d(TAG, "Fetched earthquake: M ${gempa.magnitude}, DateTime: ${gempa.dateTime}")

                    if (lastSeenDateTime == null) {
                        // First run baseline setting
                        lastSeenDateTime = gempa.dateTime
                        saveLastSeenDateTime(gempa.dateTime)
                        Log.d(TAG, "Baseline earthquake stored: $lastSeenDateTime")
                    } else if (gempa.dateTime != lastSeenDateTime) {
                        // NEW EARTHQUAKE DETECTED!
                        Log.w(TAG, "⚠️ NEW EARTHQUAKE DETECTED: ${gempa.wilayah} ⚠️")
                        lastSeenDateTime = gempa.dateTime
                        saveLastSeenDateTime(gempa.dateTime)

                        // Trigger Full Warning Alert flow
                        triggerEarthquakeAlert(gempa)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in polling BMKG API: ${e.message}")
                }
                // Delay for 2 seconds to avoid rate limiting
                delay(2000)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lastUserLocation = location
                    Log.d(TAG, "Location updated: Lat: ${location.latitude}, Lng: ${location.longitude}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get location: ${e.message}")
        }
    }

    private fun saveLastSeenDateTime(dateTime: String) {
        val prefs = getSharedPreferences("gempa_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("last_seen_datetime", dateTime).apply()
    }

    private fun triggerEarthquakeAlert(gempa: Gempa) {
        serviceScope.launch {
            // 1. Calculate distance
            val distance = getDistanceToEpicenter(gempa.coordinates)

            // 2. Insert into local Room database history
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
                userDistance = distance
            )
            repository.insertEarthquake(entity)

            // 3. Enforce critical criteria: Magnitude > 5.0 and within 50km radius
            val mag = gempa.magnitude.toDoubleOrNull() ?: 0.0
            val isCriticalNearAndStrong = mag >= 5.0 && (distance != null && distance <= 50.0)

            // Fallback: general threshold check set by user
            val prefs = getSharedPreferences("gempa_prefs", Context.MODE_PRIVATE)
            val threshold = prefs.getFloat("magnitude_threshold", 3.0f).toDouble()
            val meetsThreshold = mag >= threshold

            if (isCriticalNearAndStrong || meetsThreshold) {
                // Play NHK Chime sound
                launch {
                    EewChimePlayer().playChime()
                }

                // Vibrate phone
                vibrateDevice()

                // Send High Priority Notification
                sendCriticalNotification(gempa, distance)

                // Launch AlertActivity on top of other applications (overlay alert)
                launchAlertActivity(gempa, distance)
            }
        }
    }

    private fun triggerTestAlert() {
        serviceScope.launch {
            Log.d(TAG, "Triggering Test Alarm...")
            // Update location before generating mock alarm to ensure it's "gempa terdekat"
            updateLocation()
            delay(500) // Small wait for location request if fast

            val userLoc = lastUserLocation
            val (mockLat, mockLng) = if (userLoc != null) {
                // Generate epicentre ~15km away (roughly offset by 0.1 degree)
                Pair(userLoc.latitude + 0.09, userLoc.longitude + 0.09)
            } else {
                // Fallback to Jakarta near coordinates
                Pair(-6.20 + 0.09, 106.81 + 0.09)
            }

            val sampleGempa = Gempa(
                tanggal = "30 Jun 2026",
                jam = "12:00:00 WIB",
                dateTime = "2026-06-30T05:00:00+00:00",
                coordinates = "$mockLat,$mockLng",
                lintang = if (mockLat < 0) "%.2f LS".format(-mockLat) else "%.2f LU".format(mockLat),
                bujur = if (mockLng < 0) "%.2f BB".format(-mockLng) else "%.2f BT".format(mockLng),
                magnitude = "6.5",
                kedalaman = "10 km",
                wilayah = "SIMULASI EEW - Gempa Terdekat Terdeteksi!",
                potensi = "POTENSI TSUNAMI - LAKUKAN SIMULASI EVAKUASI SEGERA",
                dirasakan = "VI MMI (Sangat Kuat)",
                shakemap = null
            )

            val distance = getDistanceToEpicenter(sampleGempa.coordinates)

            // Play Chime
            launch {
                EewChimePlayer().playChime()
            }

            // Vibrate
            vibrateDevice()

            // Notification
            sendCriticalNotification(sampleGempa, distance)

            // Overlay Activity
            launchAlertActivity(sampleGempa, distance)
        }
    }

    private fun getDistanceToEpicenter(eqCoords: String): Double? {
        val userLoc = lastUserLocation ?: return null
        return try {
            val parts = eqCoords.split(",")
            if (parts.size == 2) {
                val eqLat = parts[0].trim().toDouble()
                val eqLng = parts[1].trim().toDouble()

                val lat1 = userLoc.latitude
                val lon1 = userLoc.longitude

                val r = 6371.0 // Earth radius in km
                val dLat = Math.toRadians(eqLat - lat1)
                val dLon = Math.toRadians(eqLng - lon1)
                val a = sin(dLat / 2) * sin(dLat / 2) +
                        cos(Math.toRadians(lat1)) * cos(Math.toRadians(eqLat)) *
                        sin(dLon / 2) * sin(dLon / 2)
                val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                val dist = r * c
                Log.d(TAG, "Distance calculated: $dist km")
                dist
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating distance: ${e.message}")
            null
        }
    }

    private fun vibrateDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Intense vibration pattern: wait 0ms, vibrate 500ms, wait 200ms, vibrate 500ms, repeated
            val pattern = longArrayOf(0, 600, 200, 600, 200, 600, 200, 600)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255)
            val effect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(2000)
        }
    }

    private fun sendCriticalNotification(gempa: Gempa, distance: Double?) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 1, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val distanceText = if (distance != null) {
            "Jarak ke Pusat: %.1f km".format(distance)
        } else {
            "Jarak ke Pusat: Menghitung..."
        }

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("⚠️ PERINGATAN GEMPA M ${gempa.magnitude} ⚠️")
            .setContentText("${gempa.wilayah} ($distanceText)")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Wilayah: ${gempa.wilayah}\nKekuatan: M ${gempa.magnitude}\nKedalaman: ${gempa.kedalaman}\n$distanceText\nPotensi: ${gempa.potensi}"))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(202, notification)
    }

    private fun launchAlertActivity(gempa: Gempa, distance: Double?) {
        val alertIntent = Intent(this, AlertActivity::class.java).apply {
            putExtra("tanggal", gempa.tanggal)
            putExtra("jam", gempa.jam)
            putExtra("magnitude", gempa.magnitude)
            putExtra("kedalaman", gempa.kedalaman)
            putExtra("wilayah", gempa.wilayah)
            putExtra("potensi", gempa.potensi)
            putExtra("dirasakan", gempa.dirasakan)
            putExtra("distance", distance ?: -1.0)
            putExtra("coordinates", gempa.coordinates)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(alertIntent)
    }
}
