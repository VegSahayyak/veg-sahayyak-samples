package com.vegsahayyak.samples.ubi

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * =========================================================================================
 * Veg Sahayyak SDK - Sample 5: Usage-Based Insurance (UBI) & Telematics Risk Profiling
 * =========================================================================================
 *
 * This sample demonstrates how insurance carriers and insurtech partners integrate the
 * Veg Sahayyak SDK (:speed-core) to build Pay-How-You-Drive (PHYD) telematics applications:
 *
 * 1. REAL-TIME ACTUARIAL RISK SCORING:
 *    Calculates an on-device driving safety score (0–100) based on continuous speed compliance.
 *    Safe driving maintains preferred Tier 1 discount rates, while violations dynamically
 *    degrade the policy score and trigger surcharge tiers.
 *
 * 2. REGULATORY & STATUTORY ZONE MANAGEMENT:
 *    Demonstrates dynamic adjustment of driving profiles ([DrivingProfile]) across different
 *    territorial zones (CarCity 50 km/h, SchoolZone 30 km/h, CarHighway 120 km/h).
 *
 * 3. ANTI-SPOOFING & FRAUD MITIGATION:
 *    Insurance telematics is a prime target for GPS spoofing. The SDK's Native C++ Vault
 *    validates atomic GPS time high-water marks and detects Dalvik/ART hooking out of the box,
 *    preventing fraudulent claims.
 *
 * 4. PURE CORE TELEMATICS INTEGRATION:
 *    Demonstrates embedding telematics intelligence with standard Jetpack Compose Material 3
 *    components, decoupled from custom UI libraries.
 *
 * 5. DESK TESTING & SIMULATION:
 *    Enables underwriters, actuaries, and QA engineers to test scoring degradation and tier
 *    transitions directly from their desk without road testing.
 * =========================================================================================
 */
class MainActivity : ComponentActivity() {

    /**
     * Commercial B2B insurance license key authorized for package:
     * "com.vegsahayyak.samples.ubi" with modules: ["speed-core"].
     * Validated cryptographically at startup by the native C++ vault.
     */
    private val insuranceEnterpriseJwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGllbnRfaWQiOiJWZWcgU2FoYXl5YWsgU2FtcGxlIC0gVUJJIEluc3VyYW5jZSIsInBhY2thZ2VfbmFtZSI6ImNvbS52ZWdzYWhheXlhay5zYW1wbGVzLnViaSIsImV4cCI6MjA4Mjc1ODQwMCwibW9kdWxlcyI6WyJzcGVlZC1jb3JlIl0sIm1heF9kZXZpY2VzIjotMSwibWF4X29mZmxpbmVfZGF5cyI6MzY1LCJ1bmxpbWl0ZWQiOnRydWUsImlzcyI6InZlZ19zYWhheXlha19hZG1pbiIsImlhdCI6MTc5MTk5OTk2Nn0.NzhHmq2xPwAKmFyumgaqd_zfQmNsy1_ViIfR2uF5NRpV8MgFoT4Upth1ayO5G0kGuq_id5V0m68pXqMKdZFRYr62DfPwuial9-qOgoHDEU8wPKFACqMoMsLseG7WWscAeGYSC2Uuq20tDLYj8RZDA5RHT3PM_iUWZTlmJD6yGxL2NoobJM085nJUYvqs5TH1-88-vd_Ii6aifBURHYQeaiX7J9J0qin_Jp8jQCqCUudpWU-QIH7-AtexkgOVoqAerFNUNZsEKe5MTJfNLhHFpTkrVhc6G2DJ_fwHnR-ITdlAvjEGKxwZeZG_czWr6pRQ9oZYtUJwP0MZxc5ue4-n4w"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            initializeUBI()
        } else {
            Toast.makeText(this, "Location permission required for insurance telematics", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkPermissionsAndStart()

        setContent {
            UbiInsuranceDashboard(enterpriseJwt = insuranceEnterpriseJwt)
        }
    }

    private fun checkPermissionsAndStart() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED) {
            initializeUBI()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun initializeUBI() {
        SpeedAssistant.with(this, insuranceEnterpriseJwt)
            .setDrivingProfile(DrivingProfile.CarCity)
            .start()
    }
}

