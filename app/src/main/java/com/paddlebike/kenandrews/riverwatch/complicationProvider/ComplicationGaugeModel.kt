package com.paddlebike.kenandrews.riverwatch.complicationProvider

import com.paddlebike.kenandrews.riverwatch.USGSGuage

/**
 * Singleton class for managing the monitored gauge
 */
const val MAX_CACHED_AGE = 15 * 60 * 1000 // 15 minutes
class GaugeModel(gaugeId: String) {
    var lastUpdated: Long = 0
    val gaugeId = gaugeId
    private var gauge: USGSGuage.Gauge? = null

    public fun updateGauge(gauge: USGSGuage.Gauge) {
        this.gauge = gauge
        this.lastUpdated = System.currentTimeMillis()
    }

    public fun isStale() : Boolean {
        return this.gauge == null || this.lastUpdated + MAX_CACHED_AGE < System.currentTimeMillis()
    }

    public fun getValueForKey(key: String) : Float? {
        if (isStale() && this.gaugeId != null) {
            this.gauge = USGSGuage.fetch(this.gaugeId!!)
            if (this.gauge != null) {
                this.lastUpdated = System.currentTimeMillis()
            }
        }
        return this.gauge!!.getValueForKey(key)
    }
}

object ComplicationGaugeModel {
    private var gauges: HashMap<String,GaugeModel> = HashMap(emptyMap<String,GaugeModel>())

    public fun getGaugeModel(gaugeId: String) : GaugeModel {
        return if (gauges.containsKey(gaugeId)) {
            gauges[gaugeId]!!
        } else {
            gauges.put(gaugeId, GaugeModel(gaugeId))
            gauges[gaugeId]!!
        }
    }
}