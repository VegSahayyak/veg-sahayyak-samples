package com.vegsahayyak.samples.oem

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
import com.vegsahayyak.speedui.components.HudDisplay

class MainActivity : ComponentActivity() {

    // Commercial B2B OEM license key for Automotive sample
    private val enterpriseJwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGllbnRfaWQiOiJWZWcgU2FoYXl5YWsgU2FtcGxlIC0gT0VNIEF1dG9tb3RpdmUiLCJwYWNrYWdlX25hbWUiOiJjb20udmVnc2FoYXl5YWsuc2FtcGxlcy5vZW0iLCJleHAiOjIwODI3NTg0MDAsIm1vZHVsZXMiOlsic3BlZWQtY29yZSIsInNwZWVkLXVpIiwic3BlZWQtb3ZlcmxheSJdLCJtYXhfZGV2aWNlcyI6LTEsIm1heF9vZmZsaW5lX2RheXMiOjM2NSwidW5saW1pdGVkIjp0cnVlLCJpc3MiOiJ2ZWdfc2FoYXl5YWtfYWRtaW4iLCJpYXQiOjE3OTE5OTk5NjZ9.ffJE0m4Ej0kH28Z_eT3UqK3163u_JB5BjunMwqBA1KZ75nOgRAivDc5qV_BuZa7ymyaKGUo8NpbDr_3RHr-H9b_dF5i_vC7jMUPYZTLuyzShNV4np23LgeA8xjBJ3xmTmz-LW4aTPz08cXCtKENWAtiiP4yIrZApS53G4b9BwBkNP5WlJtedhSRuivj-2B4lk5nfx_yw-FSQSAg612Yw4v8CFU054PS0Mokh2VGv_tVA-hexzpUxW0x4qcoyx5sqHgWXrq5FYa2JFLTwIHuD9zo3HxBd_R_o1e-siBnkqb_i2mtYDNjlB806rrYq4r9aAbJp763zSrDFmVz2Gc4wOg"

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
                    color = Color.Black // Automotive displays typically use pure OLED black
                ) {
                    // Landscape Split-Screen Dashboard Layout
                    Row(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        
                        // Left Side: Mock Media / Navigation Panel
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Automotive In-Dash System\nGoogle Maps & Media Panel", 
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(24.dp))
                        
                        // Right Side: Veg Sahayyak Telematics HUD
                        Column(
                            modifier = Modifier
                                .weight(0.9f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("TELEMATICS HUD", color = Color.DarkGray, style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // High-contrast Windshield HUD display component
                            HudDisplay(
                                speed = speedKmh,
                                warningSpeed = 100f,
                                isTrackingActive = speedState is SpeedData.SpeedChanged,
                                isOverLimit = isOverLimit,
                                isMirrored = false,
                                modifier = Modifier.fillMaxWidth().height(260.dp)
                            )
                        }
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
            .setDrivingProfile(DrivingProfile.CarHighway)
            .start()
    }
}
