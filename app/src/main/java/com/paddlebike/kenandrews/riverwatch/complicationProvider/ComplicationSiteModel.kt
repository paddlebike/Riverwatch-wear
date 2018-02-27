package com.paddlebike.kenandrews.riverwatch.complicationProvider

import com.paddlebike.kenandrews.riverwatch.USGSSite

/**
 * Singleton class for managing the monitored site
 */
const val MAX_CACHED_AGE = 15 * 60 * 1000 // 15 minutes
class SiteModel(private val siteId: String) {
    private var lastUpdated: Long = 0
    private var site: USGSSite.Site? = null

    private fun isStale() : Boolean {
        return this.site == null || this.lastUpdated + MAX_CACHED_AGE < System.currentTimeMillis()
    }

    fun getValueForKey(key: String) : Float? {
        if (isStale() ) {
            this.site = USGSSite.fetch(this.siteId)
            if (this.site != null) {
                this.lastUpdated = System.currentTimeMillis()
            }
        }
        return this.site!!.getValueForKey(key)
    }

    fun getSite() : USGSSite.Site {
        if (this.site == null) {
            this.site = USGSSite.fetch(this.siteId)
        }
        return this.site!!
    }
}

object ComplicationSiteModel {
    private var sites: HashMap<String, SiteModel> = HashMap(emptyMap<String, SiteModel>())

    fun getGaugeModel(siteId: String) : SiteModel {
        return if (sites.containsKey(siteId)) {
            sites[siteId]!!
        } else {
            sites[siteId] = SiteModel(siteId)
            sites[siteId]!!
        }
    }
}