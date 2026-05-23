package com.example.data

import kotlinx.coroutines.flow.Flow

class TradeRepository(private val tradeDao: TradeDao) {
    // WORKSPACES
    val allWorkspaces: Flow<List<Workspace>> = tradeDao.getAllWorkspaces()

    suspend fun insertWorkspace(workspace: Workspace): Long {
        return tradeDao.insertWorkspace(workspace)
    }

    suspend fun updateWorkspace(workspace: Workspace) {
        tradeDao.updateWorkspace(workspace)
    }

    suspend fun deleteWorkspace(workspace: Workspace) {
        // Cascade manually
        tradeDao.deleteTradesForWorkspace(workspace.id)
        tradeDao.deleteWorkspace(workspace)
    }

    // TRADES
    fun getTradesForWorkspace(workspaceId: Long): Flow<List<Trade>> {
        return tradeDao.getTradesForWorkspace(workspaceId)
    }

    suspend fun getTradeByIdDirect(tradeId: Long): Trade? {
        return tradeDao.getTradeByIdDirect(tradeId)
    }

    fun getTradeById(tradeId: Long): Flow<Trade?> {
        return tradeDao.getTradeById(tradeId)
    }

    suspend fun insertTrade(trade: Trade): Long {
        return tradeDao.insertTrade(trade)
    }

    suspend fun updateTrade(trade: Trade) {
        tradeDao.updateTrade(trade)
    }

    suspend fun deleteTrade(trade: Trade) {
        tradeDao.deleteTrade(trade)
    }

    // SETTINGS
    val settingsFlow: Flow<AppSettings?> = tradeDao.getSettingsFlow()

    suspend fun getSettingsDirect(): AppSettings? {
        return tradeDao.getSettingsDirect()
    }

    suspend fun saveSettings(settings: AppSettings) {
        tradeDao.insertOrUpdateSettings(settings)
    }
}
