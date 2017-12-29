package com.paddlebike.kenandrews.riverwatch

import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationManager
import android.support.wearable.complications.ComplicationProviderService
import android.support.wearable.complications.ComplicationText
import android.util.Log
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*

/**
 * Complication for getting streamflow data
 */
class USGSStreamLevelComplication : ComplicationProviderService() {
    val TAG = "USGSGaugeLevel"

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

        // Retrieves your data
        doAsync {
            val gauge = USGSGuage.fetch(getString(R.string.gauge_id))
            val level = gauge.getValueForKey(GaugeConstants.GAUGE_LEVEL)
            val numberText = String.format(Locale.getDefault(), "%2.02f", level)
            uiThread {
                updateComplication(numberText, complicationId, dataType, complicationManager)
            }
        }
    }

    /*
     * Called when the complication has been deactivated.
     */
    override fun onComplicationDeactivated(complicationId: Int) {
        Log.d(TAG, "onComplicationDeactivated(): " + complicationId)
    }

    private fun updateComplication(numberText: String,
                                   complicationId: Int,
                                   dataType: Int,
                                   complicationManager: ComplicationManager) {
        var complicationData: ComplicationData? = null

        when (dataType) {

            ComplicationData.TYPE_SHORT_TEXT -> complicationData = ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                    .setShortText(ComplicationText.plainText(numberText))
                    .build()
            ComplicationData.TYPE_LONG_TEXT -> complicationData = ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                    .setLongText(ComplicationText.plainText("Level: " + numberText))
                    .build()
            else -> if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Unexpected complication type " + dataType)
            }
        }

        if (complicationData != null) {
            complicationManager.updateComplicationData(complicationId, complicationData)

        } else {
            // If no data is sent, we still need to inform the ComplicationManager, so the update
            // job can finish and the wake lock isn't held any longer than necessary.
            complicationManager.noUpdateRequired(complicationId)
        }
    }

}