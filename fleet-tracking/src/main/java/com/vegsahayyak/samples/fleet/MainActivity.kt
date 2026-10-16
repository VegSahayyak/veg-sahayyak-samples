package com.vegsahayyak.samples.fleet

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
import com.vegsahayyak.speedcore.data.location.SpeedData

class MainActivity : ComponentActivity() {

    // Commercial B2B license key for Fleet Tracking sample
    private val fleetEnterpriseJwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGllbnRfaWQiOiJWZWcgU2FoYXl5YWsgU2FtcGxlIC0gRmxlZXQgVHJhY2tpbmciLCJwYWNrYWdlX25hbWUiOiJjb20udmVnc2FoYXl5YWsuc2FtcGxlcy5mbGVldCIsImV4cCI6MjA4Mjc1ODQwMCwibW9kdWxlcyI6WyJzcGVlZC1jb3JlIl0sIm1heF9kZXZpY2VzIjotMSwibWF4X29mZmxpbmVfZGF5cyI6MzY1LCJ1bmxpbWl0ZWQiOnRydWUsImlzcyI6InZlZ19zYWhheXlha19hZG1pbiIsImlhdCI6MTc5MTk5OTk2Nn0.Eyevuvzgs10eHy63PjSAhf1YJYgaNrsGzH7mZU9fDz2vbTbw02likrdwnxbehVeTLXBRR0SCNCsN-lizIxekc86bXcKM4fZCBWjGX6lUdw76CODrzS2xrJBu_rMuamoYu0q9ChdInkpOMMKqzCJbSiSAUvgqk3DsA4SptrXQEQOHcIdEuF_dfeLwFZ-EREY2GdKcnA6W6UkBxhn4flAS0X09vSwSmIXWc62_drAuNheS5grqYjiWIfAc2dLJCxuw-D7arTB3pPI6Whw5tYDnYaosSTDGyNGbOTiYTSrR5PgsWo1V-DV2L1ltdRjopjDjYmqAqf22p1joDjSiokeG9g"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            initializeFleetTracking()
        } else {
            Toast.makeText(this, "Permissions Denied!", Toast.LENGTH_LONG).show()
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
            val isUnderLimit = (speedState as? SpeedData.SpeedChanged)?.isUnderLimit ?: false

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
                        Text("🚚 Fleet Management", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Custom Regulatory Truck Thresholds (20 - 80 km/h)")
                        
                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "${"%.1f".format(speedKmh)} km/h",
                            style = MaterialTheme.typography.displayLarge,
                            color = when {
                                isOverLimit -> MaterialTheme.colorScheme.error
                                isUnderLimit -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Card(modifier = Modifier.padding(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Regulatory Status", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = when {
                                        isOverLimit -> "⚠️ OVER TRUCK LIMIT (> 80 km/h)"
                                        isUnderLimit -> "🐢 BELOW MIN SPEED (< 20 km/h)"
                                        speedState is SpeedData.SpeedChanged -> "✅ Compliant Speed Range"
                                        else -> "🛰️ Awaiting GPS Fix..."
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Autonomous offline telemetry sync enabled", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissionsAndStart() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineLocation == PackageManager.PERMISSION_GRANTED) {
            initializeFleetTracking()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun initializeFleetTracking() {
        // Step 2: Inject custom regulatory speeds instead of consumer profiles (Float values)
        val truckMaxSpeedKmh = 80.0f
        val truckMinSpeedKmh = 20.0f

        SpeedAssistant.with(this, fleetEnterpriseJwt)
            .setWarningSpeedThreshold(truckMaxSpeedKmh)
            .setMinWarningSpeedThreshold(truckMinSpeedKmh)
            .start()
            
        Toast.makeText(this, "Truck Thresholds Enforced!", Toast.LENGTH_SHORT).show()
    }
}
