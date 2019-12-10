package com.paddlebike.kenandrews.riverwatch.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [USGSGauge::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun USGSGaugeDao(): USGSGaugeDao
}