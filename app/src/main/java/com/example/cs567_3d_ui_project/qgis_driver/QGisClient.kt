package com.example.cs567_3d_ui_project.qgis_driver

class QGisClient(private val qgisUrl: String) {

    val wms: WMS by lazy{
        WMS(qgisUrl)
    }

    val wfs: WFS by lazy{
        WFS(qgisUrl)
    }

    val wmts: WMTS by lazy {
        WMTS(qgisUrl)
    }
}