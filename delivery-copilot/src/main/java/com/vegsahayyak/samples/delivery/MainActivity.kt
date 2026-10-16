package com.vegsahayyak.samples.delivery

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vegsahayyak.speedcore.api.DrivingProfile
import com.vegsahayyak.speedcore.api.SpeedAssistant
import com.vegsahayyak.speedcore.data.location.SpeedData
import com.vegsahayyak.speedoverlay.OverlayService
import com.vegsahayyak.speedoverlay.PipWidget
import com.vegsahayyak.speedoverlay.PipWidgetConfig
import com.vegsahayyak.speedui.api.VoiceAction
import com.vegsahayyak.speedui.api.VoiceCommandParser
import com.vegsahayyak.speedui.components.VoiceCommandButton
import com.vegsahayyak.speedui.utils.RegexVoiceCommandParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Veg Sahayyak SDK - Sample 7: Delivery Co-Pilot
 *
 * Designed specifically for two-wheeler / motorcycle delivery pilots and quick-commerce
 * last-mile couriers (food, grocery, parcel delivery).
 *
 * Core Pillars:
 * 1. Portrait-Only Ergonomics: Optimized for handlebar-mounted phone holders.
 * 2. Glare-Resistant, Subtle Natural Palette: High daylight contrast without distracting the rider.
 * 3. Live GPS Telematics Stream: Reads speed and accuracy directly from the hardware GPS engine via [SpeedAssistant.observeFlow].
 * 4. Hands-Free Voice Commands (:speed-ui): Change limits and road safety modes while keeping hands on handlebars.
 * 5. Floating PiP Navigation Overlay (:speed-overlay): Draggable speed bubble over Google Maps & delivery partner apps.
 * 6. Rider Safety & Fatigue Monitor: Track shift duration, safe-riding compliance, and fatigue alerts.
 * 7. Delivery Bench Simulator: Allows desk evaluation without riding when toggled on.
 */

// Delivery Co-Pilot Color Palette (Subtle, Aviation-grade, Low Glare)
object CoPilotColors {
    val CarbonBackground = Color(0xFF0B0F17)
    val SlateSurface = Color(0xFF141C2B)
    val SurfaceBorder = Color(0xFF1E293B)
    val SurfaceBorderActive = Color(0xFF334155)

    val AviationSky = Color(0xFF0EA5E9)       // Primary action / Telematics brand
    val SafeMint = Color(0xFF10B981)          // Under speed limit (Calm green)
    val CautionAmber = Color(0xFFF59E0B)      // Approaching limit (Aviation amber)
    val HazardCrimson = Color(0xFFEF4444)     // Over limit (Alert crimson)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextMuted = Color(0xFF94A3B8)
    val TextSubtle = Color(0xFF64748B)
}

enum class DeliveryHazardZone(
    val title: String,
    val speedLimitKmh: Float,
    val description: String,
    val profile: DrivingProfile
) {
    URBAN_DELIVERY(
        title = "Urban Lane",
        speedLimitKmh = 45f,
        description = "Standard city corridor & residential zones",
        profile = DrivingProfile.CarCity
    ),
    MONSOON_WET(
        title = "Monsoon / Wet",
        speedLimitKmh = 30f,
        description = "Reduced traction & extended braking distance",
        profile = DrivingProfile.SchoolZone
    ),
    FLYOVER_BYPASS(
        title = "Flyover / Bypass",
        speedLimitKmh = 65f,
        description = "Elevated corridor & arterial ring road",
        profile = DrivingProfile.Motorcycle
    ),
    MARKET_PEDESTRIAN(
        title = "Market / School",
        speedLimitKmh = 25f,
        description = "Dense pedestrian zone & alley navigation",
        profile = DrivingProfile.Bicycle
    )
}

/**
 * Enhanced Voice Command Parser supporting natural delivery pilot phrases
 * along with the SDK's built-in regex rules.
 */