/**
 * Actuarial Insurance Policy Premium Discount Tiers based on Safe Driving Score.
 */
enum class InsuranceDiscountTier(
    val tierTitle: String,
    val discountPercent: Int,
    val policyImpactText: String,
    val badgeColor: Color
) {
    TIER_1_PREFERRED("Tier 1: Preferred", 25, "25% Premium Discount", Color(0xFF00E676)),
    TIER_2_STANDARD_PLUS("Tier 2: Standard+", 10, "10% Premium Discount", Color(0xFF00B0FF)),
    TIER_3_BASE("Tier 3: Base Rate", 0, "Standard Base Premium", Color(0xFFFFB300)),
    TIER_4_HIGH_RISK("Tier 4: High Risk", -15, "+15% Risk Surcharge", Color(0xFFFF1744))
}

/**
 * Driving Profile Zones corresponding to statutory territories in insurance policies.
 */
enum class InsuranceZone(
    val title: String,
    val speedLimitKmh: Float,
    val profile: DrivingProfile,
    val description: String
) {
    CITY("Urban City", 50f, DrivingProfile.CarCity, "50 km/h Statutory Urban Limit"),
    SCHOOL("School Zone", 30f, DrivingProfile.SchoolZone, "30 km/h Sensitive Safety Zone"),
    HIGHWAY("Expressway", 120f, DrivingProfile.CarHighway, "120 km/h Highway Limit")
}

