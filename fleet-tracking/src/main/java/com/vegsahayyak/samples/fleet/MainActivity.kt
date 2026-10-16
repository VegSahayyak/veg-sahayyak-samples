package com.vegsahayyak.samples.fleet

import android.Manifest
import android.content.pm.PackageManager
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
import com.vegsahayyak.speedcore.api.SpeedAssistant
import com.vegsahayyak.speedcore.data.location.SpeedData

/**
 * =========================================================================================
 * Veg Sahayyak SDK - Sample 2: Fleet Tracking & Regulatory Telematics
 * =========================================================================================
 *
 * This sample demonstrates how enterprise fleet management systems integrate the Veg Sahayyak
 * SDK to enforce commercial transport regulations:
 *
 * 1. DYNAMIC REGULATORY THRESHOLDS:
 *    Unlike passenger consumer apps, commercial transport (heavy goods vehicles, coaches, delivery
 *    vans) must strictly adhere to statutory speed windows. The SDK allows dynamically setting:
 *      - Upper Warning Limit: SpeedAssistant.setWarningSpeedThreshold(maxSpeed)
 *      - Lower Warning Limit: SpeedAssistant.setMinWarningSpeedThreshold(minSpeed)
 *    Falling below the minimum speed on high-speed expressways is flagged as an obstruction hazard,
 *    while exceeding the maximum limit is flagged as a regulatory violation.
 *
 * 2. ENTERPRISE JWT LICENSING & ZERO-OVERHEAD PRIVACY:
 *    The provided enterprise token includes the claim `"unlimited": true`. When detected,
 *    the SDK's internal C++ core short-circuits local telemetry disk persistence, providing
 *    a zero-overhead privacy mode for enterprise customers who manage telematics independently.
 *
 * 3. REAL-TIME TELEMATICS STREAM:
 *    The host UI observes `SpeedAssistant.observeFlow()` to receive live GPS speed updates,
 *    horizontal accuracy estimates (meters), and regulatory boundary flags.
 */
class MainActivity : ComponentActivity() {

    /**
     * Commercial B2B enterprise license token issued for package:
     * "com.vegsahayyak.samples.fleet". Validated cryptographically at startup.
     */
    private val fleetEnterpriseJwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGllbnRfaWQiOiJWZWcgU2FoYXl5YWsgU2FtcGxlIC0gRmxlZXQgVHJhY2tpbmciLCJwYWNrYWdlX25hbWUiOiJjb20udmVnc2FoYXl5YWsuc2FtcGxlcy5mbGVldCIsImV4cCI6MjA4Mjc1ODQwMCwibW9kdWxlcyI6WyJzcGVlZC1jb3JlIl0sIm1heF9kZXZpY2VzIjotMSwibWF4X29mZmxpbmVfZGF5cyI6MzY1LCJ1bmxpbWl0ZWQiOnRydWUsImlzcyI6InZlZ19zYWhheXlha19hZG1pbiIsImlhdCI6MTc5MTk5OTk2Nn0.Eyevuvzgs10eHy63PjSAhf1YJYgaNrsGzH7mZU9fDz2vbTbw02likrdwnxbehVeTLXBRR0SCNCsN-lizIxekc86bXcKM4fZCBWjGX6lUdw76CODrzS2xrJBu_rMuamoYu0q9ChdInkpOMMKqzCJbSiSAUvgqk3DsA4SptrXQEQOHcIdEuF_dfeLwFZ-EREY2GdKcnA6W6UkBxhn4flAS0X09vSwSmIXWc62_drAuNheS5grqYjiWIfAc2dLJCxuw-D7arTB3pPI6Whw5tYDnYaosSTDGyNGbOTiYTSrR5PgsWo1V-DV2L1ltdRjopjDjYmqAqf22p1joDjSiokeG9g"

    /** Commercial vehicle profile model with statutory upper and lower speed limits. */
    data class VehicleProfile(
        val name: String,
        val icon: String,
        val minKmh: Float,
        val maxKmh: Float,
        val category: String,
        val description: String
    )

    private val availableProfiles = listOf(
        VehicleProfile(
            name = "Heavy Truck",
            icon = "🚛",
            minKmh = 20.0f,
            maxKmh = 80.0f,
            category = "Class 8 HGV",
            description = "Expressway corridor regulation (20 - 80 km/h)"
        ),
        VehicleProfile(
            name = "Intercity Coach",
            icon = "🚌",
            minKmh = 30.0f,
            maxKmh = 90.0f,
            category = "Passenger Transit",
            description = "High-speed transit limit (30 - 90 km/h)"
        ),
        VehicleProfile(
            name = "Delivery Van",
            icon = "🚐",
            minKmh = 10.0f,
            maxKmh = 60.0f,
            category = "Urban Freight",
            description = "Last-mile courier regulation (10 - 60 km/h)"
        )
    )

    // Tracks whether the telematics tracking service has been initialized
    private var isTrackingStarted by mutableStateOf(false)

