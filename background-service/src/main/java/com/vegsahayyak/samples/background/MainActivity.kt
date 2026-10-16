package com.vegsahayyak.samples.background

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vegsahayyak.speedautostart.AutoStartManager
import com.vegsahayyak.speedcore.api.SpeedAssistant
import com.vegsahayyak.speedcore.api.DrivingProfile

class MainActivity : ComponentActivity() {

    // Commercial B2B license key for Background Service sample
    private val enterpriseJwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGllbnRfaWQiOiJWZWcgU2FoYXl5YWsgU2FtcGxlIC0gQmFja2dyb3VuZCBTZXJ2aWNlIiwicGFja2FnZV9uYW1lIjoiY29tLnZlZ3NhaGF5eWFrLnNhbXBsZXMuYmFja2dyb3VuZCIsImV4cCI6MjA4Mjc1ODQwMCwibW9kdWxlcyI6WyJzcGVlZC1jb3JlIiwic3BlZWQtYXV0b3N0YXJ0Il0sIm1heF9kZXZpY2VzIjotMSwibWF4X29mZmxpbmVfZGF5cyI6MzY1LCJ1bmxpbWl0ZWQiOnRydWUsImlzcyI6InZlZ19zYWhheXlha19hZG1pbiIsImlhdCI6MTc5MTk5OTk2Nn0.oEyLb9tMHHGDB8IqeMF-cTAStEnvWkbEUydikVdUvOWooplo7hH-QBF7kVNJZsq3hx2ZGHmBUtzGL6w91jBfx5hKBgZQU3aoODJKGxGVdkdRr6GOZOz_wKFmuP8LMsNQlLA7qDIxx9Ohww762G7lA0xBQujOp-nNtvenYCDaXMPrKdUzbIJLKC04XZW1cUcWsTqqaUXN6T6IW1g4zgkNxyc2282LqUuJrBXN1W8T9gnck3qJZw3Ae1nHdkt4v3QNDv9Ii6WCWm-W4wcYfchCWAeCFzzkhdXmOOm2xQ25-HemxEa49dPa_ulmCDir9rrZRk2lwafBqnB3FwKC_wmyMg"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            initializeHeadlessSdk()
        } else {
            Toast.makeText(this, "Background/Activity Permissions Denied!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkPermissionsAndStart()

        setContent {
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
                        Text("👻 Headless Autostart", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Zero-Touch Integration", style = MaterialTheme.typography.bodyLarge)
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        Text(
                            text = "You can now close this app.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The SDK will autonomously wake up via Play Services\nwhen it detects you are driving.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }
        }
    }

    private fun checkPermissionsAndStart() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            initializeHeadlessSdk()
        } else {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun initializeHeadlessSdk() {
        // 1. Initialize the SDK
        SpeedAssistant.with(this, enterpriseJwt)
            .setDrivingProfile(DrivingProfile.CarHighway)
            
        // 2. Hand control over to the AutoStart manager
        AutoStartManager.enable(
            context = this,
            onPermissionRequired = {
                Toast.makeText(this, "Activity Recognition permission needed", Toast.LENGTH_SHORT).show()
            },
            onSuccess = {
                Toast.makeText(this, "Listening for driving activity in background!", Toast.LENGTH_LONG).show()
            },
            onError = { err ->
                Toast.makeText(this, "AutoStart error: ${err.message}", Toast.LENGTH_LONG).show()
            }
        )
    }
}
