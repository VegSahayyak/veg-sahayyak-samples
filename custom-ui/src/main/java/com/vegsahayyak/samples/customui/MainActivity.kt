package com.vegsahayyak.samples.customui

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vegsahayyak.speedcore.api.DrivingProfile
import com.vegsahayyak.speedcore.api.SpeedAssistant
import com.vegsahayyak.speedcore.data.location.SpeedData
import com.vegsahayyak.speedui.components.HudDisplay
import com.vegsahayyak.speedui.components.SpeedGauge
import com.vegsahayyak.speedui.components.SpeedUiColorScheme
import com.vegsahayyak.speedui.components.ThemeMode
import kotlin.math.roundToInt

/**
 * =========================================================================================
 * Veg Sahayyak SDK - Sample 4: Custom UI (White-Label Branding)
 * =========================================================================================
 *
 * This sample demonstrates how enterprise partners seamlessly integrate the :speed-ui module
 * into their applications, customizing visual themes to match their corporate identity.
 *
 * Key Architectural Highlights:
 * 1. WHITE-LABEL BRAND THEMING ([SpeedUiColorScheme]):
 *    Clients can completely rebrand the speedometer dial, needle, glowing arcs, and warning
 *    states without having to touch low-level canvas rendering or geometry calculations.
 *
 * 2. MULTI-COMPONENT TELEMATICS VIEWPORT:
 *    - [SpeedGauge]: Production-grade analog dial gauge with smooth animated needle physics,
 *      speed step ticks, dynamic arc coloring, and satellite accuracy indicators.
 *    - [HudDisplay]: Dedicated OLED pitch-black Head-Up Display engineered for nighttime
 *      windshield reflection with vertical screen mirroring ([isMirrored]).
 *
 * 3. HIGH-CONTRAST THEME MODES ([ThemeMode]):
 *    Demonstrates passing [ThemeMode.DARK_GRAY] or [ThemeMode.OLED_BLACK] to ensure that
 *    speed readouts and dial labels maintain high-contrast legibility against custom dark surfaces.
 *
 * 4. DUAL-MODE TELEMATICS (Live GPS vs Desk Simulator):
 *    Observes the public reactive [SpeedAssistant.observeFlow] stream for real-world driving,
 *    while offering an interactive simulation control panel for rapid office/desk testing.
 *
 * 5. RESPONSIVE ADAPTIVE LAYOUT:
 *    Automatically adapts between a side-by-side split dashboard on tablets / landscape mounts,
 *    and a stacked vertical flow on handheld portrait devices.
 * =========================================================================================
 */
class MainActivity : ComponentActivity() {

    /**
     * Commercial B2B license key authorized for package: "com.vegsahayyak.samples.customui"
     * with modules: ["speed-core", "speed-ui"]. Cryptographically verified by the native C++ vault.
     */
    private val enterpriseJwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGllbnRfaWQiOiJWZWcgU2FoYXl5YWsgU2FtcGxlIC0gQ3VzdG9tIFVJIiwicGFja2FnZV9uYW1lIjoiY29tLnZlZ3NhaGF5eWFrLnNhbXBsZXMuY3VzdG9tdWkiLCJleHAiOjIwODI3NTg0MDAsIm1vZHVsZXMiOlsic3BlZWQtY29yZSIsInNwZWVkLXVpIl0sIm1heF9kZXZpY2VzIjotMSwibWF4X29mZmxpbmVfZGF5cyI6MzY1LCJ1bmxpbWl0ZWQiOnRydWUsImlzcyI6InZlZ19zYWhheXlha19hZG1pbiIsImlhdCI6MTc5MTk5OTk2Nn0.sTK92AM4VhsQOryvbcACNzx72lDwMx15pGPRzmoHaLkaMGKz3nMTNy_6m5sOfew_4QnytZ0ilOgLO0qpEFSAi0vjTs7MoivZuFkDtlzecTCe0pVIdvRq_SpFQRf6_QVvRMQD7tbgTpI0xe0Ic6AEn_X2HxzyoTuLv9Tx2M-sb5mdhtYvJ4PsupVInrnYql6EN-FHI_EkhpdauXNkpI2r7ElqO7ejRuQxaH43aWGsB2GcV5rZxCtx4Nu3LWjWelWX7OGloaDrv-_dW_zG_LlLg4K-8odcDE3IPjhA_XvJ_ngCkpNJbZheQ4ngZjLH0nb4EwKp3l6_KRbUILwCrigNnw"

