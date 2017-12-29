package com.paddlebike.kenandrews.riverwatch


import org.junit.Test

/**
 * Class for testing fetch of USGS gauge data
 */
class TestFetchUSGSGauge {

    @Test
    fun getsGauge() {
        val response = USGSGuage.fetch("01646500")
        assert(response is USGSGuage.Gauge)
        println(response)
    }
}