package com.paddlebike.kenandrews.riverwatch

import android.util.Log
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.paddlebike.kenandrews.riverwatch.GaugeConstants.Companion.GAUGE_FLOW
import com.paddlebike.kenandrews.riverwatch.GaugeConstants.Companion.GAUGE_LEVEL
import com.paddlebike.kenandrews.riverwatch.GaugeConstants.Companion.GAUGE_TEMP
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

import java.net.URL
import java.util.*

class GaugeConstants {
    companion object {
        val GAUGE_TEMP  = "00010"
        val GAUGE_LEVEL = "00065"
        val GAUGE_FLOW  = "00060"

        val ERROR_VALUE = -999999.00F
    }
}

private const val TAG = "USGSGauge"
/**
 * Class for fetching USGS stream gauge data
 */
class USGSGuage {

    companion object {
        fun fetch(siteId: String): Site {
            Log.d(TAG, "Fetching the gauge data for: " + siteId)
            val response = URL("https://waterservices.usgs.gov/nwis/iv/?&period=P1D&format=json&parameterCd=00065,00060,00010&sites=$siteId").openStream()
            Log.d(TAG, "Got response: " + response.toString())

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


                val valuesArray = item.getValue("values") as? JsonArray<JsonObject> ?:
                        throw ExceptionInInitializerError("No values in response data")

                val myValue = valuesArray[0].getValue("value") as JsonArray<JsonObject>
                val last = myValue.last()
                val reading = last.getValue("value") as String
                val time = last.getValue("dateTime") as String

                val param = GaugeParameter(reading.toFloat(), time)

                parameters[variableCode] = param

            }

            return Site(site, siteName, parameters)
        }
    }



    class Site(val siteId: String, val name: String, val parameters: HashMap<String,GaugeParameter>) {
        override fun toString(): String {
            return String.format("Site: %s Name: %s\n%s", siteId, name, showParameters())
        }


        fun showParameters(): String {
            var outString = ""
            for (item in parameters) {
                outString += String.format("    %s : %s\n", item.key, item.value.toString())
            }
            return outString
        }


        fun getValueForKey(key: String): Float {
            return  parameters[key]?.value ?: 0.0F
        }

        fun getAge(): Long {
            when {
                parameters.containsKey(GAUGE_LEVEL) -> return parameters[GAUGE_LEVEL]!!.getAge()
                parameters.containsKey(GAUGE_FLOW) -> return parameters[GAUGE_LEVEL]!!.getAge()
                parameters.containsKey(GAUGE_TEMP) -> return parameters[GAUGE_LEVEL]!!.getAge()
                else -> return -1
            }
        }

        fun getTimeStamp() : DateTime {
            when {
                parameters.containsKey(GAUGE_LEVEL) -> return parameters[GAUGE_LEVEL]!!.getTimeStamp()
                parameters.containsKey(GAUGE_FLOW) -> return parameters[GAUGE_LEVEL]!!.getTimeStamp()
                parameters.containsKey(GAUGE_TEMP) -> return parameters[GAUGE_LEVEL]!!.getTimeStamp()
                else -> return DateTime(-1)
            }
        }

    }


    class GaugeParameter (val value: Float, val date: String) {
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