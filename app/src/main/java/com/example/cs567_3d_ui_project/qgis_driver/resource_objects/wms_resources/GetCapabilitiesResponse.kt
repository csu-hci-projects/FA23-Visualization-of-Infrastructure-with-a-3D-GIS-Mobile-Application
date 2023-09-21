package com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wms_resources

import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.HttpResponse
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.HttpResponseContent
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

class GetCapabilitiesResponse(content: String, responseCode: Int)
    : HttpResponse(content, responseCode) {

    val getCapabilitiesResponseContent = deserializeJsonObject(content)

    override fun deserializeJsonObject(properties: String): GetCapabilitiesResponseContent {
        val builder = GsonBuilder()
        builder.setPrettyPrinting()

        val gson = builder.create()
        try {
            return gson.fromJson(properties, GetCapabilitiesResponseContent::class.java)
        } catch (e: Exception) {
            throw e
        }
    }
}

data class GetCapabilitiesResponseContent(
    @SerializedName("WMS_Capabilities")
    var wmsCapabilities: GetCapabilitiesContent
): HttpResponseContent()

data class GetCapabilitiesContent(
    @SerializedName("xmlns")
    var xmlns: String,

    @SerializedName("Capability")
    var capability: Capability,

    @SerializedName("xmlns:qgs")
    var xmlnsQgs: String,

    @SerializedName("xmlns:sld")
    var xmlnsSld: String,

    @SerializedName("xsi:schemaLocation")
    var xsiSchemaLocation: String,

    @SerializedName("Service")
    var service: JsonObject,

    @SerializedName("xmlns:xsi")
    var xmlnsXsi: String,

    @SerializedName("version")
    var version: String,
)

data class Capability(
    @SerializedName("Request")
    var request: JsonObject,

    @SerializedName("Layer")
    var layer: LayerMetadata,

    @SerializedName("Exception")
    var exception: JsonObject,

    @SerializedName("sld:UserDefinedSymbolization")
    var sldUserDefinedSymbolization: JsonObject,
)

data class LayerMetadata(
    @SerializedName("queryable")
    var queryable: String,

    @SerializedName("CRS")
    var crs: List<CRS>,

    @SerializedName("Abstract")
    var abstract: String,

    @SerializedName("EX_GeographicBoundingBox")
    var exGeographicBoundingBox: JsonObject,

    @SerializedName("BoundingBox")
    var boundingBox: List<BoundingBox>,

    @SerializedName("KeywordList")
    var keywordList: JsonObject,

    @SerializedName("Title")
    var title: String,

    @SerializedName("Layer")
    var layer: List<Layer>,
)

data class Layer(
    @SerializedName("Name")
    var name: String,

    @SerializedName("Title")
    var title: String,

    @SerializedName("CRS")
    var crs: List<CRS>,

    @SerializedName("EX_GeographicBoundingBox")
    var exGeographicBoundingBox: JsonObject,

    @SerializedName("BoundingBox")
    var boundingBox: List<BoundingBox>,

    //@SerializedName("Style")
    //var style: JsonArray?,

    @SerializedName("MinScaleDenominator")
    var minScaleDenominator: Double,

    @SerializedName("MaxScaleDenominator")
    var maxScaleDenominator: Double,
)

data class BoundingBox(
    @SerializedName("CRS")
    var crs: String,

    @SerializedName("minx")
    var minX: Double?,

    @SerializedName("miny")
    var minY: Double?,

    @SerializedName("maxx")
    var maxX: Double?,

    @SerializedName("maxy")
    var maxY: Double?,
){
    override fun toString(): String {
        return minX.toString() +
                "," +
                minY.toString() +
                "," +
                maxX.toString() +
                "," +
                maxY.toString()
    }
}

data class CRS(
    @SerializedName("content")
    var content: String,
)