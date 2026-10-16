package com.vegsahayyak.samples.background

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vegsahayyak.speedautostart.AutoStartManager
import com.vegsahayyak.speedcore.api.DrivingProfile
import com.vegsahayyak.speedcore.api.SpeedAssistant
import com.vegsahayyak.speedcore.data.location.SpeedData

/**
 * =========================================================================================
 * Veg Sahayyak SDK - Sample 3: Headless Background Service & AutoStart
 * =========================================================================================
 *
 * This sample demonstrates how enterprise applications run the Veg Sahayyak SDK entirely
 * in the background as a headless foreground service:
 *
 * 1. AUTOMATIC IN-VEHICLE DETECTION (AutoStart Plugin):
 *    Utilizes Google Play Services Activity Recognition (`:speed-autostart` module) to
 *    autonomously detect when the user starts driving (`DetectedActivity.IN_VEHICLE`).
 *    The `AutoStartReceiver` receives the broadcast and starts the telematic engine without
 *    requiring the user to open or touch the application.
 *
 * 2. MANUAL SIMULATION MODE (Developer Desk Testing):
 *    Allows developers and QA testers to simulate vehicle entry (`start()`) and exit (`stop()`)
 *    directly from the UI without needing to physically drive a car.
 *
 * 3. PERSISTENT FOREGROUND NOTIFICATION:
 *    When active, `SpeedometerService` maintains a persistent notification displaying
 *    real-time speed and regulatory compliance, ensuring the OS never kills the telematics process.
 *
 * 4. TOKEN CACHING & BACKGROUND WAKEUP:
 *    The first call to `SpeedAssistant.with(context, token)` cryptographically caches the JWT
 *    into encrypted preferences so background broadcast receivers can wake up the engine
 *    without requiring the UI to supply the token again.
 */
class MainActivity : ComponentActivity() {

    /**
     * Commercial B2B license key authorized for package:
     * "com.vegsahayyak.samples.background" with modules: ["speed-core", "speed-autostart"].
     */
    private val enterpriseJwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGllbnRfaWQiOiJWZWcgU2FoYXl5YWsgU2FtcGxlIC0gQmFja2dyb3VuZCBTZXJ2aWNlIiwicGFja2FnZV9uYW1lIjoiY29tLnZlZ3NhaGF5eWFrLnNhbXBsZXMuYmFja2dyb3VuZCIsImV4cCI6MjA4Mjc1ODQwMCwibW9kdWxlcyI6WyJzcGVlZC1jb3JlIiwic3BlZWQtYXV0b3N0YXJ0Il0sIm1heF9kZXZpY2VzIjotMSwibWF4X29mZmxpbmVfZGF5cyI6MzY1LCJ1bmxpbWl0ZWQiOnRydWUsImlzcyI6InZlZ19zYWhheXlha19hZG1pbiIsImlhdCI6MTc5MTk5OTk2Nn0.oEyLb9tMHHGDB8IqeMF-cTAStEnvWkbEUydikVdUvOWooplo7hH-QBF7kVNJZsq3hx2ZGHmBUtzGL6w91jBfx5hKBgZQU3aoODJKGxGVdkdRr6GOZOz_wKFmuP8LMsNQlLA7qDIxx9Ohww762G7lA0xBQujOp-nNtvenYCDaXMPrKdUzbIJLKC04XZW1cUcWsTqqaUXN6T6IW1g4zgkNxyc2282LqUuJrBXN1W8T9gnck3qJZw3Ae1nHdkt4v3QNDv9Ii6WCWm-W4wcYfchCWAeCFzzkhdXmOOm2xQ25-HemxEa49dPa_ulmCDir9rrZRk2lwafBqnB3FwKC_wmyMg"

    // UI state for AutoStart registration status
    private var isAutoStartArmed by mutableStateOf(false)
    private var autoStartStatusMessage by mutableStateOf("Initializing...")

