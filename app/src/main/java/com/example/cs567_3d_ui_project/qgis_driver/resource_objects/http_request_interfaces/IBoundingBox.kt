package com.example.cs567_3d_ui_project.qgis_driver.resource_objects.http_request_interfaces

import com.example.cs567_3d_ui_project.qgis_driver.resource_objects.wms_resources.BoundingBox

interface IBoundingBox {
    var boundingBox: BoundingBox
}

interface IOptionalBoundingBox {
    var boundingBox: BoundingBox?
}