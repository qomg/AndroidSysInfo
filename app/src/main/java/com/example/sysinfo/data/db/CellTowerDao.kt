package com.example.sysinfo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sysinfo.data.model.CellTower

@Dao
interface CellTowerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg towers: CellTower)

    @Query("SELECT * FROM CellTower ORDER BY timestamp DESC")
    fun getAllCells(): List<CellTower>

    @Query("SELECT * FROM CellTower WHERE timestamp > :since LIMIT 50")
    fun getRecentCells(since: Long): List<CellTower>
}