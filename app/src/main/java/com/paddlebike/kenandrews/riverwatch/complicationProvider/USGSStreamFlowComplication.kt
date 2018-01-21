package com.paddlebike.kenandrews.riverwatch.complicationProvider

import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationManager
import android.support.wearable.complications.ComplicationProviderService
import android.support.wearable.complications.ComplicationText
import android.util.Log
import com.paddlebike.kenandrews.riverwatch.GaugeConstants
import com.paddlebike.kenandrews.riverwatch.R
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

private const val TAG = "USGSGaugeFlow"
/**
 * Complication for getting streamflow data
 */
class USGSStreamFlowComplication : ComplicationProviderService() {

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

        val gaugeModel = ComplicationSiteModel
        val gaugeId = getGaugeId()
        val localGauge = gaugeModel.getGaugeModel(gaugeId)

        doAsync {
            val gaugeValue = localGauge.getValueForKey(GaugeConstants.GAUGE_FLOW)
            val complicationData = createComplicationData(gaugeValue!!, dataType)
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
    private fun createComplicationData(gaugeValue: Float, dataType: Int) : ComplicationData? {
        when (dataType) {

            ComplicationData.TYPE_SHORT_TEXT -> {
                val text = if (gaugeValue == GaugeConstants.ERROR_VALUE) "frozen" else String.format("%d cfs", gaugeValue.toInt())
                return ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText(text))
                        .build()
            }
            ComplicationData.TYPE_LONG_TEXT -> {
                val text = if (gaugeValue == GaugeConstants.ERROR_VALUE) "frozen" else String.format("Flow: %d cfs", gaugeValue.toInt())
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


    private fun getGaugeId() :String {
        try {
            val defaultSiteId = this.applicationContext.getString(R.string.site_id)
            val key = this.applicationContext.getString(R.string.watchface_prefs)
            val prefs = this.applicationContext.getSharedPreferences(key, 0)
            return prefs.getString("saved_gauge_id", defaultSiteId)
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting stuff")
        }
        return getString(R.string.site_id)
    }
}
