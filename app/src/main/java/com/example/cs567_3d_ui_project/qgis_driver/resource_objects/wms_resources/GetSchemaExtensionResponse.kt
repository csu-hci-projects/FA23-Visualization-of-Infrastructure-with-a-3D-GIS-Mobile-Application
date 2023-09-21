package com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wms_resources

import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.HttpResponse
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.HttpResponseContent
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName


class GetSchemaExtensionResponse(content: String, responseCode: Int)
    : HttpResponse(content, responseCode)
{
    val getSchemaExtensionResponseContent = deserializeJsonObject(content)

    override fun deserializeJsonObject(properties: String): GetSchemaExtensionResponseContent {
        val builder = GsonBuilder()
        builder.setPrettyPrinting()

        val gson = builder.create()
        try {
            return gson.fromJson(properties, GetSchemaExtensionResponseContent::class.java)
        } catch (e: Exception) {
            throw e
        }

    }

}

data class GetSchemaExtensionResponseContent(
    @SerializedName("schema")
    var schema: GetSchemaExtensionContent
) : HttpResponseContent()
data class GetSchemaExtensionContent(
    @SerializedName("xmlns")
    var xmlns: String,

    @SerializedName("elementFormDefault")
    var elementFormDefault: String,

    @SerializedName("import")
    var import: JsonObject,

    @SerializedName("xmlns:wms")
    var xmlnsWms: String,

    @SerializedName("xmlns:qgs")
    var xmlnsQgs: String,

    @SerializedName("targetNamespace")
    var targetNamespace: String,

    @SerializedName("version")
    var version: String,

    @SerializedName("element")
    var element: JsonArray,
)