    /**
     * Activity Result Launcher for standard runtime location permissions.
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // If at least one location permission was granted, initialize the telematics runtime
        if (permissions.values.any { it }) {
            initializeTelematicsSdk()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Step 1: Ensure runtime permissions are granted before starting the SDK
        checkPermissionsAndStart()

        // Step 2: Render the customizable white-label Compose dashboard
        setContent {
            CustomBrandUiApp()
        }
    }

    /**
     * Checks for FINE and COARSE location permissions and prompts the user if missing.
     */
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

    /**
     * Step 3: Unlocks and starts the telematics engine using the enterprise JWT.
     */
    private fun initializeTelematicsSdk() {
        SpeedAssistant.with(this, enterpriseJwt)
            .setDrivingProfile(DrivingProfile.CarCity)
            .start()
    }
}

/**
 * Enumeration of available display components provided by the :speed-ui module.
 */
enum class DisplayComponent(val title: String) {
    ANALOG_GAUGE("Analog Speedometer"),
    NIGHT_HUD("Night Windshield HUD")
}

/**
 * Pre-configured corporate branding palettes showcasing how [SpeedUiColorScheme]
 * dynamically styles the gauge arc tracks, glowing needle, and warning thresholds.
 */
enum class BrandPalette(
    val brandName: String,
    val surfaceColor: Color,
    val cardColor: Color,
    val accentColor: Color,
    val scheme: SpeedUiColorScheme
) {
    CYBERPUNK_NEON(
        brandName = "Cyberpunk Neon",
        surfaceColor = Color(0xFF090D14),
        cardColor = Color(0xFF131A26),
        accentColor = Color(0xFF00FFCC),
        scheme = SpeedUiColorScheme(
            safeColor = Color(0xFF00FFCC),             // Neon Cyan (Safe cruising track)
            overSpeedColor = Color(0xFFFF0055),        // Electric Crimson (Overspeed redline)
            warningApproachColor = Color(0xFFFFB800),  // Vivid Amber (Approaching warning)
            underSpeedColor = Color(0xFF00B0FF),       // Sky Blue (Underspeed alert)
            warningGlowColor = Color(0xFF00FFCC),      // Halo glow around safe speed zone
            needleColor = Color(0xFFBD00FF),           // Vivid Purple animated needle
            inactiveTrackColor = Color(0xFF232A38),    // Unlit background gauge track
            labelDark = Color(0xFF0D1117),             // Text color for light themes
            labelLight = Color(0xFFF0F6FC)             // Text color for dark themes
        )
    ),
    RACING_TRACK(
        brandName = "Racing Track",
        surfaceColor = Color(0xFF101010),
        cardColor = Color(0xFF1A1A1A),
        accentColor = Color(0xFFFFA000),
        scheme = SpeedUiColorScheme(
            safeColor = Color(0xFFFFA000),             // Electric Amber (Safe track)
            overSpeedColor = Color(0xFFFF2030),        // Race Red (Overspeed redline)
            warningApproachColor = Color(0xFFFF5722),  // Deep Orange (Warning approach)
            underSpeedColor = Color(0xFFFFD54F),       // Amber underspeed
            warningGlowColor = Color(0xFFFFA000),      // Amber halo
            needleColor = Color(0xFFFF2030),           // Race Red needle
            inactiveTrackColor = Color(0xFF2E2E2E),    // Carbon gray track
            labelDark = Color(0xFF121212),
            labelLight = Color(0xFFEEEEEE)
        )
    ),
    LUXURY_GOLD(
        brandName = "Luxury Gold",
        surfaceColor = Color(0xFF080C14),
        cardColor = Color(0xFF101626),
        accentColor = Color(0xFFD4AF37),
        scheme = SpeedUiColorScheme(
            safeColor = Color(0xFFD4AF37),             // Champagne Gold (Safe cruising)
            overSpeedColor = Color(0xFFB71C1C),        // Deep Burgundy (Overspeed)
            warningApproachColor = Color(0xFFE67E22),  // Bronze Accent (Warning approach)
            underSpeedColor = Color(0xFFF39C12),       // Goldenrod
            warningGlowColor = Color(0xFFD4AF37),      // Gold halo
            needleColor = Color(0xFFF5E6CC),           // Champagne Pearl needle
            inactiveTrackColor = Color(0xFF1C2436),    // Deep slate track
            labelDark = Color(0xFF0A0F1D),
            labelLight = Color(0xFFEDE8DF)
        )
    )
}

