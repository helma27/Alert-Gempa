package com.example.ui

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class AlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure activity to show over lock screen and turn on the screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        
        // Use full screen edge-to-edge
        enableEdgeToEdge()

        val tanggal = intent.getStringExtra("tanggal") ?: "-"
        val jam = intent.getStringExtra("jam") ?: "-"
        val magnitude = intent.getStringExtra("magnitude") ?: "0.0"
        val kedalaman = intent.getStringExtra("kedalaman") ?: "-"
        val wilayah = intent.getStringExtra("wilayah") ?: "Pusat gempa tidak diketahui"
        val potensi = intent.getStringExtra("potensi") ?: "Dirasakan untuk diteruskan ke masyarakat"
        val dirasakan = intent.getStringExtra("dirasakan")
        val distance = intent.getDoubleExtra("distance", -1.0)
        val coordinates = intent.getStringExtra("coordinates") ?: "0.0,0.0"

        setContent {
            MyApplicationTheme {
                AlertScreen(
                    tanggal = tanggal,
                    jam = jam,
                    magnitude = magnitude,
                    kedalaman = kedalaman,
                    wilayah = wilayah,
                    potensi = potensi,
                    dirasakan = dirasakan,
                    distance = distance,
                    coordinates = coordinates,
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
fun AlertScreen(
    tanggal: String,
    jam: String,
    magnitude: String,
    kedalaman: String,
    wilayah: String,
    potensi: String,
    dirasakan: String?,
    distance: Double,
    coordinates: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Pulsing animation for alarm severity
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val baseWarningColor = Color(0xFFB3261E) // Material Warning Red

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = baseWarningColor
    ) {
        // High fidelity atmospheric warning gradient
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(pulseAlpha)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                baseWarningColor,
                                Color(0xFF4A0E17)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(top = 56.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Block: High Urgency
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .testTag("warning_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning Icon",
                            tint = baseWarningColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "PERINGATAN DINI GEMPA",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sumber: BMKG (Real-time Alert)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }

                // Main Alert Glassmorphism Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(32.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "ESTIMASI MAGNITUDO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = magnitude,
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    lineHeight = 72.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "KEDALAMAN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = kedalaman.replace(" km", "").trim(),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "km",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Location / Epicenter Details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Location Pin",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Pusat Gempa ($wilayah)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        lineHeight = 18.sp
                                    )
                                    val distanceText = if (distance >= 0) {
                                        "Berjarak %.1f km dari lokasi Anda".format(distance)
                                    } else {
                                        "Menghitung jarak ke lokasi Anda..."
                                    }
                                    Text(
                                        text = distanceText,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            // Time & Wave Details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Clock Info",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Waktu Kejadian & Skala",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    val subInfo = if (dirasakan != null) {
                                        "$tanggal - $jam • Dirasakan $dirasakan"
                                    } else {
                                        "$tanggal - $jam"
                                    }
                                    Text(
                                        text = subInfo,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f),
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Potensi: $potensi",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Yellow
                                    )
                                }
                            }
                        }
                    }
                }

                // Status Active indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .alpha(pulseAlpha)
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vibrating & Audio Chime Active".uppercase(),
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }

                // 3-Column Survival Steps Grid
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SurvivalStepItem(emoji = "🛡️", title = "Berlindung", modifier = Modifier.weight(1f))
                        SurvivalStepItem(emoji = "🪟", title = "Jauhi Kaca", modifier = Modifier.weight(1f))
                        SurvivalStepItem(emoji = "🏢", title = "Evakuasi", modifier = Modifier.weight(1f))
                    }

                    // Bottom Dismiss button styled as beautiful solid white with red warning text
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = baseWarningColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("dismiss_alert_button"),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = "SAYA SUDAH AMAN / TUTUP PERINGATAN",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SurvivalStepItem(emoji: String, title: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )
        }
    }
}
