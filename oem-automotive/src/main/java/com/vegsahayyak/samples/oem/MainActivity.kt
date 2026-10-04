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
import com.vegsahayyak.speedui.compose.SpeedHUD
import com.vegsahayyak.speedui.compose.SpeedLimitSign

class MainActivity : ComponentActivity() {

    // Your commercial B2B OEM license key
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
            // Listen to real-time data
            val speedData by SpeedCoreInternal.speedRepository?.observeFlow()
                ?.collectAsState(initial = null)
                
            val speedKmh = speedData?.speedKmh ?: 0.0

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
                                    text = "Google Maps / Media Player", 
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(32.dp))
                        
                        // Right Side: Veg Sahayyak Telematics HUD
                        Column(
                            modifier = Modifier
                                .weight(0.5f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("TELEMATICS HUD", color = Color.DarkGray, style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Mocked SDK SpeedHUD component optimized for head units
                            SpeedHUD(
                                speedKmh = speedKmh,
                                modifier = Modifier.size(200.dp),
                                theme = "AutomotiveDark"
                            )
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            SpeedLimitSign(
                                limitKmh = 100, // Mocked for sample
                                modifier = Modifier.size(100.dp)
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
        // OEMs usually use Highway profiles since vehicles spend most time on highways
        SpeedAssistant.with(this, enterpriseJwt)
            .setDrivingProfile(DrivingProfile.CarHighway)
            .start()
    }
}
