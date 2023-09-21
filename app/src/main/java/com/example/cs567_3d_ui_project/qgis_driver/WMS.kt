package com.example.cs567_3d_ui_project.qgis_driver

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wms_resources.GetCapabilitiesResponse
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wms_resources.GetFeatureInfoResponse
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wms_resources.GetMapResponse
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wms_resources.GetSchemaExtensionResponse
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wms_resources.Layer
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wms_resources.wms_request_actions.GetMapHttpRequestAction
import fr.arnaudguyon.xmltojsonlib.XmlToJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class WMS(private val qgisUrl: String) {

    private val wms = "WMS"
    private val service = "SERVICE"
    private val request = "REQUEST"

    suspend fun getSchemaExtensionAsync(): GetSchemaExtensionResponse {
        //withContext(Dispatchers.IO) is required in order to execute this function on a separate thread
        return withContext(Dispatchers.IO) {

            var reqParam =
                URLEncoder.encode(service, "UTF-8") + "=" + URLEncoder.encode(wms, "UTF-8")

            reqParam += "&" + URLEncoder.encode(request, "UTF-8") +
                    "=" + URLEncoder.encode("GetSchemaExtension", "UTF-8")

            val url = URL("$qgisUrl?$reqParam")

            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"
                BufferedReader(InputStreamReader(inputStream)).use {
                    val response = StringBuffer()

                    var inputLine = it.readLine()
                    while (inputLine != null) {
                        response.append(inputLine)
                        inputLine = it.readLine()
                    }
                    it.close()

                    val xmlToJson = XmlToJson.Builder(response.toString()).build()
                    return@with GetSchemaExtensionResponse(xmlToJson.toString(), responseCode)
                }
            }
        }
    }

    suspend fun getCapabilities(): GetCapabilitiesResponse {
        return withContext(Dispatchers.IO) {
            try{
                var reqParam =
                    URLEncoder.encode(service, "UTF-8") + "=" + URLEncoder.encode(wms, "UTF-8")
                reqParam += "&" + URLEncoder.encode(
                    request,
                    "UTF-8"
                ) + "=" + URLEncoder.encode("GetCapabilities", "UTF-8")

                val url = URL("$qgisUrl?$reqParam")

                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"
                    BufferedReader(InputStreamReader(inputStream)).use {
                        val response = StringBuffer()

                        var inputLine = it.readLine()
                        while (inputLine != null) {
                            response.append(inputLine)
                            inputLine = it.readLine()
                        }
                        it.close()

                        val xmlToJson = XmlToJson.Builder(response.toString()).build()
                        return@with GetCapabilitiesResponse(xmlToJson.toString(), responseCode)
                    }
                }
            }catch (e: Exception){
                throw e
            }

        }
    }

    suspend fun getMap(
        layer: Layer? = null,
        req: GetMapHttpRequestAction? = null,
    ): GetMapResponse {
        return withContext(Dispatchers.IO) {
            var reqParam = ""
            //TODO: Old pattern that should get deprecated
            if(layer != null){

                URLEncoder.encode(service, "UTF-8") +
                        "=" + URLEncoder.encode(wms, "UTF-8")

                reqParam += "&" + URLEncoder.encode(request, "UTF-8") +
                        "=" + URLEncoder.encode("GetMap", "UTF-8")

                reqParam += "&" + URLEncoder.encode("VERSION", "UTF-8") +
                        "=" + URLEncoder.encode("1.3.0", "UTF-8")


                val boundingBox = layer.boundingBox.first()
                val spatialReference = boundingBox.crs
                reqParam += "&" + URLEncoder.encode("CRS", "UTF-8") +
                        "=" + URLEncoder.encode(spatialReference, "UTF-8")

                reqParam += "&" + URLEncoder.encode("HEIGHT", "UTF-8") +
                        "=" + URLEncoder.encode("200", "UTF-8")

                reqParam += "&" + URLEncoder.encode("WIDTH", "UTF-8") +
                        "=" + URLEncoder.encode("400", "UTF-8")

             /*   reqParam += "&" + URLEncoder.encode("BBOX", "UTF-8") +
                        "=" + URLEncoder.encode(boundingBox.toString(), "UTF-8")*/

                //

                reqParam += "&" + URLEncoder.encode("BBOX", "UTF-8") +
                        "=" + URLEncoder.encode("40.5249,-105.09,40.528,-105.086", "UTF-8")

                reqParam += "&" + URLEncoder.encode("LAYERS", "UTF-8") +
                        "=" + URLEncoder.encode(layer.name, "UTF-8")
            }
            else if(req != null){
                val boundingBox = req.boundingBox
                val spatialReference = boundingBox.crs
                val width = req.width.toString()
                val height = req.height.toString()
                val tiled = req.tiled.toString()

                val request = req.request
                val service = req.service
                val version = req.version

                val layerString = StringBuilder()
                req.layers.forEach{
                    if(!layerString.any()){
                        layerString.append(it)
                    }
                    else{
                        layerString.append(",$it")
                    }
                }

                reqParam += URLEncoder.encode("SERVICE", "UTF-8") +
                        "=" + URLEncoder.encode(service, "UTF-8")

                reqParam += "&" + URLEncoder.encode("REQUEST", "UTF-8") +
                        "=" + URLEncoder.encode(request, "UTF-8")

                reqParam += "&" + URLEncoder.encode("VERSION", "UTF-8") +
                        "=" + URLEncoder.encode(version, "UTF-8")

                reqParam += "&" + URLEncoder.encode("CRS", "UTF-8") +
                        "=" + URLEncoder.encode(spatialReference, "UTF-8")

                reqParam += "&" + URLEncoder.encode("HEIGHT", "UTF-8") +
                        "=" + URLEncoder.encode(height, "UTF-8")

                reqParam += "&" + URLEncoder.encode("WIDTH", "UTF-8") +
                        "=" + URLEncoder.encode(width, "UTF-8")

                reqParam += "&" + URLEncoder.encode("BBOX", "UTF-8") +
                        "=" + URLEncoder.encode(boundingBox.toString(), "UTF-8")

                reqParam += "&" + URLEncoder.encode("LAYERS", "UTF-8") +
                        "=" + URLEncoder.encode(layerString.toString(), "UTF-8")
                reqParam += "&" + URLEncoder.encode("TILED", "UTF-8") +
                        "=" + URLEncoder.encode(tiled, "UTF-8")

            }

            val url = URL("$qgisUrl?$reqParam")
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"

                val bitmap = BitmapFactory.decodeStream(inputStream).asImageBitmap()
                return@with GetMapResponse(bitmap, bitmap.toString(), responseCode)
            }
        }
    }

    suspend fun getFeatureInfo(layer: Layer): GetFeatureInfoResponse {
        return withContext(Dispatchers.IO) {
            val boundingBox = layer.boundingBox.first()
            val spatialReference = boundingBox.crs

            var reqParam =
                URLEncoder.encode("SERVICE", "UTF-8") + "=" + URLEncoder.encode("WMS", "UTF-8")

            reqParam += "&" + URLEncoder.encode("REQUEST", "UTF-8") +
                    "=" + URLEncoder.encode("GetFeatureInfo", "UTF-8")

            reqParam += "&" + URLEncoder.encode("VERSION", "UTF-8") +
                    "=" + URLEncoder.encode("1.3.0", "UTF-8")

            reqParam += "&" + URLEncoder.encode("CRS", "UTF-8") +
                    "=" + URLEncoder.encode(spatialReference, "UTF-8")

            reqParam += "&" + URLEncoder.encode("HEIGHT", "UTF-8") +
                    "=" + URLEncoder.encode("200", "UTF-8")

            reqParam += "&" + URLEncoder.encode("WIDTH", "UTF-8") +
                    "=" + URLEncoder.encode("400", "UTF-8")

            reqParam += "&" + URLEncoder.encode("BBOX", "UTF-8") +
                    "=" + URLEncoder.encode(boundingBox.toString(), "UTF-8")

            reqParam += "&" + URLEncoder.encode("LAYERS", "UTF-8") +
                    "=" + URLEncoder.encode(layer.name, "UTF-8")

            reqParam += "&" + URLEncoder.encode("QUERYLAYERS", "UTF-8") +
                    "=" + URLEncoder.encode(layer.name, "UTF-8")

            reqParam += "&" + URLEncoder.encode("I", "UTF-8") +
                    "=" + URLEncoder.encode("10", "UTF-8")

            reqParam += "&" + URLEncoder.encode("J", "UTF-8") +
                    "=" + URLEncoder.encode("10", "UTF-8")


            val url = URL("$qgisUrl?$reqParam")
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"

                BufferedReader(InputStreamReader(inputStream)).use {
                    val response = StringBuffer()

                    var inputLine = it.readLine()
                    while (inputLine != null) {
                        response.append(inputLine)
                        inputLine = it.readLine()
                    }
                    it.close()

                    val xmlToJson = XmlToJson.Builder(response.toString()).build()
                    return@with GetFeatureInfoResponse(xmlToJson.toString(), responseCode)
                }
            }
        }
    }




}