package com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wmts_resources

import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.HttpResponse
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.HttpResponseContent
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

class GetCapabilitiesResponse(content: String, responseCode: Int)
    : HttpResponse(content, responseCode) {

    val getCapabilitiesResponseContent = deserializeJsonObject(content)

    override fun deserializeJsonObject(properties: String): GetCapabilitiesResponseContent {
        val builder = GsonBuilder()
        builder.setPrettyPrinting()
        val gson = builder.create()
        try{
            return gson.fromJson(properties, GetCapabilitiesResponseContent::class.java)
        }catch (e: Exception){
            throw e
        }

    }
}

data class GetCapabilitiesResponseContent(
    @SerializedName("Capabilities")
    var capabilities: Capabilities
): HttpResponseContent()

data class Capabilities(
    @SerializedName("ows:ServiceIdentification")
    var serviceIdentification: ServiceIdentification,

    @SerializedName("ows:ServiceProvider")
    var serviceProvider: JsonObject,

    @SerializedName("ows:OperationsMetadata")
    var operationsMetadata: JsonObject,

    @SerializedName("Contents")
    var contents: Contents
)

data class ServiceIdentification(
    @SerializedName("ows:ServiceType")
    var serviceType: String,

    @SerializedName("ows:ServiceTypeVersion")
    var serviceTypeVersion: String,

    @SerializedName("ows:Title")
    var title: String,

    @SerializedName("ows:Abstract")
    var abstract: String,

    @SerializedName("ows:Keywords")
    var keywords: JsonObject,

    @SerializedName("ows:Fees")
    var fees: String,

    @SerializedName("ows:AccessConstraints")
    var accessConstraints: String
)

data class Contents(
    @SerializedName("Layer")
    var layer: Layer,

    @SerializedName("TileMatrixSet")
    var tileMatrixSet: List<TileMatrixSet>,
)

data class Layer(
    @SerializedName("ows:Identifier")
    var owsIdentifier: String,

    @SerializedName("ows:WGS84BoundingBox")
    var owsWGS84BoundingBOx: JsonObject,

    @SerializedName("ows:BoundingBox")
    var owsBoundingBox: JsonArray,

    @SerializedName("Style")
    var style: JsonObject,

    @SerializedName("Format")
    var format: JsonArray,

    @SerializedName("InfoFormat")
    var infoFormat: JsonArray,

    @SerializedName("TileMatrixSetLink")
    var tileMatrixSetLink: List<TileMatrixSetLink>
)

data class TileMatrixSetLink(
    @SerializedName("TileMatrixSet")
    var tileMatrixSet: String,

    @SerializedName("TileMatrixSetLimits")
    var timeMatrixSetLimits: TileMatrixSetLimits
)

data class TileMatrixSetLimits(
    @SerializedName("TileMatrixLimits")
    var tileMatrixLimits: List<TileMatrixLimit>
)

data class TileMatrixLimit(
    @SerializedName("TileMatrix")
    var tileMatrix: Int,

    @SerializedName("MinTileCol")
    var minTileCol: Int,

    @SerializedName("MaxTileCol")
    var maxTileCol: Int,

    @SerializedName("MinTileRow")
    var minTileRow: Int,

    @SerializedName("MaxTileRow")
    var maxTileRow: Int,
){

    fun to2DArray(): Array<IntArray> {
        val columnIntArray = (minTileCol..maxTileCol).toList().toIntArray()
        val rowIntArray = (minTileRow..maxTileRow).toList().toIntArray()

        return arrayOf(rowIntArray, columnIntArray)
    }
}

data class TileMatrixSet(
    @SerializedName("ows:Identifier")
    var owsIdentifier: String,

    @SerializedName("ows:SupportedCRS")
    var owsSupportedCRS: String,

    @SerializedName("TileMatrix")
    var tileMatrix: List<TileMatrix>
)

data class TileMatrix(
    @SerializedName("ows:Identifier")
    var owsIdentifier: String,

    @SerializedName("ScaleDenominator")
    var scaleDenominator: BigDecimal,

    @SerializedName("TopLeftCorner")
    var topLeftCorner: JsonPrimitive,

    @SerializedName("TileWidth")
    var tileWidth: Int,

    @SerializedName("TileHeight")
    var tileHeight: Int,

    @SerializedName("MatrixWidth")
    var matrixWidth: Int,

    @SerializedName("MatrixHeight")
    var matrixHeight: Int
)
