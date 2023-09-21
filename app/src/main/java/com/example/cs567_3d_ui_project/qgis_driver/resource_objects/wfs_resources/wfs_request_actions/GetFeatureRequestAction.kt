package com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wfs_resources.wfs_request_actions

import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.ILayer
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.IOptionalBoundingBox
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.IOptionalSpatialReference
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.IOutputFormat
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.IRequest
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces.IService
import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wms_resources.BoundingBox

class GetFeatureRequestAction(
    override var layer: String,
    override var boundingBox: BoundingBox?,
    override var srs: String?,
    override var service: String = "WFS",
    override var request: String = "GetFeature",
    override var outputFormat: String = "application/json",
) :IService, ILayer, IOptionalBoundingBox, IRequest, IOptionalSpatialReference, IOutputFormat