@Composable
fun UbiInsuranceDashboard(enterpriseJwt: String) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Territory / Driving Zone selection
    var selectedZone by remember { mutableStateOf(InsuranceZone.CITY) }

    // Telematics Source Toggle (Live GPS vs Desk Simulator)
    var isSimulationMode by remember { mutableStateOf(true) }
    var simulatedSpeedKmh by remember { mutableFloatStateOf(42f) }

    // Actuarial Safety Score (0–100 scale, starts at 100)
    var safetyScore by remember { mutableFloatStateOf(100f) }
    var overspeedSeconds by remember { mutableIntStateOf(0) }
    var totalDrivingSeconds by remember { mutableIntStateOf(0) }
    var violationIncidents by remember { mutableIntStateOf(0) }
    var wasSpeedingPreviousSecond by remember { mutableStateOf(false) }

    // Live GPS telemetry from SpeedAssistant Flow
    val speedState by SpeedAssistant.observeFlow()
        .collectAsState(initial = SpeedData.SpeedUnavailable)

    val liveSpeedKmh = when (val state = speedState) {
        is SpeedData.SpeedChanged -> state.speedMps * 3.6f
        else -> 0f
    }

    val effectiveSpeed = if (isSimulationMode) simulatedSpeedKmh else liveSpeedKmh
    val activeSpeedLimit = selectedZone.speedLimitKmh
    val isOverLimit = effectiveSpeed > activeSpeedLimit
    val isTrackingActive = if (isSimulationMode) true else (speedState is SpeedData.SpeedChanged)

    // Actuarial Risk Scoring Loop: Evaluates driving behavior every second
    LaunchedEffect(isTrackingActive, isOverLimit, effectiveSpeed) {
        if (isTrackingActive && effectiveSpeed > 2f) {
            totalDrivingSeconds += 1
            if (isOverLimit) {
                overspeedSeconds += 1
                if (!wasSpeedingPreviousSecond) {
                    violationIncidents += 1
                    wasSpeedingPreviousSecond = true
                }
                // Penalty deduction: 1.5 points per second in overspeed
                safetyScore = (safetyScore - 1.5f).coerceAtLeast(0f)
            } else {
                wasSpeedingPreviousSecond = false
                // Gradual safe-driving recovery (+0.1 pt per second)
                safetyScore = (safetyScore + 0.1f).coerceAtMost(100f)
            }
        }
        delay(1000)
    }

    // Determine Insurance Discount Tier
    val currentTier = when {
        safetyScore >= 90f -> InsuranceDiscountTier.TIER_1_PREFERRED
        safetyScore >= 75f -> InsuranceDiscountTier.TIER_2_STANDARD_PLUS
        safetyScore >= 60f -> InsuranceDiscountTier.TIER_3_BASE
        else -> InsuranceDiscountTier.TIER_4_HIGH_RISK
    }

    // Calculate Speed Compliance Rate
    val complianceRate = if (totalDrivingSeconds > 0) {
        (((totalDrivingSeconds - overspeedSeconds).toFloat() / totalDrivingSeconds) * 100f).coerceIn(0f, 100f)
    } else 100f

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF090D14)
        ) {
            if (isLandscape) {
                // Responsive Two-Column Tablet Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Policyholder Scorecard & Speed Status
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ScorecardHeroCard(
                            safetyScore = safetyScore,
                            tier = currentTier,
                            effectiveSpeed = effectiveSpeed,
                            speedLimit = activeSpeedLimit,
                            isOverLimit = isOverLimit,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Right Column: Actuarial Telematics, Zone Selection & Simulator Controls
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HeaderSection()
                        ZoneProfileSelector(
                            selectedZone = selectedZone,
                            onSelectZone = { newZone ->
                                selectedZone = newZone
                                SpeedAssistant.with(context, enterpriseJwt)
                                    .setDrivingProfile(newZone.profile)
                            }
                        )
                        ActuarialMetricsCard(
                            complianceRate = complianceRate,
                            totalDrivingSeconds = totalDrivingSeconds,
                            overspeedSeconds = overspeedSeconds,
                            violationCount = violationIncidents
                        )
                        SimulatorControlCard(
                            isSimulationMode = isSimulationMode,
                            onToggleSimulation = { isSimulationMode = it },
                            simulatedSpeedKmh = simulatedSpeedKmh,
                            onSpeedChange = { simulatedSpeedKmh = it },
                            speedLimit = activeSpeedLimit,
                            onResetScore = {
                                safetyScore = 100f
                                overspeedSeconds = 0
                                totalDrivingSeconds = 0
                                violationIncidents = 0
                            }
                        )
                    }
                }
            } else {
                // Single-Column Portrait Layout for Handheld Phones
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HeaderSection()
                    ScorecardHeroCard(
                        safetyScore = safetyScore,
                        tier = currentTier,
                        effectiveSpeed = effectiveSpeed,
                        speedLimit = activeSpeedLimit,
                        isOverLimit = isOverLimit,
                        modifier = Modifier.fillMaxWidth().height(320.dp)
                    )
                    ZoneProfileSelector(
                        selectedZone = selectedZone,
                        onSelectZone = { newZone ->
                            selectedZone = newZone
                            SpeedAssistant.with(context, enterpriseJwt)
                                .setDrivingProfile(newZone.profile)
                        }
                    )
                    ActuarialMetricsCard(
                        complianceRate = complianceRate,
                        totalDrivingSeconds = totalDrivingSeconds,
                        overspeedSeconds = overspeedSeconds,
                        violationCount = violationIncidents
                    )
                    SimulatorControlCard(
                        isSimulationMode = isSimulationMode,
                        onToggleSimulation = { isSimulationMode = it },
                        simulatedSpeedKmh = simulatedSpeedKmh,
                        onSpeedChange = { simulatedSpeedKmh = it },
                        speedLimit = activeSpeedLimit,
                        onResetScore = {
                            safetyScore = 100f
                            overspeedSeconds = 0
                            totalDrivingSeconds = 0
                            violationIncidents = 0
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "UBI Insurance",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Pay-How-You-Drive Risk Profiling (:speed-core)",
            fontSize = 12.sp,
            color = Color(0xFF00E676)
        )
    }
}

/**
 * Primary Hero Card showcasing the driving score, discount tier, and real-time speed.
 */
