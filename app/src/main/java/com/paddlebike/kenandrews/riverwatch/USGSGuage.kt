package com.paddlebike.kenandrews.riverwatch

import android.util.Log
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser

import java.net.URL

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
        fun fetch(gaugeId: String): Gauge {
            Log.d(TAG, "Fetching the gauge data for: " + gaugeId)
            val response = URL("https://waterservices.usgs.gov/nwis/iv/?&period=P1D&format=json&parameterCd=00065,00060,00010&sites=$gaugeId").openStream()
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

            return Gauge(site, siteName, parameters)
        }
    }



    class Gauge (val siteId: String, val name: String, val parameters: HashMap<String,GaugeParameter>) {
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


    }


    class GaugeParameter (val value: Float, val date: String) {
        override fun toString(): String {
            return String.format("Last value: %f at: %s", value, date)
        }
    }
}