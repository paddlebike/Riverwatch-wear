package com.paddlebike.kenandrews.riverwatch.config

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import com.paddlebike.kenandrews.riverwatch.RiverWatchFace
import java.util.ArrayList

/**
 * Data represents different views for configuring the
 * {@link ComplicationWatchFaceService} watch face's appearance and complications
 * via {@link ComplicationConfigActivity}.
 */
class ComplicationConfigData {

    companion object {
        /**
         * Returns Watch Face Service class associated with configuration Activity.
         */
        val watchFaceServiceClass: Class<*>
            get() = RiverWatchFace::class.java

        /**
         * Interface all ConfigItems must implement so the [RecyclerView]'s Adapter associated
         * with the configuration activity knows what type of ViewHolder to inflate.
         */
        interface ConfigItemType {
            val configType: Int
        }

        /**
         * Includes all data to populate each of the 5 different custom
         * [ViewHolder] types in [ComplicationConfigRecyclerViewAdapter].
         */
        fun getDataToPopulateAdapter(): ArrayList<ConfigItemType> {

            val settingsConfigData = ArrayList<ConfigItemType>()

            // Data for watch face preview and complications UX in settings Activity.
            val complicationConfigItem = PreviewAndComplicationsConfigItem()
            settingsConfigData.add(complicationConfigItem)

            val gaugeIdConfigItem = GaugeIdConfigItem()
            settingsConfigData.add(gaugeIdConfigItem)

            return settingsConfigData
        }
    }

    /**
     * Data for Watch Face Preview with Complications Preview item in RecyclerView.
     */
    class PreviewAndComplicationsConfigItem internal constructor() : ConfigItemType {
        override val configType: Int
            get() = ComplicationConfigRecyclerViewAdapter.TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG
    }


    class GaugeIdConfigItem internal constructor() : ConfigItemType {
        override val configType: Int
            get() = ComplicationConfigRecyclerViewAdapter.TYPE_GAUGE_ID_CONFIG
    }

}