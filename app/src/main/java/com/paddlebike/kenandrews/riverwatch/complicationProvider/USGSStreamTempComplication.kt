package com.paddlebike.kenandrews.riverwatch.complicationProvider

import android.graphics.drawable.Icon
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationManager
import android.support.wearable.complications.ComplicationProviderService
import android.support.wearable.complications.ComplicationText
import android.util.Log
import com.paddlebike.kenandrews.riverwatch.*
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread


private const val TAG = "USGSGaugeTemp"
/**
 * Complication for getting streamflow data
 */
class USGSStreamTempComplication : ComplicationProviderService() {

    /*
     * Called when a complication has been activated. The method is for any one-time
     * (per complication) set-up.
     *
     * You can continue sending data for the active complicationId until onComplicationDeactivated()
     * is called.
     */
    override fun onComplicationActivated(
            complicationId: Int, dataType: Int, complicationManager: ComplicationManager?) {
        Log.d(TAG, "onComplicationActivated(): " + complicationId)

    }

    /*
     * Called when the complication needs updated data from your provider. There are four scenarios
     * when this will happen:
     *
     *   1. An active watch face complication is changed to use this provider
     *   2. A complication using this provider becomes active
     *   3. The period of time you specified in the manifest has elapsed (UPDATE_PERIOD_SECONDS)
     *   4. You triggered an update from your own class via the
     *       ProviderUpdateRequester.requestUpdate() method.
     */
    override fun onComplicationUpdate(
            complicationId: Int, dataType: Int, complicationManager: ComplicationManager) {
        Log.d(TAG, "onComplicationUpdate() id: " + complicationId)

        val siteData = USGSSitePrefs(this.applicationContext)
        val siteId = defaultSharedPreferences.getString(
                this.applicationContext.getString(R.string.prefs_site_id),
                this.applicationContext.getString(R.string.site_id))
        doAsync {
            try {
                val site = USGSSite.fetch(siteId)
                siteData.saveSite(site)
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
            val gaugeValue = siteData.getGaugeFloat(siteId, GaugeConstants.GAUGE_TEMP, PrefsConstants.LAST_VAL)

            val complicationData = if (willDisplayFahrenheit()) {
                createFahrenheitComplicationData(gaugeValue, dataType)
            } else {
                createComplicationData(gaugeValue, dataType)
            }

            uiThread {
                updateComplication(complicationData, complicationId, complicationManager)
            }
        }
    }

    /*
     * Called when the complication has been deactivated.
     */
    override fun onComplicationDeactivated(complicationId: Int) {
        Log.d(TAG, "onComplicationDeactivated(): $complicationId")
    }


    /**
     * Send the complication data back to the ComplicationManager
     */
    private fun updateComplication(complicationData: ComplicationData?,
                                   complicationId: Int,
                                   complicationManager: ComplicationManager) {

        if (complicationData != null) {
            complicationManager.updateComplicationData(complicationId, complicationData)
        } else {
            // If no data is sent, we still need to inform the ComplicationManager, so the update
            // job can finish and the wake lock isn't held any longer than necessary.
            complicationManager.noUpdateRequired(complicationId)
        }
    }


    /**
     * Create the complication data
     */
    private fun createComplicationData(gaugeValue: Float, dataType: Int) : ComplicationData? {
        when (dataType) {
            ComplicationData.TYPE_SHORT_TEXT -> return ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                    .setIcon(Icon.createWithResource(this.applicationContext, R.drawable.thermometer))
                    .setShortText(ComplicationText.plainText(String.format("%2.1f C", gaugeValue)))
                    .build()
            ComplicationData.TYPE_LONG_TEXT -> return ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                    .setIcon(Icon.createWithResource(this.applicationContext, R.drawable.thermometer))
                    .setLongText(ComplicationText.plainText(String.format("Temp: %2.1fC", gaugeValue)))
                    .build()
            ComplicationData.TYPE_RANGED_VALUE -> return ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                    .setIcon(Icon.createWithResource(this.applicationContext, R.drawable.thermometer))
                    .setMinValue(0F)
                    .setValue(gaugeValue)
                    .setMaxValue(50F)
                    .setShortText(ComplicationText.plainText(String.format("%2.1f C", gaugeValue)))
                    .build()
            else -> if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Unexpected complication type $dataType")
            }
        }
        return null
    }

    /**
     * Create the complication data
     */
    private fun createFahrenheitComplicationData(gaugeValue: Float, dataType: Int) : ComplicationData? {
        val fahrenheit = cToF(gaugeValue)

        when (dataType) {
            ComplicationData.TYPE_SHORT_TEXT -> return ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                    .setIcon(Icon.createWithResource(this.applicationContext, R.drawable.thermometer))
                    .setShortText(ComplicationText.plainText(String.format("%2.1f F", fahrenheit)))
                    .build()
            ComplicationData.TYPE_LONG_TEXT -> return ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                    .setIcon(Icon.createWithResource(this.applicationContext, R.drawable.thermometer))
                    .setLongText(ComplicationText.plainText(String.format("Temp: %2.1fF", fahrenheit)))
                    .build()
            ComplicationData.TYPE_RANGED_VALUE -> return ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                    .setIcon(Icon.createWithResource(this.applicationContext, R.drawable.thermometer))
                    .setMinValue(32F)
                    .setValue(gaugeValue)
                    .setMaxValue(120F)
                    .setShortText(ComplicationText.plainText(String.format("%2.1f F", fahrenheit)))
                    .build()
            else -> if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Unexpected complication type " + dataType)
            }
        }
        return null
    }


    private fun willDisplayFahrenheit() :Boolean {
        try {
            val key = this.applicationContext.getString(R.string.fahrenheit_display_pref)
            return defaultSharedPreferences.getBoolean(key, true)
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting stuff")
        }
        return false
    }

    fun cToF(c: Float) = (9F/5.0F * c) + 32F
}
