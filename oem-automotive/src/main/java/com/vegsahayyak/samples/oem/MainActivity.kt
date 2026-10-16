package com.vegsahayyak.samples.oem

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vegsahayyak.speedcore.api.DrivingProfile
import com.vegsahayyak.speedcore.api.SpeedAssistant
import com.vegsahayyak.speedcore.data.location.SpeedData
import com.vegsahayyak.speedoverlay.PipWidget
import com.vegsahayyak.speedoverlay.PipWidgetConfig
import com.vegsahayyak.speedui.components.HudDisplay
import com.vegsahayyak.speedui.components.SpeedGauge
import com.vegsahayyak.speedui.components.SpeedUiColorScheme
import com.vegsahayyak.speedui.components.ThemeMode
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Veg Sahayyak SDK - Sample 6: OEM Automotive Digital Cockpit
 *
 * This sample showcases how Tier-1 automotive suppliers, OEMs, and connected vehicle
 * platform developers integrate Veg Sahayyak telematics into Android Automotive OS (AAOS)
 * head units or in-dash digital cockpit tablets.
 *
 * Key Capabilities Demonstrated:
 * 1. Landscape-Optimized Digital Cockpit (Split-Screen In-Dash Console)
 * 2. Multi-Display Component Comparison:
 *    - Comparative Dual Cluster: Digital Dial ([SpeedGauge]) and Night Windshield HUD ([HudDisplay]) side-by-side
 *    - Embedded Navigation Speed Overlay ([PipWidget] from :speed-overlay)
 * 3. Dynamic ADAS Territory Profiles ([DrivingProfile]):
 *    - Expressway (100 km/h)
 *    - Arterial Highway (80 km/h)
 *    - Urban Corridor (50 km/h)
 *    - Safety Zone (30 km/h)
 * 4. Bench Testing & CAN-Bus Telematics Simulator:
 *    - Allows software teams to simulate vehicle speeds, gear presets, and GPS locks on a desk.
 * 5. OEM Curated Color Schemes ([SpeedUiColorScheme]):
 *    - Titanium Tech (Sky Slate), Nordic Slate (Minimalist Ice), GT Sport (Warm Amber).
 */
class MainActivity : ComponentActivity() {

    // Commercial B2B OEM enterprise license key authorizing :speed-core, :speed-ui, :speed-overlay
    private val enterpriseJwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGllbnRfaWQiOiJWZWcgU2FoYXl5YWsgU2FtcGxlIC0gT0VNIEF1dG9tb3RpdmUiLCJwYWNrYWdlX25hbWUiOiJjb20udmVnc2FoYXl5YWsuc2FtcGxlcy5vZW0iLCJleHAiOjIwODI3NTg0MDAsIm1vZHVsZXMiOlsic3BlZWQtY29yZSIsInNwZWVkLXVpIiwic3BlZWQtb3ZlcmxheSJdLCJtYXhfZGV2aWNlcyI6LTEsIm1heF9vZmZsaW5lX2RheXMiOjM2NSwidW5saW1pdGVkIjp0cnVlLCJpc3MiOiJ2ZWdfc2FoYXl5YWtfYWRtaW4iLCJpYXQiOjE3OTE5OTk5NjZ9.ffJE0m4Ej0kH28Z_eT3UqK3163u_JB5BjunMwqBA1KZ75nOgRAivDc5qV_BuZa7ymyaKGUo8NpbDr_3RHr-H9b_dF5i_vC7jMUPYZTLuyzShNV4np23LgeA8xjBJ3xmTmz-LW4aTPz08cXCtKENWAtiiP4yIrZApS53G4b9BwBkNP5WlJtedhSRuivj-2B4lk5nfx_yw-FSQSAg612Yw4v8CFU054PS0Mokh2VGv_tVA-hexzpUxW0x4qcoyx5sqHgWXrq5FYa2JFLTwIHuD9zo3HxBd_R_o1e-siBnkqb_i2mtYDNjlB806rrYq4r9aAbJp763zSrDFmVz2Gc4wOg"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            initializeTelematicsSdk()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissionsAndStart()

