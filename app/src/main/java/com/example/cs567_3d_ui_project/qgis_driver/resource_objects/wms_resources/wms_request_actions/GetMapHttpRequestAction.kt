package com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wms_resources.wms_request_actions

import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.IBoundingBox
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.IDimensions
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.ILayers
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.IRequest
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.IService
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.ITiled
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.IVersion
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wms_resources.BoundingBox

class GetMapHttpRequestAction(
    override var width: Int = 400,
    override var height: Int = 200,
    override var layers: List<String>,
    override var boundingBox: BoundingBox,
    override var tiled: Boolean = true,
    override var request: String = "GetMap",
    override var service: String = "WMS",
    override var version: String = "1.3.0",
) : IService, IRequest, IVersion, ILayers, IBoundingBox, IDimensions, ITiled  {

}
