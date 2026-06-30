package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.db.EarthquakeEntity
import com.example.data.model.Gempa
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val latestGempa by viewModel.latestPolledGempa.collectAsState()
    val historyList by viewModel.filteredEarthquakes.collectAsState()
    val magnitudeThreshold by viewModel.magnitudeThreshold.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val minMagnitudeFilter by viewModel.minMagnitudeFilter.collectAsState()
    val testCountdown by viewModel.testCountdown.collectAsState()
    var showTestWarningDialog by remember { mutableStateOf(false) }

    if (showTestWarningDialog) {
        AlertDialog(
            onDismissRequest = { showTestWarningDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Peringatan",
                        tint = Color(0xFFB3261E),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mulai Uji Coba Latar Belakang",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1B1F)
                    )
                }
            },
            text = {
                Text(
                    text = "Aplikasi akan ditutup (minimize) secara otomatis demi simulasi background. " +
                            "Dalam countdown 10 detik di latar belakang, alarm kritis demo gempa terdekat (M > 5, <50km) akan muncul di layar Anda.\n\n" +
                            "Pastikan Anda telah mengizinkan izin 'Tampilkan di atas aplikasi lain' (Overlay Permission).",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTestWarningDialog = false
                        viewModel.startTestSimulation()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Mulai Uji Coba", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTestWarningDialog = false }
                ) {
                    Text("Batal", color = Color.Gray, fontSize = 13.sp)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Location & Notification Permissions state
    val permissionList = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val permissionsState = rememberMultiplePermissionsState(permissions = permissionList)

    // Check overlay permission
    var hasOverlayPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        )
    }

    // Refresh overlay permission state on launch/resume
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasOverlayPermission = Settings.canDrawOverlays(context)
        }
        // Auto-request standard permissions on start to make it plug-and-play
        permissionsState.launchMultiplePermissionRequest()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "App Icon",
                            tint = Color(0xFFB3261E),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Info Gempa & EEW",
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            fontSize = 20.sp,
                            color = Color(0xFF1C1B1F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1C1B1F)
                )
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = Color(0xFFF9F9FB)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Floating active countdown banner if background test is running
                if (testCountdown >= 0) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFB3261E).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F0)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    progress = { testCountdown / 10f },
                                    modifier = Modifier.size(36.dp),
                                    color = Color(0xFFB3261E),
                                    strokeWidth = 3.dp,
                                    trackColor = Color.LightGray.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Simulasi Alarm Latar Belakang Aktif",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1C1B1F)
                                    )
                                    Text(
                                        text = "Memicu alarm gempa terdekat dalam $testCountdown detik...",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. SERVICE STATUS & QUICK CONTROLS
                item {
                    MonitoringStatusCard(
                        isServiceRunning = isServiceRunning,
                        hasPermissions = permissionsState.allPermissionsGranted,
                        hasOverlay = hasOverlayPermission,
                        onToggleService = {
                            if (isServiceRunning) {
                                viewModel.stopService()
                            } else {
                                // Request permissions first if not granted
                                if (!permissionsState.allPermissionsGranted) {
                                    permissionsState.launchMultiplePermissionRequest()
                                }
                                viewModel.startService()
                            }
                        },
                        onTestAlert = {
                            showTestWarningDialog = true
                        }
                    )
                }

                // 2. PERMISSION ALERT BANNER
                if (!permissionsState.allPermissionsGranted || !hasOverlayPermission) {
                    item {
                        PermissionSettingsCard(
                            isLocationGranted = permissionsState.permissions.any { it.permission.contains("LOCATION") && it.status.toString().contains("Granted") },
                            isNotificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionsState.permissions.any { it.permission.contains("POST_NOTIFICATIONS") && it.status.toString().contains("Granted") }
                            } else true,
                            hasOverlayPermission = hasOverlayPermission,
                            onRequestPermissions = { permissionsState.launchMultiplePermissionRequest() },
                            onRequestOverlay = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                }

                // 3. LATEST BMKG EARTHQUAKE DISPLAY
                item {
                    LatestEarthquakeCard(gempa = latestGempa)
                }

                // 4. SETTINGS
                item {
                    ThresholdSettingsCard(
                        threshold = magnitudeThreshold,
                        onThresholdChange = { viewModel.setMagnitudeThreshold(it) }
                    )
                }

                // 5. HISTORY SECTION HEADER
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Riwayat Peringatan",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F)
                        )

                        if (historyList.isNotEmpty() || searchQuery.isNotEmpty() || minMagnitudeFilter > 0.0) {
                            IconButton(
                                onClick = { viewModel.clearHistory() },
                                modifier = Modifier.testTag("clear_history_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Bersihkan Riwayat",
                                    tint = Color(0xFFB3261E)
                                )
                            }
                        }
                    }
                }

                // 5b. SEARCH AND FILTER CONTROLS
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_history_input"),
                            placeholder = { Text("Cari wilayah...", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFB3261E),
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                focusedContainerColor = Color(0xFFF9F9FB),
                                unfocusedContainerColor = Color(0xFFF9F9FB)
                            )
                        )

                        // Sort and Magnitude Filters Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sort Dropdown Selector
                            var sortMenuExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { sortMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1C1B1F)),
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = "Sort Icon",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFFB3261E)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = sortBy.displayName,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false }
                                ) {
                                    MainViewModel.SortOption.values().forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.displayName, fontSize = 12.sp) },
                                            onClick = {
                                                viewModel.setSortBy(option)
                                                sortMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Filter Magnitude Dropdown
                            var magMenuExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { magMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1C1B1F)),
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Mag Icon",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFFB3261E)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (minMagnitudeFilter == 0.0) "Semua Mag" else "Min M %.1f".format(minMagnitudeFilter),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = magMenuExpanded,
                                    onDismissRequest = { magMenuExpanded = false }
                                ) {
                                    listOf(0.0, 3.0, 4.0, 5.0, 6.0).forEach { mag ->
                                        DropdownMenuItem(
                                            text = { Text(if (mag == 0.0) "Semua Magnitudo" else "M >= %.1f".format(mag), fontSize = 12.sp) },
                                            onClick = {
                                                viewModel.setMinMagnitudeFilter(mag)
                                                magMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. HISTORY LIST
                if (historyList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isNotEmpty() || minMagnitudeFilter > 0.0) {
                                        "Tidak ditemukan riwayat gempa yang cocok."
                                    } else {
                                        "Belum ada riwayat gempa terdeteksi."
                                    },
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(historyList) { entity ->
                        HistoryItemRow(entity = entity)
                    }
                }
            }
        }
    }
}