        setContent {
            OemCockpitApp(
                onProfileChanged = { profile ->
                    SpeedAssistant.with(this, enterpriseJwt)
                        .setDrivingProfile(profile)
                        .start()
                }
            )
        }
    }

    private fun checkPermissionsAndStart() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED) {
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
        SpeedAssistant.with(this, enterpriseJwt)
            .setDrivingProfile(DrivingProfile.CarHighway)
            .start()
    }
}

/**
 * Display modes available for the Driver Instrument Cluster.
 */
enum class InstrumentMode(val title: String) {
    COMPARATIVE_DUAL("⚡ Dual Comparison"),
    COCKPIT_GAUGE("🏎️ Digital Dial"),
    WINDSHIELD_HUD("🌌 Windshield HUD"),
    PIP_OVERLAY("📍 PiP Showcase")
}

/**
 * ADAS Road Profile Territories configuring dynamic speed limits.
 */
enum class AdasProfile(
    val title: String,
    val speedLimitKmh: Float,
    val drivingProfile: DrivingProfile
) {
    AUTOBAHN("Expressway", 100f, DrivingProfile.CarHighway),
    ARTERIAL("Arterial Road", 80f, DrivingProfile.TruckHighway),
    CITY("Urban Corridor", 50f, DrivingProfile.CarCity),
    SCHOOL("Safety Zone", 30f, DrivingProfile.SchoolZone)
}

/**
 * Carefully curated automotive interior color themes.
 */
enum class CockpitTheme(
    val title: String,
    val accentColor: Color,
    val safeColor: Color,
    val warningColor: Color,
    val alertColor: Color,
    val cardBg: Color,
    val borderColor: Color
) {
    TITANIUM(
        title = "Titanium Tech",
        accentColor = Color(0xFF38BDF8),
        safeColor = Color(0xFF10B981),
        warningColor = Color(0xFFF59E0B),
        alertColor = Color(0xFFEF4444),
        cardBg = Color(0xFF111726),
        borderColor = Color(0xFF1E283D)
    ),
    NORDIC(
        title = "Nordic Slate",
        accentColor = Color(0xFF93C5FD),
        safeColor = Color(0xFF34D399),
        warningColor = Color(0xFFFBBF24),
        alertColor = Color(0xFFF87171),
        cardBg = Color(0xFF0F172A),
        borderColor = Color(0xFF1E293B)
    ),
    GT_AMBER(
        title = "GT Sport",
        accentColor = Color(0xFFFBBF24),
        safeColor = Color(0xFF22C55E),
        warningColor = Color(0xFFF97316),
        alertColor = Color(0xFFDC2626),
        cardBg = Color(0xFF181512),
        borderColor = Color(0xFF2E261F)
    )
}

/**
 * Drive powertrain modes.
 */
enum class DriveMode(val title: String) {
    SPORT("SPORT"),
    COMFORT("COMFORT"),
    ECO("ECO")
}

/**
 * Root Composable rendering the in-dash automotive digital cockpit.
 */