    // Activity Result Launcher for Location Permissions
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            initializeFleetTracking(availableProfiles.first())
        } else {
            Toast.makeText(this, "Location permission required for fleet tracking", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check runtime permissions and initialize telematics
        checkPermissionsAndStart()

        setContent {
            // Selected active vehicle regulatory profile
            var selectedProfile by remember { mutableStateOf(availableProfiles.first()) }

            // Observe the live telematics reactive Flow from the SDK
            val speedState by SpeedAssistant.observeFlow()
                .collectAsState(initial = SpeedData.SpeedUnavailable)

            // Extract calculated speed in km/h and regulatory flags
            val currentSpeedKmh = when (val state = speedState) {
                is SpeedData.SpeedChanged -> state.speedMps * 3.6f
                else -> 0.0f
            }
            val isOverLimit = (speedState as? SpeedData.SpeedChanged)?.isOverLimit ?: false
            val isUnderLimit = (speedState as? SpeedData.SpeedChanged)?.isUnderLimit ?: false
            val accuracyMeters = (speedState as? SpeedData.SpeedChanged)?.accuracyMeters ?: -1.0f

            FleetTrackingDashboard(
                selectedProfile = selectedProfile,
                availableProfiles = availableProfiles,
                currentSpeedKmh = currentSpeedKmh,
                isOverLimit = isOverLimit,
                isUnderLimit = isUnderLimit,
                accuracyMeters = accuracyMeters,
                speedState = speedState,
                isTracking = isTrackingStarted,
                onSelectProfile = { newProfile ->
                    selectedProfile = newProfile
                    applyProfileThresholds(newProfile)
                },
                onToggleTracking = {
                    if (isTrackingStarted) {
                        stopFleetTracking()
                    } else {
                        checkPermissionsAndStart()
                    }
                }
            )
        }
    }

    /**
     * Verifies fine location permissions prior to starting GPS hardware.
     */
    private fun checkPermissionsAndStart() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineLocation == PackageManager.PERMISSION_GRANTED) {
            initializeFleetTracking(availableProfiles.first())
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
     * Initializes the SDK with the enterprise license and applies initial regulatory bounds.
     */
    private fun initializeFleetTracking(profile: VehicleProfile) {
        try {
            // 1. Bind enterprise license token and set bounds
            SpeedAssistant.with(this, fleetEnterpriseJwt)
                .setWarningSpeedThreshold(profile.maxKmh)
                .setMinWarningSpeedThreshold(profile.minKmh)
                .start()

            isTrackingStarted = true
            Toast.makeText(this, "Fleet Tracking Active: ${profile.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "License Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Dynamically updates the upper and lower speed limits when switching vehicle profiles.
     */
    private fun applyProfileThresholds(profile: VehicleProfile) {
        try {
            SpeedAssistant.with(this)
                .setWarningSpeedThreshold(profile.maxKmh)
                .setMinWarningSpeedThreshold(profile.minKmh)

            Toast.makeText(this, "Enforced ${profile.name} Limits (${profile.minKmh.toInt()}-${profile.maxKmh.toInt()} km/h)", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Gracefully halts GPS tracking and stops the foreground service.
     */
    private fun stopFleetTracking() {
        SpeedAssistant.with(this).stop()
        isTrackingStarted = false
        Toast.makeText(this, "Fleet Tracking Stopped", Toast.LENGTH_SHORT).show()
    }
}

/**
 * =========================================================================================
 * Sleek Dark Telematics Dispatcher Dashboard (Compose UI)
 * =========================================================================================
 */
@Composable
fun FleetTrackingDashboard(
    selectedProfile: MainActivity.VehicleProfile,
    availableProfiles: List<MainActivity.VehicleProfile>,
    currentSpeedKmh: Float,
    isOverLimit: Boolean,
    isUnderLimit: Boolean,
    accuracyMeters: Float,
    speedState: SpeedData,
    isTracking: Boolean,
    onSelectProfile: (MainActivity.VehicleProfile) -> Unit,
    onToggleTracking: () -> Unit
) {
    // Industrial Dark Telematics Color Palette
    val bgDark = Color(0xFF0B0F19)
    val cardDark = Color(0xFF131A29)
    val borderDark = Color(0xFF1E293B)
    val emeraldAccent = Color(0xFF10B981) // Compliant / In-Range
    val amberAccent = Color(0xFFF59E0B)   // Under-Speed Warning
    val crimsonAccent = Color(0xFFEF4444) // Over-Speed Violation
    val cyanAccent = Color(0xFF06B6D4)    // Telematics & Branding
    val textPrimary = Color(0xFFF8FAFC)
    val textSecondary = Color(0xFF94A3B8)

    // Animated dynamic numeral color based on compliance state
    val speedNumeralColor by animateColorAsState(
        targetValue = when {
            !isTracking -> textSecondary
            isOverLimit -> crimsonAccent
            isUnderLimit -> amberAccent
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
                // 1. Header with Official Veg Sahayyak Logo & Dispatcher Badge
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
                                    text = "FLEET B2B",
                                    color = cyanAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Commercial Regulatory Telematics",
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // License Opt-Out Privacy Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Text(
                            text = "Unlimited JWT",
                            color = Color(0xFFA5B4FC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // =========================================================================
                // 2. Main Speedometer & Regulatory Compliance Card
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

                        // Compliance Badge
                        val badgeText = when {
                            !isTracking -> "TRACKING INACTIVE"
                            isOverLimit -> "⚠️ OVER REGULATORY LIMIT (> ${selectedProfile.maxKmh.toInt()} km/h)"
                            isUnderLimit -> "🐢 BELOW HIGHWAY MINIMUM (< ${selectedProfile.minKmh.toInt()} km/h)"
                            speedState is SpeedData.SpeedChanged -> "✅ COMPLIANT SPEED (${selectedProfile.minKmh.toInt()} - ${selectedProfile.maxKmh.toInt()} km/h)"
                            speedState is SpeedData.LowAccuracy -> "🛰️ LOW GPS ACCURACY"
                            speedState is SpeedData.GPSDisabled -> "⚠️ DEVICE GPS DISABLED"
                            else -> "🛰️ ACQUIRING SATELLITE FIX..."
                        }

                        val badgeColor = when {
                            !isTracking -> textSecondary
                            isOverLimit -> crimsonAccent
                            isUnderLimit -> amberAccent
                            speedState is SpeedData.SpeedChanged -> emeraldAccent
                            else -> cyanAccent
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = badgeColor.copy(alpha = 0.12f),
                            modifier = Modifier.padding(bottom = 14.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        // Digital Speed Readout
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isTracking) "%.1f".format(currentSpeedKmh) else "--.-",
                                fontSize = 68.sp,
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
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Visual Regulatory Corridor Bar
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0B0F19), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Min: ${selectedProfile.minKmh.toInt()} km/h",
                                    color = amberAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Corridor: [${selectedProfile.minKmh.toInt()} - ${selectedProfile.maxKmh.toInt()}]",
                                    color = emeraldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Max: ${selectedProfile.maxKmh.toInt()} km/h",
                                    color = crimsonAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Corridor Progress Bar
                            val maxScale = (selectedProfile.maxKmh * 1.25f).coerceAtLeast(100f)
                            val progressFraction = if (isTracking) (currentSpeedKmh / maxScale).coerceIn(0f, 1f) else 0f

                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = speedNumeralColor,
                                trackColor = Color(0xFF1E293B)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Controls
                        Button(
                            onClick = onToggleTracking,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTracking) crimsonAccent else emeraldAccent
                            )
                        ) {
                            Text(
                                text = if (isTracking) "Stop Fleet Telematics" else "Start Fleet Telematics",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // =========================================================================
                // 3. Dynamic Vehicle Profile Selector
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
                                text = "Regulatory Vehicle Profile",
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Dynamic SDK Bounds",
                                color = cyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        availableProfiles.forEach { profile ->
                            val isSelected = profile == selectedProfile
                            Surface(
                                onClick = { onSelectProfile(profile) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) cyanAccent.copy(alpha = 0.12f) else Color(0xFF0F172A),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) cyanAccent else Color(0xFF1E293B)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = profile.icon, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = profile.name,
                                                color = if (isSelected) cyanAccent else textPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• ${profile.category}",
                                                color = textSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Text(
                                            text = profile.description,
                                            color = textSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = "${profile.minKmh.toInt()}-${profile.maxKmh.toInt()} km/h",
                                        color = if (isSelected) cyanAccent else Color(0xFFCBD5E1),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // 4. Telematic Diagnostics Card
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
                        Text(
                            text = "Telematic Diagnostics",
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GPS Horizontal Accuracy:", color = textSecondary, fontSize = 12.sp)
                            Text(
                                text = if (accuracyMeters >= 0f) "±${"%.1f".format(accuracyMeters)} m" else "Acquiring Fix",
                                color = if (accuracyMeters in 0f..15f) emeraldAccent else amberAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Offline Telematics Sync:", color = textSecondary, fontSize = 12.sp)
                            Text(
                                text = "Autonomous (TelemetryJobService)",
                                color = cyanAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Privacy / Enterprise Opt-Out:", color = textSecondary, fontSize = 12.sp)
                            Text(
                                text = "Active (\"unlimited\": true)",
                                color = Color(0xFFA5B4FC),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
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
// 1. Enforce Custom Commercial Thresholds
SpeedAssistant.with(context, fleetEnterpriseJwt)
    .setWarningSpeedThreshold(${selectedProfile.maxKmh}f)
    .setMinWarningSpeedThreshold(${selectedProfile.minKmh}f)
    .start()

// 2. Observe Reactive Telematics Flow
val speedState by SpeedAssistant.observeFlow()
    .collectAsState(SpeedData.SpeedUnavailable)

// 3. React to Regulatory Flags
val isOver = (speedState as? SpeedData.SpeedChanged)?.isOverLimit
val isUnder = (speedState as? SpeedData.SpeedChanged)?.isUnderLimit
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