/**
 * Main application composable containing the white-label custom UI experience.
 */
@Composable
fun CustomBrandUiApp() {
    // Component selection: Analog dial vs Night HUD
    var selectedDisplay by remember { mutableStateOf(DisplayComponent.ANALOG_GAUGE) }
    
    // Active brand color palette
    var selectedPalette by remember { mutableStateOf(BrandPalette.CYBERPUNK_NEON) }
    
    // Desk testing simulator state
    var isSimulationMode by remember { mutableStateOf(true) }
    var simulatedSpeedKmh by remember { mutableFloatStateOf(55f) }
    var simulatedGpsAccuracy by remember { mutableFloatStateOf(3.2f) }
    var isHudMirrored by remember { mutableStateOf(false) }

    // Regulatory speed threshold (e.g. 80 km/h)
    val warningSpeedLimit = 80f

    // Observe real-time telematics stream from SpeedAssistant Flow
    val speedState by SpeedAssistant.observeFlow()
        .collectAsState(initial = SpeedData.SpeedUnavailable)

    val liveSpeedKmh = when (val state = speedState) {
        is SpeedData.SpeedChanged -> state.speedMps * 3.6f
        else -> 0f
    }

    // Determine effective speed and GPS accuracy depending on simulator toggle
    val effectiveSpeed = if (isSimulationMode) simulatedSpeedKmh else liveSpeedKmh
    val isTrackingActive = if (isSimulationMode) true else (speedState is SpeedData.SpeedChanged)
    val isOverLimit = effectiveSpeed > warningSpeedLimit
    val gpsAccuracyMeters = when {
        isSimulationMode -> simulatedGpsAccuracy
        speedState is SpeedData.SpeedChanged -> (speedState as SpeedData.SpeedChanged).accuracyMeters
        speedState is SpeedData.LowAccuracy -> 30f
        else -> -1f
    }

    // Detect device orientation to provide adaptive split-screen layout on tablets
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = selectedPalette.surfaceColor
        ) {
            if (isLandscape) {
                // Two-column responsive tablet / landscape layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Visual Telematics Viewport (SpeedGauge or HudDisplay)
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(selectedPalette.cardColor)
                            .border(1.dp, selectedPalette.accentColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (selectedDisplay) {
                            DisplayComponent.ANALOG_GAUGE -> {
                                // Drop-in Analog Speedometer Gauge styled with active brand theme
                                SpeedGauge(
                                    speed = effectiveSpeed,
                                    warningSpeed = warningSpeedLimit,
                                    isTrackingActive = isTrackingActive,
                                    isOverLimit = isOverLimit,
                                    gpsAccuracyMeters = gpsAccuracyMeters,
                                    modifier = Modifier.fillMaxSize(),
                                    themeMode = ThemeMode.DARK_GRAY,
                                    colorScheme = selectedPalette.scheme
                                )
                            }
                            DisplayComponent.NIGHT_HUD -> {
                                // Drop-in Night Windshield HUD with vertical mirror flip support
                                HudDisplay(
                                    speed = effectiveSpeed,
                                    warningSpeed = warningSpeedLimit,
                                    isTrackingActive = isTrackingActive,
                                    isOverLimit = isOverLimit,
                                    isMirrored = isHudMirrored,
                                    gpsAccuracyMeters = gpsAccuracyMeters,
                                    onToggleMirror = { isHudMirrored = !isHudMirrored },
                                    modifier = Modifier.fillMaxSize(),
                                    colorScheme = selectedPalette.scheme
                                )
                            }
                        }
                    }

                    // Right Column: Controls, Brand Switcher & Desk Simulator
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HeaderSection(selectedPalette)
                        DisplayModeSelector(selectedDisplay, selectedPalette) { selectedDisplay = it }
                        BrandPaletteSelector(selectedPalette) { selectedPalette = it }
                        DeskSimulatorControls(
                            isSimulationMode = isSimulationMode,
                            onToggleSimulation = { isSimulationMode = it },
                            simulatedSpeedKmh = simulatedSpeedKmh,
                            onSpeedChange = { simulatedSpeedKmh = it },
                            simulatedGpsAccuracy = simulatedGpsAccuracy,
                            onGpsAccuracyChange = { simulatedGpsAccuracy = it },
                            effectiveSpeed = effectiveSpeed,
                            warningLimit = warningSpeedLimit,
                            palette = selectedPalette
                        )
                    }
                }
            } else {
                // Portrait single-column layout for handheld smartphones
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HeaderSection(selectedPalette)
                    DisplayModeSelector(selectedDisplay, selectedPalette) { selectedDisplay = it }

                    // Visual Viewport
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(selectedPalette.cardColor)
                            .border(1.dp, selectedPalette.accentColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (selectedDisplay) {
                            DisplayComponent.ANALOG_GAUGE -> {
                                SpeedGauge(
                                    speed = effectiveSpeed,
                                    warningSpeed = warningSpeedLimit,
                                    isTrackingActive = isTrackingActive,
                                    isOverLimit = isOverLimit,
                                    gpsAccuracyMeters = gpsAccuracyMeters,
                                    modifier = Modifier.fillMaxSize(),
                                    themeMode = ThemeMode.DARK_GRAY,
                                    colorScheme = selectedPalette.scheme
                                )
                            }
                            DisplayComponent.NIGHT_HUD -> {
                                HudDisplay(
                                    speed = effectiveSpeed,
                                    warningSpeed = warningSpeedLimit,
                                    isTrackingActive = isTrackingActive,
                                    isOverLimit = isOverLimit,
                                    isMirrored = isHudMirrored,
                                    gpsAccuracyMeters = gpsAccuracyMeters,
                                    onToggleMirror = { isHudMirrored = !isHudMirrored },
                                    modifier = Modifier.fillMaxSize(),
                                    colorScheme = selectedPalette.scheme
                                )
                            }
                        }
                    }

                    BrandPaletteSelector(selectedPalette) { selectedPalette = it }
                    DeskSimulatorControls(
                        isSimulationMode = isSimulationMode,
                        onToggleSimulation = { isSimulationMode = it },
                        simulatedSpeedKmh = simulatedSpeedKmh,
                        onSpeedChange = { simulatedSpeedKmh = it },
                        simulatedGpsAccuracy = simulatedGpsAccuracy,
                        onGpsAccuracyChange = { simulatedGpsAccuracy = it },
                        effectiveSpeed = effectiveSpeed,
                        warningLimit = warningSpeedLimit,
                        palette = selectedPalette
                    )
                }
            }
        }
    }
}

