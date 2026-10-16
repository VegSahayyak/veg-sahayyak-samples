package com.vegsahayyak.samples.basic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import com.vegsahayyak.speedcore.api.DrivingProfile
import com.vegsahayyak.speedcore.api.SpeedAssistant
import com.vegsahayyak.speedcore.data.location.SpeedData

/**
 * =========================================================================================
 * Veg Sahayyak SDK - Basic Integration Sample (MainActivity)
 * =========================================================================================
 *
 * This sample demonstrates the core entry-point and lifecycle for integrating the Veg Sahayyak
 * B2B Telematics SDK (:speed-core) into any Android application:
 *
 * 1. CRYPTOGRAPHIC UNLOCK:
 *    Supplying the commercial RSA-2048 signed JWT token via [SpeedAssistant.with] to unlock
 *    the native C++ vault and initialize the telematics runtime engine.
 *
 * 2. RUNTIME PERMISSIONS:
 *    Standard Android runtime permission checking for [Manifest.permission.ACCESS_FINE_LOCATION]
 *    and [Manifest.permission.ACCESS_COARSE_LOCATION].
 *
 * 3. REACTIVE FLOW OBSERVATION:
 *    Consuming the public reactive [SpeedAssistant.observeFlow] Kotlin Flow which emits
 *    type-safe [SpeedData] updates (SpeedChanged, LowAccuracy, GPSDisabled, etc.).
 *
 * 4. DYNAMIC DRIVING PROFILES:
 *    Configuring overspeed thresholds on the fly using built-in [DrivingProfile] presets
 *    (e.g., CarCity, CarHighway, SchoolZone).
 * =========================================================================================
 */
class MainActivity : ComponentActivity() {

    /**
     * Dedicated commercial Evaluation JWT for this sample.
     * This token is cryptographically bound to package name: "com.vegsahayyak.samples.basic"
     * and grants authorized access to the ":speed-core" telematics engine.
     */
    private val enterpriseJwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGllbnRfaWQiOiJWZWcgU2FoYXl5YWsgU2FtcGxlIC0gQmFzaWMgSW50ZWdyYXRpb24iLCJwYWNrYWdlX25hbWUiOiJjb20udmVnc2FoYXl5YWsuc2FtcGxlcy5iYXNpYyIsImV4cCI6MjA4Mjc1ODQwMCwibW9kdWxlcyI6WyJzcGVlZC1jb3JlIl0sIm1heF9kZXZpY2VzIjotMSwibWF4X29mZmxpbmVfZGF5cyI6MzY1LCJ1bmxpbWl0ZWQiOnRydWUsImlzcyI6InZlZ19zYWhheXlha19hZG1pbiIsImlhdCI6MTc5MTk5OTk2Nn0.oqWfpjc4xS__eMU2nuGmANJwIvY5fTKqhqYm0VE-to27dJ_GkLSDD1XxC2-mqzuBtoODYEaTUaHEQtw-E-k_GqwjmNbsPXuMaMVJ8zI39lVKc1bxVNfN6V0pV425oVO8ZL8QxzJJssfT11LSUeG_pSnVMcJaG0AT-P3UMqUzA1rhAq2e0zRg9nmHInUM3phrfkatA9CWplzKV-D8x1GYbOjTHhpap9ZV6pa60jURFfkU8C-KsIxeBOueI6EKxfT7dRrDyb1DrFyYa2ho_YHiHP2MkdKkTtJ9gBXh0vJpWBn-mP_9RVMIGbWZ0Bj8-DtUGCrRuis6ji3nnyXYPamtBA"

    // State tracking the active driving profile threshold
    private var currentProfile by mutableStateOf<DrivingProfile>(DrivingProfile.CarCity)

    // State indicating whether foreground speed tracking is currently running
    private var isTrackingActive by mutableStateOf(false)

    /**
     * Register runtime permission contract for fine & coarse location.
     * When granted, the SDK is unlocked and foreground tracking begins immediately.
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            initializeTelematicsSdk(currentProfile)
        } else {
            Toast.makeText(this, "Location permission is required for telematics tracking.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Step 1: Ensure location permissions before starting the telematics engine
        checkPermissionsAndStart()

        // Step 2: Render Jetpack Compose UI
        setContent {
            /**
             * Observe the live telematics stream from the SDK.
             * [SpeedAssistant.observeFlow] returns a Kotlin StateFlow/Flow that emits [SpeedData].
             * By using [collectAsState], Compose recomposes smoothly whenever speed changes.
             */
            val speedState by SpeedAssistant.observeFlow()
                .collectAsState(initial = SpeedData.SpeedUnavailable)

            var useMph by remember { mutableStateOf(false) }

