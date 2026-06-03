package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import kotlin.math.pow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Custom Theme Colors matching luxurious Slate/Carbon theme
val CharcoalDark = Color(0xFF12141C)
val SlateGrey = Color(0xFF1E2230)
val CarbonCard = Color(0xFF262C3E)
val MatrixGreen = Color(0xFF00FF87)
val RetailGold = Color(0xFFFFB300)
val FleetBlue = Color(0xFF00C6FF)
val ActivePink = Color(0xFFFF2E93)
val CrashRed = Color(0xFFFF4949)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val sector by viewModel.sector.collectAsStateWithLifecycle()
    val cash by viewModel.cash.collectAsStateWithLifecycle()
    val totalTaps by viewModel.totalTaps.collectAsStateWithLifecycle()
    val loanAmount by viewModel.loanAmount.collectAsStateWithLifecycle()
    val departments by viewModel.departments.collectAsStateWithLifecycle()
    val upgrades by viewModel.upgrades.collectAsStateWithLifecycle()
    val currentEvent by viewModel.currentEvent.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val offlineEarnings by viewModel.offlineEarnings.collectAsStateWithLifecycle()
    val offlineDuration by viewModel.offlineDurationSeconds.collectAsStateWithLifecycle()

    val activeAdUpgradeId by viewModel.activeAdUpgradeId.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalDark),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (sector.isNotEmpty() && !isLoading) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SlateGrey,
                        titleContentColor = Color.White
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (sector) {
                                    "TECH" -> Icons.Default.Computer
                                    "RETAIL" -> Icons.Default.ShoppingCart
                                    else -> Icons.Default.DirectionsCar
                                },
                                contentDescription = "Sector Icon",
                                tint = when (sector) {
                                    "TECH" -> MatrixGreen
                                    "RETAIL" -> RetailGold
                                    else -> FleetBlue
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = when (sector) {
                                    "TECH" -> "Tech Startup Hub"
                                    "RETAIL" -> "Retail Chain Empire"
                                    else -> "Logistics Cargo Network"
                                },
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.resetGame() },
                            modifier = Modifier.testTag("reset_game_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Game",
                                tint = Color.LightGray
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CharcoalDark)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MatrixGreen)
                }
            } else if (sector.isEmpty()) {
                StartupChoiceScreen(onChoose = { viewModel.chooseSector(it) })
            } else {
                DashboardScreen(
                    viewModel = viewModel,
                    cash = cash,
                    totalTaps = totalTaps,
                    loanAmount = loanAmount,
                    departments = departments,
                    upgrades = upgrades,
                    currentEvent = currentEvent,
                    sector = sector
                )
            }

            // Offline Earnings Dialog
            offlineEarnings?.let { earnings ->
                OfflineProgressDialog(
                    earnings = earnings,
                    durationSeconds = offlineDuration,
                    viewModel = viewModel,
                    onDismiss = { viewModel.dismissOfflineDialog() }
                )
            }

            // Simulated Fullscreen Video Ad Dialog
            activeAdUpgradeId?.let { upgradeId ->
                SimulatedRewardAdDialog(
                    viewModel = viewModel,
                    upgradeId = upgradeId,
                    onDismiss = { viewModel.dismissAd() }
                )
            }
        }
    }
}