/**
 * Top header banner showing title, subtitle, and the active brand chip.
 */
@Composable
private fun HeaderSection(palette: BrandPalette) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Custom Brand UI",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "B2B White-Label Compose Components",
            fontSize = 12.sp,
            color = palette.accentColor
        )
    }
}

/**
 * Segmented component selector allowing the driver to switch between Analog and HUD modes.
 */
@Composable
private fun DisplayModeSelector(
    selectedDisplay: DisplayComponent,
    palette: BrandPalette,
    onSelect: (DisplayComponent) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.cardColor)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DisplayComponent.entries.forEach { component ->
            val isSelected = selectedDisplay == component
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) palette.accentColor else Color.Transparent)
                    .clickable { onSelect(component) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = component.title,
                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Brand palette selection chips with live preview color dots.
 */
@Composable
private fun BrandPaletteSelector(
    selectedPalette: BrandPalette,
    onSelect: (BrandPalette) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(selectedPalette.cardColor)
            .padding(12.dp)
    ) {
        Text(
            text = "BRAND PALETTES (SpeedUiColorScheme)",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BrandPalette.entries.forEach { palette ->
                val isSelected = selectedPalette == palette
                val borderColor = if (isSelected) palette.accentColor else Color.Transparent
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) palette.accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                        .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
                        .clickable { onSelect(palette) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Color swatch indicator dots (safe, approach, overspeed)
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(palette.scheme.safeColor))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(palette.scheme.warningApproachColor))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(palette.scheme.overSpeedColor))
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = palette.brandName,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) palette.accentColor else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Desk testing controls: quick speed preset buttons and slider to verify color transitions.
 */
