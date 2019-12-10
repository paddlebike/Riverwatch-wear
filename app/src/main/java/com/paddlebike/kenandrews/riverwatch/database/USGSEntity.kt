package com.paddlebike.kenandrews.riverwatch.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

@Entity
data class USGSGauge(
        @PrimaryKey val gaugeId: String,
        @ColumnInfo val siteName: String,
        @ColumnInfo val date: DateTime,
        @ColumnInfo val reading: Float) {

    constructor(gaugeId: String,
                siteName: String,
                date: String,
                reading: String) :
            this(gaugeId,
                siteName,
                DateTime(date).withZone(DateTimeZone.getDefault()),
                reading.toFloat())

    fun getAge(): Long {
        val gaugeTime = DateTime(date)
        val currentTime = DateTime()
        val delta = currentTime.minus(gaugeTime.millis)
        return delta.millis
    }
}