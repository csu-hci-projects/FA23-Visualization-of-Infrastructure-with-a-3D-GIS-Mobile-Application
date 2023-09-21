package com.example.cs567_3d_ui_project.qgis_driver

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wmts_resources.GetCapabilitiesResponse
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wmts_resources.GetTileResponse
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wmts_resources.wmts_request_actions.GetCapabilitiesRequestAction
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wmts_resources.wmts_request_actions.GetTileRequestAction
import fr.arnaudguyon.xmltojsonlib.XmlToJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class WMTS(private val qgisUrl: String) {

    private val service = "SERVICE"
    private val request = "REQUEST"

    suspend fun getCapabilities(req: GetCapabilitiesRequestAction): GetCapabilitiesResponse {
        return withContext(Dispatchers.IO){

            var reqParam = URLEncoder.encode(service, "UTF-8") + "=" +
                    URLEncoder.encode(req.service, "UTF-8")

            reqParam += "&" + URLEncoder.encode(request, "UTF-8") + "=" +
                    URLEncoder.encode(req.request, "UTF-8")

            val url = URL("$qgisUrl?$reqParam")

            with(url.openConnection() as HttpURLConnection){
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
        }
    }

    suspend fun getTile(req: GetTileRequestAction) : GetTileResponse {
        return withContext(Dispatchers.IO){
            var reqParam = URLEncoder.encode(service, "UTF-8") + "=" +
                    URLEncoder.encode(req.service, "UTF-8")

            reqParam += "&" + URLEncoder.encode(request, "UTF-8") + "=" +
                    URLEncoder.encode(req.request, "UTF-8")

            reqParam += "&" + URLEncoder.encode("LAYER", "UTF-8") + "=" +
                    URLEncoder.encode(req.layer, "UTF-8")

            reqParam += "&" + URLEncoder.encode("TILEMATRIXSET", "UTF-8") + "=" +
                    URLEncoder.encode(req.tileMatrixSet, "UTF-8")

            reqParam += "&" + URLEncoder.encode("TILEMATRIX", "UTF-8") + "=" +
                    URLEncoder.encode(req.tileMatrix.toString(), "UTF-8")

            reqParam += "&" + URLEncoder.encode("TILEROW", "UTF-8") + "=" +
                    URLEncoder.encode(req.tileRow.toString(), "UTF-8")

            reqParam += "&" + URLEncoder.encode("TILECOL", "UTF-8") + "=" +
                    URLEncoder.encode(req.tileCol.toString(), "UTF-8")

            reqParam += "&" + URLEncoder.encode("FORMAT", "UTF-8") + "=" +
                    URLEncoder.encode(req.format, "UTF-8")

            val url = URL("$qgisUrl?$reqParam")

            with(url.openConnection() as HttpURLConnection){
                try{
                    requestMethod = "GET"

                    val bitmap = BitmapFactory.decodeStream(inputStream).asImageBitmap()
                    return@with GetTileResponse(bitmap, bitmap.toString(), responseCode)
                }catch (e: Exception){
                    throw e
                }

            }

        }

    }
}