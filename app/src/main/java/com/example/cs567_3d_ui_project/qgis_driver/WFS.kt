package com.example.cs567_3d_ui_project.qgis_driver

import android.util.Log
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources.GetCapabilitiesResponse
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources.GetFeatureResponse
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources.wfs_request_actions.GetFeatureRequestAction
import fr.arnaudguyon.xmltojsonlib.XmlToJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class WFS(private val qgisUrl: String) {

    private val wfs = "WFS"
    private val service = "SERVICE"
    private val request = "REQUEST"

    suspend fun getCapabilities(): GetCapabilitiesResponse {
        return withContext(Dispatchers.IO) {
            var reqParam =
                URLEncoder.encode(service, "UTF-8") + "=" + URLEncoder.encode(wfs, "UTF-8")
            reqParam += "&" + URLEncoder.encode(request, "UTF-8") +
                    "=" + URLEncoder.encode("GetCapabilities", "UTF-8")

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
        }
    }

    suspend fun getFeature(getFeatureRequestAction: GetFeatureRequestAction): GetFeatureResponse{
        return withContext(Dispatchers.IO) {

            var reqParam =
                URLEncoder.encode(service, "UTF-8") + "=" +
                        URLEncoder.encode(getFeatureRequestAction.service, "UTF-8")

            reqParam += "&" + URLEncoder.encode(request, "UTF-8") +
                    "=" + URLEncoder.encode(getFeatureRequestAction.request, "UTF-8")

            reqParam += "&" + URLEncoder.encode("OUTPUTFORMAT", "UTF-8") +
                    "=" + URLEncoder.encode(getFeatureRequestAction.outputFormat, "UTF-8")

            reqParam += "&" + URLEncoder.encode("TYPENAME", "UTF-8") +
                    "=" + URLEncoder.encode(getFeatureRequestAction.layer, "UTF-8")

            if(getFeatureRequestAction.boundingBox != null){
                reqParam += "&" + URLEncoder.encode("BBOX", "UTF-8") +
                        "=" + URLEncoder.encode(getFeatureRequestAction.boundingBox.toString(), "UTF-8")

                reqParam += "&" + URLEncoder.encode("SRSNAME", "UTF-8") +
                        "=" + URLEncoder.encode(getFeatureRequestAction.boundingBox!!.crs, "UTF-8")
            }

            val url = URL("$qgisUrl?$reqParam")
            try{
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

                        return@with GetFeatureResponse(response.toString(), responseCode)
                    }
                }
            }catch (e: Exception){
                Log.e("Error", e.message.toString())
                throw e
            }

        }
    }
}