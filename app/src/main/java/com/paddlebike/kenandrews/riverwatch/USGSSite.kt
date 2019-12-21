package com.paddlebike.kenandrews.riverwatch

import android.util.Log
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.paddlebike.kenandrews.riverwatch.GaugeConstants.Companion.GAUGE_FLOW
import com.paddlebike.kenandrews.riverwatch.GaugeConstants.Companion.GAUGE_LEVEL
import com.paddlebike.kenandrews.riverwatch.GaugeConstants.Companion.GAUGE_TEMP
import com.paddlebike.kenandrews.riverwatch.data.USGSTimeSeries
import com.paddlebike.kenandrews.riverwatch.data.parseUSGSTimeSeries
import com.paddlebike.kenandrews.riverwatch.database.USGSGauge
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.net.URL

class GaugeConstants {
    companion object {
        const val GAUGE_TEMP  = "00010"
        const val GAUGE_LEVEL = "00065"
        const val GAUGE_FLOW  = "00060"

        const val ERROR_VALUE = -999999.00F
    }
}

private const val TAG = "USGSSite"
/**
 * Class for fetching USGS stream gauge data
 */
class USGSSite {

    companion object {
        fun fetchToString(siteId: String) : String {
            Log.d(TAG, "Fetching the gauge data for: $siteId")
            val response = URL("https://waterservices.usgs.gov/nwis/iv/?&period=P1D&format=json&parameterCd=00065,00060,00010&sites=$siteId").openStream()
            val klx = Parser().parse(response) as? JsonObject ?:
            throw ExceptionInInitializerError("No response data")
            return klx.toJsonString(false)
        }



        fun parseJSONtoUSGSTimeSeries(jsonString: String): USGSTimeSeries? = parseUSGSTimeSeries(jsonString)

        fun fetchSite(siteId: String): HashMap<String,USGSGauge> {
            val jsonString = fetchToString(siteId)
            val ts = parseUSGSTimeSeries(jsonString)

            val  site: HashMap<String,USGSGauge> = hashMapOf()

            for (gauge in ts.value.timeSeries) {
                val siteName = gauge.sourceInfo.siteName
                val siteID = gauge.name

                for (item in gauge.values) {
                    if (item.value.isNotEmpty()) {
                        val readingTime = item.value.last().dateTime
                        val readingValue = item.value.last().value
                        site[siteID] =  USGSGauge(siteName, siteID, readingTime, readingValue)
                        break
                    }
                }
            }
            return site
        }

        fun fetch(siteId: String): Site {
            val jsonString = fetchToString(siteId)
            val ts = parseUSGSTimeSeries(jsonString)


            val parameters: HashMap<String, GaugeParameter> = hashMapOf()
            var gaugeName: String? = null

            for (gauge in ts.value.timeSeries) {
                if (gaugeName == null) gaugeName = gauge.sourceInfo.siteName


                for (item in gauge.values) {
                    if (item.value.isNotEmpty()) {
                        val readingTime = item.value.last().dateTime
                        val readingValue = item.value.last().value.toFloat()
                        val variableCode = gauge.name.split(":")[2]
                        val param = GaugeParameter(readingValue, readingTime)
                        parameters[variableCode] = param
                    }
                }
            }
            return Site(siteId, gaugeName!!, parameters)
        }
    }


    data class Site(val siteId: String, val name: String,val parameters: HashMap<String,GaugeParameter>) {

        fun getValueForKey(key: String): Float {
            return  parameters[key]?.value ?: 0.0F
        }

        fun getAge(): Long {
            return when {
                parameters.containsKey(GAUGE_LEVEL) -> parameters[GAUGE_LEVEL]!!.getAge()
                parameters.containsKey(GAUGE_FLOW) -> parameters[GAUGE_LEVEL]!!.getAge()
                parameters.containsKey(GAUGE_TEMP) -> parameters[GAUGE_LEVEL]!!.getAge()
                else -> -1
            }
        }

        fun getTimeStamp() : DateTime {
            return when {
                parameters.containsKey(GAUGE_LEVEL) -> parameters[GAUGE_LEVEL]!!.getTimeStamp()
                parameters.containsKey(GAUGE_FLOW) -> parameters[GAUGE_LEVEL]!!.getTimeStamp()
                parameters.containsKey(GAUGE_TEMP) -> parameters[GAUGE_LEVEL]!!.getTimeStamp()
                else -> DateTime(-1)
            }
        }

    }


    data class GaugeParameter (val value: Float, val date: String) {
        override fun toString(): String {
            return String.format("Last value: %f %d minutes ago", value, getAge()/(1000*60))
        }

        fun getAge(): Long {
            val gaugeTime = DateTime(date)
            val currentTime = DateTime()
            val delta = currentTime.minus(gaugeTime.millis)
            return delta.millis
        }

        fun getTimeStamp() : DateTime {
            val tz = DateTimeZone.getDefault()
            return DateTime(date).withZone(tz)
        }
    }



}