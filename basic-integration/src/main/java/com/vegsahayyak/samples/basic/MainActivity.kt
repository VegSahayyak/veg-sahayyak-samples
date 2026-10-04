package com.vegsahayyak.samples.basic

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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vegsahayyak.speedcore.api.SpeedAssistant
import com.vegsahayyak.speedcore.api.DrivingProfile
import com.vegsahayyak.speedcore.internal.SpeedCoreInternal
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    // Your commercial license key provided by Veg Sahayyak
    private val enterpriseJwt = "eyJhbGciOiJSUzI1NiIs..."

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            initializeTelematicsSdk()
        } else {
            Toast.makeText(this, "Location Permissions Denied!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkPermissionsAndStart()

        setContent {
            // Actively listen to the SDK's speed flow!
            val speedKmh by SpeedCoreInternal.speedRepository?.observeFlow()
                ?.map { it.speedKmh }
                ?.collectAsState(initial = 0.0) ?: remember { mutableStateOf(0.0) }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Veg Sahayyak SDK", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Basic Integration Sample", style = MaterialTheme.typography.bodyLarge)
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        // Displaying the real-time speed listener data
                        Text(
                            text = "${"%.1f".format(speedKmh)} km/h", 
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary
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
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun initializeTelematicsSdk() {
        // Step 1: Unlock the C++ Vault and start tracking
        SpeedAssistant.with(this, enterpriseJwt)
            .setDrivingProfile(DrivingProfile.CarCity)
            .start()
            
        Toast.makeText(this, "SDK Initialized & Tracking Started!", Toast.LENGTH_SHORT).show()
    }
}
