package com.paddlebike.kenandrews.riverwatch

import com.paddlebike.kenandrews.riverwatch.GaugeConstants.Companion.GAUGE_LEVEL
import com.paddlebike.kenandrews.riverwatch.data.USGSTimeSeries
import com.paddlebike.kenandrews.riverwatch.database.USGSGauge
import org.joda.time.format.DateTimeFormat
import org.junit.Test


/**
 * Class for testing fetch of USGS gauge data
 */
class TestFetchUSGSSite {



    @Test
    fun getsGauge() {
        val response = USGSSite.fetch("01646500")
        assert(response is USGSSite.Site)
        println(response)
    }

    @Test
    fun reportsAge() {
        val response = USGSSite.fetch("01646500")
        assert(response is USGSSite.Site)
        println(response)
        val age = response.getAge()
        assert(age != -1L)
        val seconds = age / 1000
        println(String.format("Age is %d seconds", seconds))
        println(String.format("Or %d minutes", seconds/60))
        println(String.format("Or %d hours", seconds/(60*60)))
    }

    @Test
    fun reportsTimeStamp() {
        val site: USGSSite.Site = USGSSite.fetch("01646500")
        println(site.getTimeStamp().toLocalTime().toString())
        val formatter = DateTimeFormat.forPattern("HH:mm")
        println(site.getTimeStamp().toString(formatter))
    }

    @Test
    fun fetchToStringReturnsAString() {
        val response = USGSSite.fetchToString("01646500")
        assert(response is String)
        println(response)
    }



    @Test
    fun kotlinxParser() {
        val jsonString = USGSSite.fetchToString("01646500")
        println(jsonString)
        val ts = USGSSite.parseJSONtoUSGSTimeSeries(jsonString)
        assert(ts is USGSTimeSeries)
        println(ts)

        for (gauge in ts?.value!!.timeSeries) {
            val siteName = gauge.sourceInfo.siteName
            val siteID = gauge.name

            for (item in gauge.values) {
                if (item.value.isNotEmpty()) {
                    val readingTime = item.value.last().dateTime
                    val readingValue = item.value.last().value
                    val usgsGauge = USGSGauge(siteName, siteID, readingTime, readingValue)
                    println("Gauge $usgsGauge")
                    break
                }
            }
        }
        println("Done!!")
    }

    @Test
    fun newFetchAndParse() {
        val site = USGSSite.fetchSite("01646500")

        val level = site["USGS:01646500:$GAUGE_LEVEL:00000"]

        assert(level != null)
        println("${level!!.gaugeId} level was ${level.reading} at ${level.date}")
        println("Done!!")
    }
}