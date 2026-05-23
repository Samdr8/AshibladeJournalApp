package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Instruments {
    val standardVolatility = listOf(
        "Volatility 10 Index",
        "Volatility 25 Index",
        "Volatility 30 Index",
        "Volatility 50 Index",
        "Volatility 75 Index",
        "Volatility 90 Index",
        "Volatility 100 Index"
    )
    val oneSecondVolatility = listOf(
        "Volatility 10 (1s) Index",
        "Volatility 15 (1s) Index",
        "Volatility 25 (1s) Index",
        "Volatility 30 (1s) Index",
        "Volatility 50 (1s) Index",
        "Volatility 75 (1s) Index",
        "Volatility 90 (1s) Index",
        "Volatility 100 (1s) Index",
        "Volatility 150 (1s) Index",
        "Volatility 200 (1s) Index",
        "Volatility 250 (1s) Index",
        "Volatility 300 (1s) Index"
    )
    val otherSynthetic = listOf(
        "Step Index",
        "Boom 300 Index",
        "Boom 500 Index",
        "Boom 1000 Index",
        "Crash 300 Index",
        "Crash 500 Index",
        "Crash 1000 Index",
        "Jump 10 Index",
        "Jump 25 Index",
        "Jump 50 Index",
        "Jump 75 Index",
        "Jump 100 Index"
    )
    val listAll = standardVolatility + oneSecondVolatility + otherSynthetic
}

class JournalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TradeRepository

    // Settings & security state
    val settingsState = MutableStateFlow<AppSettings?>(null)
    val isLockedState = MutableStateFlow(false)

    // Workspace & Trade state
    val workspacesState = MutableStateFlow<List<Workspace>>(emptyList())
    val activeWorkspaceIdState = MutableStateFlow<Long>(0L)
    val activeWorkspaceTradesState = MutableStateFlow<List<Trade>>(emptyList())

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TradeRepository(database.tradeDao())

        // Monitor settings
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                val current = settings ?: AppSettings(1, 0L, null, false)
                settingsState.value = current
                
                // If there's security configured, start locked
                if ((current.isBiometricEnabled || !current.backupPinCode.isNullOrEmpty()) && !isUnlockedThisSession) {
                    isLockedState.value = true
                }
            }
        }

        // Monitor workspaces
        viewModelScope.launch {
            repository.allWorkspaces.collect { workspaces ->
                workspacesState.value = workspaces
                if (workspaces.isEmpty()) {
                    // Seed a default workspace to ensure a good first UI setup
                    val newId = repository.insertWorkspace(Workspace(name = "Main Portfolio"))
                    setActiveWorkspace(newId)
                } else {
                    // Sync active workspace reference
                    val settings = settingsState.value ?: AppSettings(1, 0L, null, false)
                    var activeId = settings.activeWorkspaceId
                    if (activeId == 0L || workspaces.none { it.id == activeId }) {
                        activeId = workspaces.first().id
                        setActiveWorkspace(activeId)
                    } else {
                        activeWorkspaceIdState.value = activeId
                    }
                }
            }
        }

        // Monitor current workspace trades
        viewModelScope.launch {
            activeWorkspaceIdState.collect { id ->
                if (id > 0) {
                    repository.getTradesForWorkspace(id).collect { trades ->
                        activeWorkspaceTradesState.value = trades
                        evaluateAlerts(trades)
                    }
                } else {
                    activeWorkspaceTradesState.value = emptyList()
                }
            }
        }
    }

    companion object {
        private var isUnlockedThisSession = false
    }

    fun unlock() {
        isUnlockedThisSession = true
        isLockedState.value = false
    }

    fun lock() {
        isUnlockedThisSession = false
        isLockedState.value = true
    }

    // WORKSPACE CRUD
    fun addWorkspace(name: String) = viewModelScope.launch {
        val id = repository.insertWorkspace(Workspace(name = name))
        setActiveWorkspace(id)
    }

    fun renameWorkspace(id: Long, newName: String) = viewModelScope.launch {
        repository.updateWorkspace(Workspace(id = id, name = newName))
    }

    fun deleteWorkspace(id: Long) = viewModelScope.launch {
        val workspaces = workspacesState.value
        val toDelete = workspaces.find { it.id == id } ?: return@launch
        repository.deleteWorkspace(toDelete)
        
        // Select next active
        val remaining = workspaces.filter { it.id != id }
        if (remaining.isNotEmpty()) {
            setActiveWorkspace(remaining.first().id)
        } else {
            activeWorkspaceIdState.value = 0L
        }
    }

    fun setActiveWorkspace(id: Long) = viewModelScope.launch {
        activeWorkspaceIdState.value = id
        val currentSettings = settingsState.value ?: AppSettings(1, 0L, null, false)
        repository.saveSettings(currentSettings.copy(activeWorkspaceId = id))
    }

    // SAFETY PARAMETERS
    fun updateSecurity(isBiometricEnabled: Boolean, pinCode: String?) = viewModelScope.launch {
        val currentSettings = settingsState.value ?: AppSettings(1, 0L, null, false)
        val updated = currentSettings.copy(
            isBiometricEnabled = isBiometricEnabled,
            backupPinCode = if (pinCode.isNullOrBlank()) null else pinCode
        )
        repository.saveSettings(updated)
    }

    // TRADE CRUD
    fun addTrade(
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
    ) = viewModelScope.launch {
        val timeLong = parseDateTimeToLong(dateStr, timeStr)
        val trade = Trade(
            workspaceId = activeWorkspaceIdState.value,
            dateStr = dateStr,
            timeStr = timeStr,
            timestampLong = timeLong,
            instrument = instrument,
            direction = direction,
            entryPrice = entryPrice,
            stopLoss = stopLoss,
            takeProfit = takeProfit,
            lotSize = lotSize,
            pnl = pnl,
            outcome = outcome,
            photoUri = photoUri,
            grade = grade,
            setupName = setupName,
            tags = tags,
            emotions = emotions,
            entryExecutionNotes = entryExecutionNotes,
            reasoningNotes = reasoningNotes,
            confirmationNotes = confirmationNotes,
            takeawayNotes = takeawayNotes
        )
        repository.insertTrade(trade)
    }

    fun updateTrade(trade: Trade) = viewModelScope.launch {
        repository.updateTrade(trade)
    }

    fun deleteTrade(trade: Trade) = viewModelScope.launch {
        repository.deleteTrade(trade)
    }

    private fun parseDateTimeToLong(date: String, time: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            format.parse("$date $time")?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    // THEME & CORE BUSINESS UPDATE METHODS
    fun toggleTheme(isDarkMode: Boolean) = viewModelScope.launch {
        val currentSettings = settingsState.value ?: AppSettings(1, 0L, null, false)
        repository.saveSettings(currentSettings.copy(isDarkMode = isDarkMode))
    }

    fun updateStartBalance(balance: Double) = viewModelScope.launch {
        val currentSettings = settingsState.value ?: AppSettings(1, 0L, null, false)
        repository.saveSettings(currentSettings.copy(startBalance = balance))
    }

    fun updateAlertSettings(
        isPnlEnabled: Boolean,
        pnlThreshold: Double,
        isWinRateEnabled: Boolean,
        winRateThreshold: Double,
        isConsecutiveLossesEnabled: Boolean,
        consecutiveLossesThreshold: Int
    ) = viewModelScope.launch {
        val currentSettings = settingsState.value ?: AppSettings(1, 0L, null, false)
        lastTriggeredAlertMessage = null
        repository.saveSettings(currentSettings.copy(
            isAlertPnlEnabled = isPnlEnabled,
            alertPnlThreshold = pnlThreshold,
            isAlertWinRateEnabled = isWinRateEnabled,
            alertWinRateThreshold = winRateThreshold,
            isAlertConsecutiveLossesEnabled = isConsecutiveLossesEnabled,
            alertConsecutiveLossesThreshold = consecutiveLossesThreshold
        ))
    }

    // ALERTS ENGINE
    val activeInAppAlert = MutableStateFlow<String?>(null)
    private var lastTriggeredAlertMessage: String? = null

    fun clearActiveAlert() {
        activeInAppAlert.value = null
    }

    private fun evaluateAlerts(trades: List<Trade>) {
        val currentSettings = settingsState.value ?: return
        if (trades.isEmpty()) return

        // 1. P&L Threshold Alert
        if (currentSettings.isAlertPnlEnabled) {
            val limit = currentSettings.alertPnlThreshold
            val recentTrade = trades.firstOrNull() // Ordered by timestampLong desc
            if (recentTrade != null && recentTrade.pnl >= limit) {
                val msg = "Trade Alert: ${recentTrade.instrument} reached/exceeded P&L threshold of \$${String.format("%.2f", limit)} with a PNL of \$${String.format("%.2f", recentTrade.pnl)}!"
                triggerAlert(msg)
            }
        }

        // 2. Win Rate Alert
        if (currentSettings.isAlertWinRateEnabled) {
            val threshold = currentSettings.alertWinRateThreshold
            val winTrades = trades.count { it.outcome == "Win" }
            val completedTrades = trades.count { it.outcome == "Win" || it.outcome == "Loss" }
            if (completedTrades > 0) {
                val winRate = (winTrades.toDouble() / completedTrades) * 100.0
                if (winRate >= threshold) {
                    val msg = "Performance Alert: Your overall Win Rate reached ${String.format("%.1f", winRate)}% (Threshold: ${String.format("%.1f", threshold)}%)!"
                    triggerAlert(msg)
                }
            }
        }

        // 3. Consecutive Losses Alert
        if (currentSettings.isAlertConsecutiveLossesEnabled) {
            val threshold = currentSettings.alertConsecutiveLossesThreshold
            var consecutiveLosses = 0
            val sortedTrades = trades.sortedByDescending { it.timestampLong }
            for (t in sortedTrades) {
                if (t.outcome == "Loss") {
                    consecutiveLosses++
                    if (consecutiveLosses >= threshold) {
                        val msg = "Risk Alert: Reached consecutive losses limit of $threshold trades (Current: $consecutiveLosses losses in a row)!"
                        triggerAlert(msg)
                        break
                    }
                } else if (t.outcome == "Win") {
                    break
                }
            }
        }
    }

    private fun triggerAlert(message: String) {
        if (lastTriggeredAlertMessage == message) return
        lastTriggeredAlertMessage = message
        activeInAppAlert.value = message
        sendNativeSystemNotification(message)
    }

    private fun sendNativeSystemNotification(message: String) {
        val context = getApplication<Application>().applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        if (notificationManager != null) {
            val channelId = "ashiblade_alerts"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Ashiblade Alerts",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Customizable target alerts & threshold notifications"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Ashiblade Trade Alert")
                .setContentText(message)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            try {
                notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
            } catch (e: Exception) {
                // fallbacks safely
            }
        }
    }
}