@Composable
private fun ScorecardHeroCard(
    safetyScore: Float,
    tier: InsuranceDiscountTier,
    effectiveSpeed: Float,
    speedLimit: Float,
    isOverLimit: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedBgColor by animateColorAsState(
        targetValue = if (isOverLimit) Color(0xFF261014) else Color(0xFF131A26),
        label = "heroBgColor"
    )
    val borderColor = if (isOverLimit) Color(0xFFFF1744) else tier.badgeColor

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(animatedBgColor)
            .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Tier Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(tier.badgeColor.copy(alpha = 0.15f))
                    .border(1.dp, tier.badgeColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = tier.policyImpactText.uppercase(),
                    color = tier.badgeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Massive Actuarial Score Readout
            Text(
                text = "${safetyScore.roundToInt()}",
                fontSize = 84.sp,
                fontWeight = FontWeight.ExtraBold,
                color = tier.badgeColor
            )

            Text(
                text = "DRIVING SAFETY SCORE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Real-Time Speed vs Zone Limit Status Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CURRENT SPEED",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${effectiveSpeed.roundToInt()} km/h",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverLimit) Color(0xFFFF1744) else Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isOverLimit) Color(0xFFFF1744).copy(alpha = 0.2f) else Color(0xFF00E676).copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isOverLimit) "🚨 SPEEDING DETECTED" else "✓ WITHIN POLICY LIMIT",
                        color = if (isOverLimit) Color(0xFFFF1744) else Color(0xFF00E676),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ZONE LIMIT",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${speedLimit.roundToInt()} km/h",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

/**
 * Territory / Statutory Zone profile selection chips.
 */
@Composable
private fun ZoneProfileSelector(
    selectedZone: InsuranceZone,
    onSelectZone: (InsuranceZone) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF131A26))
            .padding(12.dp)
    ) {
        Text(
            text = "POLICY TERRITORY (DrivingProfile)",
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
            InsuranceZone.entries.forEach { zone ->
                val isSelected = selectedZone == zone
                val borderColor = if (isSelected) Color(0xFF00E676) else Color.Transparent
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF00E676).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                        .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
                        .clickable { onSelectZone(zone) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = zone.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${zone.speedLimitKmh.roundToInt()} km/h",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Granular Actuarial Metrics Card showing breakdown of driving compliance.
 */
@Composable
private fun ActuarialMetricsCard(
    complianceRate: Float,
    totalDrivingSeconds: Int,
    overspeedSeconds: Int,
    violationCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF131A26))
            .padding(12.dp)
    ) {
        Text(
            text = "ACTUARIAL TELEMATICS BREAKDOWN",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricPill("Speed Compliance", "${complianceRate.roundToInt()}%", Color(0xFF00E676))
            MetricPill("Total Time", "${totalDrivingSeconds}s", Color.White)
            MetricPill("Overspeed Time", "${overspeedSeconds}s", if (overspeedSeconds > 0) Color(0xFFFF1744) else Color(0xFF00E676))
            MetricPill("Violations", "$violationCount", if (violationCount > 0) Color(0xFFFFB300) else Color(0xFF00E676))
        }
    }
}

@Composable
private fun MetricPill(title: String, value: String, valueColor: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

/**
 * Simulator controls allowing underwriters and testers to simulate vehicle speeds on a desk.
 */
@Composable
private fun SimulatorControlCard(
    isSimulationMode: Boolean,
    onToggleSimulation: (Boolean) -> Unit,
    simulatedSpeedKmh: Float,
    onSpeedChange: (Float) -> Unit,
    speedLimit: Float,
    onResetScore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF131A26))
            .padding(12.dp)
    ) {
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
                    color = Color(0xFF00E676)
                )
            }

            Switch(
                checked = isSimulationMode,
                onCheckedChange = onToggleSimulation,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00E676),
                    checkedTrackColor = Color(0xFF00E676).copy(alpha = 0.4f)
                )
            )
        }

        if (isSimulationMode) {
            Spacer(modifier = Modifier.height(10.dp))

            // Four quick speed buttons for instant testing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val presets = listOf(
                    Triple("0 km/h", 0f, "Idle"),
                    Triple("42 km/h", 42f, "Safe"),
                    Triple("58 km/h", 58f, "Minor"),
                    Triple("78 km/h", 78f, "Severe")
                )
                presets.forEach { (label, speedVal, desc) ->
                    val isActive = (simulatedSpeedKmh.roundToInt() == speedVal.roundToInt())
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) Color(0xFF00E676) else Color.White.copy(alpha = 0.08f))
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

            // Smooth Slider
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
                Text(
                    text = "Reset Score",
                    fontSize = 11.sp,
                    color = Color(0xFF00E676),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onResetScore() }
                )
            }

            Slider(
                value = simulatedSpeedKmh,
                onValueChange = onSpeedChange,
                valueRange = 0f..140f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00E676),
                    activeTrackColor = Color(0xFF00E676),
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                )
            )
        }
    }
}
