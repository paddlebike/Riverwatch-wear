package com.paddlebike.kenandrews.riverwatch

import android.content.Context
import android.content.SharedPreferences
import org.joda.time.DateTime


class PrefsConstants {
    companion object {
        const val MIN_VAL = "min"
        const val MAX_VAL = "max"
        const val LAST_VAL = "current"
        const val TIMESTAMP = "timestamp"
        const val SITE_NAME = "site_name"
    }
}

private const val TAG = "USGSSitePrefs"
/**
 * Class for storage of USGS stream flow site data
 */
class USGSSitePrefs (private val context: Context) {

    private val key = this.context.getString(R.string.watchface_prefs)
    private val prefs = this.context.getSharedPreferences(key, 0)


    fun saveSite(site: USGSSite.Site) {
        if (this.prefs != null) {
            val editor = this.prefs.edit()
            if (editor != null) {
                editor.putString(site.siteId + "." + PrefsConstants.SITE_NAME, site.name)
                for (gauge in site.parameters) {
                    saveGauge(editor, site.siteId ,gauge)
                }
                editor.apply()
            }
        }
    }

    fun getSiteName(siteId: String) : String {
        if (prefs != null) {
            return this.prefs.getString(siteId + "." + PrefsConstants.SITE_NAME, "")
        }
        return ""
    }

    fun getGaugeFloat(siteId: String, gaugeName: String, paramName: String) : Float {
        if (this.prefs != null) {
            return this.prefs.getFloat(makeKey(siteId, gaugeName, paramName), 0.0F)
        }
        return 0.0F
    }

    fun getGaugeTimeStamp(siteId: String, gaugeName: String) : DateTime {
        if (this.prefs != null) {
            val dateString = this.prefs.getString(makeKey(siteId, gaugeName, PrefsConstants.TIMESTAMP), null)
            if (dateString != null) {
                return DateTime(dateString)
            }
        }

        return return DateTime(-1)
    }

    private fun saveGauge(editor: SharedPreferences.Editor,
                          siteId: String,
                          gauge: MutableMap.MutableEntry<String, USGSSite.GaugeParameter>) {

        editor.putFloat(makeKey(siteId, gauge.key, PrefsConstants.LAST_VAL), gauge.value.value)
        editor.putString(makeKey(siteId, gauge.key, PrefsConstants.TIMESTAMP), gauge.value.getTimeStamp().toString())

        val min = this.prefs.getFloat(makeKey(siteId, gauge.key, PrefsConstants.MIN_VAL), 0.0F)
        editor.putFloat(makeKey(siteId, gauge.key, PrefsConstants.MIN_VAL), minOf(min, gauge.value.value))

        val max = this.prefs.getFloat(makeKey(siteId, gauge.key, PrefsConstants.MAX_VAL), 0.0F)
        editor.putFloat(makeKey(siteId, gauge.key, PrefsConstants.MAX_VAL), maxOf(max, gauge.value.value))
    }

    private fun makeKey(siteId: String, gaugeId: String, paramter: String) : String {
        return String.format("%s.%s.%s", siteId, gaugeId, paramter)
    }
}