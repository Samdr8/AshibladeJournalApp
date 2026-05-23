package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.composed
import coil.compose.AsyncImage
import com.example.data.AppSettings
import com.example.data.Trade
import com.example.data.Workspace
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: JournalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Observe DB States
    val workspaces by viewModel.workspacesState.collectAsState()
    val activeWorkspaceId by viewModel.activeWorkspaceIdState.collectAsState()
    val trades by viewModel.activeWorkspaceTradesState.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val isLocked by viewModel.isLockedState.collectAsState()

    val isLight = MaterialTheme.colorScheme.background != SlateDark
    val textColor = if (isLight) Color(0xFF0B0E14) else Color.White
    val labelColor = if (isLight) Color(0xFF64748B) else Color.Gray

    // Active screen navigation
    var currentTab by remember { mutableStateOf("Dashboard") }

    // Dialog flags
    var showAddTradeDialog by remember { mutableStateOf(false) }
    var tradeToEdit by remember { mutableStateOf<Trade?>(null) }
    var selectedViewTrade by remember { mutableStateOf<Trade?>(null) }

    var showWorkspaceManager by remember { mutableStateOf(false) }

    // Security screen handler
    if (isLocked) {
        BiometricSecurityOverlay(
            settings = settings,
            onUnlock = { viewModel.unlock() }
        )
    } else {
        Scaffold(
            topBar = {
                AshibladeHeader(
                    currentWorkspaceName = workspaces.find { it.id == activeWorkspaceId }?.name ?: "No Workspace",
                    workspaces = workspaces,
                    onSelectWorkspace = { viewModel.setActiveWorkspace(it) },
                    onManageWorkspaces = { showWorkspaceManager = true }
                )
            },
            bottomBar = {
                AshibladeBottomNav(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
            },
            floatingActionButton = {
                if (currentTab != "Settings") {
                    FloatingActionButton(
                        onClick = { showAddTradeDialog = true },
                        containerColor = BrandBlue,
                        contentColor = Color.Black,
                        modifier = Modifier
                            .testTag("add_trade_fab")
                            .shadowGlow(BrandBlue),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Log Trade")
                    }
                }
            },
            modifier = modifier.fillMaxSize(),
            containerColor = SlateDark
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .drawBehind {
                        // Drawing subtle gradient glows
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(BrandBlue.copy(alpha = 0.08f), Color.Transparent),
                                radius = 800f
                            )
                        )
                    }
            ) {
                val activeAlert by viewModel.activeInAppAlert.collectAsState()
                if (activeAlert != null) {
                    AlertDialog(
                        onDismissRequest = { viewModel.clearActiveAlert() },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.NotificationsActive,
                                    contentDescription = "Alert Triggered",
                                    tint = BrandBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ashiblade Trade Alert",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = textColor
                                )
                            }
                        },
                        text = {
                            Text(
                                text = activeAlert ?: "",
                                fontSize = 14.sp,
                                color = textColor
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.clearActiveAlert() },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                            ) {
                                Text("ACKNOWLEDGE", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        containerColor = if (isLight) Color.White else SurfaceDark,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                when (currentTab) {
                    "Dashboard" -> DashboardTab(
                        trades = trades,
                        startBalance = settings?.startBalance ?: 10000.0,
                        onUpdateBalance = { viewModel.updateStartBalance(it) },
                        onTradeClick = { selectedViewTrade = it }
                    )
                    "Ledger" -> LedgerTab(
                        trades = trades,
                        onTradeClick = { selectedViewTrade = it }
                    )
                    "Settings" -> SettingsTab(
                        settings = settings,
                        workspaces = workspaces,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Modal: Create or Edit Workspace dialog
    if (showWorkspaceManager) {
        WorkspaceManagerDialog(
            workspaces = workspaces,
            activeWorkspaceId = activeWorkspaceId,
            onCreateWorkspace = { viewModel.addWorkspace(it) },
            onRenameWorkspace = { id, name -> viewModel.renameWorkspace(id, name) },
            onDeleteWorkspace = { viewModel.deleteWorkspace(it) },
            onDismiss = { showWorkspaceManager = false }
        )
    }

    // Modal: Form to Log or Edit Trade
    if (showAddTradeDialog || tradeToEdit != null) {
        val formTrade = tradeToEdit
        TradeFormDialog(
            trade = formTrade,
            onSave = { dateStr, timeStr, inst, dir, entry, sl, tp, lot, pnl, outc, photo, gr, setup, tgs, emo, n1, n2, n3, n4 ->
                if (formTrade == null) {
                    viewModel.addTrade(
                        dateStr, timeStr, inst, dir, entry, sl, tp, lot, pnl, outc, photo, gr, setup, tgs, emo, n1, n2, n3, n4
                    )
                } else {
                    viewModel.updateTrade(
                        formTrade.copy(
                            dateStr = dateStr,
                            timeStr = timeStr,
                            timestampLong = parseDateTimeToLong(dateStr, timeStr),
                            instrument = inst,
                            direction = dir,
                            entryPrice = entry,
                            stopLoss = sl,
                            takeProfit = tp,
                            lotSize = lot,
                            pnl = pnl,
                            outcome = outc,
                            photoUri = photo,
                            grade = gr,
                            setupName = setup,
                            tags = tgs,
                            emotions = emo,
                            entryExecutionNotes = n1,
                            reasoningNotes = n2,
                            confirmationNotes = n3,
                            takeawayNotes = n4
                        )
                    )
                }
                showAddTradeDialog = false
                tradeToEdit = null
            },
            onDismiss = {
                showAddTradeDialog = false
                tradeToEdit = null
            }
        )
    }

    // Modal: View detailed trade with image synthesis download & sharing triggers
    if (selectedViewTrade != null) {
        val viewTrade = selectedViewTrade!!
        TradeDetailsDialog(
            trade = viewTrade,
            onEditClick = {
                tradeToEdit = viewTrade
                selectedViewTrade = null
            },
            onDeleteClick = {
                viewModel.deleteTrade(viewTrade)
                selectedViewTrade = null
            },
            onShareClick = {
                ShareComposer.shareTradeSetup(context, viewTrade)
            },
            onDismiss = { selectedViewTrade = null }
        )
    }
}

// Helper to convert date and time inputs to sorting timestamps
fun parseDateTimeToLong(date: String, time: String): Long {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        format.parse("$date $time")?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

// Persistently copy selected Uri to app sandboxed files dir
fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val folder = File(context.filesDir, "trade_setups")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val destinationFile = File(folder, "setup_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(destinationFile)
        inputStream.copyTo(outputStream)
        outputStream.flush()
        outputStream.close()
        inputStream.close()
        destinationFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// ==========================================
// CUSTOM RENDERING & EXTENSION UTILITIES
// ==========================================

// Extension modifier to add premium neon borders and glows
fun Modifier.glassmorphism(
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    opacity: Float = 1.0f
) = composed {
    val isLight = MaterialTheme.colorScheme.background != SlateDark
    val cardBackground = if (isLight) Color.White else SurfaceDark
    val borderColor = if (isLight) Color(0xFFE2E8F0) else GlassBorder
    
    this
        .background(cardBackground.copy(alpha = opacity), shape)
        .border(1.dp, borderColor, shape)
}

fun Modifier.shadowGlow(color: Color) = this.drawBehind {
    // Elegant glow shadow background effect
}

// Header Composable matching the design guidelines
@Composable
fun AshibladeHeader(
    currentWorkspaceName: String,
    workspaces: List<Workspace>,
    onSelectWorkspace: (Long) -> Unit,
    onManageWorkspaces: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    val isLight = MaterialTheme.colorScheme.background != SlateDark
    val textColor = if (isLight) Color(0xFF0B0E14) else Color.White
    val dropdownBg = if (isLight) Color.White else SurfaceDark
    val dividerColor = if (isLight) Color(0xFFE2E8F0) else GlassBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "ASHIBLADE",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BrandBlue,
                letterSpacing = 2.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { expandedMenu = true }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = currentWorkspaceName,
                    fontSize = 14.sp,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = "Switch portfolio",
                    tint = BrandBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = expandedMenu,
                onDismissRequest = { expandedMenu = false },
                modifier = Modifier.background(dropdownBg)
            ) {
                workspaces.forEach { ws ->
                    DropdownMenuItem(
                        text = { Text(ws.name, color = textColor) },
                        onClick = {
                            onSelectWorkspace(ws.id)
                            expandedMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Folder,
                                contentDescription = null,
                                tint = ElectricBlue
                            )
                        }
                    )
                }
                Divider(color = dividerColor)
                DropdownMenuItem(
                    text = { Text("Manage Portfolios", color = BrandBlue, fontWeight = FontWeight.Bold) },
                    onClick = {
                        onManageWorkspaces()
                        expandedMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = null,
                            tint = BrandBlue
                        )
                    }
                )
            }
        }

        // Mini status glowing indicator
        Box(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .background(BrandBlue.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, BrandBlue.copy(alpha = 0.4f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(WinGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "OFFLINE FLOW",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

// Navigation matching guidelines
@Composable
fun AshibladeBottomNav(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background != SlateDark
    val navBg = if (isLight) Color.White else SurfaceDark.copy(alpha = 0.95f)

    NavigationBar(
        containerColor = navBg,
        tonalElevation = 8.dp,
        modifier = Modifier.navigationBarsPadding(),
        windowInsets = WindowInsets.navigationBars
    ) {
        NavigationBarItem(
            selected = currentTab == "Dashboard",
            onClick = { onTabSelected("Dashboard") },
            icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Stats") },
            label = { Text("Summary") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = BrandBlue,
                indicatorColor = BrandBlue,
                unselectedIconColor = BreakEvenGray,
                unselectedTextColor = BreakEvenGray
            ),
            modifier = Modifier.testTag("nav_summary")
        )
        NavigationBarItem(
            selected = currentTab == "Ledger",
            onClick = { onTabSelected("Ledger") },
            icon = { Icon(Icons.Filled.ListAlt, contentDescription = "Ledger") },
            label = { Text("Ledger") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = BrandBlue,
                indicatorColor = BrandBlue,
                unselectedIconColor = BreakEvenGray,
                unselectedTextColor = BreakEvenGray
            ),
            modifier = Modifier.testTag("nav_ledger")
        )
        NavigationBarItem(
            selected = currentTab == "Settings",
            onClick = { onTabSelected("Settings") },
            icon = { Icon(Icons.Filled.AccountCircle, contentDescription = "Settings") },
            label = { Text("Profile") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = BrandBlue,
                indicatorColor = BrandBlue,
                unselectedIconColor = BreakEvenGray,
                unselectedTextColor = BreakEvenGray
            ),
            modifier = Modifier.testTag("nav_profile")
        )
    }
}

// ==========================================
// SCREEN 1: PORTFOLIO DASHBOARD & CALENDAR
// ==========================================
enum class BenchmarkPeriod(val displayName: String, val daysCount: Int) {
    LAST_7_DAYS("Last 7 Days", 7),
    LAST_14_DAYS("Last 14 Days", 14),
    LAST_30_DAYS("Last 30 Days", 30),
    ALL_TIME("All Time", 365)
}

@Composable
fun DashboardTab(
    trades: List<Trade>,
    startBalance: Double,
    onUpdateBalance: (Double) -> Unit,
    onTradeClick: (Trade) -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background != SlateDark
    val textColor = if (isLight) Color(0xFF0B0E14) else Color.White
    val labelColor = if (isLight) Color(0xFF64748B) else Color.Gray

    // Calculate Analytics Metrics
    val concludedTrades = trades.filter { it.outcome.uppercase() != "PENDING" }
    val winsCount = concludedTrades.count { it.outcome.uppercase() == "WIN" }
    
    // Win Rate: Wins / Total concluded
    val winRatePercentage = if (concludedTrades.isNotEmpty()) {
        (winsCount.toFloat() / concludedTrades.size.toFloat() * 100f).toInt()
    } else {
        0
    }

    // Profit Factor: Total Gross profits / Absolute Total Gross losses
    val grossProfit = trades.filter { it.pnl > 0 }.sumOf { it.pnl }
    val grossLoss = trades.filter { it.pnl < 0 }.sumOf { it.pnl }
    val absoluteGrossLoss = Math.abs(grossLoss)

    val profitFactorStr = when {
        trades.isEmpty() -> "0.00"
        absoluteGrossLoss == 0.0 -> if (grossProfit > 0) "∞" else "0.00"
        else -> String.format(Locale.US, "%.2f", grossProfit / absoluteGrossLoss)
    }

    // Total Net P&L in selected Portfolio
    val netPnl = trades.sumOf { it.pnl }
    val portfolioBalance = startBalance + netPnl // Seed default balance

    // Calendar selection state
    var selectedCalendarYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedCalendarMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) } // 0-indexed

    var showEditBalanceDialog by remember { mutableStateOf(false) }

    if (showEditBalanceDialog) {
        var inputVal by remember { mutableStateOf(startBalance.toString()) }
        AlertDialog(
            onDismissRequest = { showEditBalanceDialog = false },
            title = { Text("Set Starting Capital Baseline", fontWeight = FontWeight.Bold, color = textColor) },
            text = {
                Column {
                    Text("Baseline designation computes portfolio savings and growth benchmarks.", fontSize = 12.sp, color = labelColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputVal,
                        onValueChange = { inputVal = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = labelColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = inputVal.toDoubleOrNull()
                        if (parsed != null) {
                            onUpdateBalance(parsed)
                            showEditBalanceDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Set capital baseline", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBalanceDialog = false }) {
                    Text("Cancel", color = labelColor)
                }
            },
            containerColor = if (isLight) Color.White else SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main balance stats board
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphism(RoundedCornerShape(24.dp))
                    .clickable { showEditBalanceDialog = true }
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TOTAL PORTFOLIO SAVINGS (EDITABLE)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = labelColor,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format("$%,.2f", portfolioBalance),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                // Net Profit indicators
                val sign = if (netPnl >= 0) "+" else ""
                val pnlColor = if (netPnl >= 0) WinGreen else LossRed
                Text(
                    text = "$sign${String.format("$%,.2f", netPnl)} net P&L",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = pnlColor
                )
            }
        }

        // Live grid diagnostics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .glassmorphism()
                        .padding(16.dp)
                ) {
                    Text("WIN-RATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = labelColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$winRatePercentage%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = if (winRatePercentage >= 50) WinGreen else LossRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$winsCount of ${concludedTrades.size} wins",
                        fontSize = 12.sp,
                        color = labelColor
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .glassmorphism()
                        .padding(16.dp)
                ) {
                    Text("PROFIT FACTOR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = labelColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = profitFactorStr,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gross Ratio",
                        fontSize = 12.sp,
                        color = labelColor
                    )
                }
            }
        }

        // Live progressive curve chart of equity
        item {
            TradezellaDailyPnlChart(trades = trades)
        }

        // Performance Benchmarking Tool
        item {
            PerformanceBenchmarkingCard(trades = trades)
        }

        // Tradezella analytical quick stats
        item {
            TradezellaQuickStats(trades = trades)
        }

        // Modern Monthly Calendar Matrix showing green vs red days
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphism()
                    .padding(16.dp)
            ) {
                // Calendar header switcher
                val monthNames = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (selectedCalendarMonth == 0) {
                                selectedCalendarMonth = 11
                                selectedCalendarYear--
                            } else {
                                selectedCalendarMonth--
                            }
                        }
                    ) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Prev month", tint = BrandBlue)
                    }

                    Text(
                        text = "${monthNames[selectedCalendarMonth]} $selectedCalendarYear",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue
                    )

                    IconButton(
                        onClick = {
                            if (selectedCalendarMonth == 11) {
                                selectedCalendarMonth = 0
                                selectedCalendarYear++
                            } else {
                                selectedCalendarMonth++
                            }
                        }
                    ) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Next month", tint = BrandBlue)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Weekday titles Row
                Row(modifier = Modifier.fillMaxWidth()) {
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    days.forEach { d ->
                        Text(
                            text = d,
                            color = labelColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Build days grid list
                val gridDays = getDaysInMonthGrid(selectedCalendarYear, selectedCalendarMonth)
                val rows = gridDays.chunked(7)

                rows.forEach { rowDays ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowDays.forEach { dateObj ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                            ) {
                                if (dateObj != null) {
                                    val calendarHelper = Calendar.getInstance().apply { time = dateObj }
                                    val dayNo = calendarHelper.get(Calendar.DAY_OF_MONTH)
                                    val dateKeyString = String.format(
                                        Locale.US,
                                        "%04d-%02d-%02d",
                                        selectedCalendarYear,
                                        selectedCalendarMonth + 1,
                                        dayNo
                                    )

                                    // Filter trades on this specific day
                                    val tradesOnDay = trades.filter { it.dateStr == dateKeyString }
                                    val dailyNetPnl = tradesOnDay.sumOf { it.pnl }

                                    val isDayTrade = tradesOnDay.isNotEmpty()
                                    val dayCellBg = when {
                                        !isDayTrade -> Color.Transparent
                                        dailyNetPnl > 0.0 -> WinGreen.copy(alpha = 0.25f)
                                        dailyNetPnl < 0.0 -> LossRed.copy(alpha = 0.25f)
                                        else -> BreakEvenGray.copy(alpha = 0.25f)
                                    }
                                    val dayBorderColor = when {
                                        !isDayTrade -> if (isLight) Color(0xFFE2E8F0) else GlassBorder
                                        dailyNetPnl > 0.0 -> WinGreen.copy(alpha = 0.6f)
                                        dailyNetPnl < 0.0 -> LossRed.copy(alpha = 0.6f)
                                        else -> BreakEvenGray.copy(alpha = 0.6f)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(dayCellBg, RoundedCornerShape(8.dp))
                                            .border(1.dp, dayBorderColor, RoundedCornerShape(8.dp))
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "$dayNo",
                                                color = textColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (isDayTrade) {
                                                Text(
                                                    text = if (dailyNetPnl >= 0) "+$${dailyNetPnl.toInt()}" else "-$${Math.abs(dailyNetPnl.toInt())}",
                                                    color = if (dailyNetPnl >= 0) WinGreen else LossRed,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
            }
        }

        // Mini preview of latest setups
        item {
            Text(
                "RECENT SETUPS LOGGED",
                fontSize = 12.sp,
                color = labelColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }

        if (trades.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphism()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.BookmarkBorder,
                            contentDescription = "No Trades",
                            tint = labelColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No trades logged in this workspace.\nTap the + button to add one!",
                            color = labelColor,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(trades.take(10)) { trade ->
                TradeItemCard(
                    trade = trade,
                    onItemClick = { onTradeClick(trade) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TradezellaDailyPnlChart(trades: List<Trade>) {
    val isLight = MaterialTheme.colorScheme.background != SlateDark
    val textColor = if (isLight) Color(0xFF0B0E14) else Color.White
    val labelColor = if (isLight) Color(0xFF64748B) else Color.Gray

    val completedTrades = trades.filter { it.outcome.uppercase() == "WIN" || it.outcome.uppercase() == "LOSS" }
    val points = mutableListOf<Pair<String, Double>>()
    var cumulative = 0.0
    val sorted = completedTrades.sortedBy { it.timestampLong }
    
    for (t in sorted) {
        cumulative += t.pnl
        points.add(t.dateStr.takeLast(5) to cumulative)
    }
    
    val chartData = points.takeLast(8)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphism()
            .padding(16.dp)
    ) {
        Text(
            text = "EQUITY GROWTH CURVE (LAST 8 TRADES)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            letterSpacing = 1.2.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (chartData.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Insufficient trades to plot curve. Log more trades to see Tradezella-grade analytics!",
                    fontSize = 12.sp,
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            val maxVal = chartData.maxOf { it.second }
            val minVal = chartData.minOf { it.second }
            val valueRange = if (maxVal != minVal) maxVal - minVal else 1.0
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(horizontal = 8.dp)
            ) {
                val width = size.width
                val height = size.height
                val stepX = width / (chartData.size - 1)
                
                val path = androidx.compose.ui.graphics.Path()
                val fillPath = androidx.compose.ui.graphics.Path()
                
                for (i in chartData.indices) {
                    val p = chartData[i]
                    val x = i * stepX
                    val pct = (p.second - minVal) / valueRange
                    val y = height - (pct * height).toFloat()
                    
                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                    
                    if (i == chartData.size - 1) {
                        fillPath.lineTo(x, height)
                    }
                }
                
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(BrandBlue.copy(alpha = 0.25f), Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )
                
                drawPath(
                    path = path,
                    color = BrandBlue,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 4f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
                
                for (i in chartData.indices) {
                    val x = i * stepX
                    val pct = (chartData[i].second - minVal) / valueRange
                    val y = height - (pct * height).toFloat()
                    
                    drawCircle(
                        color = ElectricBlue,
                        radius = 6f,
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5f,
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                chartData.forEach { p ->
                    Text(text = p.first, fontSize = 9.sp, color = labelColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PerformanceBenchmarkingCard(trades: List<Trade>) {
    var selectedPeriod by remember { mutableStateOf(BenchmarkPeriod.LAST_7_DAYS) }
    
    val isLight = MaterialTheme.colorScheme.background != SlateDark
    val textColor = if (isLight) Color(0xFF0B0E14) else Color.White
    val labelColor = if (isLight) Color(0xFF64748B) else Color.Gray

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphism()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PERFORMANCE BENCHMARKS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = labelColor,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Relative vs Previous Period",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            
            Box {
                var expanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .clickable { expanded = true }
                        .background(BrandBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedPeriod.displayName,
                        color = BrandBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(if (isLight) Color.White else SurfaceDark)
                ) {
                    BenchmarkPeriod.values().forEach { period ->
                        DropdownMenuItem(
                            text = { Text(period.displayName, color = textColor) },
                            onClick = {
                                selectedPeriod = period
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val now = System.currentTimeMillis()
        val periodMs = selectedPeriod.daysCount * 24L * 60L * 60L * 1000L
        
        val currentPeriodTrades = trades.filter { 
            val diff = now - it.timestampLong
            diff in 0..periodMs
        }
        
        val previousPeriodTrades = trades.filter {
            val diff = now - it.timestampLong
            diff in (periodMs + 1)..(2 * periodMs)
        }
        
        val currentPnl = currentPeriodTrades.sumOf { it.pnl }
        val previousPnl = previousPeriodTrades.sumOf { it.pnl }
        
        val currentWins = currentPeriodTrades.count { it.outcome.uppercase() == "WIN" }
        val currentConcluded = currentPeriodTrades.count { it.outcome.uppercase() == "WIN" || it.outcome.uppercase() == "LOSS" }
        val currentWinRate = if (currentConcluded > 0) (currentWins.toDouble() / currentConcluded) * 100.0 else 0.0
        
        val previousWins = previousPeriodTrades.count { it.outcome.uppercase() == "WIN" }
        val previousConcluded = previousPeriodTrades.count { it.outcome.uppercase() == "WIN" || it.outcome.uppercase() == "LOSS" }
        val previousWinRate = if (previousConcluded > 0) (previousWins.toDouble() / previousConcluded) * 100.0 else 0.0
        
        val currentGrossProfit = currentPeriodTrades.filter { it.pnl > 0 }.sumOf { it.pnl }
        val currentGrossLoss = Math.abs(currentPeriodTrades.filter { it.pnl < 0 }.sumOf { it.pnl })
        val currentPF = if (currentGrossLoss > 0) currentGrossProfit / currentGrossLoss else if (currentGrossProfit > 0) 99.9 else 0.0
        
        val previousGrossProfit = previousPeriodTrades.filter { it.pnl > 0 }.sumOf { it.pnl }
        val previousGrossLoss = Math.abs(previousPeriodTrades.filter { it.pnl < 0 }.sumOf { it.pnl })
        val previousPF = if (previousGrossLoss > 0) previousGrossProfit / previousGrossLoss else if (previousGrossProfit > 0) 99.9 else 0.0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BenchmarkMetricCell(
                title = "NET P&L",
                currentVal = String.format("$%,.2f", currentPnl),
                prevVal = String.format("$%,.2f", previousPnl),
                isPnl = true,
                changePercent = if (previousPnl != 0.0) ((currentPnl - previousPnl) / Math.abs(previousPnl)) * 100.0 else null,
                isBetter = currentPnl >= previousPnl,
                industryStandard = "Gain > $0",
                isIndustryBetter = currentPnl > 0.0,
                modifier = Modifier.weight(1f)
            )
            
            BenchmarkMetricCell(
                title = "WIN RATE",
                currentVal = String.format("%.1f%%", currentWinRate),
                prevVal = String.format("%.1f%%", previousWinRate),
                isPnl = false,
                changePercent = currentWinRate - previousWinRate,
                isBetter = currentWinRate >= previousWinRate,
                industryStandard = ">= 50%",
                isIndustryBetter = currentWinRate >= 50.0,
                modifier = Modifier.weight(1f)
            )
            
            BenchmarkMetricCell(
                title = "PROFIT FACTOR",
                currentVal = String.format("%.2f", currentPF),
                prevVal = String.format("%.2f", previousPF),
                isPnl = false,
                changePercent = currentPF - previousPF,
                isBetter = currentPF >= previousPF,
                industryStandard = ">= 1.50",
                isIndustryBetter = currentPF >= 1.5,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun BenchmarkMetricCell(
    title: String,
    currentVal: String,
    prevVal: String,
    isPnl: Boolean,
    changePercent: Double?,
    isBetter: Boolean,
    industryStandard: String,
    isIndustryBetter: Boolean,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background != SlateDark
    val textColor = if (isLight) Color(0xFF0B0E14) else Color.White
    val labelColor = if (isLight) Color(0xFF64748B) else Color.Gray
    val subCellBg = if (isLight) Color(0xFFF8FAFC) else SlateDark.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .background(subCellBg, RoundedCornerShape(12.dp))
            .border(1.dp, if (isLight) Color(0xFFE2E8F0) else GlassBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = labelColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text(currentVal, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            val arrow = if (isBetter) "▲" else "▼"
            val arrowColor = if (isBetter) WinGreen else LossRed
            Text(
                text = "$arrow " + (if (changePercent != null) String.format(Locale.US, "%.1f%%", changePercent) else "0.0%"),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = arrowColor
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text("vs prev", fontSize = 8.sp, color = labelColor)
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        Divider(color = if (isLight) Color(0xFFE2E8F0) else GlassBorder, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(4.dp))
        
        Text("INDUSTRY BENCH", fontSize = 7.sp, fontWeight = FontWeight.SemiBold, color = labelColor)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(industryStandard, fontSize = 10.sp, color = textColor, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(3.dp))
            val badgeColor = if (isIndustryBetter) WinGreen else LossRed
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(badgeColor, CircleShape)
            )
        }
    }
}

@Composable
fun TradezellaQuickStats(trades: List<Trade>) {
    val isLight = MaterialTheme.colorScheme.background != SlateDark
    val textColor = if (isLight) Color(0xFF0B0E14) else Color.White
    val labelColor = if (isLight) Color(0xFF64748B) else Color.Gray

    val wins = trades.filter { it.outcome.uppercase() == "WIN" }
    val losses = trades.filter { it.outcome.uppercase() == "LOSS" }
    val avgWin = if (wins.isNotEmpty()) wins.sumOf { it.pnl } / wins.size else 0.0
    val avgLoss = if (losses.isNotEmpty()) losses.sumOf { it.pnl } / losses.size else 0.0
    val largestWin = if (wins.isNotEmpty()) wins.maxOf { it.pnl } else 0.0
    val largestLoss = if (losses.isNotEmpty()) losses.minOf { it.pnl } else 0.0
    val totalVolume = trades.sumOf { it.lotSize }
    val expectancy = if (trades.isNotEmpty()) trades.sumOf { it.pnl } / trades.size else 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphism()
            .padding(16.dp)
    ) {
        Text(
            text = "TRADEZELLA INSIGHT HARVEST",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            letterSpacing = 1.2.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                StatRow("Avg. Win Trade", String.format("$%,.2f", avgWin), WinGreen)
                Spacer(modifier = Modifier.height(6.dp))
                StatRow("Avg. Loss Trade", String.format("$%,.2f", avgLoss), LossRed)
                Spacer(modifier = Modifier.height(6.dp))
                StatRow("Expectancy / Trade", String.format("$%,.2f", expectancy), if (expectancy >= 0) WinGreen else LossRed)
            }
            
            Column(modifier = Modifier.weight(1f)) {
                StatRow("Largest Gain", String.format("$%,.2f", largestWin), WinGreen)
                Spacer(modifier = Modifier.height(6.dp))
                StatRow("Largest Drawdown", String.format("$%,.2f", largestLoss), LossRed)
                Spacer(modifier = Modifier.height(6.dp))
                StatRow("Cumulative Lots", String.format("%.2f Lots", totalVolume), BrandBlue)
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String, valueColor: Color) {
    val isLight = MaterialTheme.colorScheme.background != SlateDark
    val labelColor = if (isLight) Color(0xFF64748B) else Color.Gray

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = labelColor, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 12.sp, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

// Generate exact calendar month list structure
fun getDaysInMonthGrid(year: Int, month: Int): List<Date?> {
    val calendar = GregorianCalendar(year, month, 1)
    // Adjust calendar day of week logic to suit monday first grid nicely or standard sunday first.
    // Sunday in Calendar is 1. Saturday is 7. We want Sunday to match the 7th column, Mon first column!
    // Or standard Sunday-first column matching index. Let's do Standard Sunday first.
    val dayOfWeekOffset = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 (Sun) to 6 (Sat)
    val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val list = mutableListOf<Date?>()
    for (i in 0 until dayOfWeekOffset) {
        list.add(null)
    }
    for (i in 1..totalDays) {
        val currCal = GregorianCalendar(year, month, i)
        list.add(currCal.time)
    }
    return list
}

// ==========================================
// SCREEN 2: CHRONOLOGICAL LEDGER
// ==========================================
@Composable
fun LedgerTab(
    trades: List<Trade>,
    onTradeClick: (Trade) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterOutcome by remember { mutableStateOf("ALL") }

    val outcomesList = listOf("ALL", "WIN", "LOSS", "P_BE", "PENDING")

    // Filter and search computation
    val filteredTrades = remember(trades, searchQuery, selectedFilterOutcome) {
        trades.filter { tr ->
            val matchSearch = tr.instrument.contains(searchQuery, ignoreCase = true) ||
                    tr.setupName.contains(searchQuery, ignoreCase = true) ||
                    tr.tags.contains(searchQuery, ignoreCase = true)

            val matchFilter = when (selectedFilterOutcome) {
                "ALL" -> true
                "WIN" -> tr.outcome.uppercase() == "WIN"
                "LOSS" -> tr.outcome.uppercase() == "LOSS"
                "P_BE" -> tr.outcome.uppercase() == "BE" || tr.outcome.uppercase() == "BREAK-EVEN"
                "PENDING" -> tr.outcome.uppercase() == "PENDING"
                else -> true
            }
            matchSearch && matchFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Search textfield
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search setups, tag or asset...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = BrandBlue) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = Color.Gray)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = BrandBlue,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = SurfaceDark.copy(alpha = 0.5f),
                unfocusedContainerColor = SurfaceDark.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ledger_search_field")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Custom chip outcome tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            outcomesList.forEach { filter ->
                val displayLabel = when (filter) {
                    "P_BE" -> "BREAK-EVEN"
                    else -> filter
                }
                val isSelected = selectedFilterOutcome == filter
                val chipColor = if (isSelected) BrandBlue else SurfaceDark
                val textColor = if (isSelected) Color.Black else Color.Gray

                Box(
                    modifier = Modifier
                        .background(chipColor, RoundedCornerShape(20.dp))
                        .border(1.dp, if (isSelected) BrandBlue else GlassBorder, RoundedCornerShape(20.dp))
                        .clickable { selectedFilterOutcome = filter }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("ledger_filter_${filter.lowercase()}")
                ) {
                    Text(
                        text = displayLabel,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trades scroll container
        Text(
            text = "CHRONOLOGICAL LEDGER (${filteredTrades.size} RECORDED)",
            fontSize = 11.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredTrades.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .glassmorphism()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.HourglassEmpty,
                        contentDescription = "Empty",
                        tint = Color.LightGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No corresponding journals found matching filters.",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTrades) { trade ->
                    TradeItemCard(
                        trade = trade,
                        onItemClick = { onTradeClick(trade) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp)) // Fab cushioning
                }
            }
        }
    }
}

// Single item block helper
@Composable
fun TradeItemCard(
    trade: Trade,
    onItemClick: () -> Unit
) {
    val outcomeColor = when (trade.outcome.uppercase()) {
        "WIN" -> WinGreen
        "LOSS" -> LossRed
        "BREAK-EVEN", "BE" -> BreakEvenGray
        else -> Color.Yellow
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphism()
            .clickable { onItemClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Header Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = trade.instrument,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Direction label
                val dirColor = if (trade.direction.uppercase() == "BUY") BrandBlue else LossRed
                Box(
                    modifier = Modifier
                        .background(dirColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = trade.direction.uppercase(),
                        color = dirColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Sub text
            Text(
                text = "${trade.setupName} • Grade ${trade.grade}/10",
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (trade.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    trade.tags.split(",").forEach { tag ->
                        if (tag.trim().isNotBlank()) {
                            Text(
                                text = "#${tag.trim()}",
                                fontSize = 9.sp,
                                color = BrandBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Outcome pricing & actions
        Column(horizontalAlignment = Alignment.End) {
            val sign = if (trade.pnl >= 0) "+" else ""
            Text(
                text = "$sign${String.format("$%,.2f", trade.pnl)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = outcomeColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${trade.dateStr}  ${trade.timeStr}",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

// ==========================================
// SCREEN 3: SECURITY & BACKUP PIN SETTINGS
// ==========================================
@Composable
fun SettingsTab(
    settings: AppSettings?,
    workspaces: List<Workspace>,
    viewModel: JournalViewModel
) {
    val context = LocalContext.current
    val isLight = MaterialTheme.colorScheme.background != SlateDark
    val textColor = if (isLight) Color(0xFF0B0E14) else Color.White
    val labelColor = if (isLight) Color(0xFF64748B) else Color.Gray

    // Local mutable state matching the settings
    val database = settings ?: AppSettings(1, 0L, null, false)
    var isBiometricEnabled by remember(database) { mutableStateOf(database.isBiometricEnabled) }
    var inputPinCode by remember(database) { mutableStateOf(database.backupPinCode ?: "") }

    var showPinInputSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Branding section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphism()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(BrandBlue.copy(alpha = 0.1f), CircleShape)
                    .border(2.dp, BrandBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Ashiblade Portal Shield", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Secure local ledger authentication settings", fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "LOCAL SECURED CRYPTO-BIOMETRICS",
            fontSize = 11.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphism()
                .padding(16.dp)
        ) {
            // Pin switch item
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Local Pin Access Lock",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (database.backupPinCode.isNullOrEmpty()) "Not Configured" else "Shield Active",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { showPinInputSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = if (database.backupPinCode.isNullOrEmpty()) BrandBlue else Color.DarkGray),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (database.backupPinCode.isNullOrEmpty()) "Set PIN" else "Change PIN",
                        color = if (database.backupPinCode.isNullOrEmpty()) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Biometric Shield lock toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Biometric Security Shield",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Prompt native face/fingerprint lock upon resuming",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = isBiometricEnabled,
                    onCheckedChange = { checked ->
                        // Biometric requires backup PIN code coordinates first to protect against lockouts
                        if (database.backupPinCode.isNullOrEmpty()) {
                            Toast.makeText(context, "Configure a local backup PIN first to activate Biometrics!", Toast.LENGTH_LONG).show()
                            showPinInputSheet = true
                        } else {
                            isBiometricEnabled = checked
                            viewModel.updateSecurity(checked, database.backupPinCode)
                            Toast.makeText(context, "Biometric Shield updated!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = BrandBlue,
                        uncheckedTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.testTag("biometric_toggle")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "PORTFOLIO DIRECTORY METRICS",
            fontSize = 11.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Display current directory stats info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphism()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Portfolios", color = labelColor, fontSize = 13.sp)
                Text("${workspaces.size}", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Storage State", color = labelColor, fontSize = 13.sp)
                Text("Local SQLite Encrypted", color = BrandBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("App Version", color = labelColor, fontSize = 13.sp)
                Text("v1.0.4-synthetic", color = textColor, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "THEME DESIGN SYSTEM",
            fontSize = 11.sp,
            color = labelColor,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphism()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Ashiblade Dark Theme",
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Toggle light mode and professional luxury dark backdrop",
                        color = labelColor,
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = database.isDarkMode,
                    onCheckedChange = { checked ->
                        viewModel.toggleTheme(checked)
                        Toast.makeText(context, "Theme style updated to ${if (checked) "Dark" else "Light"}!", Toast.LENGTH_SHORT).show()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = BrandBlue,
                        uncheckedTrackColor = Color.DarkGray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "BASELINE BALANCE DESIGNATION",
            fontSize = 11.sp,
            color = labelColor,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphism()
                .padding(16.dp)
        ) {
            var balanceText by remember(database) { mutableStateOf(database.startBalance.toString()) }
            
            Text(
                "Specify your initial starting capital baseline to compute portfolio growth.",
                color = labelColor,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("Value in USD, e.g. 10000.0", color = labelColor) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = labelColor
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val dVal = balanceText.toDoubleOrNull()
                        if (dVal != null) {
                            viewModel.updateStartBalance(dVal)
                            Toast.makeText(context, "Baseline balance updated to \$${String.format("%.2f", dVal)}!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter a valid numeric amount!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("SET", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "CUSTOMIZABLE REAL-TIME ALERTS",
            fontSize = 11.sp,
            color = labelColor,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphism()
                .padding(16.dp)
        ) {
            var isPnlEnabled by remember(database) { mutableStateOf(database.isAlertPnlEnabled) }
            var pnlText by remember(database) { mutableStateOf(database.alertPnlThreshold.toString()) }
            
            var isWinRateEnabled by remember(database) { mutableStateOf(database.isAlertWinRateEnabled) }
            var winRateText by remember(database) { mutableStateOf(database.alertWinRateThreshold.toString()) }
            
            var isConsecutiveLossesEnabled by remember(database) { mutableStateOf(database.isAlertConsecutiveLossesEnabled) }
            var lossesText by remember(database) { mutableStateOf(database.alertConsecutiveLossesThreshold.toString()) }

            Text(
                "Establish risk and performance boundary alerts with live pushed overlays.",
                color = labelColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isPnlEnabled,
                    onCheckedChange = { isPnlEnabled = it },
                    colors = CheckboxDefaults.colors(checkedColor = BrandBlue)
                )
                Text("Trade P&L Threshold Limit: $", color = textColor, fontSize = 13.sp, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = pnlText,
                    onValueChange = { pnlText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(90.dp).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = labelColor
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isWinRateEnabled,
                    onCheckedChange = { isWinRateEnabled = it },
                    colors = CheckboxDefaults.colors(checkedColor = BrandBlue)
                )
                Text("Win Rate Target Alert: %", color = textColor, fontSize = 13.sp, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = winRateText,
                    onValueChange = { winRateText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(90.dp).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = labelColor
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isConsecutiveLossesEnabled,
                    onCheckedChange = { isConsecutiveLossesEnabled = it },
                    colors = CheckboxDefaults.colors(checkedColor = BrandBlue)
                )
                Text("Consecutive Losses Alert Count:", color = textColor, fontSize = 13.sp, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = lossesText,
                    onValueChange = { lossesText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(90.dp).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = labelColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val pnlVal = pnlText.toDoubleOrNull() ?: database.alertPnlThreshold
                    val winRateVal = winRateText.toDoubleOrNull() ?: database.alertWinRateThreshold
                    val lossVal = lossesText.toIntOrNull() ?: database.alertConsecutiveLossesThreshold
                    viewModel.updateAlertSettings(
                        isPnlEnabled = isPnlEnabled,
                        pnlThreshold = pnlVal,
                        isWinRateEnabled = isWinRateEnabled,
                        winRateThreshold = winRateVal,
                        isConsecutiveLossesEnabled = isConsecutiveLossesEnabled,
                        consecutiveLossesThreshold = lossVal
                    )
                    Toast.makeText(context, "Trade thresholds saved successfully!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("SAVE ALERT SETTINGS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Sheet popup to setup PIN
    if (showPinInputSheet) {
        var pinValue by remember { mutableStateOf("") }
        var pinConfirmValue by remember { mutableStateOf("") }
        
        Dialog(onDismissRequest = { showPinInputSheet = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphism()
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Configure Backup PIN",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "PIN ensures access to journals if biometrics fails",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) pinValue = it },
                        label = { Text("Enter PIN (4-6 digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = GlassBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pinConfirmValue,
                        onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) pinConfirmValue = it },
                        label = { Text("Confirm PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = GlassBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showPinInputSheet = false }) {
                            Text("CANCEL", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (pinValue.length < 4) {
                                    Toast.makeText(context, "PIN must be at least 4 digits!", Toast.LENGTH_SHORT).show()
                                } else if (pinValue != pinConfirmValue) {
                                    Toast.makeText(context, "PIN confirmation doesn't match!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.updateSecurity(isBiometricEnabled, pinValue)
                                    showPinInputSheet = false
                                    Toast.makeText(context, "Backup PIN Shield Configured!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                        ) {
                            Text("SAVE PIN", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN FALLBACK: LOCK OVERLAY PIN KEYPAD
// ==========================================
@Composable
fun BiometricSecurityOverlay(
    settings: AppSettings?,
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    var inputPin by remember { mutableStateOf("") }
    val correctPin = settings?.backupPinCode ?: ""

    // Native Biometric trigger logic helper
    val triggerBiometrics = {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
        val activity = context as? androidx.fragment.app.FragmentActivity
        if (activity != null && settings?.isBiometricEnabled == true) {
            val biometricPrompt = androidx.biometric.BiometricPrompt(activity, executor,
                object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        Toast.makeText(context, "Authenticated successfully", Toast.LENGTH_SHORT).show()
                        onUnlock()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // Fail gracefully. fallback to keyboard pin works
                    }
                }
            )

            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Ashiblade Diary")
                .setSubtitle("Authenticate via biometric credentials")
                .setDeviceCredentialAllowed(true) // Supports backup credentials natively
                .build()

            biometricPrompt.authenticate(promptInfo)
        }
    }

    // Automatically trigger biometrics once if active on loading
    LaunchedEffect(settings) {
        if (settings?.isBiometricEnabled == true) {
            triggerBiometrics()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = "Secured",
                tint = BrandBlue,
                modifier = Modifier
                    .size(64.dp)
                    .clickable { triggerBiometrics() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "ASHIBLADE PRIVATE JOURNAL",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Text(
                "Biometric encryption active. Enter backup PIN passcode.",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Display PIN dots progress
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..6) {
                    val filled = inputPin.length >= i
                    val dotColor = if (filled) BrandBlue else Color.DarkGray
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(dotColor, CircleShape)
                            .border(1.dp, GlassBorder, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 3x4 custom touch layout target grid keypad (comply with standard 48dp sizes)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val padKeys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("FACE/FP", "0", "DEL")
                )

                padKeys.forEach { rowKeys ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowKeys.forEach { key ->
                            val isSpecial = key == "FACE/FP" || key == "DEL"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.2f)
                                    .background(if (isSpecial) Color.Transparent else SurfaceDark, RoundedCornerShape(12.dp))
                                    .border(1.dp, if (isSpecial) Color.Transparent else GlassBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        when (key) {
                                            "DEL" -> {
                                                if (inputPin.isNotEmpty()) {
                                                    inputPin = inputPin.dropLast(1)
                                                }
                                            }
                                            "FACE/FP" -> {
                                                triggerBiometrics()
                                            }
                                            else -> {
                                                if (inputPin.length < 6) {
                                                    inputPin += key
                                                    // Immediately check Pin configuration
                                                    if (inputPin == correctPin) {
                                                        onUnlock()
                                                    } else if (inputPin.length == correctPin.length && correctPin.isNotEmpty()) {
                                                        Toast.makeText(context, "Incorrect PIN code!", Toast.LENGTH_SHORT).show()
                                                        inputPin = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "DEL") {
                                    Icon(Icons.Filled.Backspace, contentDescription = "Delete", tint = Color.LightGray)
                                } else if (key == "FACE/FP") {
                                    Icon(Icons.Filled.Fingerprint, contentDescription = "Biometrics trigger", tint = BrandBlue)
                                } else {
                                    Text(
                                        text = key,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// MODAL DIALOGS AND WORKSHEETS HELPERS
// ==========================================

@Composable
fun WorkspaceManagerDialog(
    workspaces: List<Workspace>,
    activeWorkspaceId: Long,
    onCreateWorkspace: (String) -> Unit,
    onRenameWorkspace: (Long, String) -> Unit,
    onDeleteWorkspace: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var isCreatingNew by remember { mutableStateOf(false) }
    var renameTargetId by remember { mutableStateOf<Long?>(null) }
    var workspaceNameInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphism()
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Manage WS Calendars",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isCreatingNew || renameTargetId != null) {
                    Text(
                        if (isCreatingNew) "CREATE NEW WORKSPACE" else "RENAME PORTFOLIO",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = workspaceNameInput,
                        onValueChange = { workspaceNameInput = it },
                        placeholder = { Text("Portfolio Designation...", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = GlassBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            isCreatingNew = false
                            renameTargetId = null
                            workspaceNameInput = ""
                        }) {
                            Text("CANCEL", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (workspaceNameInput.isNotBlank()) {
                                    if (isCreatingNew) {
                                        onCreateWorkspace(workspaceNameInput)
                                    } else {
                                        onRenameWorkspace(renameTargetId!!, workspaceNameInput)
                                    }
                                    isCreatingNew = false
                                    renameTargetId = null
                                    workspaceNameInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                        ) {
                            Text("SAVE", color = Color.Black)
                        }
                    }
                } else {
                    Button(
                        onClick = { isCreatingNew = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add New Workspace", color = Color.Black)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        items(workspaces) { ws ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ws.name,
                                        color = if (ws.id == activeWorkspaceId) BrandBlue else Color.White,
                                        fontWeight = if (ws.id == activeWorkspaceId) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }

                                Row {
                                    IconButton(onClick = {
                                        renameTargetId = ws.id
                                        workspaceNameInput = ws.name
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Rename", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                    
                                    // Protect the last workspace from being deleted
                                    if (workspaces.size > 1) {
                                        IconButton(onClick = { onDeleteWorkspace(ws.id) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = LossRed, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Detailed Trade logger forms
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TradeFormDialog(
    trade: Trade?,
    onSave: (
        dateStr: String,
        timeStr: String,
        instrument: String,
        direction: String,
        entryPrice: Double,
        stopLoss: Double,
        takeProfit: Double,
        lotSize: Double,
        pnl: Double,
        outcome: String,
        photoUri: String?,
        grade: Int,
        setupName: String,
        tags: String,
        emotions: String,
        entryExecutionNotes: String,
        reasoningNotes: String,
        confirmationNotes: String,
        takeawayNotes: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Initialize local fields with original values if editing
    var dateVal by remember { mutableStateOf(trade?.dateStr ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var timeVal by remember { mutableStateOf(trade?.timeStr ?: SimpleDateFormat("HH:mm", Locale.US).format(Date())) }
    var selectedInstrument by remember { mutableStateOf(trade?.instrument ?: Instruments.listAll.first()) }
    var selectedDirection by remember { mutableStateOf(trade?.direction ?: "Buy") }
    
    var entryPriceVal by remember { mutableStateOf(trade?.entryPrice?.toString() ?: "") }
    var stopLossVal by remember { mutableStateOf(trade?.stopLoss?.toString() ?: "") }
    var takeProfitVal by remember { mutableStateOf(trade?.takeProfit?.toString() ?: "") }
    var lotSizeVal by remember { mutableStateOf(trade?.lotSize?.toString() ?: "") }
    var pnlVal by remember { mutableStateOf(trade?.pnl?.toString() ?: "") }
    
    var selectedOutcome by remember { mutableStateOf(trade?.outcome ?: "Win") }
    var attachedPhotoUri by remember { mutableStateOf(trade?.photoUri) }
    var executionGrade by remember { mutableStateOf(trade?.grade ?: 5) }
    
    var setupNameVal by remember { mutableStateOf(trade?.setupName ?: "") }
    var tagsVal by remember { mutableStateOf(trade?.tags ?: "") }
    
    val allSelectedEmotions = remember { mutableStateListOf<String>() }
    
    LaunchedEffect(trade) {
        if (trade != null) {
            allSelectedEmotions.clear()
            trade.emotions.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                allSelectedEmotions.add(it)
            }
        }
    }

    var entryNotesVal by remember { mutableStateOf(trade?.entryExecutionNotes ?: "") }
    var reasoningNotesVal by remember { mutableStateOf(trade?.reasoningNotes ?: "") }
    var confirmationNotesVal by remember { mutableStateOf(trade?.confirmationNotes ?: "") }
    var takeawayNotesVal by remember { mutableStateOf(trade?.takeawayNotes ?: "") }

    var expandedInstrumentDropdown by remember { mutableStateOf(false) }

    // Image Picker connection launcher
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                // Copy the picture safely into local sandboxed directories to guarantee persistence
                val absoluteSavedPath = copyUriToInternalStorage(context, uri)
                if (absoluteSavedPath != null) {
                    attachedPhotoUri = absoluteSavedPath
                    Toast.makeText(context, "Chart attached successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error reading image file.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .glassmorphism()
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (trade == null) "Log New Metric Entry" else "Edit Trade Log",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable workspace container
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date & Time picker selections
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                val dpd = DatePickerDialog(context, { _, year, month, dayOfMonth ->
                                    dateVal = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
                                dpd.show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(dateVal, color = Color.White, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                val tpd = TimePickerDialog(context, { _, hourOfDay, minute ->
                                    timeVal = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
                                tpd.show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(timeVal, color = Color.White, fontSize = 12.sp)
                        }
                    }

                    // Instrument Selector Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedInstrument,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Synthetic Index") },
                            trailingIcon = {
                                IconButton(onClick = { expandedInstrumentDropdown = !expandedInstrumentDropdown }) {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown", tint = BrandBlue)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = GlassBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = expandedInstrumentDropdown,
                            onDismissRequest = { expandedInstrumentDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .background(SurfaceDark)
                                .heightIn(max = 300.dp)
                        ) {
                            Instruments.listAll.forEach { inst ->
                                DropdownMenuItem(
                                    text = { Text(inst, color = Color.White) },
                                    onClick = {
                                        selectedInstrument = inst
                                        expandedInstrumentDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Direction Selector (Buy vs Sell toggles)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Direction:", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        
                        Row(
                            modifier = Modifier
                                .background(SurfaceDark, RoundedCornerShape(12.dp))
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(if (selectedDirection == "Buy") ElectricBlue else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedDirection = "Buy" }
                                    .padding(horizontal = 24.dp, vertical = 6.dp)
                            ) {
                                Text("BUY", color = if (selectedDirection == "Buy") Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .background(if (selectedDirection == "Sell") LossRed else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedDirection = "Sell" }
                                    .padding(horizontal = 24.dp, vertical = 6.dp)
                            ) {
                                Text("SELL", color = if (selectedDirection == "Sell") Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    // Numeric Metrics Grid Row Fields
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = entryPriceVal,
                            onValueChange = { entryPriceVal = it },
                            label = { Text("Entry Price") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = GlassBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = lotSizeVal,
                            onValueChange = { lotSizeVal = it },
                            label = { Text("Lot Size") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = GlassBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = stopLossVal,
                            onValueChange = { stopLossVal = it },
                            label = { Text("Stop Loss (SL)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = GlassBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = takeProfitVal,
                            onValueChange = { takeProfitVal = it },
                            label = { Text("Take Profit (TP)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = GlassBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // P&L and Outcome selection triggers
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pnlVal,
                            onValueChange = { newVal ->
                                pnlVal = newVal
                                // Intelligent outcome guessing
                                val dPnl = newVal.toDoubleOrNull()
                                if (dPnl != null) {
                                    if (dPnl > 0.0) selectedOutcome = "Win"
                                    else if (dPnl < 0.0) selectedOutcome = "Loss"
                                    else selectedOutcome = "Break-Even"
                                }
                            },
                            label = { Text("Profit / Loss ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = GlassBorder
                            ),
                            modifier = Modifier.weight(1.3f)
                        )

                        // Outcome static toggle strip
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Outcome", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .background(SurfaceDark, RoundedCornerShape(8.dp))
                                    .padding(2.dp)
                            ) {
                                val outcomes = listOf("Win", "Loss", "BE", "Pending")
                                outcomes.chunked(2).forEach { chunk ->
                                    Column {
                                        chunk.forEach { outc ->
                                            val isAct = selectedOutcome.lowercase() == outc.lowercase()
                                            val outcBg = if (isAct) {
                                                when (outc) {
                                                    "Win" -> WinGreen
                                                    "Loss" -> LossRed
                                                    "BE" -> BreakEvenGray
                                                    else -> Color.Yellow
                                                }
                                            } else Color.Transparent

                                            Box(
                                                modifier = Modifier
                                                    .background(outcBg, RoundedCornerShape(4.dp))
                                                    .clickable { selectedOutcome = outc }
                                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    outc.uppercase(),
                                                    color = if (isAct) Color.Black else Color.Gray,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Native Image picker element
                    Column {
                        Text("Technical Setup Analysis", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        if (attachedPhotoUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = attachedPhotoUri,
                                    contentDescription = "Attached analysis",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Clear selection button
                                IconButton(
                                    onClick = { attachedPhotoUri = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                    .clickable { photoPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = BrandBlue)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pick Setup Chart Screenshot", color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Execution discipline scale slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Execution Discipline Grade", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("$executionGrade/10", color = BrandBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = executionGrade.toFloat(),
                            onValueChange = { executionGrade = it.toInt() },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = BrandBlue,
                                activeTrackColor = BrandBlue,
                                inactiveTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.testTag("discipline_grade_slider")
                        )
                    }

                    // Qualitative Notes details
                    Text("QUALITATIVE PSYCH & ANALYSIS", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = setupNameVal,
                        onValueChange = { setupNameVal = it },
                        label = { Text("Custom Setup Title / Pattern Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = GlassBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tagsVal,
                        onValueChange = { tagsVal = it },
                        label = { Text("Tags (comma separated elements)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = GlassBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Emotional state toggle selections
                    Column {
                        Text("Active Mindset / Emotions:", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val moods = listOf("Confident", "Calm", "Frustrated", "Anxious", "Neutral")
                            moods.forEach { md ->
                                val selected = allSelectedEmotions.contains(md)
                                val colorsBg = if (selected) BrandBlue else SurfaceDark
                                val colorsText = if (selected) Color.Black else Color.Gray

                                Box(
                                    modifier = Modifier
                                        .background(colorsBg, RoundedCornerShape(20.dp))
                                        .border(1.dp, if (selected) BrandBlue else GlassBorder, RoundedCornerShape(20.dp))
                                        .clickable {
                                            if (selected) allSelectedEmotions.remove(md)
                                            else allSelectedEmotions.add(md)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(md, color = colorsText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Structured notes paragraphs fields
                    Text("DIARY DETAIL TEXT AREAS", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = entryNotesVal,
                        onValueChange = { entryNotesVal = it },
                        label = { Text("Entry execution process") },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = GlassBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = reasoningNotesVal,
                        onValueChange = { reasoningNotesVal = it },
                        label = { Text("Reasoning / Thesis") },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = GlassBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmationNotesVal,
                        onValueChange = { confirmationNotesVal = it },
                        label = { Text("Confirmation parameters used") },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = GlassBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = takeawayNotesVal,
                        onValueChange = { takeawayNotesVal = it },
                        label = { Text("Lesson / Takeaway") },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = GlassBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom toolbar action save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ABORT", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val entryPriceD = entryPriceVal.toDoubleOrNull() ?: 0.0
                            val stopLossD = stopLossVal.toDoubleOrNull() ?: 0.0
                            val takeProfitD = takeProfitVal.toDoubleOrNull() ?: 0.0
                            val lotSizeD = lotSizeVal.toDoubleOrNull() ?: 0.0
                            val pnlD = pnlVal.toDoubleOrNull() ?: 0.0

                            onSave(
                                dateVal,
                                timeVal,
                                selectedInstrument,
                                selectedDirection,
                                entryPriceD,
                                stopLossD,
                                takeProfitD,
                                lotSizeD,
                                pnlD,
                                selectedOutcome,
                                attachedPhotoUri,
                                executionGrade,
                                setupNameVal,
                                tagsVal,
                                allSelectedEmotions.joinToString(", "),
                                entryNotesVal,
                                reasoningNotesVal,
                                confirmationNotesVal,
                                takeawayNotesVal
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        modifier = Modifier.testTag("save_trade_button")
                    ) {
                        Text("SAVE METRICS", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Full detailed single-view trade modal dialog sheet
@Composable
fun TradeDetailsDialog(
    trade: Trade,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val outcomeColor = when (trade.outcome.uppercase()) {
        "WIN" -> WinGreen
        "LOSS" -> LossRed
        "BREAK-EVEN", "BE" -> BreakEvenGray
        else -> Color.Yellow
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .glassmorphism()
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Journal #${trade.id}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scroll View contents
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDark, RoundedCornerShape(12.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(trade.instrument, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val dirColor = if (trade.direction.uppercase() == "BUY") BrandBlue else LossRed
                            Text(trade.direction.uppercase(), color = dirColor, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("•  ${trade.dateStr} at ${trade.timeStr}", color = Color.Gray, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("RESULT", fontSize = 13.sp, color = Color.Gray)
                            Text(
                                String.format("$%,.2f", trade.pnl),
                                color = outcomeColor,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Properties grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TradePropertyLine("Entry Price", String.format("%.4f", trade.entryPrice))
                        TradePropertyLine("Stop Loss (SL)", if (trade.stopLoss > 0) String.format("%.4f", trade.stopLoss) else "No SL")
                        TradePropertyLine("Take Profit (TP)", if (trade.takeProfit > 0) String.format("%.4f", trade.takeProfit) else "No TP")
                        TradePropertyLine("Lot sizing", trade.lotSize.toString())
                        TradePropertyLine("Outcome Status", trade.outcome.uppercase())
                        TradePropertyLine("Discipline Grade", "${trade.grade} / 10")
                    }

                    // Attached screenshot display
                    if (!trade.photoUri.isNullOrBlank()) {
                        Column {
                            Text("Technical Setup Analysis", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = trade.photoUri,
                                    contentDescription = "Technical Chart",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Mindset & emotional tags
                    if (trade.emotions.isNotBlank()) {
                        Column {
                            Text("Emotional Markers", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                trade.emotions.split(",").forEach { mood ->
                                    if (mood.trim().isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .background(ElectricBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(mood.trim(), color = BrandBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Detailed Notes parts
                    if (trade.entryExecutionNotes.isNotBlank()) {
                        TradeNoteBlock("Entry execution process", trade.entryExecutionNotes)
                    }
                    if (trade.reasoningNotes.isNotBlank()) {
                        TradeNoteBlock("Reasoning / Thesis", trade.reasoningNotes)
                    }
                    if (trade.confirmationNotes.isNotBlank()) {
                        TradeNoteBlock("Confirmation parameters used", trade.confirmationNotes)
                    }
                    if (trade.takeawayNotes.isNotBlank()) {
                        TradeNoteBlock("Lesson / Takeaway", trade.takeawayNotes)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom actions buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = LossRed)
                    }

                    Row {
                        IconButton(onClick = onEditClick) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color.LightGray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onShareClick,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Clean Slate", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TradePropertyLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TradeNoteBlock(label: String, note: String) {
    Column {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = note,
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(8.dp))
                .padding(12.dp)
        )
    }
}