@Composable
fun OemCockpitApp(onProfileChanged: (DrivingProfile) -> Unit) {
    // Active driver instrument mode (Defaults to DUAL COMPARISON for maximum comparative impact)
    var selectedInstrument by remember { mutableStateOf(InstrumentMode.COMPARATIVE_DUAL) }

    // Active ADAS Road Profile
    var activeAdasProfile by remember { mutableStateOf(AdasProfile.AUTOBAHN) }

    // Active Interior Color Theme
    var activeTheme by remember { mutableStateOf(CockpitTheme.TITANIUM) }

    // Drive Mode
    var activeDriveMode by remember { mutableStateOf(DriveMode.SPORT) }

    // Desk testing CAN-Bus simulator state
    var isSimulationMode by remember { mutableStateOf(true) }
    var simulatedSpeedKmh by remember { mutableFloatStateOf(85f) }
    var simulatedGpsAccuracy by remember { mutableFloatStateOf(3.2f) }
    var isHudMirrored by remember { mutableStateOf(false) }

    // Trip & telematics analytics state
    var tripDistanceKm by remember { mutableDoubleStateOf(14.8) }
    var totalDrivingSeconds by remember { mutableIntStateOf(640) }
    var overspeedSeconds by remember { mutableIntStateOf(12) }

    // Observe live speed updates from the Speed Assistant telematics engine
    val speedState by SpeedAssistant.observeFlow()
        .collectAsState(initial = SpeedData.SpeedUnavailable)

    val liveSpeedKmh = when (val state = speedState) {
        is SpeedData.SpeedChanged -> state.speedMps * 3.6f
        else -> 0f
    }

    val effectiveSpeed = if (isSimulationMode) simulatedSpeedKmh else liveSpeedKmh
    val warningSpeedLimit = activeAdasProfile.speedLimitKmh
    val isTrackingActive = if (isSimulationMode) true else (speedState is SpeedData.SpeedChanged)
    val isOverLimit = effectiveSpeed > warningSpeedLimit

    val gpsAccuracyMeters = when {
        isSimulationMode -> simulatedGpsAccuracy
        speedState is SpeedData.SpeedChanged -> (speedState as SpeedData.SpeedChanged).accuracyMeters
        speedState is SpeedData.LowAccuracy -> 30f
        else -> -1f
    }

    val currentEffectiveSpeed by rememberUpdatedState(effectiveSpeed)
    val currentIsOverLimit by rememberUpdatedState(isOverLimit)

    // Steady 1-second interval to accumulate realistic trip telemetry while vehicle is in motion
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (currentEffectiveSpeed > 1f) {
                totalDrivingSeconds += 1
                tripDistanceKm += (currentEffectiveSpeed / 3600.0)
                if (currentIsOverLimit) {
                    overspeedSeconds += 1
                }
            }
        }
    }

    val complianceRate = if (totalDrivingSeconds > 0) {
        ((totalDrivingSeconds - overspeedSeconds).toFloat() / totalDrivingSeconds.toFloat() * 100f).coerceIn(0f, 100f)
    } else 100f

    // Dynamic SpeedUiColorScheme mapped to active Cockpit Theme
    val currentColorScheme = remember(activeTheme) {
        SpeedUiColorScheme(
            safeColor = activeTheme.safeColor,
            overSpeedColor = activeTheme.alertColor,
            underSpeedColor = activeTheme.accentColor,
            warningApproachColor = activeTheme.warningColor,
            warningGlowColor = activeTheme.safeColor.copy(alpha = 0.3f),
            needleColor = activeTheme.alertColor,
            inactiveTrackColor = Color(0xFF1E293B),
            labelDark = Color(0xFF0F172A),
            labelLight = Color(0xFFF1F5F9)
        )
    }

    val cockpitBackground = Color(0xFF080C14)

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = cockpitBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Top In-Dash Cockpit Header
                CockpitHeader(
                    adasProfile = activeAdasProfile,
                    driveMode = activeDriveMode,
                    onDriveModeCycle = {
                        activeDriveMode = when (activeDriveMode) {
                            DriveMode.SPORT -> DriveMode.COMFORT
                            DriveMode.COMFORT -> DriveMode.ECO
                            DriveMode.ECO -> DriveMode.SPORT
                        }
                    },
                    isSimulationMode = isSimulationMode,
                    activeTheme = activeTheme,
                    onThemeSelected = { activeTheme = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Landscape Split-Screen Dashboard Layout
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Left Column (44% Width): In-Dash Infotainment, Navigation & ADAS Console
                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Card 1: Connected Navigation & Active Maneuver with Live PipWidget Embedded
                        NavigationManeuverCard(
                            speed = effectiveSpeed,
                            isOverLimit = isOverLimit,
                            theme = activeTheme
                        )

                        // Card 2: ADAS Road Profile Territory Selector
                        AdasProfileSelector(
                            currentProfile = activeAdasProfile,
                            onSelect = { profile ->
                                activeAdasProfile = profile
                                onProfileChanged(profile.drivingProfile)
                            },
                            theme = activeTheme
                        )

                        // Card 3: Connected In-Dash Media Player
                        ConnectedMediaWidget(
                            theme = activeTheme
                        )

                        // Card 4: Trip & Safety Telemetrics Summary
                        TripTelemetricsCard(
                            distanceKm = tripDistanceKm,
                            drivingSeconds = totalDrivingSeconds,
                            overspeedSeconds = overspeedSeconds,
                            complianceRate = complianceRate,
                            theme = activeTheme
                        )
                    }

                    // Right Column (56% Width): Comparative Driver Instrument Cluster & Bench Simulator
                    Column(
                        modifier = Modifier
                            .weight(1.22f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Display Mode Switcher (Dual Comparison vs Dial vs HUD vs PiP)
                        InstrumentSelector(
                            selected = selectedInstrument,
                            onSelect = { selectedInstrument = it },
                            theme = activeTheme
                        )

                        // Main Instrument Cluster Viewport (Height 360dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
                                .border(1.dp, activeTheme.borderColor, RoundedCornerShape(16.dp))
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (selectedInstrument) {
                                InstrumentMode.COMPARATIVE_DUAL -> {
                                    // Side-by-Side Synchronous Component Comparison
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Left Half: Digital Cockpit Dial
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF0A0F1A))
                                                .border(1.dp, activeTheme.borderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                                .padding(4.dp)
                                        ) {
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                Text(
                                                    text = "ANALOG DIAL (:speed-ui)",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = activeTheme.accentColor,
                                                    letterSpacing = 1.sp,
                                                    modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .graphicsLayer(scaleX = 0.90f, scaleY = 0.90f),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    SpeedGauge(
                                                        speed = effectiveSpeed,
                                                        warningSpeed = warningSpeedLimit,
                                                        isTrackingActive = isTrackingActive,
                                                        isOverLimit = isOverLimit,
                                                        gpsAccuracyMeters = gpsAccuracyMeters,
                                                        colorScheme = currentColorScheme,
                                                        themeMode = ThemeMode.DARK_GRAY,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                        }

                                        // Right Half: Night Windshield HUD with dedicated mirror toggle
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.Black)
                                                .border(1.dp, activeTheme.borderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                                .padding(4.dp)
                                        ) {
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "NIGHT HUD (:speed-ui)",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = activeTheme.accentColor,
                                                        letterSpacing = 1.sp
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color.White.copy(alpha = 0.14f))
                                                            .clickable { isHudMirrored = !isHudMirrored }
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isHudMirrored) "MIRROR: ON" else "MIRROR: OFF",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .graphicsLayer(scaleX = 0.72f, scaleY = 0.72f),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    HudDisplay(
                                                        speed = effectiveSpeed,
                                                        warningSpeed = warningSpeedLimit,
                                                        isTrackingActive = isTrackingActive,
                                                        isOverLimit = isOverLimit,
                                                        isMirrored = isHudMirrored,
                                                        onToggleMirror = null,
                                                        gpsAccuracyMeters = gpsAccuracyMeters,
                                                        colorScheme = currentColorScheme,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                InstrumentMode.COCKPIT_GAUGE -> {
                                    // Fluid analog instrument dial with animated needle & custom theme
                                    SpeedGauge(
                                        speed = effectiveSpeed,
                                        warningSpeed = warningSpeedLimit,
                                        isTrackingActive = isTrackingActive,
                                        isOverLimit = isOverLimit,
                                        gpsAccuracyMeters = gpsAccuracyMeters,
                                        colorScheme = currentColorScheme,
                                        themeMode = ThemeMode.DARK_GRAY,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                InstrumentMode.WINDSHIELD_HUD -> {
                                    // High-contrast windshield HUD with vertical reflection flip
                                    HudDisplay(
                                        speed = effectiveSpeed,
                                        warningSpeed = warningSpeedLimit,
                                        isTrackingActive = isTrackingActive,
                                        isOverLimit = isOverLimit,
                                        isMirrored = isHudMirrored,
                                        onToggleMirror = { isHudMirrored = !isHudMirrored },
                                        gpsAccuracyMeters = gpsAccuracyMeters,
                                        colorScheme = currentColorScheme,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                InstrumentMode.PIP_OVERLAY -> {
                                    // Picture-in-Picture speed widget with animated sonar wave
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "EMBEDDED SPEED OVERLAY (:speed-overlay)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.7f),
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        PipWidget(
                                            speed = effectiveSpeed,
                                            unitLabel = "km/h",
                                            isOverLimit = isOverLimit,
                                            config = PipWidgetConfig(
                                                size = 110.dp,
                                                safeColor = activeTheme.safeColor,
                                                warningColor = activeTheme.alertColor,
                                                speedTextSize = 38.sp,
                                                unitTextSize = 13.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = if (isOverLimit) "🚨 Regulatory Speed Limit Exceeded!" else "✓ Cruising safely within ADAS limit",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isOverLimit) activeTheme.alertColor else activeTheme.safeColor
                                        )
                                    }
                                }
                            }
                        }

                        // Vehicle CAN-Bus & Bench Testing Simulator Controls
                        CanBusSimulatorCard(
                            isSimulationMode = isSimulationMode,
                            onToggleSimulation = { isSimulationMode = it },
                            simulatedSpeedKmh = simulatedSpeedKmh,
                            onSpeedChange = { simulatedSpeedKmh = it },
                            simulatedGpsAccuracy = simulatedGpsAccuracy,
                            onGpsAccuracyChange = { simulatedGpsAccuracy = it },
                            warningLimit = warningSpeedLimit,
                            theme = activeTheme
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top Header Banner showing application title, theme selector, and vehicle ADAS transmission status.
 */
@Composable
private fun CockpitHeader(
    adasProfile: AdasProfile,
    driveMode: DriveMode,
    onDriveModeCycle: () -> Unit,
    isSimulationMode: Boolean,
    activeTheme: CockpitTheme,
    onThemeSelected: (CockpitTheme) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "OEM Automotive Cockpit",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Connected In-Dash Telematics (:speed-ui, :speed-core, :speed-overlay)",
                fontSize = 10.sp,
                color = activeTheme.accentColor
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Theme Selector Chips
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                CockpitTheme.entries.forEach { theme ->
                    val isSelected = activeTheme == theme
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) theme.accentColor else Color.Transparent)
                            .clickable { onThemeSelected(theme) }
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = theme.title.split(" ").first(),
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            // Drive Mode Pill (Clickable to cycle)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, activeTheme.accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable { onDriveModeCycle() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "DRIVE", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                    Text(text = driveMode.title, fontSize = 10.sp, color = activeTheme.accentColor, fontWeight = FontWeight.Bold)
                }
            }

            // CAN-Bus Status Pill
            StatusPill(
                label = "CAN-BUS",
                value = if (isSimulationMode) "SIMULATED" else "LIVE GPS",
                color = if (isSimulationMode) activeTheme.safeColor else activeTheme.accentColor
            )

            // Zone Limit Pill
            StatusPill(
                label = "ADAS LIMIT",
                value = "${adasProfile.speedLimitKmh.roundToInt()} km/h",
                color = Color.White
            )
        }
    }
}

@Composable
private fun StatusPill(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            Text(text = value, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Navigation & Maneuver Banner simulating in-dash route guidance with an embedded PipWidget.
 */
@Composable
private fun NavigationManeuverCard(
    speed: Float,
    isOverLimit: Boolean,
    theme: CockpitTheme
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.cardBg)
            .border(1.dp, theme.borderColor, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NAVIGATION & ROUTE GUIDANCE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
            Text(
                text = "ETA 18 min • 14.2 km",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.accentColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(theme.accentColor.copy(alpha = 0.15f))
                    .border(1.dp, theme.accentColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "↱", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = theme.accentColor)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "In 350m Turn Right",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Silicon Expressway North • Exit 24B",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            // Real Embedded PipWidget from :speed-overlay inside navigation card
            PipWidget(
                speed = speed,
                unitLabel = "km/h",
                isOverLimit = isOverLimit,
                config = PipWidgetConfig(
                    size = 56.dp,
                    safeColor = theme.safeColor,
                    warningColor = theme.alertColor,
                    speedTextSize = 20.sp,
                    unitTextSize = 8.sp
                )
            )
        }
    }
}

/**
 * ADAS Territory Selector allowing driver/system to adapt regulatory speed limits.
 */
@Composable
private fun AdasProfileSelector(
    currentProfile: AdasProfile,
    onSelect: (AdasProfile) -> Unit,
    theme: CockpitTheme
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.cardBg)
            .border(1.dp, theme.borderColor, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(
            text = "ADAS ROAD PROFILE (DrivingProfile)",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AdasProfile.entries.forEach { profile ->
                val isSelected = currentProfile == profile
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) theme.accentColor.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f))
                        .border(1.dp, if (isSelected) theme.accentColor else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { onSelect(profile) }
                        .padding(vertical = 7.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = profile.title,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) theme.accentColor else Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${profile.speedLimitKmh.roundToInt()} km/h",
                            fontSize = 8.sp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * In-Dash Connected Media Widget.
 */
@Composable
private fun ConnectedMediaWidget(
    theme: CockpitTheme
) {
    var isPlaying by remember { mutableStateOf(true) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.cardBg)
            .border(1.dp, theme.borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🎵", fontSize = 14.sp)
            }
            Column {
                Text(text = "Night Drive - Electronic Horizon", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "In-Dash Audio Stream • Bluetooth 5.2", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
            }
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(theme.accentColor.copy(alpha = 0.15f))
                .border(1.dp, theme.accentColor.copy(alpha = 0.4f), CircleShape)
                .clickable { isPlaying = !isPlaying }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isPlaying) "⏸ Pause" else "▶ Play",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = theme.accentColor
            )
        }
    }
}

