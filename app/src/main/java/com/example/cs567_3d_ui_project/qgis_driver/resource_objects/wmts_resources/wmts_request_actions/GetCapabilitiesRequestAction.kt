package com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wmts_resources.wmts_request_actions

import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.IRequest
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.IService

class GetCapabilitiesRequestAction(
    override var request: String = "GetCapabilities",
    override var service: String = "WMTS",
) : IService, IRequest {
}