            BasicSampleScreen(
                speedState = speedState,
                useMph = useMph,
                onToggleUnit = { useMph = !useMph },
                selectedProfile = currentProfile,
                onSelectProfile = { profile ->
                    currentProfile = profile
                    // Dynamically update the SDK's speed threshold profile in real-time
                    if (isTrackingActive) {
                        initializeTelematicsSdk(profile)
                    }
                },
                isTracking = isTrackingActive,
                onToggleTracking = {
                    if (isTrackingActive) {
                        // Stop the foreground GPS service
                        SpeedAssistant.with(this).stop()
                        isTrackingActive = false
                    } else {
                        // Restart telematics tracking
                        initializeTelematicsSdk(currentProfile)
                    }
                }
            )
        }
    }

    /**
     * Inspects permission status. If already granted, starts the SDK; otherwise requests them.
     */
    private fun checkPermissionsAndStart() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineLocation == PackageManager.PERMISSION_GRANTED) {
            initializeTelematicsSdk(currentProfile)
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    /**
     * Core SDK Initialization:
     * - [SpeedAssistant.with(context, token)] validates the JWT, ensures device limits,
     *   and unlocks the native C++ spatial math engine.
     * - [.setDrivingProfile(profile)] applies preset speed limits (e.g. CarCity = 80 km/h max).
     * - [.start()] launches the foreground service with notifications and begins high-frequency GPS tracking.
     */
    private fun initializeTelematicsSdk(profile: DrivingProfile) {
        SpeedAssistant.with(this, enterpriseJwt)
            .setDrivingProfile(profile)
            .start()
        isTrackingActive = true
    }
}

/**
 * Modern Jetpack Compose dashboard designed for clarity and intuitive developer exploration:
 * - Brand header showcasing official Veg Sahayyak SDK logo
 * - Live speed gauge card with real-time overspeed hysteresis coloring
 * - Dynamic Driving Profile threshold switcher (City, Highway, School Zone)
 * - Developer Quick-Start code snippet box
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicSampleScreen(
    speedState: SpeedData,
    useMph: Boolean,
    onToggleUnit: () -> Unit,
    selectedProfile: DrivingProfile,
    onSelectProfile: (DrivingProfile) -> Unit,
    isTracking: Boolean,
    onToggleTracking: () -> Unit
) {
    // Official Veg Sahayyak brand palette
    val bgDark = Color(0xFF0F172A)
    val cardDark = Color(0xFF1E293B)
    val borderDark = Color(0xFF334155)
    val cyanAccent = Color(0xFF00F2FE)
    val textPrimary = Color(0xFFF8FAFC)
    val textSecondary = Color(0xFF94A3B8)

    // Speed in meters per second (SI unit delivered by GPS)
    val currentSpeedMps = when (speedState) {
        is SpeedData.SpeedChanged -> speedState.speedMps
        else -> 0f
    }

    // Unit conversion: 1 m/s = 3.6 km/h = 2.23694 mph
    val displaySpeed = if (useMph) currentSpeedMps * 2.23694f else currentSpeedMps * 3.6f
    val unitLabel = if (useMph) "mph" else "km/h"

    // Hysteresis calculation performed natively inside the SDK core
    val isOverLimit = (speedState as? SpeedData.SpeedChanged)?.isOverLimit ?: false

    // State label representing the telemetry engine status
    val statusText = when (speedState) {
        is SpeedData.SpeedChanged -> if (isOverLimit) "⚠️ Over Limit Warning" else "🟢 Tracking Live Telematics"
        is SpeedData.LowAccuracy -> "🟡 Weak Satellite Signal"
        is SpeedData.GPSDisabled -> "🔴 GPS Turned Off"
        is SpeedData.SpeedUnavailable -> "⚪ Acquiring Satellite Fix..."
        is SpeedData.Disabled -> "⏹️ Telematics Stopped"
    }

    Scaffold(
        containerColor = bgDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Official Veg Sahayyak logo
                        Image(
                            painter = painterResource(id = R.drawable.ic_veg_sahayyak_logo),
                            contentDescription = "Veg Sahayyak Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                        Column {
                            Text(
                                "Veg Sahayyak SDK",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                "Basic Integration Sample",
                                color = cyanAccent,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF090D16)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // =========================================================================
            // 1. Live Telematics Card
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
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = if (isOverLimit) Color(0xFFFF5252) else cyanAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    // Main Speed Numerals
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "%.1f".format(displaySpeed),
                            fontSize = 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isOverLimit) Color(0xFFFF416C) else cyanAccent,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = unitLabel,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Unit & Tracking Toggle Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = onToggleUnit,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, cyanAccent)
                        ) {
                            Text(
                                text = "Unit: $unitLabel",
                                color = cyanAccent,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = onToggleTracking,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTracking) Color(0xFFEF4444) else Color(0xFF10B981)
                            )
                        ) {
                            Text(
                                text = if (isTracking) "Stop Tracking" else "Start Tracking",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 2. Interactive Driving Profile Selector
            // =========================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderDark, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Driving Profile Thresholds",
                        color = textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Thresholds adjust overspeed alerts in real-time:",
                        color = textSecondary,
                        fontSize = 12.sp
                    )

                    val profiles = listOf(
                        DrivingProfile.CarCity to "City (80 km/h)",
                        DrivingProfile.CarHighway to "Highway (120 km/h)",
                        DrivingProfile.SchoolZone to "School (30 km/h)"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        profiles.forEach { (profile, label) ->
                            val isSelected = selectedProfile == profile
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectProfile(profile) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = cyanAccent,
                                    selectedLabelColor = Color(0xFF0F172A),
                                    containerColor = Color(0xFF0F172A),
                                    labelColor = textSecondary
                                )
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 3. Developer Integration Snippet Card
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
                        text = "Developer Quick-Start",
                        color = cyanAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = """
// 1. Initialize & Start Tracking
SpeedAssistant.with(context, jwt)
    .setDrivingProfile(DrivingProfile.CarCity)
    .start()

// 2. Observe Live Reactive Speed Flow
val speedState by SpeedAssistant.observeFlow()
    .collectAsState(SpeedData.SpeedUnavailable)
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
