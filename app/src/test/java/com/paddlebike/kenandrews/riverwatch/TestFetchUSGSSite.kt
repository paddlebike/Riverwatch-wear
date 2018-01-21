package com.paddlebike.kenandrews.riverwatch



import org.joda.time.format.DateTimeFormat
import org.junit.Test




/**
 * Class for testing fetch of USGS gauge data
 */
class TestFetchUSGSSite {



    @Test
    fun getsGauge() {
        val response = USGSGuage.fetch("01646500")
        assert(response is USGSGuage.Site)
        println(response)
    }

    @Test
    fun reportsAge() {
        val response = USGSGuage.fetch("01646500")
        assert(response is USGSGuage.Site)
        println(response)
        val age = response.getAge()
        assert(age != -1L)
        val seconds = age / 1000
        println(String.format("Age is %d seconds", seconds))
        println(String.format("Or %d minutes", seconds/60))
        println(String.format("Or %d hours", seconds/(60*60)))
    }

    @Test
    fun reportsTimeStamp() {
        val site: USGSGuage.Site = USGSGuage.fetch("01646500")
        println(site.getTimeStamp().toLocalTime().toString())
        val formatter = DateTimeFormat.forPattern("HH:mm")
        println(site.getTimeStamp().toString(formatter))
    }
}