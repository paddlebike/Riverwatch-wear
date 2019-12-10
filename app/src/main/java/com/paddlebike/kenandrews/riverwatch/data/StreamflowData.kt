package com.paddlebike.kenandrews.riverwatch.data

import kotlinx.serialization.*
import kotlinx.serialization.json.JSON

/*
data class TimeSeriesResponseType(
        @SerializedName("name") val name: String,
      @SerializedName("declaredType") val declaredType: String,
      @SerializedName("scope") val scope: String,
      @SerializedName("value") val value: Collection<TimeSeriesResponseTypeValue>,
      @SerializedName("nil") val nil: Boolean,
      @SerializedName("globalScope") val globalScope: Boolean,
      @SerializedName("typeSubstituted") val typeSubstituted: Boolean)

data class TimeSeriesResponseTypeValue(
        @SerializedName("queryInfo") val queryInfo: Collection<TimeSeriesQueryInfo>,
        @SerializedName("timeSeries") val timeSeries: Collection<TimeSeries>)

data class TimeSeriesQueryInfo(
        @SerializedName("key") val key: String,
        @SerializedName("value") val queryInfo: Collection<JsonObject>
)
data class TimeSeries(
        @SerializedName("sourceInfo") val sourceInfo: Collection<TimeSeriesSite>,
        @SerializedName("variable") val variable: Collection<JsonObject>,
        @SerializedName("values") val values: Collection<TimeSeriesSiteValue>,
        @SerializedName("name") val name: String
)

data class TimeSeriesSite(
        @SerializedName("siteName") val siteName: String,
        @SerializedName("siteCode") val siteCode: Collection<TimeSeriesSiteCode>,
        @SerializedName("timeZoneInfo") val timeZoneInfo: Collection<JsonObject>,
        @SerializedName("geoLocation") val geoLocation: Collection<JsonObject>,
        @SerializedName("note") val note: JsonArray,
        @SerializedName("siteType") val siteType: JsonArray,
        @SerializedName("siteProperty") val siteProperty: JsonArray
)

data class TimeSeriesSiteCode(
        @SerializedName("value") val value: String,
        @SerializedName("network") val network: String,
        @SerializedName("agencyCode") val agencyCode: String
)

data class TimeSeriesSiteValue(
        @SerializedName("sourceInfo") val sourceInfo: JsonObject,
        @SerializedName("variable") val variable: JsonObject,
        @SerializedName("values") val values: JsonArray,
        @SerializedName("name") val name: String
)
*/

@Serializable
data class USGSTimeSeries(
    val declaredType: String,
    val globalScope: Boolean,
    val name: String,
    val nil: Boolean,
    val scope: String,
    val typeSubstituted: Boolean,
    val value: Value
)

@Serializable
data class Value(
    val queryInfo: QueryInfo,
    val timeSeries: List<TimeSeries>
)

@Serializable
data class QueryInfo(
    val criteria: Criteria,
    val note: List<Note>,
    val queryURL: String
)

@Serializable
data class Criteria(
        val locationParam: String,
        val parameter: List<@ContextualSerialization Any>,
        val variableParam: String
)

@Serializable
data class Note(
    val title: String,
    val value: String
)

@Serializable
data class TimeSeries(
    val name: String,
    val sourceInfo: SourceInfo,
    val values: List<ValueX>,
    val variable: Variable
)

@Serializable
data class SourceInfo(
    val geoLocation: GeoLocation,
    val note: List<@ContextualSerialization Any>,
    val siteCode: List<SiteCode>,
    val siteName: String,
    val siteProperty: List<SiteProperty>,
    val siteType: List<@ContextualSerialization Any>,
    val timeZoneInfo: TimeZoneInfo
)

@Serializable
data class GeoLocation(
    val geogLocation: GeogLocation,
    val localSiteXY: List<@ContextualSerialization Any>
)

@Serializable
data class GeogLocation(
    val latitude: Double,
    val longitude: Double,
    val srs: String
)

@Serializable
data class SiteCode(
    val agencyCode: String,
    val network: String,
    val value: String
)

@Serializable
data class SiteProperty(
    val name: String,
    val value: String
)

@Serializable
data class TimeZoneInfo(
    val daylightSavingsTimeZone: DaylightSavingsTimeZone,
    val defaultTimeZone: DefaultTimeZone,
    val siteUsesDaylightSavingsTime: Boolean
)

@Serializable
data class DaylightSavingsTimeZone(
    val zoneAbbreviation: String,
    val zoneOffset: String
)

@Serializable
data class DefaultTimeZone(
    val zoneAbbreviation: String,
    val zoneOffset: String
)

@Serializable
data class ValueX(
    val censorCode: List<@ContextualSerialization Any>,
    val method: List<Method>,
    val offset: List<@ContextualSerialization Any>,
    val qualifier: List<Qualifier>,
    val qualityControlLevel: List<@ContextualSerialization Any>,
    val sample: List<@ContextualSerialization Any>,
    val source: List<@ContextualSerialization Any>,
    val value: List<ValueXX>
)

@Serializable
data class Method(
    val methodDescription: String,
    val methodID: Int
)

@Serializable
data class Qualifier(
    val network: String,
    val qualifierCode: String,
    val qualifierDescription: String,
    val qualifierID: Int,
    val vocabulary: String
)

@Serializable
data class ValueXX(
    val dateTime: String,
    val qualifiers: List<String>,
    val value: String
)

@Serializable
data class Variable(
    val noDataValue: Double,
    val note: List<@ContextualSerialization Any>,
    val oid: String,
    val options: Options,
    val unit: Unit,
    val valueType: String,
    val variableCode: List<VariableCode>,
    val variableDescription: String,
    val variableName: String,
    val variableProperty: List<@ContextualSerialization Any>
)

@Serializable
data class Options(
    val option: List<Option>
)

@Serializable
data class Option(
    val name: String,
    val optionCode: String
)

@Serializable
data class Unit(
    val unitCode: String
)

@Serializable
data class VariableCode(
    val default: Boolean,
    val network: String,
    val value: String,
    val variableID: Int,
    val vocabulary: String
)

fun parseUSGSTimeSeries(jsonString: String) : USGSTimeSeries {
    return JSON.parse(USGSTimeSeries.serializer(), jsonString)
}