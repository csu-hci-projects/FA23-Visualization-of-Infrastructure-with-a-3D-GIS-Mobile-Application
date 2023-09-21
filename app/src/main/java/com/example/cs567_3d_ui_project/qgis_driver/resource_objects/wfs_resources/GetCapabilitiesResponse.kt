package com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources

import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.HttpResponse
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.HttpResponseContent
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

class GetCapabilitiesResponse(content: String, responseCode: Int):
    HttpResponse(content, responseCode) {

    val getCapabilitiesResponse = deserializeJsonObject(content)

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
    @SerializedName("WFS_Capabilities")
    val wfsCapabilities: Capability,
): HttpResponseContent()

data class Capability(
    @SerializedName("ows:ServiceIdentification")
    val owsServiceIdentification: JsonObject,

    @SerializedName("updateSequence")
    val updateSequence: String,

    @SerializedName("xsi:schemaLocation")
    val xsiSchemaLocation: String,

    @SerializedName("xmlns:xsi")
    val xmlnsXsi: String,

    @SerializedName("FeatureTypeList")
    val featureTypeList: JsonObject,

    @SerializedName("ogc:Filter_Capabilities")
    val ogcFilterCapabilities: JsonObject,

    @SerializedName("ows:ServiceProvider")
    val owsServiceProvider: JsonObject,

    @SerializedName("version")
    val version: String,

    @SerializedName("xmlns")
    val xmlns: String,

    @SerializedName("xmlns:gml")
    val xmlnsGml: String,

    @SerializedName("ows:OperationsMetadata")
    val owsOperationsMetadata: JsonObject,

    @SerializedName("xmlns:ows")
    val xmlnsOws: String,

    @SerializedName("xmlns:ogc")
    val xmlnsOgc: String,

    @SerializedName("xmlns:xlink")
    val xmlnsXLink: String,
)