@Composable
fun StartupChoiceScreen(onChoose: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("startup_choice_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "CHOOSE YOUR SECTOR",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Launch your startup journey. Every sector has unique departments, resources, and custom manager upgrades.",
            color = Color.LightGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp, start = 12.dp, end = 12.dp)
        )

        // TECH Sector Choice
        SectorChoiceCard(
            title = "Tech Startup",
            description = "Unleash supercomputers, build quantum neural AI algorithms, and run dynamic cloud architectures.",
            icon = Icons.Default.Computer,
            accentColor = MatrixGreen,
            onClick = { onChoose("TECH") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // RETAIL Sector Choice
        SectorChoiceCard(
            title = "Retail Chain Empire",
            description = "Fulfill consumer demands, establish mega franchises, and dominate high-value malls.",
            icon = Icons.Default.ShoppingCart,
            accentColor = RetailGold,
            onClick = { onChoose("RETAIL") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // LOGISTICS Sector Choice
        SectorChoiceCard(
            title = "Logistics Cargo Network",
            description = "Optimize cargo routes, manage state-of-the-art long haul semis, and sail global dynamic freighters.",
            icon = Icons.Default.DirectionsCar,
            accentColor = FleetBlue,
            onClick = { onChoose("LOGISTICS") }
        )
    }
}

@Composable
fun SectorChoiceCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, Brush.linearGradient(listOf(accentColor.copy(alpha = 0.5f), Color.Transparent)), RoundedCornerShape(16.dp))
            .testTag("sector_choice_${title.replace(" ", "_").lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonCard)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: GameViewModel,
    cash: Double,
    totalTaps: Int,
    loanAmount: Double,
    departments: List<DepartmentState>,
    upgrades: List<ActiveUpgradeState>,
    currentEvent: MarketEventState?,
    sector: String
) {
    // Tapper scale animation
    var triggerTapAnimation by remember { mutableStateOf(false) }
    val tapScale by animateFloatAsState(
        targetValue = if (triggerTapAnimation) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        finishedListener = { triggerTapAnimation = false }
    )

    // Floating tapping indicators state
    var floaters by remember { mutableStateOf(listOf<Pair<Long, Double>>()) }
    // Clean up floaters
    LaunchedEffect(floaters) {
        if (floaters.isNotEmpty()) {
            delay(1000)
            floaters = floaters.filter { System.currentTimeMillis() - it.first < 900 }
        }
    }

    val sectorColor = when (sector) {
        "TECH" -> MatrixGreen
        "RETAIL" -> RetailGold
        else -> FleetBlue
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. STATS BANNER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateGrey),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Balance & Passive income
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL CAPITAL",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = viewModel.formatMoney(cash),
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("total_capital_text")
                            )
                        }

                        // Passive Income Rate
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "NET EARNING",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val netPay = viewModel.getPassiveIncomePerSecond()
                                Text(
                                    text = "+${viewModel.formatMoney(netPay)}/s",
                                    color = if (netPay > 0) MatrixGreen else Color.Gray,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.testTag("net_earning_text")
                                )
                            }
                        }
                    }

                    // Loan outstanding if present
                    if (loanAmount > 0.0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CrashRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = "Debt outstanding",
                                    tint = CrashRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Outstanding Debt:",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = viewModel.formatMoney(loanAmount),
                                color = CrashRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Taps Tracker
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Industrial Operations Conducted: $totalTaps",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }

        // 2. ACTIVE MARKET EVENT ALERT (Flashing)
        currentEvent?.let { event ->
            item {
                val isBoom = event.type == "BOOM"
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBoom) RetailGold.copy(alpha = 0.15f) else CrashRed.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isBoom) RetailGold.copy(alpha = 0.5f) else CrashRed.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isBoom) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = "Event icon",
                            tint = if (isBoom) RetailGold else CrashRed,
                            modifier = Modifier
                                .size(36.dp)
                                .padding(end = 8.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                color = if (isBoom) RetailGold else CrashRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = event.description,
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // Remaining seconds
                        val remMs = event.finishTimestamp - System.currentTimeMillis()
                        val secLeft = maxOf(0, remMs / 1000)
                        Text(
                            text = "${secLeft}s",
                            color = if (isBoom) RetailGold else CrashRed,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        // 3. CORE CLICKER INTERACTION PAD
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TAP HERE FOR MANUAL VENTURES",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(tapScale)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(sectorColor.copy(alpha = 0.3f), Color.Transparent)
                            ),
                            CircleShape
                        )
                        .border(3.dp, sectorColor, CircleShape)
                        .clip(CircleShape)
                        .clickable {
                            triggerTapAnimation = true
                            val tapVal = viewModel.getTapIncomeAmount()
                            floaters = floaters + Pair(System.currentTimeMillis(), tapVal)
                            viewModel.tapToEarn()
                        }
                        .testTag("tap_station_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Tap button",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "+${viewModel.formatMoney(viewModel.getTapIncomeAmount())}",
                            color = sectorColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Floating Tapping Feedback
                    floaters.forEach { floater ->
                        val duration = (System.currentTimeMillis() - floater.first).toFloat()
                        // Compute Y offset
                        val progress = minOf(1.0f, duration / 900f)
                        val offsetY = -100 * progress
                        val alpha = 1.0f - progress

                        Box(
                            modifier = Modifier.offset(y = offsetY.dp)
                        ) {
                            Text(
                                text = "+${viewModel.formatMoney(floater.second)}",
                                color = MatrixGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.scale(1.0f + progress * 0.4f)
                            )
                        }
                    }
                }
            }
        }

        // 4. DEPARTMENTS SECTION
        item {
            Text(
                text = "DEPARTMENTS & WORKSTATIONS",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(departments) { dept ->
            val isLocked = dept.level == 0
            val costForLevel = dept.baseCost * (dept.costMultiplier.pow(dept.level))
            val isCapitalAvailable = cash >= costForLevel

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isLocked) CardDefaults.cardColors().containerColor else CarbonCard
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (isLocked) Color.DarkGray else sectorColor.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dept_card_${dept.id.lowercase()}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dept.name,
                                    color = if (isLocked) Color.Gray else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1
                                )
                                if (!isLocked) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(sectorColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Lvl ${dept.level}",
                                            color = sectorColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (!isLocked) {
                                val actualRevenue = dept.baseRevenue * (dept.revenueMultiplier.pow(dept.level - 1))
                                Text(
                                    text = "Base yield: ${viewModel.formatMoney(actualRevenue)}/sec",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            } else {
                                Text(
                                    text = "Locked / Not Commissioned",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Unlock or Upgrade Button
                        if (isLocked) {
                            Button(
                                onClick = { viewModel.unlockDepartment(dept.id) },
                                enabled = cash >= dept.baseCost,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = sectorColor,
                                    disabledContainerColor = Color.DarkGray
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("unlock_dept_${dept.id.lowercase()}")
                            ) {
                                Text(
                                    text = "Unlock ${viewModel.formatMoney(dept.baseCost)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cash >= dept.baseCost) Color.Black else Color.LightGray
                                )
                            }
                        } else {
                            Button(
                                onClick = { viewModel.upgradeDepartment(dept.id) },
                                enabled = isCapitalAvailable,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = sectorColor,
                                    disabledContainerColor = Color.Transparent
                                ),
                                border = if (!isCapitalAvailable) BorderStroke(1.dp, Color.DarkGray) else null,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("upgrade_dept_${dept.id.lowercase()}")
                            ) {
                                Text(
                                    text = "Upgrade ${viewModel.formatMoney(costForLevel)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCapitalAvailable) Color.Black else Color.Gray
                                )
                            }
                        }
                    }

                    // Manager / Automation Row
                    if (!isLocked) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (dept.isAutomated) Icons.Default.CheckCircle else Icons.Default.Lock,
                                    contentDescription = "Automation Status",
                                    tint = if (dept.isAutomated) MatrixGreen else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (dept.isAutomated) "Automated by ${dept.managerName}" else "Manager Unhired",
                                    color = if (dept.isAutomated) MatrixGreen else Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (!dept.isAutomated) {
                                OutlinedButton(
                                    onClick = { viewModel.hireManagerForDepartment(dept.id) },
                                    enabled = cash >= dept.managerCost,
                                    border = BorderStroke(1.dp, if (cash >= dept.managerCost) RetailGold else Color.DarkGray),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = RetailGold,
                                        disabledContentColor = Color.Gray
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("hire_manager_${dept.id.lowercase()}")
                                ) {
                                    Text(
                                        text = "Hire ${dept.managerName} (${viewModel.formatMoney(dept.managerCost)})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. EXPANSION TIME-BASED UPGRADES SECTION
        item {
            Text(
                text = "TECHNOLOGICAL TIME-BASED EXPANSIONS",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "These dynamic upgrades run in real-time and continue offline as long-range operations.",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        items(upgrades) { upgrade ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (upgrade.isFinished) SlateGrey else CarbonCard
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upgrade_card_${upgrade.id.lowercase()}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = upgrade.title,
                                color = if (upgrade.isFinished) sectorColor else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Category: ${if (upgrade.id.contains("T1")) "Tier 1 (Early)" else if (upgrade.id.contains("T2")) "Tier 2 (Mid)" else "Tier 3 (Late)"}",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }

                        // Status Badge / Action Button
                        when {
                            upgrade.isFinished -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Finished Upgrade",
                                    tint = MatrixGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            upgrade.hasStarted -> {
                                Text(
                                    text = "In Progress",
                                    fontSize = 12.sp,
                                    color = RetailGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            else -> {
                                Button(
                                    onClick = { viewModel.startExpansionUpgrade(upgrade.id) },
                                    enabled = cash >= upgrade.cost,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = sectorColor,
                                        disabledContainerColor = Color.DarkGray
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("start_expansion_${upgrade.id.lowercase()}")
                                ) {
                                    Text(
                                        text = "Install ${viewModel.formatMoney(upgrade.cost)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (cash >= upgrade.cost) Color.Black else Color.LightGray
                                    )
                                }
                            }
                        }
                    }

                    // Ticker state details for ongoing actions
                    if (upgrade.hasStarted && !upgrade.isFinished) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val curTime = System.currentTimeMillis()
                        val diffMillis = upgrade.finishTimestamp - curTime
                        val secondsLeft = maxOf(0L, diffMillis / 1000)

                        // Format seconds beautifully (hours : minutes : seconds)
                        val hr = secondsLeft / 3600
                        val min = (secondsLeft % 3600) / 60
                        val sec = secondsLeft % 60
                        val timeStr = if (hr > 0) String.format("%02d:%02d:%02d", hr, min, sec) else String.format("%02d:%02d", min, sec)

                        val progressRatio = 1.0f - (secondsLeft.toFloat() / upgrade.totalDurationSeconds.toFloat())

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LinearProgressIndicator(
                                progress = { progressRatio.coerceIn(0.0f, 1.0f) },
                                color = RetailGold,
                                trackColor = Color.DarkGray,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = timeStr,
                                color = RetailGold,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }

                        // Watch ad to skip 5 minutes button
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.triggerWatchAd(upgrade.id) },
                            border = BorderStroke(1.dp, MatrixGreen),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MatrixGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("skip_timer_ad_${upgrade.id.lowercase()}"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = "Watch Ad Icon",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Watch Ad: Skip 5 Minutes",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. BANKING LOANS & SIMULATOR
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateGrey),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.DarkGray),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .testTag("loan_card_bureau")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "BANKING LOAN DEPARTMENT",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Need quick investment liquid? Borrow capital instantly. 15% of all automated and manual revenue will be auto-diverted to cover repayments safely.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // Outstanding loan balance display
                    if (loanAmount > 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "OUTSTANDING", color = Color.Gray, fontSize = 9.sp)
                                Text(
                                    text = viewModel.formatMoney(loanAmount),
                                    color = CrashRed,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Row {
                                Button(
                                    onClick = { viewModel.repayBankLoanAmount(1000.0) },
                                    enabled = cash >= 1000.0 && loanAmount > 0.0,
                                    colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, disabledContainerColor = Color.DarkGray),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("repay_1k_button")
                                ) {
                                    Text("Repay $1K", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { viewModel.repayBankLoanAmount(loanAmount) },
                                    enabled = cash >= loanAmount && loanAmount > 0.0,
                                    colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen, disabledContainerColor = Color.DarkGray),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("repay_all_button")
                                ) {
                                    Text("Repay Max", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Taking standard loans options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LoanOptionButton(amount = 10000.0, title = "$10K", viewModel = viewModel)
                        LoanOptionButton(amount = 50000.0, title = "$50K", viewModel = viewModel)
                        LoanOptionButton(amount = 250000.0, title = "$250K", viewModel = viewModel)
                    }
                }
            }
        }

        // 7. DEV SIMULATION SHORTCUT (Trigger Event manually)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                OutlinedButton(
                    onClick = { viewModel.triggerRandomMarketEvent() },
                    border = BorderStroke(1.dp, sectorColor.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                ) {
                    Text("Trigger Random Market Event", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LoanOptionButton(
    amount: Double,
    title: String,
    viewModel: GameViewModel
) {
    Button(
        onClick = { viewModel.takeBankLoan(amount) },
        colors = ButtonDefaults.buttonColors(containerColor = SlateGrey),
        border = BorderStroke(1.dp, Color.Gray),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier
            .testTag("take_loan_${amount.toInt()}")
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Borrow", color = Color.LightGray, fontSize = 9.sp)
            Text(title, color = MatrixGreen, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

// Dialog explaining exactly how much cash was generated while application was in the background
@Composable
fun OfflineProgressDialog(
    earnings: Double,
    durationSeconds: Long,
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Welcome Back, CEO!",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = MatrixGreen,
                fontFamily = FontFamily.SansSerif
            )
        },
        text = {
            val h = durationSeconds / 3600
            val m = (durationSeconds % 3600) / 60
            val s = durationSeconds % 60
            val timeDesc = if (h > 0) "${h}h ${m}m" else if (m > 0) "${m}m ${s}s" else "${s}s"

            Column {
                Text(
                    text = "Your business operations continued securely offline for $timeDesc during your corporate leave.",
                    color = Color.White,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MatrixGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Passive Earning:", color = Color.LightGray, fontSize = 13.sp)
                    Text(
                        text = viewModel.formatMoney(earnings),
                        color = MatrixGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (viewModel.loanAmount.value > 0.0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "* 15% auto-deduction applied automatically as safe loan servicing repayment.",
                        color = CrashRed,
                        fontSize = 10.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MatrixGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Collect Revenue", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = SlateGrey,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}

// Gorgeous simulated Fullscreen/Heavy video sponsor ad showing interactive loading countdown
@Composable
fun SimulatedRewardAdDialog(
    viewModel: GameViewModel,
    upgradeId: String,
    onDismiss: () -> Unit
) {
    val countdown by viewModel.adTimerSeconds.collectAsStateWithLifecycle()
    val isReady by viewModel.isAdRewardReady.collectAsStateWithLifecycle()
    val sponsor by viewModel.adCompany.collectAsStateWithLifecycle()

    Dialog(
        onDismissRequest = { if (isReady) viewModel.claimAdTimeSkip() else onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CharcoalDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .testTag("reward_ad_simulator"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sponsor Label
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "Ad",
                            tint = RetailGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Corporate Broadcast",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Countdown Label / Close
                    if (isReady) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.LightGray
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = countdown.toString(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Center visual representing sponsor streams
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateGrey),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16 / 10f)
                            .border(1.dp, MatrixGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Infinite custom animated circular sonar rings mimicking a stream playing
                            val infiniteTransition = rememberInfiniteTransition()
                            val alphaPulse by infiniteTransition.animateFloat(
                                initialValue = 0.2f,
                                targetValue = 0.8f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                )
                            )
                            val ringMultiplier by infiniteTransition.animateFloat(
                                initialValue = 0.6f,
                                targetValue = 1.3f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                )
                            )

                            Canvas(modifier = Modifier.size(180.dp)) {
                                drawCircle(
                                    color = MatrixGreen,
                                    radius = 60.dp.toPx() * ringMultiplier,
                                    alpha = 0.25f * (1.0f - (ringMultiplier - 0.6f) / 0.7f)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = "Sponsor Stream",
                                    tint = MatrixGreen,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .scale(alphaPulse)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = sponsor,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Enhancing dynamic networks globally",
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = if (isReady) "Corporate Sponsor rewards ready!" else "Securing server handshake stream... please wait.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Claim Reward Button
                Button(
                    onClick = { viewModel.claimAdTimeSkip() },
                    enabled = isReady,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MatrixGreen,
                        disabledContainerColor = Color.DarkGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("claim_ad_time_skip_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isReady) "CLAIM 5-MINUTE TIME SKIP" else "BROADCAST ENDS IN ${countdown}s",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = if (isReady) Color.Black else Color.LightGray
                    )
                }
            }
        }
    }
}
