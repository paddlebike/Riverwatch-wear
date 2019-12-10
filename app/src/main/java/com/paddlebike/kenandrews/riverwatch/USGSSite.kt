package com.paddlebike.kenandrews.riverwatch

import android.util.Log
import com.beust.klaxon.Json
import com.beust.klaxon.JsonArray
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
import java.util.*
import kotlin.collections.HashMap

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

        fun parseJson(jsonString: String): Site {
            val klx = Parser().parse(jsonString.byteInputStream()) as? JsonObject ?:
            throw ExceptionInInitializerError("No response data")

            val value = klx.getValue("value") as? JsonObject ?:
            throw ExceptionInInitializerError("No value in response data")

            val ts = value.getValue("timeSeries") as? JsonArray<*> ?:  // JsonArray<JsonObject>
            throw ExceptionInInitializerError("No time series in response data")


            val parameters: HashMap<String, GaugeParameter> = hashMapOf()

            val tsVal = ts[0] as JsonObject
            val sourceInfo = tsVal.getValue("sourceInfo") as? JsonObject ?:
            throw ExceptionInInitializerError("No time sourceInfo in response data")

            val siteName = sourceInfo.getValue("siteName") as? String ?:
            throw ExceptionInInitializerError("No siteName series in response data")

            val name: String = tsVal.getValue("name") as String
            val site = name.split(":")[1]

            for (i in ts ) {
                val item = i as JsonObject
                val gaugeName: String = item.getValue("name") as String
                val variableCode = gaugeName.split(":")[2]


                val valuesArray = item.getValue("values") as? JsonArray<*> ?:
                throw ExceptionInInitializerError("No values in response data")

                for (v in valuesArray) {
                    val firstValue = v as JsonObject
                    val myValue = firstValue.getValue("value") as JsonArray<*>
                    if (myValue.isNotEmpty()) {
                        val last = myValue.last() as JsonObject
                        val reading = last.getValue("value") as String
                        val time = last.getValue("dateTime") as String

                        val param = GaugeParameter(reading.toFloat(), time)

                        parameters[variableCode] = param
                    }
                }
            }

            return Site(site, siteName, parameters)
        }


        fun fetch(siteId: String): Site {
            Log.d(TAG, "Fetching the gauge data for: $siteId")
            val response = URL("https://waterservices.usgs.gov/nwis/iv/?&period=P1D&format=json&parameterCd=00065,00060,00010&sites=$siteId").openStream()
            Log.d(TAG, "Got response: $response" )

            val klx = Parser().parse(response) as? JsonObject ?:
                    throw ExceptionInInitializerError("No response data")

            val value = klx.getValue("value") as? JsonObject ?:
                    throw ExceptionInInitializerError("No value in response data")

            val ts = value.getValue("timeSeries") as? JsonArray<JsonObject> ?:
                    throw ExceptionInInitializerError("No time series in response data")


            val parameters: HashMap<String, GaugeParameter> = hashMapOf()
            val sourceInfo = ts[0].getValue("sourceInfo") as? JsonObject ?:
                    throw ExceptionInInitializerError("No time sourceInfo in response data")

            val siteName = sourceInfo.getValue("siteName") as? String ?:
                    throw ExceptionInInitializerError("No siteName series in response data")

            val name: String = ts[0].getValue("name") as String
            val site = name.split(":")[1]

            for (item in ts) {
                val gaugeName: String = item.getValue("name") as String
                val variableCode = gaugeName.split(":")[2]


                val valuesArray = item.getValue("values") as? JsonArray<*> ?:
                        throw ExceptionInInitializerError("No values in response data")
                val firstValue = valuesArray[0] as JsonObject

                val myValue = firstValue.getValue("value") as JsonArray<*>

                if (myValue.isNotEmpty()) {
                    val last = myValue.last() as JsonObject
                    val reading = last.getValue("value") as String
                    val time = last.getValue("dateTime") as String
                    val param = GaugeParameter(reading.toFloat(), time)
                    parameters[variableCode] = param
                }
            }

            return Site(site, siteName, parameters)
        }

        fun kotlinxParse(jsonString: String): USGSTimeSeries? = parseUSGSTimeSeries(jsonString)

        fun fetchSite(siteId: String): HashMap<String,USGSGauge> {
            val jsonString = fetchToString(siteId)
            val ts = parseUSGSTimeSeries(jsonString)

            val  site: HashMap<String,USGSGauge> = hashMapOf()

            for (gauge in ts?.value!!.timeSeries) {
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