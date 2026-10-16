package com.vegsahayyak.samples.customui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vegsahayyak.speedcore.api.SpeedAssistant
import com.vegsahayyak.speedcore.api.DrivingProfile
import com.vegsahayyak.speedcore.data.location.SpeedData
import com.vegsahayyak.speedui.components.SpeedGauge
import com.vegsahayyak.speedui.components.SpeedUiColorScheme

class MainActivity : ComponentActivity() {

    // Commercial B2B license key for Custom UI sample
    private val enterpriseJwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGllbnRfaWQiOiJWZWcgU2FoYXl5YWsgU2FtcGxlIC0gQ3VzdG9tIFVJIiwicGFja2FnZV9uYW1lIjoiY29tLnZlZ3NhaGF5eWFrLnNhbXBsZXMuY3VzdG9tdWkiLCJleHAiOjIwODI3NTg0MDAsIm1vZHVsZXMiOlsic3BlZWQtY29yZSIsInNwZWVkLXVpIl0sIm1heF9kZXZpY2VzIjotMSwibWF4X29mZmxpbmVfZGF5cyI6MzY1LCJ1bmxpbWl0ZWQiOnRydWUsImlzcyI6InZlZ19zYWhheXlha19hZG1pbiIsImlhdCI6MTc5MTk5OTk2Nn0.sTK92AM4VhsQOryvbcACNzx72lDwMx15pGPRzmoHaLkaMGKz3nMTNy_6m5sOfew_4QnytZ0ilOgLO0qpEFSAi0vjTs7MoivZuFkDtlzecTCe0pVIdvRq_SpFQRf6_QVvRMQD7tbgTpI0xe0Ic6AEn_X2HxzyoTuLv9Tx2M-sb5mdhtYvJ4PsupVInrnYql6EN-FHI_EkhpdauXNkpI2r7ElqO7ejRuQxaH43aWGsB2GcV5rZxCtx4Nu3LWjWelWX7OGloaDrv-_dW_zG_LlLg4K-8odcDE3IPjhA_XvJ_ngCkpNJbZheQ4ngZjLH0nb4EwKp3l6_KRbUILwCrigNnw"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            initializeTelematicsSdk()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkPermissionsAndStart()

        setContent {
            // White-label brand colors (Modern Neon/Dark mode brand)
            val brandDark = Color(0xFF121212)
            val brandAccent = Color(0xFF00FFCC) // Neon Cyan
            val brandWarning = Color(0xFFFF2A55) // Neon Pink/Red

            // Listen to real-time telematics stream
            val speedState by SpeedAssistant.observeFlow()
                .collectAsState(initial = SpeedData.SpeedUnavailable)
                
            val speedKmh = when (val state = speedState) {
                is SpeedData.SpeedChanged -> state.speedMps * 3.6f
                else -> 0f
            }
            val isOverLimit = (speedState as? SpeedData.SpeedChanged)?.isOverLimit ?: false

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = brandDark
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "MyBrand Telematics", 
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Embed the SDK's pre-built SpeedGauge component styled to match client brand!
                        SpeedGauge(
                            speed = speedKmh,
                            warningSpeed = 80f,
                            isTrackingActive = speedState is SpeedData.SpeedChanged,
                            isOverLimit = isOverLimit,
                            modifier = Modifier.size(280.dp),
                            colorScheme = SpeedUiColorScheme(
                                safeColor = brandAccent,
                                overSpeedColor = brandWarning,
                                inactiveTrackColor = Color(0xFF1E1E1E),
                                labelLight = Color.White
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = if (isOverLimit) "⚠️ Warning: Exceeding 80 km/h" else "Smooth & Safe Driving",
                            color = if (isOverLimit) brandWarning else brandAccent,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    private fun checkPermissionsAndStart() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineLocation == PackageManager.PERMISSION_GRANTED) {
            initializeTelematicsSdk()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            )
        }
    }

    private fun initializeTelematicsSdk() {
        SpeedAssistant.with(this, enterpriseJwt)
            .setDrivingProfile(DrivingProfile.CarCity)
            .start()
    }
}
