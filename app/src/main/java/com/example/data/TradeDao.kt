package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    // WORKSPACES
    @Query("SELECT * FROM workspaces ORDER BY timestamp DESC")
    fun getAllWorkspaces(): Flow<List<Workspace>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: Workspace): Long

    @Update
    suspend fun updateWorkspace(workspace: Workspace)

    @Delete
    suspend fun deleteWorkspace(workspace: Workspace)

    @Query("DELETE FROM trades WHERE workspaceId = :workspaceId")
    suspend fun deleteTradesForWorkspace(workspaceId: Long)

    // TRADES
    @Query("SELECT * FROM trades WHERE workspaceId = :workspaceId ORDER BY timestampLong DESC")
    fun getTradesForWorkspace(workspaceId: Long): Flow<List<Trade>>

    @Query("SELECT * FROM trades WHERE id = :tradeId LIMIT 1")
    suspend fun getTradeByIdDirect(tradeId: Long): Trade?

    @Query("SELECT * FROM trades WHERE id = :tradeId")
    fun getTradeById(tradeId: Long): Flow<Trade?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: Trade): Long

    @Update
    suspend fun updateTrade(trade: Trade)

    @Delete
    suspend fun deleteTrade(trade: Trade)

    // SETTINGS
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: AppSettings)
}