@Composable
private fun DeskSimulatorControls(
    isSimulationMode: Boolean,
    onToggleSimulation: (Boolean) -> Unit,
    simulatedSpeedKmh: Float,
    onSpeedChange: (Float) -> Unit,
    simulatedGpsAccuracy: Float,
    onGpsAccuracyChange: (Float) -> Unit,
    effectiveSpeed: Float,
    warningLimit: Float,
    palette: BrandPalette
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.cardColor)
            .padding(12.dp)
    ) {
        // Toggle switch between live antenna and desk simulator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TELEMATICS SOURCE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (isSimulationMode) "Desk Simulator (Interactive)" else "Live GPS Antenna",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.accentColor
                )
            }

            Switch(
                checked = isSimulationMode,
                onCheckedChange = onToggleSimulation,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.accentColor,
                    checkedTrackColor = palette.accentColor.copy(alpha = 0.4f)
                )
            )
        }

        if (isSimulationMode) {
            Spacer(modifier = Modifier.height(10.dp))
            
            // Four quick speed buttons for instant boundary testing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val presets = listOf(
                    Triple("0 km/h", 0f, "Idle"),
                    Triple("55 km/h", 55f, "Safe"),
                    Triple("76 km/h", 76f, "Warn"),
                    Triple("98 km/h", 98f, "Alert")
                )
                presets.forEach { (label, speedVal, desc) ->
                    val isActive = (simulatedSpeedKmh.roundToInt() == speedVal.roundToInt())
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) palette.accentColor else Color.White.copy(alpha = 0.08f))
                            .clickable { onSpeedChange(speedVal) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) Color.Black else Color.White
                            )
                            Text(
                                text = desc,
                                fontSize = 9.sp,
                                color = if (isActive) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Speed Slider & Dynamic Status Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Adjust Speed: ${simulatedSpeedKmh.roundToInt()} km/h",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                val statusText = when {
                    effectiveSpeed > warningLimit -> "⚠️ OVERSPEED"
                    effectiveSpeed >= warningLimit * 0.85f -> "⚡ APPROACHING"
                    else -> "✓ SAFE SPEED"
                }
                val statusColor = when {
                    effectiveSpeed > warningLimit -> palette.scheme.overSpeedColor
                    effectiveSpeed >= warningLimit * 0.85f -> palette.scheme.warningApproachColor
                    else -> palette.scheme.safeColor
                }
                Text(
                    text = statusText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Slider(
                value = simulatedSpeedKmh,
                onValueChange = onSpeedChange,
                valueRange = 0f..140f,
                colors = SliderDefaults.colors(
                    thumbColor = palette.accentColor,
                    activeTrackColor = palette.accentColor,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // GPS Signal Simulation Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GPS ACCURACY STATUS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
                Text(
                    text = when {
                        simulatedGpsAccuracy < 0f -> "No Fix (--)"
                        simulatedGpsAccuracy <= 5f -> "±${simulatedGpsAccuracy.roundToInt()}m (Good)"
                        simulatedGpsAccuracy <= 20f -> "±${simulatedGpsAccuracy.roundToInt()}m (Fair)"
                        else -> "±${simulatedGpsAccuracy.roundToInt()}m (Weak)"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        simulatedGpsAccuracy < 0f -> Color.Gray
                        simulatedGpsAccuracy <= 5f -> Color(0xFF00E676)
                        simulatedGpsAccuracy <= 20f -> Color(0xFFFFB300)
                        else -> Color(0xFFFF1744)
                    }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val gpsPresets = listOf(
                    Pair("Good (±3m)", 3.2f),
                    Pair("Fair (±12m)", 12f),
                    Pair("Weak (±35m)", 35f),
                    Pair("No Fix", -1f)
                )
                gpsPresets.forEach { (label, accVal) ->
                    val isAccActive = (simulatedGpsAccuracy == accVal)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isAccActive) palette.accentColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                            .border(1.dp, if (isAccActive) palette.accentColor else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable { onGpsAccuracyChange(accVal) }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            fontWeight = if (isAccActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAccActive) palette.accentColor else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
