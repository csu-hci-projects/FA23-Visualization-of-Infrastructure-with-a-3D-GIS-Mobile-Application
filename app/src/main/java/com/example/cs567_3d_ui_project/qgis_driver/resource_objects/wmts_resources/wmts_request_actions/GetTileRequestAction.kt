package com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wmts_resources.wmts_request_actions

import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.IFormat
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.ILayer
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.IRequest
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.IService
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.ITileMatrix
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.ITileMatrixSet

class GetTileRequestAction(
    override var layer: String,
    override var tileMatrixSet: String,
    override var tileMatrix: Int,
    override var tileRow: Int,
    override var tileCol: Int,
    override var request: String = "GetTile",
    override var service: String = "WMTS",
    override var format: String = "image/png"
    ) : IService, IRequest, ILayer, IFormat, ITileMatrixSet, ITileMatrix {
}