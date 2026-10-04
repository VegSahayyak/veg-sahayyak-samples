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
import com.vegsahayyak.speedcore.internal.SpeedCoreInternal

class MainActivity : ComponentActivity() {

    // Your commercial B2B license key provided by Veg Sahayyak
    private val fleetEnterpriseJwt = "eyJhbGciOiJSUzI1NiIs..."

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
            // Listen for offline-first telemetry syncing metrics
            val unsyncedViolations by SpeedCoreInternal.telemetryManager?.observeUnsyncedCount()
                ?.collectAsState(initial = 0) ?: remember { mutableStateOf(0) }

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
                        Text("Custom Regulatory Truck Thresholds")
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        // Displaying the offline-first telemetry queue
                        Card(modifier = Modifier.padding(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Offline Telemetry Queue", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$unsyncedViolations Pending Syncs",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = if (unsyncedViolations > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Text("Will autonomously push to Supabase via JobScheduler", style = MaterialTheme.typography.bodySmall)
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
        // Step 2: Inject custom regulatory speeds instead of consumer profiles
        val truckMaxSpeedKmh = 80.0
        val truckMinSpeedKmh = 20.0

        SpeedAssistant.with(this, fleetEnterpriseJwt)
            .setWarningSpeedThreshold(truckMaxSpeedKmh)
            .setMinWarningSpeedThreshold(truckMinSpeedKmh)
            .start()
            
        Toast.makeText(this, "Truck Thresholds Enforced!", Toast.LENGTH_SHORT).show()
    }
}