    // UI state for tracking service lifecycle
    private var isServiceRunning by mutableStateOf(false)
    private var isScreenOffTrackingEnabled by mutableStateOf(true)

    // Activity Result Launcher for Location and Activity Recognition Permissions
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (fineLocationGranted) {
            initializeAndArmHeadlessSdk()
        } else {
            autoStartStatusMessage = "Location Permission Denied"
            Toast.makeText(this, "Location permission is required for telematics", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verify runtime permissions and arm the AutoStart engine
        checkPermissionsAndArm()

        setContent {
            // Observe the live telematics reactive Flow
            val speedState by SpeedAssistant.observeFlow()
                .collectAsState(initial = SpeedData.SpeedUnavailable)

            val currentSpeedKmh = when (val state = speedState) {
                is SpeedData.SpeedChanged -> state.speedMps * 3.6f
                else -> 0.0f
            }
            val isOverLimit = (speedState as? SpeedData.SpeedChanged)?.isOverLimit ?: false
            val accuracyMeters = (speedState as? SpeedData.SpeedChanged)?.accuracyMeters ?: -1.0f

            HeadlessBackgroundDashboard(
                isAutoStartArmed = isAutoStartArmed,
                autoStartStatusMessage = autoStartStatusMessage,
                isServiceRunning = isServiceRunning,
                isScreenOffTrackingEnabled = isScreenOffTrackingEnabled,
                currentSpeedKmh = currentSpeedKmh,
                isOverLimit = isOverLimit,
                accuracyMeters = accuracyMeters,
                speedState = speedState,
                onArmAutoStart = { armAutoStart() },
                onSimulateVehicleEnter = { startManualService() },
                onSimulateVehicleExit = { stopManualService() },
                onToggleScreenOffTracking = { toggleScreenOffTracking() }
            )
        }
    }

    /**
     * Checks permissions safely adhering to Android runtime requirements.
     */
    private fun checkPermissionsAndArm() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Activity Recognition is a runtime permission on Android 10+ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        // Notification permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            initializeAndArmHeadlessSdk()
        } else {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    /**
     * Initializes the SDK, caches the JWT for background wakeups, and arms AutoStart.
     */
    private fun initializeAndArmHeadlessSdk() {
        try {
            // 1. Initialize SDK with enterprise JWT (caches token for background AutoStartReceiver)
            SpeedAssistant.with(this, enterpriseJwt)
                .setDrivingProfile(DrivingProfile.CarHighway)

            // 2. Configure screen-off background updates
            SpeedAssistant.with(this).preferences.setUpdateWhileScreenOff(true)
            isScreenOffTrackingEnabled = true

            // 3. Check if service is currently running
            isServiceRunning = SpeedAssistant.with(this).preferences.isRunning()

            // 4. Arm Google Play Services Activity Recognition
            armAutoStart()
        } catch (e: Exception) {
            autoStartStatusMessage = "Init Error: ${e.message}"
            Toast.makeText(this, "License Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Registers Activity Recognition transitions via AutoStartManager.
     */
    private fun armAutoStart() {
        AutoStartManager.enable(
            context = this,
            onPermissionRequired = {
                autoStartStatusMessage = "Activity Recognition Permission Needed"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION))
                }
            },
            onSuccess = {
                isAutoStartArmed = true
                autoStartStatusMessage = "Armed: Listening for IN_VEHICLE activity"
                Toast.makeText(this, "AutoStart Armed: Listening for vehicle motion", Toast.LENGTH_SHORT).show()
            },
            onError = { err ->
                isAutoStartArmed = false
                autoStartStatusMessage = "AutoStart Notice: ${err.message ?: "Play Services pending"}"
            }
        )
    }

    /**
     * Desk-testing simulation: Manually simulates an IN_VEHICLE event by starting the service.
     */
    private fun startManualService() {
        try {
            SpeedAssistant.with(this).start()
            isServiceRunning = true
            Toast.makeText(this, "Simulated Vehicle Entry: Foreground Service Started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Start Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Desk-testing simulation: Manually simulates an EXIT_VEHICLE event by stopping the service.
     */
    private fun stopManualService() {
        try {
            SpeedAssistant.with(this).stop()
            isServiceRunning = false
            Toast.makeText(this, "Simulated Vehicle Exit: Foreground Service Stopped", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Stop Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Toggles whether the telematics engine continues querying GPS when the screen turns off.
     */
    private fun toggleScreenOffTracking() {
        val newState = !isScreenOffTrackingEnabled
        SpeedAssistant.with(this).preferences.setUpdateWhileScreenOff(newState)
        isScreenOffTrackingEnabled = newState
        val label = if (newState) "Enabled (Continuous Background GPS)" else "Disabled (Conserve Battery)"
        Toast.makeText(this, "Screen-Off Tracking: $label", Toast.LENGTH_SHORT).show()
    }
}

/**
 * =========================================================================================
 * Sleek Dark Headless Telematics Dashboard (Compose UI)
 * =========================================================================================
 */
@Composable
fun HeadlessBackgroundDashboard(
    isAutoStartArmed: Boolean,
    autoStartStatusMessage: String,
    isServiceRunning: Boolean,
    isScreenOffTrackingEnabled: Boolean,
    currentSpeedKmh: Float,
    isOverLimit: Boolean,
    accuracyMeters: Float,
    speedState: SpeedData,
    onArmAutoStart: () -> Unit,
    onSimulateVehicleEnter: () -> Unit,
    onSimulateVehicleExit: () -> Unit,
    onToggleScreenOffTracking: () -> Unit
) {
    val bgDark = Color(0xFF0B0F19)
    val cardDark = Color(0xFF131A29)
    val borderDark = Color(0xFF1E293B)
    val emeraldAccent = Color(0xFF10B981) // Armed / Running / Compliant
    val amberAccent = Color(0xFFF59E0B)   // Pending / Warning
    val crimsonAccent = Color(0xFFEF4444) // Over-Speed / Stopped
    val cyanAccent = Color(0xFF06B6D4)    // Branding & Status
    val textPrimary = Color(0xFFF8FAFC)
    val textSecondary = Color(0xFF94A3B8)

    val speedNumeralColor by animateColorAsState(
        targetValue = when {
            !isServiceRunning -> textSecondary
            isOverLimit -> crimsonAccent
            speedState is SpeedData.SpeedChanged -> emeraldAccent
            else -> cyanAccent
        },
        label = "speedColor"
    )

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = bgDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // =========================================================================
                // 1. Header with Official Veg Sahayyak Logo & Headless Badge
                // =========================================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_veg_sahayyak_logo),
                        contentDescription = "Veg Sahayyak Logo",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(4.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Veg Sahayyak",
                                color = textPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = cyanAccent.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "HEADLESS B2B",
                                    color = cyanAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Background Telematics & AutoStart",
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // Foreground Service Running Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isServiceRunning) emeraldAccent.copy(alpha = 0.15f) else Color(0xFF1E293B)
                    ) {
                        Text(
                            text = if (isServiceRunning) "● Service Active" else "○ Service Idle",
                            color = if (isServiceRunning) emeraldAccent else textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // =========================================================================
                // 2. Headless Telematics Monitor Card
                // =========================================================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderDark, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Service State Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = (if (isServiceRunning) emeraldAccent else textSecondary).copy(alpha = 0.12f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = if (isServiceRunning) "FOREGROUND TELEMATICS SERVICE ACTIVE" else "HEADLESS SERVICE SLEEPING",
                                color = if (isServiceRunning) emeraldAccent else textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        // Live Speed Numerals
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isServiceRunning) "%.1f".format(currentSpeedKmh) else "--.-",
                                fontSize = 64.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = speedNumeralColor,
                                fontFamily = FontFamily.SansSerif
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "km/h",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        Text(
                            text = if (isServiceRunning) {
                                if (accuracyMeters >= 0f) "GPS Horizontal Accuracy: ±${"%.1f".format(accuracyMeters)} m" else "Acquiring Satellite Lock..."
                            } else {
                                "Start or trigger vehicle detection to launch background service"
                            },
                            color = textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Notification Notice
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0B0F19),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔔", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Persistent System Notification",
                                        color = textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "SpeedometerService runs as a foreground service with live notification and TTS audio alerts.",
                                        color = textSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // 3. Section A: Automatic In-Vehicle Wakeup (Auto Mode)
                // =========================================================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderDark, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🤖 Auto Mode: Activity Recognition",
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isAutoStartArmed) emeraldAccent.copy(alpha = 0.15f) else amberAccent.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isAutoStartArmed) "ARMED" else "UNARMED",
                                    color = if (isAutoStartArmed) emeraldAccent else amberAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Play Services Activity Recognition listens for DetectedActivity.IN_VEHICLE transitions. When vehicle motion is confirmed, AutoStartReceiver wakes up SpeedAssistant autonomously.",
                            color = textSecondary,
                            fontSize = 12.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Status: $autoStartStatusMessage",
                                color = if (isAutoStartArmed) emeraldAccent else Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        if (!isAutoStartArmed) {
                            Button(
                                onClick = onArmAutoStart,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = cyanAccent)
                            ) {
                                Text("Re-Arm Activity Recognition", color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // =========================================================================
                // 4. Section B: Manual Simulation Mode (Desk Testing Controls)
                // =========================================================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderDark, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🛠️ Manual Simulation Mode (Desk Testing)",
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Instant Test",
                                color = cyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = "Simulate vehicle entry/exit triggers on physical hardware or test screen-off continuous tracking without driving:",
                            color = textSecondary,
                            fontSize = 12.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onSimulateVehicleEnter,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                enabled = !isServiceRunning,
                                colors = ButtonDefaults.buttonColors(containerColor = emeraldAccent)
                            ) {
                                Text("Simulate Entry\n(Start Service)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onSimulateVehicleExit,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                enabled = isServiceRunning,
                                colors = ButtonDefaults.buttonColors(containerColor = crimsonAccent)
                            ) {
                                Text("Simulate Exit\n(Stop Service)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Screen-off background tracking toggle
                        Surface(
                            onClick = onToggleScreenOffTracking,
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Screen-Off Continuous GPS",
                                        color = textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isScreenOffTrackingEnabled) "Keeps tracking GPS when screen locks" else "Halts GPS on screen lock to save power",
                                        color = textSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = isScreenOffTrackingEnabled,
                                    onCheckedChange = { onToggleScreenOffTracking() }
                                )
                            }
                        }
                    }
                }

                // =========================================================================
                // 5. Developer Quick-Start Reference Card
                // =========================================================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderDark, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF090D16))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Developer Integration Reference",
                            color = cyanAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = """
// 1. Initialize SDK & Cache Token in Application.onCreate()
SpeedAssistant.with(context, enterpriseJwt)
    .setDrivingProfile(DrivingProfile.CarHighway)

// 2. Arm Activity Recognition AutoStart
AutoStartManager.enable(
    context = context,
    onPermissionRequired = { /* Prompt user for ACTIVITY_RECOGNITION */ },
    onSuccess = { /* Armed: AutoStartReceiver handles IN_VEHICLE */ },
    onError = { err -> /* Handle Play Services availability */ }
)

// 3. Or Manually Start/Stop Foreground Service
SpeedAssistant.with(context).start() // Launches SpeedometerService
SpeedAssistant.with(context).stop()  // Halts GPS & Notification
                            """.trimIndent(),
                            color = Color(0xFFCBD5E1),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
