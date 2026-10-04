package com.vegsahayyak.samples.ubi

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
import com.vegsahayyak.speedcore.internal.SpeedCoreInternal
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    // Your commercial B2B insurance license key
    private val insuranceEnterpriseJwt = "eyJhbGciOiJSUzI1NiIs..."

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
            val isSpeeding by SpeedCoreInternal.speedRepository?.observeFlow()
                ?.map { it.isSpeeding }
                ?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }

            // A simple UBI risk score simulation: Increase risk while speeding
            var riskScore by remember { mutableStateOf(0) }
            LaunchedEffect(isSpeeding) {
                while (isSpeeding) {
                    kotlinx.coroutines.delay(1000) // 1 penalty point per second of speeding
                    riskScore += 1
                }
            }
            
            val bgColor = if (isSpeeding) Color(0x66FF0000) else MaterialTheme.colorScheme.background

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
