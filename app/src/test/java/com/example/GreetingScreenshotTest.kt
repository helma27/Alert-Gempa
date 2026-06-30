package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.AlertScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent { 
      MyApplicationTheme { 
        AlertScreen(
          tanggal = "30 Jun 2026",
          jam = "12:00:00 WIB",
          magnitude = "6.5",
          kedalaman = "10 km",
          wilayah = "Uji Coba Sistem Peringatan Gempa (MOCK)",
          potensi = "POTENSI TSUNAMI - LAKUKAN SIMULASI EVAKUASI",
          dirasakan = "V MMI Jakarta, IV MMI Tangerang",
          distance = 42.0,
          coordinates = "-6.20,106.81",
          onDismiss = {}
        )
      } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