@Composable
fun MonitoringStatusCard(
    isServiceRunning: Boolean,
    hasPermissions: Boolean,
    hasOverlay: Boolean,
    onToggleService: () -> Unit,
    onTestAlert: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MonitoringPulse")
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScalePulse"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Status indicator capsule
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isServiceRunning) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .alpha(if (isServiceRunning) scalePulse else 1.0f)
                            .clip(CircleShape)
                            .background(if (isServiceRunning) Color(0xFF2E7D32) else Color(0xFFC62828))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isServiceRunning) "AKTIF (2S)" else "LAYANAN MATI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isServiceRunning) Color(0xFF2E7D32) else Color(0xFFC62828),
                        letterSpacing = 0.5.sp
                    )
                }

                Button(
                    onClick = onToggleService,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServiceRunning) Color(0xFF1C1B1F) else Color(0xFFB3261E)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("toggle_service_btn")
                ) {
                    Text(
                        text = if (isServiceRunning) "Matikan" else "Aktifkan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isServiceRunning) {
                    "Aplikasi mendeteksi rilis info gempa BMKG setiap 2 detik secara real-time. Jika terdeteksi gempa baru, alarm suara kritis (NHK) dan getaran akan menyala otomatis."
                } else {
                    "Layanan latar belakang tidak aktif. Aktifkan agar aplikasi dapat memonitor data BMKG di latar belakang secara real-time."
                },
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Test trigger button
            Button(
                onClick = onTestAlert,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("test_alert_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1B1F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Test Notification",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "UJI COBA ALARM GEMPA (NHK CHIME)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun PermissionSettingsCard(
    isLocationGranted: Boolean,
    isNotificationGranted: Boolean,
    hasOverlayPermission: Boolean,
    onRequestPermissions: () -> Unit,
    onRequestOverlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFFB74D).copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Izin Diperlukan untuk Alert",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFE65100)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            PermissionItemRow(
                label = "Deteksi Lokasi (Hitung Jarak)",
                isGranted = isLocationGranted,
                onRequest = onRequestPermissions
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionItemRow(
                label = "Izin Notifikasi (Android 13+)",
                isGranted = isNotificationGranted,
                onRequest = onRequestPermissions
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionItemRow(
                label = "Tampil Di Atas Aplikasi Lain (Overlay)",
                isGranted = hasOverlayPermission,
                onRequest = onRequestOverlay
            )
        }
    }
}

@Composable
fun PermissionItemRow(
    label: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF1C1B1F),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isGranted) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                .clickable(enabled = !isGranted, onClick = onRequest)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isGranted) "Aktif" else "BERI IZIN",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isGranted) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }
    }
}

@Composable
fun LatestEarthquakeCard(gempa: Gempa?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Data Gempa BMKG Terkini",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1B1F),
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (gempa == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Loading",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Menghubungkan ke server BMKG...",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Magnitude Badge styled as circular warning red icon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFB3261E))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "MAG",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = gempa.magnitude,
                                fontSize = 24.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = gempa.wilayah,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Kedalaman: ${gempa.kedalaman} • Waktu: ${gempa.tanggal} ${gempa.jam}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Potensi: ${gempa.potensi}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB3261E)
                        )
                    }
                }

                // If shake map is available, show it inside a beautiful rounded card
                if (!gempa.shakemap.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        val shakemapUrl = "https://data.bmkg.go.id/DataMKG/TEWS/${gempa.shakemap}"
                        AsyncImage(
                            model = shakemapUrl,
                            contentDescription = "Peta Guncangan Shakemap BMKG",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        text = "* Peta guncangan resmi BMKG (Shakemap)",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 6.dp, start = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ThresholdSettingsCard(
    threshold: Double,
    onThresholdChange: (Double) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Sensitivitas Alarm (Minimum Magnitudo)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1B1F)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Alarm NHK Chime hanya berbunyi jika kekuatan gempa di atas threshold ini.",
                fontSize = 11.sp,
                color = Color.Gray,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "M %.1f".format(threshold),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFB3261E)
                )

                Slider(
                    value = threshold.toFloat(),
                    onValueChange = { onThresholdChange(it.toDouble()) },
                    valueRange = 1.0f..9.0f,
                    steps = 15,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .testTag("threshold_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFB3261E),
                        activeTrackColor = Color(0xFFB3261E),
                        inactiveTrackColor = Color.LightGray.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

@Composable
fun HistoryItemRow(entity: EarthquakeEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFEBEE))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "M ${entity.magnitude}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = Color(0xFFB3261E)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gempa Bumi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1C1B1F)
                    )
                }

                Text(
                    text = "${entity.tanggal} ${entity.jam}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = entity.wilayah,
                fontSize = 13.sp,
                color = Color(0xFF1C1B1F),
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium
            )

            if (entity.userDistance != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE3F2FD))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Jarak ke Anda: %.1f km".format(entity.userDistance),
                        color = Color(0xFF1565C0),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
