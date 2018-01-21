package com.paddlebike.kenandrews.riverwatch.complicationProvider

import com.paddlebike.kenandrews.riverwatch.USGSGuage

/**
 * Singleton class for managing the monitored site
 */
const val MAX_CACHED_AGE = 15 * 60 * 1000 // 15 minutes
class SiteModel(siteId: String) {
    var lastUpdated: Long = 0
    val siteId = siteId
    private var site: USGSGuage.Site? = null

    public fun updateGauge(site: USGSGuage.Site) {
        this.site = site
        this.lastUpdated = System.currentTimeMillis()
    }

    public fun isStale() : Boolean {
        return this.site == null || this.lastUpdated + MAX_CACHED_AGE < System.currentTimeMillis()
    }

    public fun getValueForKey(key: String) : Float? {
        if (isStale() && this.siteId != null) {
            this.site = USGSGuage.fetch(this.siteId!!)
            if (this.site != null) {
                this.lastUpdated = System.currentTimeMillis()
            }
        }
        return this.site!!.getValueForKey(key)
    }

    public fun getSite() : USGSGuage.Site {
        return this.site!!
    }
}

object ComplicationSiteModel {
    private var sites: HashMap<String, SiteModel> = HashMap(emptyMap<String, SiteModel>())

    public fun getGaugeModel(siteId: String) : SiteModel {
        return if (sites.containsKey(siteId)) {
            sites[siteId]!!
        } else {
            sites.put(siteId, SiteModel(siteId))
            sites[siteId]!!
        }
    }
}