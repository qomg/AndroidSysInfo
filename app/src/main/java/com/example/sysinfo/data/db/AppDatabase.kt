package com.example.sysinfo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.sysinfo.data.model.CellTower

@Database(entities = [CellTower::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cellTowerDao(): CellTowerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun get(ctx: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(ctx, AppDatabase::class.java, "sysinfo.db")
                .fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}