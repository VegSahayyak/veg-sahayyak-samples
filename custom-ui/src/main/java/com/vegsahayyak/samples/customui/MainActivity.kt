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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vegsahayyak.speedcore.api.SpeedAssistant
import com.vegsahayyak.speedcore.api.DrivingProfile
import com.vegsahayyak.speedcore.internal.SpeedCoreInternal
import kotlinx.coroutines.flow.map

// Mocking the speed-ui compose components for the sample
import com.vegsahayyak.speedui.compose.SpeedGauge
import com.vegsahayyak.speedui.compose.SpeedLimitSign

class MainActivity : ComponentActivity() {

    // Your commercial B2B license key
    private val enterpriseJwt = "eyJhbGciOiJSUzI1NiIs..."

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
            // White-label brand colors (e.g., a modern Neon/Dark mode brand)
            val brandDark = Color(0xFF121212)
            val brandAccent = Color(0xFF00FFCC) // Neon Cyan
            val brandWarning = Color(0xFFFF2A55) // Neon Pink/Red

            // Listen to real-time data
            val speedData by SpeedCoreInternal.speedRepository?.observeFlow()
                ?.collectAsState(initial = null)
                
            val speedKmh = speedData?.speedKmh ?: 0.0
            val isSpeeding = speedData?.isSpeeding ?: false

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
                            text = "MyBrand Navigation", 
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(64.dp))
                        
                        // Drop in the SDK's pre-built SpeedGauge component
                        // but aggressively style it to match the brand!
                        SpeedGauge(
                            speedKmh = speedKmh,
                            modifier = Modifier.size(280.dp),
                            trackColor = Color.DarkGray,
                            indicatorColor = if (isSpeeding) brandWarning else brandAccent,
                            textColor = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Drop in the SDK's Speed Limit Sign
                        SpeedLimitSign(
                            limitKmh = 60, // Mocked for sample
                            modifier = Modifier.size(80.dp)
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