class DeliveryVoiceCommandParser(
    private val baseParser: RegexVoiceCommandParser = RegexVoiceCommandParser()
) : VoiceCommandParser {

    override suspend fun parse(text: String): VoiceAction? {
        val lower = text.lowercase().trim()

        // Delivery Hazard Profile Shortcuts
        if (lower.contains("monsoon") || lower.contains("rain") || lower.contains("wet")) {
            return VoiceAction.SetMaxLimit(30f)
        }
        if (lower.contains("urban") || lower.contains("city") || lower.contains("residential")) {
            return VoiceAction.SetMaxLimit(45f)
        }
        if (lower.contains("flyover") || lower.contains("highway") || lower.contains("express")) {
            return VoiceAction.SetMaxLimit(65f)
        }
        if (lower.contains("market") || lower.contains("school") || lower.contains("slow")) {
            return VoiceAction.SetMaxLimit(25f)
        }

        // Delegate to SDK's default regex parser for general commands ("set max limit to 40", etc.)
        return baseParser.parse(text)
    }
}

class MainActivity : ComponentActivity() {

    private val deliveryLicenseToken =
        "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGllbnRfaWQiOiJWZWcgU2FoYXl5YWsgU2FtcGxlIC0gRGVsaXZlcnkgQ28tUGlsb3QiLCJwYWNrYWdlX25hbWUiOiJjb20udmVnc2FoYXl5YWsuc2FtcGxlcy5kZWxpdmVyeSIsImV4cCI6MjA4Mjc1ODQwMCwibW9kdWxlcyI6WyJzcGVlZC1jb3JlIiwic3BlZWQtdWkiLCJzcGVlZC1vdmVybGF5Il0sIm1heF9kZXZpY2VzIjotMSwibWF4X29mZmxpbmVfZGF5cyI6MzY1LCJ1bmxpbWl0ZWQiOnRydWUsImlzcyI6InZlZ19zYWhheXlha19hZG1pbiIsImlhdCI6MTc5MjE2MDc5MH0.EwJYrcvmG2ME8fsLHv-mafJV1Q6BZ4WTBI6tjILF78oFy7eJQPRN265YOlhR7tgDi20NK8se1Nzg3zbnpMHNDv49P5avuUIb_M1omunApW63SBQoZs9NA-9KzntK_4rC6qJu6NtRfTbG1FtNHX4-BFsjXzZgCeLklCt89Icc5prmpuxsce46aw0SPL_FsE-sr1kqNOgAhNYiMvnlz_A4UMSLhaY5TPQiwVDwUu8ey7BStD9Te9I1TXVQ6a0pdTXd2a_9OF5AJUneheo2Op9CQz_dw2k9aIyO661ZBtz5R-6sz7DcOHb542ZKR0QDvSxeNIsRHcX5Xh305dc0S8FsYw"

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineLocation = results[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = results[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineLocation || coarseLocation) {
            initializeTelematicsSdk(DrivingProfile.CarCity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure location permissions and start the GPS tracking engine immediately
        checkPermissionsAndStart()

        setContent {
            DeliveryCoPilotScreen(
                onProfileChanged = { profile ->
                    SpeedAssistant.with(this, deliveryLicenseToken)
                        .setDrivingProfile(profile)
                        .start()
                },
                onLimitChanged = { limit ->
                    try {
                        SpeedAssistant.with(this, deliveryLicenseToken)
                            .preferences
                            .setWarningSpeedThreshold(limit)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onLaunchOverlay = { launchFloatingOverlay() },
                onStopOverlay = { stopFloatingOverlay() },
                hasOverlayPermission = { canDrawOverlays() },
                onRequestOverlayPermission = { requestOverlayPermission() }
            )
        }
    }

    private fun checkPermissionsAndStart() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        val hasAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

        if (fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED) {
            initializeTelematicsSdk(DrivingProfile.CarCity)
        } else {
            val list = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (hasAudio != PackageManager.PERMISSION_GRANTED) {
                list.add(Manifest.permission.RECORD_AUDIO)
            }
            permissionLauncher.launch(list.toTypedArray())
        }
    }

    private fun initializeTelematicsSdk(profile: DrivingProfile) {
        try {
            SpeedAssistant.with(this, deliveryLicenseToken)
                .setDrivingProfile(profile)
                .start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun launchFloatingOverlay() {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopFloatingOverlay() {
        val intent = Intent(this, OverlayService::class.java)
        stopService(intent)
    }
}

@Composable
fun DeliveryCoPilotScreen(
    onProfileChanged: (DrivingProfile) -> Unit,
    onLimitChanged: (Float) -> Unit,
    onLaunchOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    hasOverlayPermission: () -> Boolean,
    onRequestOverlayPermission: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Telematics State
    var speedLimitKmh by remember { mutableStateOf(45f) }
    var selectedHazardZone by remember { mutableStateOf(DeliveryHazardZone.URBAN_DELIVERY) }

    // Live GPS Telematics Stream from SpeedAssistant
    val speedState by SpeedAssistant.observeFlow()
        .collectAsState(initial = SpeedData.SpeedUnavailable)

    var lastValidLiveSpeedKmh by remember { mutableFloatStateOf(0f) }

    val liveSpeedKmh = when (val state = speedState) {
        is SpeedData.SpeedChanged -> {
            val rawKmh = (state.speedMps * 3.6f).coerceAtLeast(0f)
            // Filter out unrealistic teleportation leaps (> 180 km/h) from cell/wifi jumps
            if (rawKmh in 0f..180f) {
                lastValidLiveSpeedKmh = rawKmh
                rawKmh
            } else {
                lastValidLiveSpeedKmh
            }
        }
        else -> 0f
    }

    val liveGpsAccuracy = when (val state = speedState) {
        is SpeedData.SpeedChanged -> state.accuracyMeters
        else -> -1f
    }

    val liveIsOverLimit = when (val state = speedState) {
        is SpeedData.SpeedChanged -> {
            if ((state.speedMps * 3.6f) in 0f..180f) state.isOverLimit else false
        }
        else -> false
    }

    val gpsStatusLabel = when (val state = speedState) {
        is SpeedData.SpeedChanged -> {
            if (state.accuracyMeters <= 5f) "GPS: ±${state.accuracyMeters.roundToInt()}m (Excellent)"
            else if (state.accuracyMeters <= 15f) "GPS: ±${state.accuracyMeters.roundToInt()}m (Good)"
            else "GPS: ±${state.accuracyMeters.roundToInt()}m (Fair)"
        }
        is SpeedData.LowAccuracy -> "GPS: Weak Signal (${state.reason})"
        is SpeedData.SpeedUnavailable -> "GPS: Acquiring Satellites..."
        is SpeedData.GPSDisabled -> "GPS: Signal Lost / Searching"
        is SpeedData.Disabled -> "GPS: Standby"
    }

    // Simulator State (defaults to FALSE so app reads from LIVE GPS!)
    var isSimulatorMode by remember { mutableStateOf(false) }
    var simulatedSpeedKmh by remember { mutableFloatStateOf(42f) }
    var simulatedGpsAccuracy by remember { mutableFloatStateOf(3f) }

    // Active speed and telematics source resolution
    val effectiveSpeed = if (isSimulatorMode) simulatedSpeedKmh else liveSpeedKmh
    val effectiveAccuracy = if (isSimulatorMode) simulatedGpsAccuracy else liveGpsAccuracy
    val isOverLimit = if (isSimulatorMode) (effectiveSpeed > speedLimitKmh) else (effectiveSpeed > speedLimitKmh || liveIsOverLimit)
    val isApproaching = effectiveSpeed in (speedLimitKmh - 5f)..speedLimitKmh
    val isTrackingActive = if (isSimulatorMode) true else (speedState is SpeedData.SpeedChanged)

    // Rider Shift & Fatigue Tracking
    var shiftSeconds by remember { mutableLongStateOf(4820L) } // ~1h 20m default
    var overspeedIncidents by remember { mutableIntStateOf(1) }
    var complianceScore by remember { mutableIntStateOf(98) }
    var lastVoiceTranscript by remember { mutableStateOf("Ready for voice commands") }
    var isOverlayActive by remember { mutableStateOf(false) }

    // Shift timer loop
    LaunchedEffect(isTrackingActive) {
        while (isTrackingActive) {
            delay(1000)
            shiftSeconds++
        }
    }

    val speedStatusColor by animateColorAsState(
        targetValue = when {
            isOverLimit -> CoPilotColors.HazardCrimson
            isApproaching -> CoPilotColors.CautionAmber
            else -> CoPilotColors.SafeMint
        },
        animationSpec = tween(250),
        label = "statusColor"
    )

    val speedStatusLabel = when {
        isOverLimit -> "LIMIT EXCEEDED"
        isApproaching -> "APPROACHING LIMIT"
        else -> "SAFE SPEED"
    }

    Scaffold(
        containerColor = CoPilotColors.CarbonBackground,
        floatingActionButton = {
            // Hands-Free Voice Command Button (:speed-ui)
            VoiceCommandButton(
                parser = remember { DeliveryVoiceCommandParser() },
                onActionParsed = { action ->
                    when (action) {
                        is VoiceAction.SetMaxLimit -> {
                            speedLimitKmh = action.limit
                            onLimitChanged(action.limit)
                            lastVoiceTranscript = "Speed limit updated: ${action.limit.roundToInt()} km/h"
                        }
                        is VoiceAction.SetMinLimit -> {
                            lastVoiceTranscript = "Min speed limit updated: ${action.limit.roundToInt()} km/h"
                        }
                        is VoiceAction.ChangeUnit -> {
                            lastVoiceTranscript = "Unit preference updated: ${action.unitName}"
                        }
                        is VoiceAction.Unknown -> {
                            lastVoiceTranscript = "Command not recognized. Say 'Set limit to 40'"
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Top Status Bar (App Title & Active Telematics Source Status)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Delivery Co-Pilot",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoPilotColors.TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (isSimulatorMode) "CAN-Bus Bench Simulator (:speed-core)" else "Hardware GPS Telematics (:speed-core)",
                        fontSize = 11.sp,
                        color = if (isSimulatorMode) CoPilotColors.CautionAmber else CoPilotColors.AviationSky,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Active Source Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSimulatorMode) CoPilotColors.CautionAmber.copy(alpha = 0.15f)
                            else CoPilotColors.SafeMint.copy(alpha = 0.15f)
                        )
                        .border(
                            1.dp,
                            if (isSimulatorMode) CoPilotColors.CautionAmber.copy(alpha = 0.4f)
                            else CoPilotColors.SafeMint.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (isSimulatorMode) "SIMULATOR" else "LIVE GPS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSimulatorMode) CoPilotColors.CautionAmber else CoPilotColors.SafeMint,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Delivery Order Context Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CoPilotColors.SlateSurface)
                    .border(1.dp, CoPilotColors.SurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CoPilotColors.AviationSky.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🛵",
                            fontSize = 18.sp
                        )
                    }
                    Column {
                        Text(
                            text = "Order #408: Green Valley Grocers",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CoPilotColors.TextPrimary
                        )
                        Text(
                            text = "Drop in 2.1 km • Silicon Avenue Sector 4",
                            fontSize = 11.sp,
                            color = CoPilotColors.TextMuted
                        )
                    }
                }

                Text(
                    text = "ETA 7m",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoPilotColors.AviationSky
                )
            }

            // Primary Rider Glance Cockpit (High-Contrast, Sunlight-Legible Bezel)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF070A10))
                    .border(2.dp, speedStatusColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Status Badge Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(speedStatusColor.copy(alpha = 0.2f))
                            .border(1.dp, speedStatusColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = speedStatusLabel,
                            color = speedStatusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    // Large High-Glance Speed Number (Directly from GPS or Simulator)
                    Text(
                        text = effectiveSpeed.roundToInt().toString(),
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 94.sp,
                        color = speedStatusColor,
                        lineHeight = 94.sp
                    )

                    // Unit Label
                    Text(
                        text = "KM/H",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoPilotColors.TextMuted,
                        letterSpacing = 2.sp
                    )

                    // Live GPS Accuracy & Satellite Fix
                    Text(
                        text = if (isSimulatorMode) "SIMULATOR: ±${simulatedGpsAccuracy.roundToInt()}m" else gpsStatusLabel,
                        fontSize = 11.sp,
                        color = if (isSimulatorMode || speedState is SpeedData.SpeedChanged) CoPilotColors.SafeMint else CoPilotColors.CautionAmber,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Speed Regulation Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CoPilotColors.SlateSurface)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURRENT LIMIT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CoPilotColors.TextSubtle
                            )
                            Text(
                                text = "${speedLimitKmh.roundToInt()} km/h",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoPilotColors.TextPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ROAD HAZARD ZONE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CoPilotColors.TextSubtle
                            )
                            Text(
                                text = selectedHazardZone.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoPilotColors.AviationSky
                            )
                        }
                    }
                }
            }

            // Floating Navigation Overlay (:speed-overlay) Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CoPilotColors.SlateSurface)
                    .border(1.dp, CoPilotColors.SurfaceBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Floating PiP Overlay (:speed-overlay)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoPilotColors.TextPrimary
                        )
                        Text(
                            text = "Draws speed bubble over Google Maps & delivery apps",
                            fontSize = 11.sp,
                            color = CoPilotColors.TextMuted
                        )
                    }

                    // Live PiP Preview Thumbnail
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CoPilotColors.CarbonBackground)
                            .border(1.5.dp, speedStatusColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${effectiveSpeed.roundToInt()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = speedStatusColor
                        )
                    }
                }

                // Action Buttons for Overlay
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (!hasOverlayPermission()) {
                                onRequestOverlayPermission()
                            } else {
                                if (isOverlayActive) {
                                    onStopOverlay()
                                    isOverlayActive = false
                                } else {
                                    onLaunchOverlay()
                                    isOverlayActive = true
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOverlayActive) CoPilotColors.HazardCrimson else CoPilotColors.AviationSky
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = if (isOverlayActive) "STOP OVERLAY" else "LAUNCH OVERLAY",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { onRequestOverlayPermission() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(46.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CoPilotColors.TextPrimary
                        )
                    ) {
                        Text(
                            text = "PERMISSIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Hands-Free Voice Command Feed Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CoPilotColors.SlateSurface)
                    .border(1.dp, CoPilotColors.SurfaceBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hands-Free Voice Commands (:speed-ui)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoPilotColors.TextPrimary
                    )
                    Text(
                        text = "GLOVE-FRIENDLY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoPilotColors.SafeMint
                    )
                }

                // Spoken Transcript Output
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CoPilotColors.CarbonBackground)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "🎙️ \"$lastVoiceTranscript\"",
                        fontSize = 12.sp,
                        color = CoPilotColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "Try saying: \"Monsoon mode\", \"Set limit to 40\", or \"Flyover mode\"",
                    fontSize = 11.sp,
                    color = CoPilotColors.TextMuted
                )
            }

            // Road Hazard & Delivery Zone Selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CoPilotColors.SlateSurface)
                    .border(1.dp, CoPilotColors.SurfaceBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Delivery Road Profiles & Weather Safety",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoPilotColors.TextPrimary
                )

                // 2x2 Grid of Road Profiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeliveryHazardZone.values().take(2).forEach { zone ->
                        HazardZoneChip(
                            zone = zone,
                            isSelected = selectedHazardZone == zone,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedHazardZone = zone
                                speedLimitKmh = zone.speedLimitKmh
                                onProfileChanged(zone.profile)
                                onLimitChanged(zone.speedLimitKmh)
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeliveryHazardZone.values().drop(2).forEach { zone ->
                        HazardZoneChip(
                            zone = zone,
                            isSelected = selectedHazardZone == zone,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedHazardZone = zone
                                speedLimitKmh = zone.speedLimitKmh
                                onProfileChanged(zone.profile)
                                onLimitChanged(zone.speedLimitKmh)
                            }
                        )
                    }
                }
            }

            // Rider Shift & Fatigue Safety Monitor
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CoPilotColors.SlateSurface)
                    .border(1.dp, CoPilotColors.SurfaceBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Rider Shift & Fatigue Monitor",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoPilotColors.TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        title = "Shift Duration",
                        value = "${shiftSeconds / 3600}h ${(shiftSeconds % 3600) / 60}m",
                        valueColor = CoPilotColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Compliance",
                        value = "$complianceScore%",
                        valueColor = CoPilotColors.SafeMint,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Overspeed",
                        value = "$overspeedIncidents alerts",
                        valueColor = if (overspeedIncidents > 0) CoPilotColors.CautionAmber else CoPilotColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Fatigue Advisory Pill
                val fatigueText = when {
                    shiftSeconds < 5400L -> "Status: Fit to Ride • Hydration Good"
                    shiftSeconds < 7200L -> "Recommendation: 10-min rest stop after next drop"
                    else -> "Fatigue Warning: Continuous ride limit reached • Take a break"
                }
                val fatigueColor = when {
                    shiftSeconds < 5400L -> CoPilotColors.SafeMint
                    shiftSeconds < 7200L -> CoPilotColors.CautionAmber
                    else -> CoPilotColors.HazardCrimson
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(fatigueColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "🛡️", fontSize = 12.sp)
                    Text(
                        text = fatigueText,
                        fontSize = 11.sp,
                        color = fatigueColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Bench Simulator Card (For Desk Testing Without Motorbike)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CoPilotColors.SlateSurface)
                    .border(1.dp, CoPilotColors.SurfaceBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSimulatorMode = !isSimulatorMode },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Rider Telematics Simulator",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoPilotColors.TextPrimary
                        )
                        Text(
                            text = if (isSimulatorMode) "Simulator active (Overrides live GPS)" else "Disabled (Reading real GPS)",
                            fontSize = 10.sp,
                            color = if (isSimulatorMode) CoPilotColors.CautionAmber else CoPilotColors.SafeMint
                        )
                    }

                    Switch(
                        checked = isSimulatorMode,
                        onCheckedChange = { isSimulatorMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CoPilotColors.AviationSky,
                            checkedTrackColor = CoPilotColors.AviationSky.copy(alpha = 0.5f)
                        )
                    )
                }

                if (isSimulatorMode) {
                    // Quick Speed Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Idle (0)" to 0f,
                            "Pickup (20)" to 20f,
                            "Cruise (42)" to 42f,
                            "Rush (58)" to 58f
                        ).forEach { (label, speed) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (simulatedSpeedKmh.roundToInt() == speed.roundToInt())
                                            CoPilotColors.AviationSky
                                        else
                                            CoPilotColors.CarbonBackground
                                    )
                                    .clickable { simulatedSpeedKmh = speed }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (simulatedSpeedKmh.roundToInt() == speed.roundToInt())
                                        Color.White
                                    else
                                        CoPilotColors.TextMuted
                                )
                            }
                        }
                    }

                    // Throttle Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Motorcycle Throttle: ${simulatedSpeedKmh.roundToInt()} km/h",
                                fontSize = 11.sp,
                                color = CoPilotColors.TextMuted
                            )
                            if (isOverLimit) {
                                Text(
                                    text = "⚡ OVERSPEED ACTIVE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoPilotColors.HazardCrimson
                                )
                            }
                        }
                        Slider(
                            value = simulatedSpeedKmh,
                            onValueChange = { simulatedSpeedKmh = it },
                            valueRange = 0f..90f,
                            colors = SliderDefaults.colors(
                                thumbColor = CoPilotColors.AviationSky,
                                activeTrackColor = CoPilotColors.AviationSky,
                                inactiveTrackColor = CoPilotColors.SurfaceBorderActive
                            )
                        )
                    }

                    // GPS Accuracy Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GPS Lock Accuracy:",
                            fontSize = 11.sp,
                            color = CoPilotColors.TextMuted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "±3m" to 3f,
                                "±12m" to 12f,
                                "±30m" to 30f
                            ).forEach { (label, acc) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (simulatedGpsAccuracy == acc)
                                                CoPilotColors.SafeMint.copy(alpha = 0.2f)
                                            else
                                                CoPilotColors.CarbonBackground
                                        )
                                        .border(
                                            1.dp,
                                            if (simulatedGpsAccuracy == acc)
                                                CoPilotColors.SafeMint
                                            else
                                                CoPilotColors.SurfaceBorder,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { simulatedGpsAccuracy = acc }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        color = if (simulatedGpsAccuracy == acc)
                                            CoPilotColors.SafeMint
                                        else
                                            CoPilotColors.TextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp)) // Leave space for Floating Voice Button
        }
    }
}

@Composable
fun HazardZoneChip(
    zone: DeliveryHazardZone,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) CoPilotColors.AviationSky.copy(alpha = 0.15f)
                else CoPilotColors.CarbonBackground
            )
            .border(
                1.dp,
                if (isSelected) CoPilotColors.AviationSky else CoPilotColors.SurfaceBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = zone.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) CoPilotColors.AviationSky else CoPilotColors.TextPrimary
            )
            Text(
                text = "${zone.speedLimitKmh.roundToInt()} km/h limit",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) CoPilotColors.TextPrimary else CoPilotColors.TextMuted
            )
        }
    }
}

@Composable
fun MetricBox(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CoPilotColors.CarbonBackground)
            .padding(vertical = 8.dp, horizontal = 10.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 10.sp,
                color = CoPilotColors.TextSubtle,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 13.sp,
                color = valueColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
