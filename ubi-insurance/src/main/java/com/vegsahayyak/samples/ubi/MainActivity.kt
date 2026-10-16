package com.vegsahayyak.samples.ubi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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

class MainActivity : ComponentActivity() {

    // Commercial B2B insurance license key for UBI sample
    private val insuranceEnterpriseJwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGllbnRfaWQiOiJWZWcgU2FoYXl5YWsgU2FtcGxlIC0gVUJJIEluc3VyYW5jZSIsInBhY2thZ2VfbmFtZSI6ImNvbS52ZWdzYWhheXlhay5zYW1wbGVzLnViaSIsImV4cCI6MjA4Mjc1ODQwMCwibW9kdWxlcyI6WyJzcGVlZC1jb3JlIl0sIm1heF9kZXZpY2VzIjotMSwibWF4X29mZmxpbmVfZGF5cyI6MzY1LCJ1bmxpbWl0ZWQiOnRydWUsImlzcyI6InZlZ19zYWhheXlha19hZG1pbiIsImlhdCI6MTc5MTk5OTk2Nn0.NzhHmq2xPwAKmFyumgaqd_zfQmNsy1_ViIfR2uF5NRpV8MgFoT4Upth1ayO5G0kGuq_id5V0m68pXqMKdZFRYr62DfPwuial9-qOgoHDEU8wPKFACqMoMsLseG7WWscAeGYSC2Uuq20tDLYj8RZDA5RHT3PM_iUWZTlmJD6yGxL2NoobJM085nJUYvqs5TH1-88-vd_Ii6aifBURHYQeaiX7J9J0qin_Jp8jQCqCUudpWU-QIH7-AtexkgOVoqAerFNUNZsEKe5MTJfNLhHFpTkrVhc6G2DJ_fwHnR-ITdlAvjEGKxwZeZG_czWr6pRQ9oZYtUJwP0MZxc5ue4-n4w"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            initializeUBI()
        } else {
            Toast.makeText(this, "Permissions Denied!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkPermissionsAndStart()

        setContent {
            // Listen specifically for the speeding metric to build a risk profile
            val speedState by SpeedAssistant.observeFlow()
                .collectAsState(initial = SpeedData.SpeedUnavailable)

            val isSpeeding = (speedState as? SpeedData.SpeedChanged)?.isOverLimit ?: false
            val speedKmh = when (val state = speedState) {
                is SpeedData.SpeedChanged -> state.speedMps * 3.6f
                else -> 0f
            }

            // A simple UBI risk score simulation: Increase risk while speeding
            var riskScore by remember { mutableStateOf(0) }
            LaunchedEffect(isSpeeding) {
                while (isSpeeding) {
                    kotlinx.coroutines.delay(1000) // 1 penalty point per second of speeding
                    riskScore += 1
                }
            }
            
            val bgColor = if (isSpeeding) Color(0x33FF0000) else MaterialTheme.colorScheme.background

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = bgColor
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🛡️ Usage-Based Insurance", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Pay-How-You-Drive Analytics")
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        Text("Trip Risk Score", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "$riskScore Points",
                            style = MaterialTheme.typography.displayLarge,
                            color = if (riskScore > 10) Color.Red else MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "${"%.1f".format(speedKmh)} km/h",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        if (isSpeeding) {
                            Text("🚨 SPEEDING DETECTED", color = Color.Red, style = MaterialTheme.typography.titleLarge)
                            Text("Logging penalty to risk profile...", color = Color.Red)
                        } else {
                            Text("✅ Driving Safely", color = Color(0xFF388E3C))
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissionsAndStart() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineLocation == PackageManager.PERMISSION_GRANTED) {
            initializeUBI()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun initializeUBI() {
        SpeedAssistant.with(this, insuranceEnterpriseJwt)
            .setDrivingProfile(DrivingProfile.CarCity)
            .start()
            
        Toast.makeText(this, "Risk Analytics Active!", Toast.LENGTH_SHORT).show()
    }
}
