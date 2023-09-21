package com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wmts_resources

import androidx.compose.ui.graphics.ImageBitmap
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.HttpResponse
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.HttpResponseContent

class GetTileResponse(val bitmap: ImageBitmap, content: String, responseCode: Int)
    : HttpResponse(content, responseCode) {

    override fun deserializeJsonObject(properties: String): HttpResponseContent {
        throw NotImplementedError("Not yet implemented")
    }
}