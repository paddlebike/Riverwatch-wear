package com.paddlebike.kenandrews.riverwatch.complicationProvider

import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationManager
import android.support.wearable.complications.ComplicationProviderService
import android.support.wearable.complications.ComplicationText
import android.util.Log
import com.paddlebike.kenandrews.riverwatch.GaugeConstants.Companion.ERROR_VALUE
import com.paddlebike.kenandrews.riverwatch.GaugeConstants.Companion.GAUGE_FLOW
import com.paddlebike.kenandrews.riverwatch.GaugeConstants.Companion.GAUGE_LEVEL
import com.paddlebike.kenandrews.riverwatch.GaugeConstants.Companion.GAUGE_TEMP
import com.paddlebike.kenandrews.riverwatch.R
import com.paddlebike.kenandrews.riverwatch.USGSGuage
import net.danlew.android.joda.JodaTimeAndroid
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.joda.time.format.DateTimeFormat


private const val TAG = "USGSSiteSummary"
/**
 * Complication for getting long summary of a site
 */
class USGSStreamSummaryComplication : ComplicationProviderService() {

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
        JodaTimeAndroid.init(this)

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

        val siteModel = ComplicationSiteModel
        val siteId = getSiteId()
        val model = siteModel.getGaugeModel(siteId)

        doAsync {
            val complicationData = createComplicationData(model.getSite(), dataType)
            uiThread {
                updateComplication(complicationData, complicationId, complicationManager)
            }
        }
    }

    /*
     * Called when the complication has been deactivated.
     */
    override fun onComplicationDeactivated(complicationId: Int) {
        Log.d(TAG, "onComplicationDeactivated(): " + complicationId)
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
    private fun createComplicationData(site: USGSGuage.Site, dataType: Int) : ComplicationData? {
        when (dataType) {
            ComplicationData.TYPE_LONG_TEXT -> {
                val text = createSummaryString(site)
                return ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                        .setLongText(ComplicationText.plainText(text))
                        .build()
            }
            else -> if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Unexpected complication type " + dataType)
            }
        }
        return null
    }

    private fun createSummaryString(site: USGSGuage.Site) : String {
        val level = if (site.parameters.containsKey(GAUGE_LEVEL)) {
            if (site.getValueForKey(GAUGE_LEVEL) == ERROR_VALUE) "ICE "
            else String.format("%2.02fft ", site.getValueForKey(GAUGE_LEVEL))
        } else if (site.parameters.containsKey(GAUGE_FLOW)) {
                if (site.getValueForKey(GAUGE_FLOW) == ERROR_VALUE) "ICE "
                else String.format("%d cfs ", site.getValueForKey(GAUGE_FLOW).toInt())
        } else {
            ""
        }

        val temp = if (site.parameters.containsKey(GAUGE_TEMP))
            String.format("%2.1fC ", site.getValueForKey(GAUGE_TEMP))
        else
            ""

        val formatter = DateTimeFormat.forPattern("HH:mm")
        val age = site.getTimeStamp().toString(formatter)
        return String.format("%s %s at %s", level, temp, age)
    }

    private fun getSiteId() :String {
        try {
            val defaultSiteId = this.applicationContext.getString(R.string.site_id)
            val key = this.applicationContext.getString(R.string.watchface_prefs)
            val prefs = this.applicationContext.getSharedPreferences(key, 0)
            return prefs.getString(this.applicationContext.getString(R.string.prefs_site_id), defaultSiteId)
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting stuff")
        }
        return getString(R.string.site_id)
    }
}

