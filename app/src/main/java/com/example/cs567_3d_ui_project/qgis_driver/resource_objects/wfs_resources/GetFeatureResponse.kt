package com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources

import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.HttpResponse
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.HttpResponseContent
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

class GetFeatureResponse(content: String, responseCode: Int):
    HttpResponse(content, responseCode) {

    val getFeatureResponseContent = deserializeJsonObject(content)

    override fun deserializeJsonObject(properties: String): GetFeatureResponseContent {
        val builder = GsonBuilder()
        builder.setPrettyPrinting()

        val gson = builder.create()
        try {
           return gson.fromJson(properties, GetFeatureResponseContent::class.java)
        } catch (e: Exception) {
            throw e
        }
    }
}

data class GetFeatureResponseContent(
    @SerializedName("type")
    val type: String,

    @SerializedName("bbox")
    val bbox: List<Double>,

    @SerializedName("features")
    val features: List<Feature>,

):HttpResponseContent()

data class Feature(
    @SerializedName("id")
    val id: String,

    @SerializedName("geometry")
    val geometry: Geometry,

    @SerializedName("properties")
    val properties: JsonObject,

    @SerializedName("bbox")
    val bbox: JsonArray?
)