/**
 * Trip Telemetry & Speed Compliance Summary.
 */
@Composable
private fun TripTelemetricsCard(
    distanceKm: Double,
    drivingSeconds: Int,
    overspeedSeconds: Int,
    complianceRate: Float,
    theme: CockpitTheme
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.cardBg)
            .border(1.dp, theme.borderColor, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(
            text = "TRIP TELEMETRICS & SPEED COMPLIANCE",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricBox("Trip Distance", String.format("%.1f km", distanceKm), Color.White)
            MetricBox("Drive Time", "${drivingSeconds / 60}m ${drivingSeconds % 60}s", Color.White)
            MetricBox("Overspeed", "${overspeedSeconds}s", if (overspeedSeconds > 0) theme.alertColor else theme.safeColor)
            MetricBox("Compliance", "${complianceRate.roundToInt()}%", if (complianceRate >= 90f) theme.safeColor else theme.alertColor)
        }
    }
}

@Composable
private fun MetricBox(title: String, value: String, valueColor: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

/**
 * Instrument Cluster Selector Tabs.
 */
@Composable
private fun InstrumentSelector(
    selected: InstrumentMode,
    onSelect: (InstrumentMode) -> Unit,
    theme: CockpitTheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(theme.cardBg)
            .border(1.dp, theme.borderColor, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        InstrumentMode.entries.forEach { mode ->
            val isSelected = selected == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) theme.accentColor else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.title,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

/**
 * Vehicle CAN-Bus & Bench Simulator Controls.
 */
@Composable
private fun CanBusSimulatorCard(
    isSimulationMode: Boolean,
    onToggleSimulation: (Boolean) -> Unit,
    simulatedSpeedKmh: Float,
    onSpeedChange: (Float) -> Unit,
    simulatedGpsAccuracy: Float,
    onGpsAccuracyChange: (Float) -> Unit,
    warningLimit: Float,
    theme: CockpitTheme
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.cardBg)
            .border(1.dp, theme.borderColor, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TELEMATICS SOURCE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (isSimulationMode) "CAN-Bus Bench Simulator" else "Live GPS Antenna",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.accentColor
                )
            }

            Switch(
                checked = isSimulationMode,
                onCheckedChange = onToggleSimulation,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = theme.accentColor,
                    checkedTrackColor = theme.accentColor.copy(alpha = 0.4f)
                )
            )
        }

        if (isSimulationMode) {
            Spacer(modifier = Modifier.height(6.dp))

            // Automotive Gear / Speed Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val presets = listOf(
                    Triple("P (0)", 0f, "Park"),
                    Triple("D (48)", 48f, "City"),
                    Triple("D (85)", 85f, "Cruise"),
                    Triple("D (118)", 118f, "Autobahn")
                )
                presets.forEach { (label, speedVal, desc) ->
                    val isActive = (simulatedSpeedKmh.roundToInt() == speedVal.roundToInt())
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) theme.accentColor else Color.White.copy(alpha = 0.07f))
                            .clickable { onSpeedChange(speedVal) }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) Color.Black else Color.White
                            )
                            Text(
                                text = desc,
                                fontSize = 8.sp,
                                color = if (isActive) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Throttle Slider & Status Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Vehicle Throttle: ${simulatedSpeedKmh.roundToInt()} km/h",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                val statusText = when {
                    simulatedSpeedKmh > warningLimit -> "🚨 SPEED LIMIT EXCEEDED"
                    simulatedSpeedKmh >= warningLimit * 0.85f -> "⚡ APPROACHING LIMIT"
                    else -> "✓ SAFE COMPLIANT"
                }
                val statusColor = when {
                    simulatedSpeedKmh > warningLimit -> theme.alertColor
                    simulatedSpeedKmh >= warningLimit * 0.85f -> theme.warningColor
                    else -> theme.safeColor
                }
                Text(
                    text = statusText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Slider(
                value = simulatedSpeedKmh,
                onValueChange = onSpeedChange,
                valueRange = 0f..160f,
                colors = SliderDefaults.colors(
                    thumbColor = theme.accentColor,
                    activeTrackColor = theme.accentColor,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // GPS Signal Quality Simulator Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GPS LOCK ACCURACY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
                Text(
                    text = when {
                        simulatedGpsAccuracy < 0f -> "No Lock (--)"
                        simulatedGpsAccuracy <= 5f -> "±${simulatedGpsAccuracy.roundToInt()}m (Good)"
                        simulatedGpsAccuracy <= 20f -> "±${simulatedGpsAccuracy.roundToInt()}m (Fair)"
                        else -> "±${simulatedGpsAccuracy.roundToInt()}m (Weak)"
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        simulatedGpsAccuracy < 0f -> Color.Gray
                        simulatedGpsAccuracy <= 5f -> theme.safeColor
                        simulatedGpsAccuracy <= 20f -> theme.warningColor
                        else -> theme.alertColor
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val gpsPresets = listOf(
                    Pair("Good (±3m)", 3.2f),
                    Pair("Fair (±12m)", 12f),
                    Pair("Weak (±35m)", 35f),
                    Pair("No Lock", -1f)
                )
                gpsPresets.forEach { (label, accVal) ->
                    val isAccActive = (simulatedGpsAccuracy == accVal)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isAccActive) theme.accentColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                            .border(1.dp, if (isAccActive) theme.accentColor else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable { onGpsAccuracyChange(accVal) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 8.sp,
                            fontWeight = if (isAccActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAccActive) theme.accentColor else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
