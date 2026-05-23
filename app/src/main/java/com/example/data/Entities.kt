package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspaces")
data class Workspace(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "trades")
data class Trade(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: Long,
    val dateStr: String, // YYYY-MM-DD
    val timeStr: String, // HH:MM
    val timestampLong: Long, // constructed from date/time to help in calendar/by-day ordering
    val instrument: String,
    val direction: String, // "Buy" or "Sell"
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val lotSize: Double,
    val pnl: Double, // Profit & Loss in dollars
    val outcome: String, // "Win", "Loss", "Pending", "Break-Even"
    val photoUri: String? = null, // Linked screenshot uri
    val grade: Int, // 1 to 10 scale of execution discipline
    val setupName: String, // E.g., "M1 Wyckoff Spring"
    val tags: String, // Comma-separated tags
    val emotions: String, // E.g., "Confident, Calm" (comma separated)
    val entryExecutionNotes: String,
    val reasoningNotes: String,
    val confirmationNotes: String,
    val takeawayNotes: String
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1, // Single-row config
    val activeWorkspaceId: Long = 0,
    val backupPinCode: String? = null,
    val isBiometricEnabled: Boolean = false,
    val isDarkMode: Boolean = false, // Defaults to light mode
    val startBalance: Double = 10000.0,
    val alertPnlThreshold: Double = 100.0,
    val isAlertPnlEnabled: Boolean = false,
    val alertWinRateThreshold: Double = 60.0,
    val isAlertWinRateEnabled: Boolean = false,
    val alertConsecutiveLossesThreshold: Int = 3,
    val isAlertConsecutiveLossesEnabled: Boolean = false
)
