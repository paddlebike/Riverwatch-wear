package com.paddlebike.kenandrews.riverwatch.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface USGSGaugeDao {
    @Query("SELECT * FROM USGSGauge ORDER BY date")
    fun getAll(): List<USGSGauge>

    @Query("SELECT * FROM USGSGauge WHERE gaugeId = id ORDER BY date DESC LIMIT 1")
    fun getGauge(id: String): USGSGauge

    @Insert
    fun insertAll(vararg gauges: USGSGauge)

    @Delete
    fun delete(gauge: USGSGauge)
}