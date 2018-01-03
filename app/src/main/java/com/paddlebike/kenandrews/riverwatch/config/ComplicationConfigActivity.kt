package com.paddlebike.kenandrews.riverwatch.config

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.wear.widget.WearableRecyclerView
import android.support.wearable.complications.ComplicationProviderInfo
import android.support.wearable.complications.ProviderChooserIntent
import android.util.Log
import com.paddlebike.kenandrews.riverwatch.R

/**
 * The watch-side config activity for {@link RiverWatch}, which
 * allows for setting the left, right. and bottom complications of watch face and
 * unread notifications toggle.
 */
class ComplicationConfigActivity : Activity (){
    private val TAG = ComplicationConfigActivity::class.java.simpleName

    companion object {
        val COMPLICATION_CONFIG_REQUEST_CODE = 1001
    }


    private var mWearableRecyclerView: WearableRecyclerView? = null
    private var mAdapter: ComplicationConfigRecyclerViewAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_complication_config)

        mAdapter = ComplicationConfigRecyclerViewAdapter(
                applicationContext,
                ComplicationConfigData.watchFaceServiceClass,
                ComplicationConfigData.getDataToPopulateAdapter(this))

        mWearableRecyclerView = findViewById(R.id.wearable_recycler_view) as WearableRecyclerView

        // Aligns the first and last items on the list vertically centered on the screen.
        mWearableRecyclerView!!.isEdgeItemsCenteringEnabled = true

        mWearableRecyclerView!!.layoutManager = LinearLayoutManager(this)

        // Improves performance because we know changes in content do not change the layout size of
        // the RecyclerView.
        mWearableRecyclerView!!.setHasFixedSize(true)

        mWearableRecyclerView!!.setAdapter(mAdapter)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {

        if (requestCode == COMPLICATION_CONFIG_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            // Retrieves information for selected Complication provider.
            val complicationProviderInfo = data.getParcelableExtra<ComplicationProviderInfo>(ProviderChooserIntent.EXTRA_PROVIDER_INFO)
            Log.d(TAG, "Provider: " + complicationProviderInfo)

            // Updates preview with new complication information for selected complication id.
            // Note: complication id is saved and tracked in the adapter class.
            mAdapter!!.updateSelectedComplication(complicationProviderInfo)

        }
    }
}